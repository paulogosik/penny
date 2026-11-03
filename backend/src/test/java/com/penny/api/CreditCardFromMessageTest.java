package com.penny.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.penny.support.IntegrationTest;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.containsStringIgnoringCase;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CreditCardFromMessageTest extends IntegrationTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 13);
    private static final String CARD_MESSAGE = "{\"text\":\"meu cartão nubank fecha dia 3 e vence dia 10\"}";

    @Test
    void cardMessageRegistersANewCardThatTheAiSeesNextTime() throws Exception {
        long user = registerUser();
        classifier.willReturnCreditCard("Nubank", 3, 10, null);

        JsonNode created = body(postAs(user, "/api/messages/text", CARD_MESSAGE)
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.type").value("credit_card_created"))
            .andExpect(jsonPath("$.creditCard.name").value("Nubank"))
            .andExpect(jsonPath("$.creditCard.closingDay").value(3))
            .andExpect(jsonPath("$.creditCard.dueDay").value(10))
            .andExpect(jsonPath("$.creditCard.creditLimit").isEmpty()));
        assertThat(created.has("expense")).isFalse();

        getAs(user, "/api/credit-cards")
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].name").value("Nubank"));
        getAs(user, "/api/expenses").andExpect(jsonPath("$.totalElements").value(0));

        classifier.willReturn("Compras", "120.00", TODAY, "Fone", 1, "nubank");
        JsonNode expense = body(postAs(user, "/api/messages/text", "{\"text\":\"fone 120 no nubank\"}")
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.type").value("expense"))
            .andExpect(jsonPath("$.expense.creditCardName").value("Nubank")));
        assertThat(expense.has("creditCard")).isFalse();
        assertThat(classifier.lastContext().creditCards()).containsExactly("Nubank");
    }

    @Test
    void creditLimitIsSavedWhenTheMessageHasOne() throws Exception {
        long user = registerUser();
        classifier.willReturnCreditCard("Inter", 5, 12, "5000.00");

        postAs(user, "/api/messages/text", "{\"text\":\"cartão inter fecha 5 vence 12 limite 5 mil\"}")
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.creditCard.creditLimit").value(5000.00));
    }

    @Test
    void missingDetailsAreExplainedAndNothingIsSaved() throws Exception {
        long user = registerUser();

        classifier.willReturnCreditCard("Inter", null, null, null);
        postAs(user, "/api/messages/text", "{\"text\":\"tenho um cartão inter\"}")
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("CREDIT_CARD_DETAILS_MISSING"))
            .andExpect(jsonPath("$.message", containsString("fechamento")))
            .andExpect(jsonPath("$.message", containsString("vencimento")))
            .andExpect(jsonPath("$.message", containsString("fecha dia")))
            .andExpect(jsonPath("$.message", containsString("Inter")));

        classifier.willReturnCreditCard("Inter", 5, null, null);
        postAs(user, "/api/messages/text", "{\"text\":\"cartão inter fecha dia 5\"}")
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("CREDIT_CARD_DETAILS_MISSING"))
            .andExpect(jsonPath("$.message", containsString("vencimento")))
            .andExpect(jsonPath("$.message", not(containsString("fechamento"))));

        classifier.willReturnCreditCard("Inter", null, 12, null);
        postAs(user, "/api/messages/text", "{\"text\":\"cartão inter vence dia 12\"}")
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("CREDIT_CARD_DETAILS_MISSING"))
            .andExpect(jsonPath("$.message", containsString("fechamento")))
            .andExpect(jsonPath("$.message", not(containsString("vencimento"))));

        for (String missingName : new String[]{null, "  "}) {
            classifier.willReturnCreditCard(missingName, 5, 12, null);
            postAs(user, "/api/messages/text", "{\"text\":\"meu cartão fecha dia 5 e vence dia 12\"}")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("CREDIT_CARD_DETAILS_MISSING"))
                .andExpect(jsonPath("$.message", containsString("nome")))
                .andExpect(jsonPath("$.message", not(containsString("fechamento"))))
                .andExpect(jsonPath("$.message", not(containsString("vencimento"))));
        }

        getAs(user, "/api/credit-cards?includeArchived=true").andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void daysOutsideOneToThirtyOneAreRejected() throws Exception {
        long user = registerUser();

        classifier.willReturnCreditCard("Nubank", 0, 10, null);
        postAs(user, "/api/messages/text", CARD_MESSAGE)
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("INVALID_CREDIT_CARD_DAY"));

        classifier.willReturnCreditCard("Nubank", 3, 32, null);
        postAs(user, "/api/messages/text", CARD_MESSAGE)
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("INVALID_CREDIT_CARD_DAY"));

        classifier.willReturnCreditCard("Nubank", 32, 10, null);
        postAs(user, "/api/messages/text", CARD_MESSAGE)
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("INVALID_CREDIT_CARD_DAY"));

        classifier.willReturnCreditCard("Nubank", 3, 0, null);
        postAs(user, "/api/messages/text", CARD_MESSAGE)
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("INVALID_CREDIT_CARD_DAY"));

        classifier.willReturnCreditCard("Nubank", 31, 1, null);
        postAs(user, "/api/messages/text", CARD_MESSAGE).andExpect(status().isCreated());

        getAs(user, "/api/credit-cards").andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void limitIsOptionalNegativeIsIgnoredAndTooLargeIsRejected() throws Exception {
        long user = registerUser();

        classifier.willReturnCreditCard("Nubank", 3, 10, "-100.00");
        postAs(user, "/api/messages/text", CARD_MESSAGE)
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.creditCard.creditLimit").isEmpty());

        classifier.willReturnCreditCard("Inter", 5, 12, "100000000000.00");
        postAs(user, "/api/messages/text", "{\"text\":\"cartão inter fecha 5 vence 12 limite 100 bilhões\"}")
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("AMOUNT_TOO_LARGE"));

        classifier.willReturnCreditCard("C6", 1, 8, "9999999999.99");
        postAs(user, "/api/messages/text", "{\"text\":\"cartão c6 fecha 1 vence 8 limite máximo\"}")
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.creditCard.creditLimit").value(9999999999.99));

        getAs(user, "/api/credit-cards")
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[?(@.name == 'Inter')]").doesNotExist());
    }

    @Test
    void negativeLimitOnAnUpdateKeepsTheLimitTheCardHad() throws Exception {
        long user = registerUser();
        postAs(user, "/api/credit-cards", "{\"name\":\"Nubank\",\"closingDay\":3,\"dueDay\":10,\"creditLimit\":2000}")
            .andExpect(status().isCreated());
        classifier.willReturnCreditCard("Nubank", 5, 12, "-500.00");

        postAs(user, "/api/messages/text", "{\"text\":\"nubank fecha dia 5 e vence dia 12\"}")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.creditCard.closingDay").value(5))
            .andExpect(jsonPath("$.creditCard.creditLimit").value(2000));
    }

    @Test
    void nameIsTrimmedAndCutToFiftyCharacters() throws Exception {
        long user = registerUser();

        classifier.willReturnCreditCard("  Nubank  ", 3, 10, null);
        postAs(user, "/api/messages/text", CARD_MESSAGE)
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.creditCard.name").value("Nubank"));

        classifier.willReturnCreditCard("B".repeat(80), 5, 12, null);
        postAs(user, "/api/messages/text", "{\"text\":\"cartão com nome enorme fecha 5 vence 12\"}")
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.creditCard.name").value("B".repeat(50)));
    }

    @Test
    void existingCardWithTheSameNameIsUpdatedInsteadOfDuplicated() throws Exception {
        long user = registerUser();
        postAs(user, "/api/credit-cards", "{\"name\":\"Itaú\",\"closingDay\":3,\"dueDay\":10,\"creditLimit\":2000}")
            .andExpect(status().isCreated());

        classifier.willReturnCreditCard("ITAU", 20, 27, null);
        JsonNode updated = body(postAs(user, "/api/messages/text", "{\"text\":\"meu itau agora fecha dia 20 e vence dia 27\"}")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.type").value("credit_card_updated"))
            .andExpect(jsonPath("$.creditCard.name").value("Itaú"))
            .andExpect(jsonPath("$.creditCard.closingDay").value(20))
            .andExpect(jsonPath("$.creditCard.dueDay").value(27))
            .andExpect(jsonPath("$.creditCard.creditLimit").value(2000)));
        assertThat(updated.has("expense")).isFalse();

        classifier.willReturnCreditCard("itaú", 20, 27, "3500.00");
        postAs(user, "/api/messages/text", "{\"text\":\"o limite do itaú agora é 3500, fecha 20 e vence 27\"}")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.creditCard.creditLimit").value(3500));

        getAs(user, "/api/credit-cards?includeArchived=true")
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].name").value("Itaú"))
            .andExpect(jsonPath("$[0].closingDay").value(20));
    }

    @Test
    void updatingACardKeepsPastExpensesOnTheirStatementAndAppliesToNewOnes() throws Exception {
        long user = registerUser();
        createCard(user, "Nubank", 3, 10);
        classifier.willReturn("Compras", "100.00", TODAY, "Fone", 1, "Nubank");
        String before = body(postAs(user, "/api/messages/text", "{\"text\":\"fone 100 no nubank\"}").andExpect(status().isCreated()))
            .path("expense").path("id").asText();

        classifier.willReturnCreditCard("Nubank", 20, 27, null);
        postAs(user, "/api/messages/text", "{\"text\":\"meu nubank agora fecha dia 20 e vence dia 27\"}").andExpect(status().isOk());

        classifier.willReturn("Compras", "50.00", TODAY, "Capa", 1, "Nubank");
        postAs(user, "/api/messages/text", "{\"text\":\"capa 50 no nubank\"}")
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.expense.billingMonth").value("2026-10-01"));
        getAs(user, "/api/expenses/" + before).andExpect(jsonPath("$.billingMonth").value("2026-11-01"));
    }

    @Test
    void cardsThatDifferOnlyByAccentPreferTheSameSpelling() throws Exception {
        long user = registerUser();
        String withAccent = createCard(user, "Itaú", 3, 10);
        String withoutAccent = createCard(user, "Itau", 5, 12);

        classifier.willReturnCreditCard("itau", 20, 27, null);
        postAs(user, "/api/messages/text", "{\"text\":\"itau fecha dia 20 e vence dia 27\"}")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.creditCard.id").value(withoutAccent));
        classifier.willReturnCreditCard("ITAÚ", 21, 28, null);
        postAs(user, "/api/messages/text", "{\"text\":\"itaú fecha dia 21 e vence dia 28\"}")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.creditCard.id").value(withAccent));

        classifier.willReturn("Compras", "10.00", TODAY, "Caneta", 1, "itaú");
        postAs(user, "/api/messages/text", "{\"text\":\"caneta 10 no itaú\"}")
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.expense.creditCardId").value(withAccent));
        classifier.willReturn("Compras", "10.00", TODAY, "Caneta", 1, "Itau");
        postAs(user, "/api/messages/text", "{\"text\":\"caneta 10 no itau\"}")
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.expense.creditCardId").value(withoutAccent));
    }

    @Test
    void removedCardWithTheSameNameDoesNotBlockRegisteringItAgain() throws Exception {
        long user = registerUser();
        String oldCard = createCard(user, "Nubank", 3, 10);
        classifier.willReturn("Compras", "100.00", TODAY, "Fone", 1, "Nubank");
        postAs(user, "/api/messages/text", "{\"text\":\"fone 100 no nubank\"}").andExpect(status().isCreated());
        deleteAs(user, "/api/credit-cards/" + oldCard).andExpect(status().isNoContent());

        classifier.willReturnCreditCard("Nubank", 7, 14, null);
        JsonNode created = body(postAs(user, "/api/messages/text", CARD_MESSAGE)
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.type").value("credit_card_created")));

        assertThat(created.path("creditCard").path("id").asText()).isNotEqualTo(oldCard);
        getAs(user, "/api/credit-cards")
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].closingDay").value(7));
        getAs(user, "/api/credit-cards?includeArchived=true").andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void anotherUsersCardWithTheSameNameIsNeverChanged() throws Exception {
        long owner = registerUser();
        long other = registerUser();
        createCard(owner, "Nubank", 3, 10);

        classifier.willReturnCreditCard("Nubank", 20, 27, null);
        postAs(other, "/api/messages/text", CARD_MESSAGE)
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.type").value("credit_card_created"));

        getAs(owner, "/api/credit-cards")
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].closingDay").value(3))
            .andExpect(jsonPath("$[0].dueDay").value(10));
        getAs(other, "/api/credit-cards").andExpect(jsonPath("$[0].closingDay").value(20));
    }

    @Test
    void cardMessageRequiresAPhoneNumber() throws Exception {
        long user = registerUserWithoutPhone();
        classifier.willReturnCreditCard("Nubank", 3, 10, null);

        postAs(user, "/api/messages/text", CARD_MESSAGE)
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("PHONE_NUMBER_REQUIRED"));
        getAs(user, "/api/credit-cards").andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void expenseMentioningAnUnregisteredCardIsRejectedWithInstructions() throws Exception {
        long user = registerUser();

        classifier.willReturn("Restaurante", "30.00", TODAY, "Almoço", 1, "Itaú");
        postAs(user, "/api/messages/text", "{\"text\":\"almoço 30 no itaú\"}")
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("CREDIT_CARD_NOT_REGISTERED"))
            .andExpect(jsonPath("$.message", containsString("Itaú")))
            .andExpect(jsonPath("$.message", containsString("fecha dia")));

        createCard(user, "Nubank", 3, 10);
        classifier.willReturn("Restaurante", "30.00", TODAY, "Almoço", 1, "Itaú");
        postAs(user, "/api/messages/text", "{\"text\":\"almoço 30 no itaú\"}")
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("CREDIT_CARD_NOT_REGISTERED"));

        classifier.willReturn("Compras", "600.00", TODAY, "TV", 6, "Itaú");
        postAs(user, "/api/messages/text", "{\"text\":\"tv 600 em 6x no itaú\"}")
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("CREDIT_CARD_NOT_REGISTERED"));

        getAs(user, "/api/expenses").andExpect(jsonPath("$.totalElements").value(0));
        getAs(user, "/api/installment-plans").andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void expenseWithoutACardMentionIsStillSavedWithoutCard() throws Exception {
        long user = registerUser();
        createCard(user, "Nubank", 3, 10);
        classifier.willReturn("Mercado", "45.00", TODAY, "Mercado", 1, null);

        postAs(user, "/api/messages/text", "{\"text\":\"gastei 45 no mercado\"}")
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.type").value("expense"))
            .andExpect(jsonPath("$.expense.creditCardId").isEmpty())
            .andExpect(jsonPath("$.expense.billingMonth").isEmpty());
    }

    @Test
    void installmentsWithoutACardTeachHowToRegisterOneThroughTheBot() throws Exception {
        long user = registerUser();
        classifier.willReturn("Compras", "600.00", TODAY, "TV", 6, null);

        postAs(user, "/api/messages/text", "{\"text\":\"tv 600 em 6x\"}")
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("INSTALLMENTS_REQUIRE_CREDIT_CARD"))
            .andExpect(jsonPath("$.message", containsString("fecha dia")))
            .andExpect(jsonPath("$.message", not(containsStringIgnoringCase("painel"))));
    }

    @Test
    void receiptUsesARegisteredCardAndSilentlyDropsAnyOther() throws Exception {
        long owner = registerUser();
        long user = registerUser();
        createCard(owner, "Inter", 5, 12);
        createCard(user, "Nubank", 3, 10);
        String removed = createCard(user, "C6", 1, 8);
        postAs(user, "/api/expenses", ("{\"categoryId\":\"%s\",\"amount\":20,\"description\":\"Café\","
                + "\"occurredOn\":\"2026-10-01\",\"creditCardId\":\"%s\"}").formatted(categoryId(user, "Mercado"), removed))
            .andExpect(status().isCreated());
        deleteAs(user, "/api/credit-cards/" + removed).andExpect(status().isNoContent());

        classifier.willReturn("Mercado", "88.00", TODAY, "Supermercado", 1, "nubank");
        postAs(user, "/api/expenses/photo", receipt())
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.source").value("telegram_photo"))
            .andExpect(jsonPath("$.creditCardName").value("Nubank"))
            .andExpect(jsonPath("$.billingMonth").value("2026-11-01"));

        for (String notUsable : new String[]{"Visa", "C6", "Inter"}) {
            classifier.willReturn("Mercado", "88.00", TODAY, "Supermercado", 1, notUsable);
            postAs(user, "/api/expenses/photo", receipt())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.creditCardId").isEmpty())
                .andExpect(jsonPath("$.billingMonth").isEmpty());
        }
        getAs(owner, "/api/expenses").andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void oldTextRouteNoLongerExists() throws Exception {
        long user = registerUser();

        postAs(user, "/api/expenses/text", "{\"text\":\"gastei 45 no mercado\"}").andExpect(status().is4xxClientError());

        assertThat(classifier.lastContext()).isNull();
        getAs(user, "/api/expenses").andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void messagesThatAreNeitherExpenseNorCardAreNotUnderstood() throws Exception {
        long user = registerUser();
        classifier.willReturnUnrecognized();

        postAs(user, "/api/messages/text", "{\"text\":\"bom dia, tudo bem?\"}")
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("MESSAGE_NOT_UNDERSTOOD"))
            .andExpect(jsonPath("$.message", containsString("gastei")))
            .andExpect(jsonPath("$.message", containsString("fecha dia")));

        getAs(user, "/api/expenses").andExpect(jsonPath("$.totalElements").value(0));
        getAs(user, "/api/credit-cards").andExpect(jsonPath("$.length()").value(0));
    }

    private static String receipt() {
        return "{\"imageBase64\":\"%s\",\"mimeType\":\"image/jpeg\"}".formatted(Base64.getEncoder().encodeToString(new byte[]{1, 2, 3}));
    }
}
