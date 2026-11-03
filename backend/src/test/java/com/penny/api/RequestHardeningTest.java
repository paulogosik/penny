package com.penny.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.penny.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RequestHardeningTest extends IntegrationTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void telegramUserIdHeaderMustBeAPositiveNumber() throws Exception {
        for (String value : List.of("0", "-5", "12abc", "1.5", "99999999999999999999")) {
            mvc.perform(get("/api/expenses").header(HEADER, value))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_HEADER"));
            mvc.perform(put("/api/me").header(HEADER, value))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_HEADER"));
        }
        mvc.perform(get("/api/expenses").header(HEADER, "   "))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("MISSING_HEADER"));
    }

    @Test
    void malformedRequestsGetTheStandardErrorFormat() throws Exception {
        long user = registerUser();

        postAs(user, "/api/expenses", "{\"amount\": ")
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        getAs(user, "/api/expenses/nao-e-um-uuid")
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        getAs(user, "/api/dashboard?month=2026-13")
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        getAs(user, "/api/rota-que-nao-existe")
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("NOT_FOUND"));
        mvc.perform(patch("/api/expenses").header(HEADER, user))
            .andExpect(status().isMethodNotAllowed())
            .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));
        mvc.perform(post("/api/expenses/text").header(HEADER, user).contentType(MediaType.TEXT_PLAIN).content("gastei 10"))
            .andExpect(status().isUnsupportedMediaType())
            .andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"));
    }

    @Test
    void unexpectedErrorsDoNotLeakInternals() throws Exception {
        long user = registerUser();
        String userId = body(getAs(user, "/api/me")).path("id").asText();
        jdbc.update("delete from expense_categories where user_id = ? and is_fallback", UUID.fromString(userId));
        classifier.willReturn("Categoria Inexistente", "10.00", LocalDate.of(2026, 10, 13), "Teste");

        String response = postAs(user, "/api/expenses/text", "{\"text\":\"gastei 10\"}")
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
            .andReturn().getResponse().getContentAsString();

        assertThat(response).doesNotContain(userId, "Exception", "com.penny", "trace");
        assertThat(objectMapper.readTree(response).fieldNames()).toIterable().containsExactly("code", "message");
    }

    @Test
    void clientCannotSetServerControlledFields() throws Exception {
        long user = registerUser();
        long other = registerUser();
        String forgedId = "00000000-0000-0000-0000-000000000001";
        String json = ("{\"categoryId\":\"%s\",\"amount\":10,\"description\":\"Café\",\"occurredOn\":\"2026-10-13\","
            + "\"id\":\"%s\",\"userId\":\"%s\",\"source\":\"recurring\",\"createdAt\":\"2000-01-01T00:00:00Z\","
            + "\"recurringExpenseId\":\"%s\"}").formatted(categoryId(user, "Mercado"), forgedId, forgedId, forgedId);

        JsonNode expense = body(postAs(user, "/api/expenses", json).andExpect(status().isCreated()));
        assertThat(expense.path("id").asText()).isNotEqualTo(forgedId);
        assertThat(expense.path("source").asText()).isEqualTo("panel");
        assertThat(expense.path("recurringExpenseId").isNull()).isTrue();
        assertThat(expense.path("createdAt").asText()).doesNotStartWith("2000");
        getAs(other, "/api/expenses/" + expense.path("id").asText()).andExpect(status().isNotFound());

        String profile = "{\"name\":\"Nova\",\"telegramUserId\":%d,\"phoneNumber\":\"+5511900000000\",\"id\":\"%s\"}"
            .formatted(other, forgedId);
        putAs(user, "/api/me", profile)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.telegramUserId").value(user))
            .andExpect(jsonPath("$.phoneNumber").value(uniquePhoneNumber(user)))
            .andExpect(jsonPath("$.name").value("Nova"));
        getAs(other, "/api/me").andExpect(jsonPath("$.name").value("Pessoa Teste"));
    }

    @Test
    void pageParametersAreClampedToSafeLimits() throws Exception {
        long user = registerUser();

        getAs(user, "/api/expenses?size=100000").andExpect(jsonPath("$.size").value(100));
        getAs(user, "/api/expenses?size=0").andExpect(jsonPath("$.size").value(1));
        getAs(user, "/api/expenses?page=-3").andExpect(jsonPath("$.page").value(0));
    }

    @Test
    void oversizedOrOutOfRangeInputsAreRejectedBeforeReachingTheDatabase() throws Exception {
        long user = registerUser();
        String category = categoryId(user, "Mercado");
        String card = createCard(user, "Nubank", 3, 10);

        postAs(user, "/api/expenses/text", "{\"text\":\"%s\"}".formatted("a".repeat(1001)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fields.text").exists());
        putAs(user, "/api/me", "{\"name\":\"%s\"}".formatted("n".repeat(101)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fields.name").exists());
        putAs(user, "/api/me/phone", "{\"phoneNumber\":\"%s\"}".formatted("1".repeat(33)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fields.phoneNumber").exists());
        postAs(user, "/api/expenses", "{\"categoryId\":\"%s\",\"amount\":12345678901,\"description\":\"%s\",\"occurredOn\":\"2026-10-13\"}"
                .formatted(category, "d".repeat(201)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fields.amount").exists())
            .andExpect(jsonPath("$.fields.description").exists());
        postAs(user, "/api/expenses", "{\"categoryId\":\"%s\",\"amount\":10.001,\"description\":\"x\",\"occurredOn\":\"2026-10-13\"}"
                .formatted(category))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fields.amount").exists());
        postAs(user, "/api/expense-categories", "{\"name\":\"Pets\",\"color\":\"javascript:alert(1)\"}")
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fields.color").exists());
        postAs(user, "/api/credit-cards", "{\"name\":\"Cartão\",\"closingDay\":32,\"dueDay\":0}")
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fields.closingDay").exists())
            .andExpect(jsonPath("$.fields.dueDay").exists());
        postAs(user, "/api/installment-plans", ("{\"creditCardId\":\"%s\",\"categoryId\":\"%s\",\"description\":\"TV\","
                + "\"totalAmount\":1000,\"installmentsCount\":100,\"purchasedOn\":\"2026-10-13\"}").formatted(card, category))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fields.installmentsCount").exists());

        assertThat(classifier.lastContext()).isNull();
        getAs(user, "/api/expenses").andExpect(jsonPath("$.totalElements").value(0));
        getAs(user, "/api/installment-plans").andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void injectionLikeTextIsStoredAndReturnedLiterally() throws Exception {
        long user = registerUser();
        String hostile = "'; drop table users; -- <script>alert('x')</script>";
        String json = "{\"categoryId\":\"%s\",\"amount\":10,\"description\":%s,\"occurredOn\":\"2026-10-13\"}"
            .formatted(categoryId(user, "Mercado"), objectMapper.writeValueAsString(hostile));

        postAs(user, "/api/expenses", json)
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.description").value(hostile));
        postAs(user, "/api/expense-categories", "{\"name\":%s}".formatted(objectMapper.writeValueAsString("Mercado' OR '1'='1")))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("Mercado' OR '1'='1"));

        getAs(user, "/api/me").andExpect(status().isOk());
        getAs(user, "/api/expenses").andExpect(jsonPath("$.content[0].description").value(hostile));
    }

    @Test
    void receiptMustBeASupportedImageWithinTheSizeLimit() throws Exception {
        long user = registerUser();
        String tiny = Base64.getEncoder().encodeToString(new byte[]{1, 2, 3});

        for (String mimeType : List.of("image/svg+xml", "text/html", "image/jpeg; charset=x", "IMAGE/JPEG")) {
            postAs(user, "/api/expenses/photo", photoJson(tiny, mimeType))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.mimeType").exists());
        }
        String oversized = Base64.getEncoder().encodeToString(new byte[10 * 1024 * 1024 + 1]);
        postAs(user, "/api/expenses/photo", photoJson(oversized, "image/jpeg"))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("INVALID_IMAGE"));

        assertThat(classifier.lastMimeType()).isNull();
        getAs(user, "/api/expenses").andExpect(jsonPath("$.totalElements").value(0));
    }

    private static String photoJson(String imageBase64, String mimeType) {
        return "{\"imageBase64\":\"%s\",\"mimeType\":\"%s\"}".formatted(imageBase64, mimeType);
    }
}
