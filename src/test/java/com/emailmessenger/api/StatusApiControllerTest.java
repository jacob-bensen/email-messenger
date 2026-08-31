package com.emailmessenger.api;

import static org.hamcrest.Matchers.matchesRegex;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;

/**
 * Slice would be nice, but @WebMvcTest pulls in the whole
 * SecurityConfig graph (which needs the JDBC token repository and a
 * DataSource), so we run under the full context. Fast enough for
 * one endpoint and keeps the permitAll wiring in the loop.
 */
@SpringBootTest
@TestPropertySource(properties = {
        "mailbox.polling.enabled=false",
        "mailbox.idle.enabled=false"
})
class StatusApiControllerTest {

    private final MockMvc mvc;

    @Autowired
    StatusApiControllerTest(WebApplicationContext ctx) {
        this.mvc = MockMvcBuilders.webAppContextSetup(ctx)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    @Test
    void statusIsPublicAndReturnsContract() throws Exception {
        mvc.perform(get("/api/v1/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.service").value("email-messenger"))
                .andExpect(jsonPath("$.apiVersion").value("v1"))
                .andExpect(jsonPath("$.status").value("ok"))
                // ISO-8601 instant, e.g. 2026-08-31T14:22:31.123456789Z
                .andExpect(jsonPath("$.serverTime").value(
                        matchesRegex("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?Z")));
    }
}
