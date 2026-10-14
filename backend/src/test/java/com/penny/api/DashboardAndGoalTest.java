package com.penny.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.penny.support.IntegrationTest;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DashboardAndGoalTest extends IntegrationTest {

    @Test
    void dashboardSummarizesTheMonth() throws Exception {
        long user = registerUser();
        String mercado = categoryId(user, "Mercado");
        putAs(user, "/api/expense-categories/" + mercado, "{\"name\":\"Mercado\",\"monthlyBudget\":500}").andExpect(status().isOk());
        createCard(user, "Nubank", 3, 10);

        classifier.willReturn("Mercado", "125.00", LocalDate.of(2026, 10, 2), "Feira");
        postAs(user, "/api/expenses/text", "{\"text\":\"feira 125\"}").andExpect(status().isCreated());
        classifier.willReturn("Restaurante", "60.00", LocalDate.of(2026, 10, 2), "Pizza", 1, "Nubank");
        postAs(user, "/api/expenses/text", "{\"text\":\"pizza 60 no nubank\"}").andExpect(status().isCreated());
        classifier.willReturn("Lazer", "40.00", LocalDate.of(2026, 9, 30), "Show");
        postAs(user, "/api/expenses/text", "{\"text\":\"show 40 ontem\"}").andExpect(status().isCreated());
        postAs(user, "/api/incomes",
            "{\"categoryId\":\"%s\",\"amount\":3000,\"description\":\"Salário\",\"receivedOn\":\"2026-10-05\"}".formatted(incomeCategoryId(user, "Salário")))
            .andExpect(status().isCreated());

        JsonNode dashboard = body(getAs(user, "/api/dashboard?month=2026-10").andExpect(status().isOk()));
        assertThat(dashboard.path("month").asText()).isEqualTo("2026-10");
        assertThat(dashboard.path("totalExpenses").decimalValue()).isEqualByComparingTo("185.00");
        assertThat(dashboard.path("totalIncome").decimalValue()).isEqualByComparingTo("3000.00");
        assertThat(dashboard.path("balance").decimalValue()).isEqualByComparingTo("2815.00");

        JsonNode first = dashboard.path("byCategory").get(0);
        assertThat(first.path("name").asText()).isEqualTo("Mercado");
        assertThat(first.path("budgetUsedPercent").decimalValue()).isEqualByComparingTo("25.0");
        assertThat(dashboard.path("byCategory")).hasSize(2);

        JsonNode bill = dashboard.path("creditCardBills").get(0);
        assertThat(bill.path("name").asText()).isEqualTo("Nubank");
        assertThat(bill.path("total").decimalValue()).isEqualByComparingTo("60.00");
        assertThat(bill.path("dueDate").asText()).isEqualTo("2026-10-10");
    }

    @Test
    void dashboardDefaultsToCurrentMonth() throws Exception {
        long user = registerUser();
        getAs(user, "/api/dashboard")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.month").value("2026-10"))
            .andExpect(jsonPath("$.totalExpenses").value(0));
    }

    @Test
    void goalsTrackProgressAndAchievement() throws Exception {
        long user = registerUser();
        JsonNode goal = body(postAs(user, "/api/goals",
            "{\"name\":\"Viagem\",\"targetAmount\":10000,\"savedAmount\":2500,\"targetDate\":\"2027-07-01\"}")
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.progressPercent").value(25.0))
            .andExpect(jsonPath("$.achievedAt").isEmpty()));

        putAs(user, "/api/goals/" + goal.path("id").asText(),
            "{\"name\":\"Viagem\",\"targetAmount\":10000,\"savedAmount\":10000}")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.progressPercent").value(100.0))
            .andExpect(jsonPath("$.achievedAt").isNotEmpty());

        getAs(user, "/api/goals").andExpect(jsonPath("$.length()").value(1));
        deleteAs(user, "/api/goals/" + goal.path("id").asText()).andExpect(status().isNoContent());
        getAs(user, "/api/goals").andExpect(jsonPath("$.length()").value(0));
    }
}
