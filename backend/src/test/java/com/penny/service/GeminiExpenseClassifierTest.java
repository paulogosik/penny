package com.penny.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.penny.exception.GeminiException;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GeminiExpenseClassifierTest {

    private record Reply(int status, String body) {
    }

    private record Received(String apiKey, JsonNode body) {
    }

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Deque<Reply> replies = new ArrayDeque<>();
    private final List<Received> received = new ArrayList<>();
    private HttpServer server;
    private GeminiExpenseClassifier classifier;

    private final ClassificationContext context = new ClassificationContext(
        List.of("Mercado", "Transporte", "Outros"),
        "Outros",
        List.of("Nubank"),
        LocalDate.of(2026, 10, 13)
    );

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/", exchange -> {
            received.add(new Received(
                exchange.getRequestHeaders().getFirst("x-goog-api-key"),
                objectMapper.readTree(exchange.getRequestBody())
            ));
            Reply reply = replies.isEmpty() ? new Reply(500, "{}") : replies.poll();
            byte[] bytes = reply.body().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(reply.status(), bytes.length);
            try (OutputStream output = exchange.getResponseBody()) {
                output.write(bytes);
            }
        });
        server.start();
        String baseUrl = "http://localhost:" + server.getAddress().getPort() + "/v1beta";
        classifier = new GeminiExpenseClassifier(
            RestClient.builder(), objectMapper, baseUrl, "test-model", "secret-key", 3, Duration.ofMillis(10)
        );
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void retriesTransientErrorsAndParsesTheClassification() throws Exception {
        replies.add(new Reply(503, "{\"error\":{\"code\":503,\"message\":\"high demand\"}}"));
        replies.add(new Reply(200, candidate(
            "{\"category\":\"Transporte\",\"amount\":23.5,\"date\":\"2026-10-12\",\"description\":\"Uber\",\"installments\":1,\"creditCard\":\"Nubank\"}"
        )));

        ClassifiedExpense result = classifier.classifyText("uber ontem 23,50 no nubank", context);

        assertThat(result.category()).isEqualTo("Transporte");
        assertThat(result.amount()).isEqualByComparingTo("23.50");
        assertThat(result.amount().scale()).isEqualTo(2);
        assertThat(result.date()).isEqualTo(LocalDate.of(2026, 10, 12));
        assertThat(result.creditCard()).isEqualTo("Nubank");
        assertThat(result.installments()).isEqualTo(1);
        assertThat(result.model()).isEqualTo("gemini-test-version");

        assertThat(received).hasSize(2);
        assertThat(received.getFirst().apiKey()).isEqualTo("secret-key");
        JsonNode schema = received.getFirst().body().path("generationConfig").path("responseSchema");
        assertThat(schema.path("properties").path("category").path("enum").toString()).contains("Mercado", "Outros");
        assertThat(schema.path("properties").path("creditCard").path("enum").toString()).contains("Nubank");
        assertThat(received.getFirst().body().path("contents").get(0).path("parts").get(0).path("text").asText())
            .contains("hoje é 2026-10-13")
            .contains("uber ontem 23,50 no nubank");
    }

    @Test
    void doesNotRetryClientErrors() {
        replies.add(new Reply(400, "{\"error\":{\"code\":400}}"));

        assertThatThrownBy(() -> classifier.classifyText("x", context)).isInstanceOf(GeminiException.class);
        assertThat(received).hasSize(1);
    }

    @Test
    void givesUpAfterMaxAttempts() {
        replies.add(new Reply(503, "{}"));
        replies.add(new Reply(503, "{}"));
        replies.add(new Reply(503, "{}"));

        assertThatThrownBy(() -> classifier.classifyText("x", context)).isInstanceOf(GeminiException.class);
        assertThat(received).hasSize(3);
    }

    @Test
    void receiptIsSentAsInlineImageAndMissingFieldsGetDefaults() throws Exception {
        replies.add(new Reply(200, candidate("{\"category\":\"Mercado\",\"amount\":33.4,\"date\":\"sem data\",\"description\":\"Padaria\"}")));

        ClassifiedExpense result = classifier.classifyReceipt(new byte[]{1, 2, 3}, "image/png", context);

        assertThat(result.date()).isEqualTo(context.today());
        assertThat(result.installments()).isEqualTo(1);
        assertThat(result.creditCard()).isNull();
        JsonNode inline = received.getFirst().body().path("contents").get(0).path("parts").get(0).path("inlineData");
        assertThat(inline.path("mimeType").asText()).isEqualTo("image/png");
        assertThat(inline.path("data").asText()).isEqualTo("AQID");
    }

    @Test
    void malformedModelOutputIsReportedAsAiFailure() throws Exception {
        replies.add(new Reply(200, candidate("isso não é json")));

        assertThatThrownBy(() -> classifier.classifyText("x", context)).isInstanceOf(GeminiException.class);
    }

    private String candidate(String json) throws Exception {
        String text = objectMapper.writeValueAsString(json);
        return "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":" + text + "}]}}],\"modelVersion\":\"gemini-test-version\"}";
    }
}
