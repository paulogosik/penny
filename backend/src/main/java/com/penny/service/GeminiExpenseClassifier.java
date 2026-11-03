package com.penny.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.penny.exception.GeminiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.http.HttpClient;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class GeminiExpenseClassifier implements ExpenseClassifier {

    private static final Logger log = LoggerFactory.getLogger(GeminiExpenseClassifier.class);

    private static final String TEXT_PROMPT = """
        Você é um assistente de finanças pessoais. A mensagem do usuário pode descrever um gasto, cadastrar um cartão de crédito ou ser outra coisa.
        - kind: "expense" se a mensagem descreve um gasto; "credit_card" se o usuário está cadastrando ou atualizando um cartão de crédito (ex.: "meu cartão Nubank fecha dia 3 e vence dia 10"); "other" para qualquer outra coisa

        Se for um gasto, preencha:
        %s
        Se for um cartão de crédito, preencha:
        - cardName: o nome do cartão (banco ou apelido)
        - closingDay: o dia do mês em que a fatura fecha, de 1 a 31; null se não foi informado
        - dueDay: o dia do mês em que a fatura vence, de 1 a 31; null se não foi informado
        - creditLimit: o limite do cartão em reais, apenas o número; null se não foi informado

        Mensagem do usuário: "%s"
        """;

    private static final String RECEIPT_PROMPT = """
        Você é um assistente de finanças pessoais. A imagem é a foto de um recibo, nota fiscal ou comprovante de pagamento.
        Extraia as informações do gasto, usando o valor total pago e a data impressa no comprovante.
        %s
        """;

    private static final Set<Integer> RETRYABLE_STATUSES = Set.of(429, 500, 502, 503, 504);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String model;
    private final String apiKey;
    private final int maxAttempts;
    private final Duration retryBackoff;

    public GeminiExpenseClassifier(
        RestClient.Builder restClientBuilder,
        ObjectMapper objectMapper,
        @Value("${gemini.api.base-url}") String baseUrl,
        @Value("${gemini.model}") String model,
        @Value("${gemini.api.key}") String apiKey,
        @Value("${gemini.retry.max-attempts:3}") int maxAttempts,
        @Value("${gemini.retry.backoff:1s}") Duration retryBackoff
    ) {
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(60));
        this.restClient = restClientBuilder.baseUrl(baseUrl).requestFactory(requestFactory).build();
        this.objectMapper = objectMapper;
        this.model = model;
        this.apiKey = apiKey;
        this.maxAttempts = Math.max(1, maxAttempts);
        this.retryBackoff = retryBackoff;
    }

    @Override
    public ClassifiedMessage classifyText(String text, ClassificationContext context) {
        String prompt = TEXT_PROMPT.formatted(expenseInstructions(context, textCardInstruction(context)), text);
        JsonNode response = generate(List.of(Map.of("text", prompt)), textSchema(context));
        JsonNode node = output(response);
        return switch (node.path("kind").asText("")) {
            case "expense" -> new ClassifiedMessage.ExpenseMessage(expense(node, response, context));
            case "credit_card" -> new ClassifiedMessage.CreditCardMessage(
                textOrNull(node.path("cardName")),
                dayOrNull(node.path("closingDay")),
                dayOrNull(node.path("dueDay")),
                node.path("creditLimit").isNumber() ? node.path("creditLimit").decimalValue().setScale(2, RoundingMode.HALF_UP) : null
            );
            default -> new ClassifiedMessage.Unrecognized();
        };
    }

    @Override
    public ClassifiedExpense classifyReceipt(byte[] image, String mimeType, ClassificationContext context) {
        String prompt = RECEIPT_PROMPT.formatted(expenseInstructions(context, receiptCardInstruction(context)));
        List<Map<String, Object>> parts = List.of(
            Map.of("inlineData", Map.of("mimeType", mimeType, "data", Base64.getEncoder().encodeToString(image))),
            Map.of("text", prompt)
        );
        JsonNode response = generate(parts, receiptSchema(context));
        return expense(output(response), response, context);
    }

    private JsonNode generate(List<Map<String, Object>> parts, Map<String, Object> schema) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new GeminiException("GEMINI_API_KEY não configurada no backend.");
        }

        Map<String, Object> body = Map.of(
            "contents", List.of(Map.of("parts", parts)),
            "generationConfig", Map.of(
                "responseMimeType", "application/json",
                "responseSchema", schema
            )
        );

        return callWithRetry(body);
    }

    private JsonNode callWithRetry(Map<String, Object> body) {
        for (int attempt = 1; ; attempt++) {
            try {
                return restClient.post()
                    .uri("/models/{model}:generateContent", model)
                    .header("x-goog-api-key", apiKey)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
            } catch (RestClientResponseException e) {
                int status = e.getStatusCode().value();
                log.warn("Gemini respondeu {} (tentativa {}/{}): {}", status, attempt, maxAttempts, abbreviate(e.getResponseBodyAsString()));
                if (!RETRYABLE_STATUSES.contains(status) || attempt >= maxAttempts) {
                    throw new GeminiException("A IA está indisponível no momento (HTTP " + status + "). Tente novamente em instantes.", e);
                }
            } catch (RestClientException e) {
                log.warn("Falha ao chamar o Gemini (tentativa {}/{}): {}", attempt, maxAttempts, e.getMessage());
                if (attempt >= maxAttempts) {
                    throw new GeminiException("Não foi possível falar com a IA. Tente novamente em instantes.", e);
                }
            }
            pause(retryBackoff.multipliedBy(attempt));
        }
    }

    private void pause(Duration duration) {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new GeminiException("A chamada à IA foi interrompida.", e);
        }
    }

    private String expenseInstructions(ClassificationContext context, String cardInstruction) {
        return """
            - category: escolha exatamente uma destas categorias: %s; se nenhuma servir, use "%s"
            - amount: o valor total da compra em reais, apenas o número (ex.: 45.90)
            - date: a data do gasto no formato YYYY-MM-DD; hoje é %s; resolva datas relativas como "ontem" a partir de hoje; se não houver data, use hoje
            - description: uma descrição curta do gasto
            - installments: o número de parcelas se a compra foi parcelada (ex.: "em 6x" = 6); caso contrário, 1
            %s
            """.formatted(
            String.join(", ", context.categories()),
            context.fallbackCategory(),
            context.today(),
            cardInstruction
        );
    }

    private String textCardInstruction(ClassificationContext context) {
        String registered = context.creditCards().isEmpty()
            ? "o usuário ainda não tem cartões cadastrados"
            : "cartões cadastrados: " + String.join(", ", context.creditCards());
        return "- creditCard: o nome do cartão de crédito citado pelo usuário, mesmo que não esteja cadastrado (%s); "
            .formatted(registered)
            + "null se nenhum cartão for citado pelo nome (\"no crédito\" ou \"no cartão\" sem nome não contam)";
    }

    private String receiptCardInstruction(ClassificationContext context) {
        return context.creditCards().isEmpty()
            ? "- creditCard: sempre null (o usuário não tem cartões cadastrados)"
            : "- creditCard: o cartão de crédito usado, escolhido entre: %s; null se nenhum cartão for mencionado"
                .formatted(String.join(", ", context.creditCards()));
    }

    private Map<String, Object> textSchema(ClassificationContext context) {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("kind", Map.of("type", "STRING", "format", "enum", "enum", List.of("expense", "credit_card", "other")));
        properties.put("category", Map.of("type", "STRING", "format", "enum", "enum", context.categories(), "nullable", true));
        properties.put("amount", Map.of("type", "NUMBER", "nullable", true));
        properties.put("date", Map.of("type", "STRING", "nullable", true));
        properties.put("description", Map.of("type", "STRING", "nullable", true));
        properties.put("installments", Map.of("type", "INTEGER", "nullable", true));
        properties.put("creditCard", Map.of("type", "STRING", "nullable", true));
        properties.put("cardName", Map.of("type", "STRING", "nullable", true));
        properties.put("closingDay", Map.of("type", "INTEGER", "nullable", true));
        properties.put("dueDay", Map.of("type", "INTEGER", "nullable", true));
        properties.put("creditLimit", Map.of("type", "NUMBER", "nullable", true));

        return Map.of("type", "OBJECT", "properties", properties, "required", List.of("kind"));
    }

    private Map<String, Object> receiptSchema(ClassificationContext context) {
        Map<String, Object> creditCard = new LinkedHashMap<>();
        creditCard.put("type", "STRING");
        creditCard.put("nullable", true);
        if (!context.creditCards().isEmpty()) {
            creditCard.put("format", "enum");
            creditCard.put("enum", context.creditCards());
        }

        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("category", Map.of("type", "STRING", "format", "enum", "enum", context.categories()));
        properties.put("amount", Map.of("type", "NUMBER"));
        properties.put("date", Map.of("type", "STRING"));
        properties.put("description", Map.of("type", "STRING"));
        properties.put("installments", Map.of("type", "INTEGER"));
        properties.put("creditCard", creditCard);

        return Map.of(
            "type", "OBJECT",
            "properties", properties,
            "required", List.of("category", "amount", "date", "description", "installments")
        );
    }

    private JsonNode output(JsonNode response) {
        String json = response == null ? "" : response
            .path("candidates").path(0)
            .path("content").path("parts").path(0)
            .path("text").asText("");
        if (json.isBlank()) {
            throw new GeminiException("A IA não retornou uma classificação.");
        }
        try {
            return objectMapper.readTree(json);
        } catch (JsonProcessingException e) {
            throw new GeminiException("A IA retornou uma resposta em formato inesperado.", e);
        }
    }

    private ClassifiedExpense expense(JsonNode node, JsonNode response, ClassificationContext context) {
        BigDecimal amount = node.path("amount").isNumber()
            ? node.path("amount").decimalValue().setScale(2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;

        return new ClassifiedExpense(
            node.path("category").asText(context.fallbackCategory()).strip(),
            amount,
            parseDate(node.path("date").asText(""), context.today()),
            node.path("description").asText("").strip(),
            Math.max(1, node.path("installments").asInt(1)),
            textOrNull(node.path("creditCard")),
            response.path("modelVersion").asText(model)
        );
    }

    private static String textOrNull(JsonNode node) {
        return node.isTextual() && !node.asText().isBlank() ? node.asText().strip() : null;
    }

    private static Integer dayOrNull(JsonNode node) {
        return node.isIntegralNumber() && node.canConvertToInt() ? node.intValue() : null;
    }

    private LocalDate parseDate(String value, LocalDate fallback) {
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            return fallback;
        }
    }

    private String abbreviate(String value) {
        return value.length() <= 500 ? value : value.substring(0, 500) + "...";
    }
}
