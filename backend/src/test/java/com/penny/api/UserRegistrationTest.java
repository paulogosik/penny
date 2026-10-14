package com.penny.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.penny.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserRegistrationTest extends IntegrationTest {

    @Test
    void registrationCreatesDefaultCategories() throws Exception {
        long user = registerUser();

        JsonNode expenseCategories = body(getAs(user, "/api/expense-categories").andExpect(status().isOk()));
        List<String> names = new ArrayList<>();
        expenseCategories.forEach(category -> names.add(category.path("name").asText()));
        assertThat(names).containsExactlyInAnyOrder(
            "Mercado", "Restaurante", "Transporte", "Moradia", "Saúde",
            "Lazer", "Educação", "Compras", "Serviços", "Outros"
        );
        assertThat(expenseCategories.findValues("fallback").stream().filter(JsonNode::asBoolean)).hasSize(1);

        getAs(user, "/api/income-categories")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].name").value("Salário"));

        getAs(user, "/api/me")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Pessoa Teste"))
            .andExpect(jsonPath("$.timezone").value("America/Sao_Paulo"));
    }

    @Test
    void registeringAgainIsIdempotentAndUpdatesProfile() throws Exception {
        long user = registerUser();

        putAs(user, "/api/me", "{\"name\":\"Novo Nome\",\"timezone\":\"Europe/Lisbon\"}")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Novo Nome"))
            .andExpect(jsonPath("$.timezone").value("Europe/Lisbon"));

        getAs(user, "/api/expense-categories").andExpect(jsonPath("$.length()").value(10));
    }

    @Test
    void registrationWithoutBodyUsesDefaults() throws Exception {
        mvc.perform(put("/api/me").header(HEADER, 987_654_321_000L))
            .andExpect(status().is2xxSuccessful())
            .andExpect(jsonPath("$.timezone").value("America/Sao_Paulo"));
    }

    @Test
    void invalidTimezoneIsRejected() throws Exception {
        long user = registerUser();
        putAs(user, "/api/me", "{\"timezone\":\"Marte/Base\"}")
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("INVALID_TIMEZONE"));
    }

    @Test
    void unknownUserIsAskedToRegister() throws Exception {
        getAs(1L, "/api/expense-categories")
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("USER_NOT_REGISTERED"));
    }

    @Test
    void missingOrInvalidHeaderIsRejected() throws Exception {
        mvc.perform(get("/api/me"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("MISSING_HEADER"));
        mvc.perform(get("/api/me").header(HEADER, "abc").contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_HEADER"));
    }
}
