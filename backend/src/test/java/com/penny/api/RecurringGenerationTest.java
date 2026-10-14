package com.penny.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.penny.service.RecurringEntryGenerator;
import com.penny.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RecurringGenerationTest extends IntegrationTest {

    @Autowired
    private RecurringEntryGenerator generator;

    @Test
    void fixedExpenseIsGeneratedOncePerMonthOnItsDueDay() throws Exception {
        long user = registerUser();
        clock.set(Instant.parse("2026-11-10T15:00:00Z"));
        String json = "{\"categoryId\":\"%s\",\"description\":\"Aluguel\",\"amount\":1500,\"dayOfMonth\":15,\"startsOn\":\"2026-11-01\"}"
            .formatted(categoryId(user, "Moradia"));
        postAs(user, "/api/recurring-expenses", json)
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.lastGeneratedMonth").isEmpty());
        getAs(user, "/api/expenses").andExpect(jsonPath("$.totalElements").value(0));

        clock.set(Instant.parse("2026-11-15T12:00:00Z"));
        generator.generateDueEntries();
        generator.generateDueEntries();

        getAs(user, "/api/expenses")
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].occurredOn").value("2026-11-15"))
            .andExpect(jsonPath("$.content[0].source").value("recurring"))
            .andExpect(jsonPath("$.content[0].recurringExpenseId").isNotEmpty());
        getAs(user, "/api/recurring-expenses").andExpect(jsonPath("$[0].lastGeneratedMonth").value("2026-11-01"));
    }

    @Test
    void dayBeyondMonthLengthUsesLastDayAndDeletedEntryIsNotRecreated() throws Exception {
        long user = registerUser();
        clock.set(Instant.parse("2026-11-30T15:00:00Z"));
        String json = "{\"categoryId\":\"%s\",\"description\":\"Academia\",\"amount\":99.90,\"dayOfMonth\":31,\"startsOn\":\"2026-11-01\"}"
            .formatted(categoryId(user, "Saúde"));
        postAs(user, "/api/recurring-expenses", json).andExpect(status().isCreated());

        JsonNode page = body(getAs(user, "/api/expenses"));
        assertThat(page.path("totalElements").asInt()).isEqualTo(1);
        assertThat(page.path("content").get(0).path("occurredOn").asText()).isEqualTo("2026-11-30");

        deleteAs(user, "/api/expenses/" + page.path("content").get(0).path("id").asText()).andExpect(status().isNoContent());
        generator.generateDueEntries();
        getAs(user, "/api/expenses").andExpect(jsonPath("$.totalElements").value(0));

        clock.set(Instant.parse("2026-12-31T15:00:00Z"));
        generator.generateDueEntries();
        getAs(user, "/api/expenses")
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].occurredOn").value("2026-12-31"));
    }

    @Test
    void inactiveOrEndedRulesAreSkipped() throws Exception {
        long user = registerUser();
        clock.set(Instant.parse("2026-11-20T15:00:00Z"));
        String moradia = categoryId(user, "Moradia");
        postAs(user, "/api/recurring-expenses",
            "{\"categoryId\":\"%s\",\"description\":\"Pausado\",\"amount\":10,\"dayOfMonth\":1,\"startsOn\":\"2026-01-01\",\"active\":false}".formatted(moradia))
            .andExpect(status().isCreated());
        postAs(user, "/api/recurring-expenses",
            "{\"categoryId\":\"%s\",\"description\":\"Encerrado\",\"amount\":10,\"dayOfMonth\":1,\"startsOn\":\"2026-01-01\",\"endsOn\":\"2026-10-31\"}".formatted(moradia))
            .andExpect(status().isCreated());
        postAs(user, "/api/recurring-expenses",
            "{\"categoryId\":\"%s\",\"description\":\"Invertido\",\"amount\":10,\"dayOfMonth\":1,\"startsOn\":\"2026-12-01\",\"endsOn\":\"2026-11-01\"}".formatted(moradia))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("INVALID_PERIOD"));

        generator.generateDueEntries();
        getAs(user, "/api/expenses").andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void fixedExpenseOnCreditCardGetsBillingMonth() throws Exception {
        long user = registerUser();
        clock.set(Instant.parse("2026-11-05T15:00:00Z"));
        String card = createCard(user, "Nubank", 3, 10);
        String json = "{\"categoryId\":\"%s\",\"creditCardId\":\"%s\",\"description\":\"Streaming\",\"amount\":39.90,\"dayOfMonth\":5,\"startsOn\":\"2026-11-01\"}"
            .formatted(categoryId(user, "Lazer"), card);
        postAs(user, "/api/recurring-expenses", json)
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.creditCardName").value("Nubank"));

        getAs(user, "/api/expenses")
            .andExpect(jsonPath("$.content[0].billingMonth").value("2026-12-01"))
            .andExpect(jsonPath("$.content[0].creditCardName").value("Nubank"));
    }

    @Test
    void salaryIsGeneratedAsIncomeAndChangingItOnlyAffectsFutureMonths() throws Exception {
        long user = registerUser();
        clock.set(Instant.parse("2026-11-05T12:00:00Z"));
        String salario = incomeCategoryId(user, "Salário");
        JsonNode rule = body(postAs(user, "/api/recurring-incomes",
            "{\"categoryId\":\"%s\",\"description\":\"Salário\",\"amount\":5000,\"dayOfMonth\":5,\"startsOn\":\"2026-11-01\"}".formatted(salario))
            .andExpect(status().isCreated()));

        getAs(user, "/api/incomes?from=2026-11-01&to=2026-11-30")
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].amount").value(5000))
            .andExpect(jsonPath("$[0].source").value("recurring"));

        putAs(user, "/api/recurring-incomes/" + rule.path("id").asText(),
            "{\"categoryId\":\"%s\",\"description\":\"Salário\",\"amount\":5500,\"dayOfMonth\":5,\"startsOn\":\"2026-11-01\"}".formatted(salario))
            .andExpect(status().isOk());

        clock.set(Instant.parse("2026-12-05T12:00:00Z"));
        generator.generateDueEntries();

        getAs(user, "/api/incomes?from=2026-11-01&to=2026-11-30").andExpect(jsonPath("$[0].amount").value(5000));
        getAs(user, "/api/incomes?from=2026-12-01&to=2026-12-31").andExpect(jsonPath("$[0].amount").value(5500));
    }
}
