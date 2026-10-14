package com.penny.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.penny.support.IntegrationTest;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CategoryAndCardManagementTest extends IntegrationTest {

    @Test
    void categoryNamesAreUniquePerUserIgnoringCase() throws Exception {
        long user = registerUser();
        postAs(user, "/api/expense-categories", "{\"name\":\"mercado\"}")
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("CATEGORY_NAME_TAKEN"));

        long other = registerUser();
        postAs(other, "/api/expense-categories", "{\"name\":\"Pets\",\"color\":\"#22AA55\",\"monthlyBudget\":200}")
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.monthlyBudget").value(200));
    }

    @Test
    void unusedCategoryIsDeletedAndUsedCategoryIsArchived() throws Exception {
        long user = registerUser();
        String lazer = categoryId(user, "Lazer");
        deleteAs(user, "/api/expense-categories/" + lazer).andExpect(status().isNoContent());
        JsonNode all = body(getAs(user, "/api/expense-categories?includeArchived=true"));
        assertThat(all.findValuesAsText("name")).doesNotContain("Lazer");

        classifier.willReturn("Mercado", "30.00", LocalDate.of(2026, 10, 13), "Pão");
        postAs(user, "/api/expenses/text", "{\"text\":\"pão 30\"}").andExpect(status().isCreated());
        String mercado = categoryId(user, "Mercado");
        deleteAs(user, "/api/expense-categories/" + mercado).andExpect(status().isNoContent());

        assertThat(body(getAs(user, "/api/expense-categories")).findValuesAsText("name")).doesNotContain("Mercado");
        getAs(user, "/api/expenses").andExpect(jsonPath("$.content[0].categoryName").value("Mercado"));

        String json = "{\"categoryId\":\"%s\",\"amount\":5,\"description\":\"x\",\"occurredOn\":\"2026-10-13\"}".formatted(mercado);
        postAs(user, "/api/expenses", json)
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("CATEGORY_ARCHIVED"));

        postAs(user, "/api/expense-categories", "{\"name\":\"Mercado\"}").andExpect(status().isCreated());
    }

    @Test
    void fallbackCategoryCannotBeDeletedButCanBeRenamed() throws Exception {
        long user = registerUser();
        String outros = categoryId(user, "Outros");

        deleteAs(user, "/api/expense-categories/" + outros)
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("FALLBACK_CATEGORY_PROTECTED"));

        putAs(user, "/api/expense-categories/" + outros, "{\"name\":\"Diversos\"}")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Diversos"))
            .andExpect(jsonPath("$.fallback").value(true));
    }

    @Test
    void incomeCategoriesCanBeManaged() throws Exception {
        long user = registerUser();
        JsonNode created = body(postAs(user, "/api/income-categories", "{\"name\":\"Freelance\"}").andExpect(status().isCreated()));
        postAs(user, "/api/income-categories", "{\"name\":\"freelance\"}").andExpect(status().isConflict());
        deleteAs(user, "/api/income-categories/" + created.path("id").asText()).andExpect(status().isNoContent());
        getAs(user, "/api/income-categories").andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void creditCardsCanBeManagedAndAreArchivedWhenUsed() throws Exception {
        long user = registerUser();
        String unused = createCard(user, "C6", 1, 8);
        postAs(user, "/api/credit-cards", "{\"name\":\"c6\",\"closingDay\":2,\"dueDay\":9}").andExpect(status().isConflict());
        postAs(user, "/api/credit-cards", "{\"name\":\"XP\",\"closingDay\":0,\"dueDay\":40}")
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fields.closingDay").exists())
            .andExpect(jsonPath("$.fields.dueDay").exists());

        putAs(user, "/api/credit-cards/" + unused, "{\"name\":\"C6 Bank\",\"closingDay\":1,\"dueDay\":8,\"creditLimit\":5000}")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.creditLimit").value(5000));
        deleteAs(user, "/api/credit-cards/" + unused).andExpect(status().isNoContent());
        getAs(user, "/api/credit-cards?includeArchived=true").andExpect(jsonPath("$.length()").value(0));

        String used = createCard(user, "Nubank", 3, 10);
        classifier.willReturn("Compras", "50.00", LocalDate.of(2026, 10, 13), "Livro", 1, "Nubank");
        postAs(user, "/api/expenses/text", "{\"text\":\"livro 50 no nubank\"}").andExpect(status().isCreated());
        deleteAs(user, "/api/credit-cards/" + used).andExpect(status().isNoContent());
        getAs(user, "/api/credit-cards").andExpect(jsonPath("$.length()").value(0));
        getAs(user, "/api/credit-cards?includeArchived=true")
            .andExpect(jsonPath("$[0].archived").value(true));
    }

    @Test
    void creditCardStatementListsTheBill() throws Exception {
        long user = registerUser();
        String card = createCard(user, "Nubank", 3, 10);
        classifier.willReturn("Compras", "90.00", LocalDate.of(2026, 10, 13), "Mochila", 3, "Nubank");
        postAs(user, "/api/expenses/text", "{\"text\":\"mochila 90 em 3x no nubank\"}").andExpect(status().isCreated());
        classifier.willReturn("Restaurante", "45.50", LocalDate.of(2026, 10, 20), "Jantar", 1, "Nubank");
        postAs(user, "/api/expenses/text", "{\"text\":\"jantar 45,50 no nubank\"}").andExpect(status().isCreated());

        getAs(user, "/api/credit-cards/" + card + "/statements/2026-11")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.dueDate").value("2026-11-10"))
            .andExpect(jsonPath("$.total").value(75.50))
            .andExpect(jsonPath("$.expenses.length()").value(2));
    }
}
