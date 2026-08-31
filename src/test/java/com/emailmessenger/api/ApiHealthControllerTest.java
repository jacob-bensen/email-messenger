package com.emailmessenger.api;

import com.emailmessenger.billing.BillingService;
import com.emailmessenger.service.ReplyService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ApiHealthControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean BillingService billingService;
    @MockitoBean ReplyService replyService;

    @Test
    void healthEndpointReturnsOkUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", startsWith("application/json")))
                .andExpect(jsonPath("$.status").value("ok"))
                .andExpect(jsonPath("$.apiVersion").value("v1"))
                .andExpect(jsonPath("$.serverTime", notNullValue()));
    }

    @Test
    void wrongMethodOnApiReturnsJson405NotCsrf403() throws Exception {
        // Two things at once: (1) proves /api/** is CSRF-exempt (otherwise
        // this POST would be rejected 403 before the controller layer) and
        // (2) proves ApiExceptionHandler maps the method mismatch to the
        // JSON error shape from docs/API.md, not to the HTML error page
        // the browser-side GlobalExceptionHandler would render.
        mockMvc.perform(post("/api/v1/health"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().string("Content-Type", startsWith("application/json")))
                .andExpect(jsonPath("$.error.code").value("METHOD_NOT_ALLOWED"));
    }
}
