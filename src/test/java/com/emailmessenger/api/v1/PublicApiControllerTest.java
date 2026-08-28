package com.emailmessenger.api.v1;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class PublicApiControllerTest {

    @Autowired MockMvc mockMvc;

    @Test
    void healthReturnsOkJsonUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(content -> {
                    String contentType = content.getResponse().getContentType();
                    if (contentType == null || !contentType.startsWith(MediaType.APPLICATION_JSON_VALUE)) {
                        throw new AssertionError("Expected JSON response, got: " + contentType);
                    }
                })
                .andExpect(jsonPath("$.status").value("ok"))
                .andExpect(jsonPath("$.version").exists())
                .andExpect(jsonPath("$.serverTime").exists());
    }
}
