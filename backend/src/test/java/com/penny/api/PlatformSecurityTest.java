package com.penny.api;

import com.penny.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PlatformSecurityTest extends IntegrationTest {

    private static final String PUBLIC_TABLES =
        "select c.relname from pg_class c join pg_namespace n on n.oid = c.relnamespace "
            + "where n.nspname = 'public' and c.relkind in ('r', 'p')";

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void everyTableHasRowLevelSecurityEnabled() {
        List<String> tables = jdbc.queryForList(PUBLIC_TABLES, String.class);
        List<String> withoutRowLevelSecurity = jdbc.queryForList(PUBLIC_TABLES + " and not c.relrowsecurity", String.class);

        assertThat(tables).contains("users", "expenses", "flyway_schema_history").hasSizeGreaterThanOrEqualTo(11);
        assertThat(withoutRowLevelSecurity).isEmpty();
    }

    @Test
    void onlyTheHealthEndpointIsExposedAndItHidesDetails() throws Exception {
        mvc.perform(get("/actuator/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP"))
            .andExpect(jsonPath("$.components").doesNotExist())
            .andExpect(jsonPath("$.details").doesNotExist());

        for (String endpoint : List.of("env", "beans", "configprops", "mappings", "heapdump", "threaddump", "loggers", "metrics", "info")) {
            mvc.perform(get("/actuator/" + endpoint)).andExpect(status().isNotFound());
        }
    }

    @Test
    void browsersOnOtherSitesCannotCallTheApi() throws Exception {
        long user = registerUser();

        mvc.perform(options("/api/expenses")
                .header("Origin", "https://site-malicioso.example")
                .header("Access-Control-Request-Method", "GET")
                .header("Access-Control-Request-Headers", HEADER))
            .andExpect(status().isForbidden())
            .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
        mvc.perform(get("/api/expenses").header(HEADER, user).header("Origin", "https://site-malicioso.example"))
            .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }
}
