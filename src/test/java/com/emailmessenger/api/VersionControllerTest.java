package com.emailmessenger.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class VersionControllerTest {

    @Autowired MockMvc mockMvc;

    @Test
    void getVersionIsPublicAndMatchesDocumentedShape() throws Exception {
        mockMvc.perform(get("/api/version"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.app").value("conexus-mail"))
                .andExpect(jsonPath("$.version").isString())
                .andExpect(jsonPath("$.api").value(VersionController.API_CONTRACT_VERSION))
                .andExpect(jsonPath("$.serverTime").isString());
    }
}
