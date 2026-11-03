package com.penny.api;

import com.penny.support.IntegrationTest;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AiOutputHandlingTest extends IntegrationTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 13);

    @Test
    void amountTooLargeForTheDatabaseIsRejectedWithAClearMessage() throws Exception {
        long user = registerUser();
        classifier.willReturn("Compras", "100000000000.00", TODAY, "Ilha");

        postAs(user, "/api/messages/text", "{\"text\":\"comprei uma ilha por 100 bilhões\"}")
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("AMOUNT_TOO_LARGE"));
        getAs(user, "/api/expenses").andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void largestAmountThatFitsIsAccepted() throws Exception {
        long user = registerUser();
        classifier.willReturn("Moradia", "9999999999.99", TODAY, "Casa");

        postAs(user, "/api/messages/text", "{\"text\":\"casa de 9.999.999.999,99\"}")
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.expense.amount").value(9999999999.99));
    }

    @Test
    void negativeAmountIsTreatedAsMissing() throws Exception {
        long user = registerUser();
        classifier.willReturn("Mercado", "-50.00", TODAY, "Estorno");

        postAs(user, "/api/messages/text", "{\"text\":\"estorno de 50\"}")
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("AMOUNT_NOT_FOUND"));
    }

    @Test
    void tooManyInstallmentsAreRejectedWithoutCreatingAPlan() throws Exception {
        long user = registerUser();
        createCard(user, "Nubank", 3, 10);
        classifier.willReturn("Compras", "1200.00", TODAY, "Sofá", 120, "Nubank");

        postAs(user, "/api/messages/text", "{\"text\":\"sofá 1200 em 120x no nubank\"}")
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("INVALID_INSTALLMENTS"));
        getAs(user, "/api/installment-plans").andExpect(jsonPath("$.length()").value(0));
        getAs(user, "/api/expenses").andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void archivedCategoryIsNotUsedAndArchivedCardIsRejected() throws Exception {
        long user = registerUser();
        String petsId = body(postAs(user, "/api/expense-categories", "{\"name\":\"Pets\"}").andExpect(status().isCreated()))
            .path("id").asText();
        String cardId = createCard(user, "Nubank", 3, 10);
        postAs(user, "/api/expenses", ("{\"categoryId\":\"%s\",\"amount\":30,\"description\":\"Ração\","
                + "\"occurredOn\":\"2026-10-01\",\"creditCardId\":\"%s\"}").formatted(petsId, cardId))
            .andExpect(status().isCreated());
        deleteAs(user, "/api/expense-categories/" + petsId).andExpect(status().isNoContent());
        deleteAs(user, "/api/credit-cards/" + cardId).andExpect(status().isNoContent());

        classifier.willReturn("Pets", "45.00", TODAY, "Ração");
        postAs(user, "/api/messages/text", "{\"text\":\"ração 45\"}")
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.expense.categoryName").value("Outros"));

        classifier.willReturn("Mercado", "45.00", TODAY, "Ração", 1, "Nubank");
        postAs(user, "/api/messages/text", "{\"text\":\"ração 45 no nubank\"}")
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("CREDIT_CARD_NOT_REGISTERED"));
        getAs(user, "/api/expenses").andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void longDescriptionIsTruncatedAndBlankDescriptionUsesTheCategoryName() throws Exception {
        long user = registerUser();
        classifier.willReturn("Mercado", "10.00", TODAY, "x".repeat(500));

        postAs(user, "/api/messages/text", "{\"text\":\"mercado 10\"}")
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.expense.description").value("x".repeat(200)));

        classifier.willReturn("Mercado", "10.00", TODAY, "   ");
        postAs(user, "/api/messages/text", "{\"text\":\"mercado 10\"}")
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.expense.description").value("Mercado"));
    }
}
