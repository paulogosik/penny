package com.penny.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.penny.dto.ExpenseResponse;
import com.penny.exception.GeminiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
public class GeminiService {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String model;
    private final String apiKey;

    // TODO — persistencia: no futuro estas categorias virao do banco (gerenciadas pelo usuario no painel).
    private static final List<String> CATEGORIAS = List.of(
        "Mercado", "Restaurante", "Transporte", "Moradia", "Saude",
        "Lazer", "Educacao", "Compras", "Servicos", "Outros"
    );

    public GeminiService(
        RestClient.Builder restClientBuilder,
        ObjectMapper objectMapper,
        @Value("${gemini.api.base-url}") String baseUrl,
        @Value("${gemini.model}") String model,
        @Value("${gemini.api.key}") String apiKey
    ) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
        this.objectMapper = objectMapper;
        this.model = model;
        this.apiKey = apiKey;
    }

    public ExpenseResponse classify(String text) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new GeminiException("GEMINI_API_KEY nao configurada no ambiente do backend.");
        }

        Map<String, Object> body = buildRequestBody(text);

        JsonNode response;
        try {
            response = restClient.post()
                .uri("/models/{model}:generateContent?key={key}", model, apiKey)
                .body(body)
                .retrieve()
                .body(JsonNode.class);
        } catch (Exception e) {
            throw new GeminiException("Falha ao chamar a API do Gemini: " + e.getMessage(), e);
        }

        return parseResponse(response, text);
    }

    private Map<String, Object> buildRequestBody(String text) {
        String prompt = """
            Voce e um assistente de financas pessoais. Extraia do texto do usuario as informacoes de um gasto.
            - category: escolha exatamente uma destas categorias: %s
            - amount: o valor gasto em reais, apenas o numero (ex.: 45.90)
            - date: a data do gasto no formato YYYY-MM-DD; se o texto nao mencionar data, use a data de hoje (%s)
            - description: uma descricao curta do gasto

            Texto do usuario: "%s"
            """.formatted(String.join(", ", CATEGORIAS), LocalDate.now(), text);

        Map<String, Object> schema = Map.of(
            "type", "OBJECT",
            "properties", Map.of(
                "category", Map.of("type", "STRING"),
                "amount", Map.of("type", "NUMBER"),
                "date", Map.of("type", "STRING"),
                "description", Map.of("type", "STRING")
            ),
            "required", List.of("category", "amount", "date", "description")
        );

        return Map.of(
            "contents", List.of(Map.of(
                "parts", List.of(Map.of("text", prompt))
            )),
            "generationConfig", Map.of(
                "responseMimeType", "application/json",
                "responseSchema", schema
            )
        );
    }

    private ExpenseResponse parseResponse(JsonNode response, String originalText) {
        try {
            String json = response
                .path("candidates").path(0)
                .path("content").path("parts").path(0)
                .path("text").asText();

            if (json == null || json.isBlank()) {
                throw new GeminiException("Resposta do Gemini sem conteudo classificavel.");
            }

            JsonNode classified = objectMapper.readTree(json);
            return new ExpenseResponse(
                classified.path("category").asText("Outros"),
                classified.path("amount").asDouble(0.0),
                classified.path("date").asText(LocalDate.now().toString()),
                classified.path("description").asText(originalText)
            );
        } catch (GeminiException e) {
            throw e;
        } catch (Exception e) {
            throw new GeminiException("Falha ao interpretar a resposta do Gemini: " + e.getMessage(), e);
        }
    }
}
