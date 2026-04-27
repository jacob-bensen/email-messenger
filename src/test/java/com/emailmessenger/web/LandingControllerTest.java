package com.emailmessenger.web;

import com.emailmessenger.repository.WaitlistEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@ExtendWith(MockitoExtension.class)
class LandingControllerTest {

    @Mock WaitlistEntryRepository waitlistEntryRepository;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LandingController controller = new LandingController(waitlistEntryRepository);
        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/WEB-INF/templates/");
        viewResolver.setSuffix(".html");
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setViewResolvers(viewResolver)
                .build();
    }

    @Test
    void rootReturnsIndexView() throws Exception {
        when(waitlistEntryRepository.count()).thenReturn(0L);

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"));
    }

    @Test
    void rootExposesWaitlistCountInModel() throws Exception {
        when(waitlistEntryRepository.count()).thenReturn(42L);

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("waitlistCount", 42L));
    }

    @Test
    void rootWithZeroWaitlistCountStillSucceeds() throws Exception {
        when(waitlistEntryRepository.count()).thenReturn(0L);

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("waitlistCount", 0L));
    }
}
