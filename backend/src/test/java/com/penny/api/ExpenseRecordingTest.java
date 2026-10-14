package com.penny.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.penny.support.IntegrationTest;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ExpenseRecordingTest extends IntegrationTest {

    @Test
    void textExpenseIsClassifiedAndSaved() throws Exception {
        long user = registerUser();
        classifier.willReturn("Restaurante", "38.00", LocalDate.of(2026, 10, 13), "Almoço");

        postAs(user, "/api/expenses/text", "{\"text\":\"almocei 38 reais hoje\"}")
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.categoryName").value("Restaurante"))
            .andExpect(jsonPath("$.amount").value(38.00))
            .andExpect(jsonPath("$.occurredOn").value("2026-10-13"))
            .andExpect(jsonPath("$.source").value("telegram_text"))
            .andExpect(jsonPath("$.creditCardId").isEmpty())
            .andExpect(jsonPath("$.installment").isEmpty());

        getAs(user, "/api/expenses")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].description").value("Almoço"));
    }

    @Test
    void todayIsComputedInTheUserTimezone() throws Exception {
        long user = registerUser();
        clock.set(Instant.parse("2026-10-14T01:30:00Z"));

        postAs(user, "/api/expenses/text", "{\"text\":\"gastei 10 hoje\"}").andExpect(status().isCreated());

        assertThat(classifier.lastContext().today()).isEqualTo(LocalDate.of(2026, 10, 13));
        assertThat(classifier.lastContext().categories()).contains("Saúde", "Outros");
        assertThat(classifier.lastContext().fallbackCategory()).isEqualTo("Outros");
    }

    @Test
    void unknownCategoryFallsBackToOutros() throws Exception {
        long user = registerUser();
        classifier.willReturn("Pets", "50.00", LocalDate.of(2026, 10, 13), "Ração");

        postAs(user, "/api/expenses/text", "{\"text\":\"ração 50\"}")
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.categoryName").value("Outros"));
    }

    @Test
    void categoryMatchingIgnoresCaseAndAccents() throws Exception {
        long user = registerUser();
        classifier.willReturn("saude", "80.00", LocalDate.of(2026, 10, 13), "Farmácia");

        postAs(user, "/api/expenses/text", "{\"text\":\"farmácia 80\"}")
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.categoryName").value("Saúde"));
    }

    @Test
    void cardExpenseGetsBillingMonth() throws Exception {
        long user = registerUser();
        createCard(user, "Nubank", 3, 10);
        classifier.willReturn("Compras", "120.00", LocalDate.of(2026, 10, 13), "Fone", 1, "nubank");

        postAs(user, "/api/expenses/text", "{\"text\":\"fone 120 no nubank\"}")
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.creditCardName").value("Nubank"))
            .andExpect(jsonPath("$.billingMonth").value("2026-11-01"));

        assertThat(classifier.lastContext().creditCards()).containsExactly("Nubank");
    }

    @Test
    void installmentPurchaseCreatesPlanAndOneExpensePerInstallment() throws Exception {
        long user = registerUser();
        createCard(user, "Nubank", 3, 10);
        classifier.willReturn("Compras", "100.00", LocalDate.of(2026, 10, 13), "Tênis", 3, "Nubank");

        postAs(user, "/api/expenses/text", "{\"text\":\"tênis 100 em 3x no nubank\"}")
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.amount").value(33.33))
            .andExpect(jsonPath("$.installment.number").value(1))
            .andExpect(jsonPath("$.installment.count").value(3))
            .andExpect(jsonPath("$.installment.totalAmount").value(100.00))
            .andExpect(jsonPath("$.billingMonth").value("2026-11-01"));

        JsonNode plans = body(getAs(user, "/api/installment-plans").andExpect(status().isOk()));
        assertThat(plans).hasSize(1);
        JsonNode installments = plans.get(0).path("installments");
        assertThat(installments).hasSize(3);
        assertThat(installments.get(0).path("amount").decimalValue()).isEqualByComparingTo("33.33");
        assertThat(installments.get(2).path("amount").decimalValue()).isEqualByComparingTo("33.34");
        assertThat(installments.get(1).path("billingMonth").asText()).isEqualTo("2026-12-01");
        assertThat(installments.get(2).path("billingMonth").asText()).isEqualTo("2027-01-01");
        assertThat(installments.get(2).path("occurredOn").asText()).isEqualTo("2026-12-13");
    }

    @Test
    void installmentsWithoutCreditCardAreRejected() throws Exception {
        long user = registerUser();
        classifier.willReturn("Compras", "600.00", LocalDate.of(2026, 10, 13), "TV", 6, null);

        postAs(user, "/api/expenses/text", "{\"text\":\"tv 600 em 6x\"}")
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("INSTALLMENTS_REQUIRE_CREDIT_CARD"));

        getAs(user, "/api/expenses").andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void messageWithoutAmountIsRejected() throws Exception {
        long user = registerUser();
        classifier.willReturn("Outros", "0", LocalDate.of(2026, 10, 13), "Oi");

        postAs(user, "/api/expenses/text", "{\"text\":\"oi\"}")
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("AMOUNT_NOT_FOUND"));
    }

    @Test
    void installmentsAreManagedThroughTheirPlan() throws Exception {
        long user = registerUser();
        createCard(user, "Itaú", 25, 5);
        classifier.willReturn("Compras", "300.00", LocalDate.of(2026, 10, 13), "Cadeira", 2, "itau");
        JsonNode first = body(postAs(user, "/api/expenses/text", "{\"text\":\"cadeira 300 em 2x no itau\"}").andExpect(status().isCreated()));
        String expenseId = first.path("id").asText();
        String planId = first.path("installment").path("planId").asText();

        deleteAs(user, "/api/expenses/" + expenseId)
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("INSTALLMENT_MANAGED_BY_PLAN"));

        deleteAs(user, "/api/installment-plans/" + planId).andExpect(status().isNoContent());
        getAs(user, "/api/expenses").andExpect(jsonPath("$.totalElements").value(0));
        getAs(user, "/api/installment-plans/" + planId).andExpect(status().isNotFound());
    }

    @Test
    void receiptPhotoIsClassifiedAsPhotoExpense() throws Exception {
        long user = registerUser();
        classifier.willReturn("Mercado", "245.70", LocalDate.of(2026, 10, 12), "Supermercado");
        String image = Base64.getEncoder().encodeToString(new byte[]{1, 2, 3, 4});

        postAs(user, "/api/expenses/photo", "{\"imageBase64\":\"%s\",\"mimeType\":\"image/jpeg\"}".formatted(image))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.source").value("telegram_photo"))
            .andExpect(jsonPath("$.categoryName").value("Mercado"));
        assertThat(classifier.lastMimeType()).isEqualTo("image/jpeg");

        postAs(user, "/api/expenses/photo", "{\"imageBase64\":\"%%%%\",\"mimeType\":\"image/jpeg\"}")
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("INVALID_IMAGE"));
        postAs(user, "/api/expenses/photo", "{\"imageBase64\":\"%s\",\"mimeType\":\"application/pdf\"}".formatted(image))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fields.mimeType").exists());
    }

    @Test
    void panelCanCreateUpdateFilterAndDeleteExpenses() throws Exception {
        long user = registerUser();
        String mercado = categoryId(user, "Mercado");
        String lazer = categoryId(user, "Lazer");
        String card = createCard(user, "Inter", 10, 20);

        String createJson = "{\"categoryId\":\"%s\",\"amount\":99.90,\"description\":\"Feira\",\"occurredOn\":\"2026-10-11\",\"creditCardId\":\"%s\"}"
            .formatted(mercado, card);
        JsonNode created = body(postAs(user, "/api/expenses", createJson)
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.source").value("panel"))
            .andExpect(jsonPath("$.billingMonth").value("2026-11-01")));
        String id = created.path("id").asText();

        String updateJson = "{\"categoryId\":\"%s\",\"amount\":120.00,\"description\":\"Cinema\",\"occurredOn\":\"2026-10-05\"}".formatted(lazer);
        putAs(user, "/api/expenses/" + id, updateJson)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.categoryName").value("Lazer"))
            .andExpect(jsonPath("$.creditCardId").isEmpty())
            .andExpect(jsonPath("$.billingMonth").isEmpty());

        getAs(user, "/api/expenses?categoryId=" + lazer + "&from=2026-10-01&to=2026-10-31")
            .andExpect(jsonPath("$.totalElements").value(1));
        getAs(user, "/api/expenses?categoryId=" + mercado).andExpect(jsonPath("$.totalElements").value(0));

        deleteAs(user, "/api/expenses/" + id).andExpect(status().isNoContent());
        getAs(user, "/api/expenses/" + id).andExpect(status().isNotFound());
    }

    @Test
    void invalidPayloadReturnsFieldErrors() throws Exception {
        long user = registerUser();
        String json = "{\"categoryId\":\"%s\",\"amount\":-5,\"description\":\"\",\"occurredOn\":\"2026-10-11\"}"
            .formatted(categoryId(user, "Mercado"));

        postAs(user, "/api/expenses", json)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
            .andExpect(jsonPath("$.fields.amount").exists())
            .andExpect(jsonPath("$.fields.description").exists());
    }

    @Test
    void usersCannotSeeEachOthersData() throws Exception {
        long owner = registerUser();
        long intruder = registerUser();
        classifier.willReturn("Mercado", "10.00", LocalDate.of(2026, 10, 13), "Pão");
        String id = body(postAs(owner, "/api/expenses/text", "{\"text\":\"pão 10\"}")).path("id").asText();

        getAs(intruder, "/api/expenses/" + id).andExpect(status().isNotFound());
        deleteAs(intruder, "/api/expenses/" + id).andExpect(status().isNotFound());
        getAs(intruder, "/api/expenses").andExpect(jsonPath("$.totalElements").value(0));

        String ownerCategory = categoryId(owner, "Mercado");
        String json = "{\"categoryId\":\"%s\",\"amount\":5,\"description\":\"x\",\"occurredOn\":\"2026-10-13\"}".formatted(ownerCategory);
        postAs(intruder, "/api/expenses", json)
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("CATEGORY_NOT_FOUND"));
    }
}
