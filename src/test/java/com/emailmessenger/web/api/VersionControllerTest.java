package com.emailmessenger.web.api;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class VersionControllerTest {

    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new VersionController())
            .build();

    @Test
    void returnsAppMetadataAsJson() throws Exception {
        mockMvc.perform(get("/api/v1/version"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.app").value(VersionController.APP))
                .andExpect(jsonPath("$.apiVersion").value(VersionController.API_VERSION))
                .andExpect(jsonPath("$.buildVersion").value(VersionController.BUILD_VERSION))
                // serverTime is a UTC ISO-8601 instant; loose regex is enough for a contract test.
                .andExpect(jsonPath("$.serverTime").isNotEmpty());
    }
}
