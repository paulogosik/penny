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
        Você é um assistente de finanças pessoais. Extraia do texto do usuário as informações de um gasto.
        %s

        Texto do usuário: "%s"
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
    public ClassifiedExpense classifyText(String text, ClassificationContext context) {
        String prompt = TEXT_PROMPT.formatted(instructions(context), text);
        return classify(List.of(Map.of("text", prompt)), context);
    }

    @Override
    public ClassifiedExpense classifyReceipt(byte[] image, String mimeType, ClassificationContext context) {
        String prompt = RECEIPT_PROMPT.formatted(instructions(context));
        List<Map<String, Object>> parts = List.of(
            Map.of("inlineData", Map.of("mimeType", mimeType, "data", Base64.getEncoder().encodeToString(image))),
            Map.of("text", prompt)
        );
        return classify(parts, context);
    }

    private ClassifiedExpense classify(List<Map<String, Object>> parts, ClassificationContext context) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new GeminiException("GEMINI_API_KEY não configurada no backend.");
        }

        Map<String, Object> body = Map.of(
            "contents", List.of(Map.of("parts", parts)),
            "generationConfig", Map.of(
                "responseMimeType", "application/json",
                "responseSchema", schema(context)
            )
        );

        return parse(callWithRetry(body), context);
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

    private String instructions(ClassificationContext context) {
        String cards = context.creditCards().isEmpty()
            ? "- creditCard: sempre null (o usuário não tem cartões cadastrados)"
            : "- creditCard: o cartão de crédito usado, escolhido entre: %s; null se nenhum cartão for mencionado"
                .formatted(String.join(", ", context.creditCards()));
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
            cards
        );
    }

    private Map<String, Object> schema(ClassificationContext context) {
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

    private ClassifiedExpense parse(JsonNode response, ClassificationContext context) {
        String json = response == null ? "" : response
            .path("candidates").path(0)
            .path("content").path("parts").path(0)
            .path("text").asText("");
        if (json.isBlank()) {
            throw new GeminiException("A IA não retornou uma classificação.");
        }

        JsonNode node;
        try {
            node = objectMapper.readTree(json);
        } catch (JsonProcessingException e) {
            throw new GeminiException("A IA retornou uma resposta em formato inesperado.", e);
        }

        BigDecimal amount = node.path("amount").isNumber()
            ? node.path("amount").decimalValue().setScale(2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;
        String creditCard = node.path("creditCard").isTextual() && !node.path("creditCard").asText().isBlank()
            ? node.path("creditCard").asText().strip()
            : null;

        return new ClassifiedExpense(
            node.path("category").asText(context.fallbackCategory()).strip(),
            amount,
            parseDate(node.path("date").asText(""), context.today()),
            node.path("description").asText("").strip(),
            Math.max(1, node.path("installments").asInt(1)),
            creditCard,
            response.path("modelVersion").asText(model)
        );
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
