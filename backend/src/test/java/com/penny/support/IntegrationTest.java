package com.penny.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.penny.web.CurrentUserArgumentResolver;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.concurrent.atomic.AtomicLong;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({TestcontainersConfiguration.class, TestBeans.class})
public abstract class IntegrationTest {

    protected static final String HEADER = CurrentUserArgumentResolver.HEADER;

    private static final AtomicLong TELEGRAM_IDS = new AtomicLong(System.currentTimeMillis());

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected FakeExpenseClassifier classifier;

    @Autowired
    protected MutableClock clock;

    @BeforeEach
    void resetTestDoubles() {
        classifier.reset();
        clock.reset();
    }

    protected long registerUser() throws Exception {
        long telegramUserId = TELEGRAM_IDS.incrementAndGet();
        mvc.perform(put("/api/me")
                .header(HEADER, telegramUserId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Pessoa Teste\"}"))
            .andExpect(status().isCreated());
        return telegramUserId;
    }

    protected ResultActions getAs(long user, String url) throws Exception {
        return mvc.perform(get(url).header(HEADER, user));
    }

    protected ResultActions postAs(long user, String url, String json) throws Exception {
        return mvc.perform(post(url).header(HEADER, user).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    protected ResultActions putAs(long user, String url, String json) throws Exception {
        return mvc.perform(put(url).header(HEADER, user).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    protected ResultActions deleteAs(long user, String url) throws Exception {
        return mvc.perform(delete(url).header(HEADER, user));
    }

    protected JsonNode body(ResultActions result) throws Exception {
        return objectMapper.readTree(result.andReturn().getResponse().getContentAsString());
    }

    protected String categoryId(long user, String name) throws Exception {
        for (JsonNode category : body(getAs(user, "/api/expense-categories?includeArchived=true"))) {
            if (category.path("name").asText().equals(name) && !category.path("archived").asBoolean()) {
                return category.path("id").asText();
            }
        }
        throw new IllegalArgumentException("Categoria não encontrada: " + name);
    }

    protected String incomeCategoryId(long user, String name) throws Exception {
        for (JsonNode category : body(getAs(user, "/api/income-categories"))) {
            if (category.path("name").asText().equals(name)) {
                return category.path("id").asText();
            }
        }
        throw new IllegalArgumentException("Categoria de renda não encontrada: " + name);
    }

    protected String createCard(long user, String name, int closingDay, int dueDay) throws Exception {
        String json = "{\"name\":\"%s\",\"closingDay\":%d,\"dueDay\":%d}".formatted(name, closingDay, dueDay);
        return body(postAs(user, "/api/credit-cards", json).andExpect(status().isCreated())).path("id").asText();
    }
}
