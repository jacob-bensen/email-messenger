package com.emailmessenger.web;

import com.emailmessenger.service.DemoService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class DemoControllerTest {

    private final DemoService demoService = new DemoService();
    private final MockMvc mvc = MockMvcBuilders
        .standaloneSetup(new DemoController(demoService))
        .setControllerAdvice(new GlobalExceptionHandler())
        .setViewResolvers(new InternalResourceViewResolver("/WEB-INF/templates/", ".html"))
        .build();

    @Test
    void demoListReturns200WithDemoThreads() throws Exception {
        mvc.perform(get("/demo"))
           .andExpect(status().isOk())
           .andExpect(view().name("demo"))
           .andExpect(model().attributeExists("demoThreads"))
           .andExpect(model().attributeExists("today"))
           .andExpect(model().attributeExists("yesterday"));
    }

    @Test
    void demoListContainsTwoSampleThreads() throws Exception {
        mvc.perform(get("/demo"))
           .andExpect(status().isOk())
           .andExpect(model().attribute("demoThreads",
               org.hamcrest.Matchers.hasSize(2)));
    }

    @Test
    void demoConversation1Returns200WithConversationView() throws Exception {
        mvc.perform(get("/demo/1"))
           .andExpect(status().isOk())
           .andExpect(view().name("conversation"))
           .andExpect(model().attribute("isDemo", true))
           .andExpect(model().attributeExists("conversation"));
    }

    @Test
    void demoConversation2Returns200WithConversationView() throws Exception {
        mvc.perform(get("/demo/2"))
           .andExpect(status().isOk())
           .andExpect(view().name("conversation"))
           .andExpect(model().attribute("isDemo", true))
           .andExpect(model().attributeExists("conversation"));
    }

    @Test
    void demoConversationUnknownIdReturns404() throws Exception {
        mvc.perform(get("/demo/99"))
           .andExpect(status().isNotFound())
           .andExpect(view().name("error"));
    }
}
