package com.emailmessenger.web;

import com.emailmessenger.repository.WaitlistEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import org.springframework.dao.DataIntegrityViolationException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

class WaitlistControllerTest {

    private MockMvc mockMvc;
    private WaitlistEntryRepository waitlistRepo;

    @BeforeEach
    void setUp() {
        waitlistRepo = mock(WaitlistEntryRepository.class);
        InternalResourceViewResolver resolver = new InternalResourceViewResolver();
        resolver.setPrefix("/WEB-INF/templates/");
        resolver.setSuffix(".html");
        mockMvc = MockMvcBuilders
                .standaloneSetup(new WaitlistController(waitlistRepo))
                .setViewResolvers(resolver)
                .build();
    }

    @Test
    void getWaitlistReturns200AndWaitlistView() throws Exception {
        when(waitlistRepo.count()).thenReturn(5L);

        mockMvc.perform(get("/waitlist"))
                .andExpect(status().isOk())
                .andExpect(view().name("waitlist"))
                .andExpect(model().attributeExists("waitlistForm"))
                .andExpect(model().attribute("waitlistCount", 5L));
    }

    @Test
    void postWithValidEmailSavesAndRedirectsWithJoinedFlag() throws Exception {
        when(waitlistRepo.existsByEmail("new@example.com")).thenReturn(false);

        mockMvc.perform(post("/waitlist")
                        .param("email", "new@example.com"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/waitlist*"))
                .andExpect(flash().attribute("joined", true));

        verify(waitlistRepo).save(any());
    }

    @Test
    void postWithDuplicateEmailRedirectsWithAlreadyJoinedFlag() throws Exception {
        when(waitlistRepo.existsByEmail("existing@example.com")).thenReturn(true);

        mockMvc.perform(post("/waitlist")
                        .param("email", "existing@example.com"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/waitlist*"))
                .andExpect(flash().attribute("alreadyJoined", true));

        verify(waitlistRepo, never()).save(any());
    }

    @Test
    void postWithBlankEmailReturnsFormWithErrors() throws Exception {
        mockMvc.perform(post("/waitlist")
                        .param("email", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("waitlist"))
                .andExpect(model().attributeHasFieldErrors("waitlistForm", "email"));

        verify(waitlistRepo, never()).existsByEmail(anyString());
        verify(waitlistRepo, never()).save(any());
    }

    @Test
    void postWithInvalidEmailReturnsFormWithErrors() throws Exception {
        mockMvc.perform(post("/waitlist")
                        .param("email", "not-an-email"))
                .andExpect(status().isOk())
                .andExpect(view().name("waitlist"))
                .andExpect(model().attributeHasFieldErrors("waitlistForm", "email"));

        verify(waitlistRepo, never()).save(any());
    }

    @Test
    void postTrimsWhitespaceFromEmail() throws Exception {
        when(waitlistRepo.existsByEmail("trimmed@example.com")).thenReturn(false);

        mockMvc.perform(post("/waitlist")
                        .param("email", "  trimmed@example.com  "))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("joined", true));
    }

    @Test
    void postWithInvalidEmailRepopulatesWaitlistCountInModel() throws Exception {
        when(waitlistRepo.count()).thenReturn(17L);

        mockMvc.perform(post("/waitlist")
                        .param("email", "bad-email"))
                .andExpect(status().isOk())
                .andExpect(view().name("waitlist"))
                .andExpect(model().attribute("waitlistCount", 17L));
    }

    @Test
    void postWithConcurrentDuplicateSaveStillReturnsJoinedFlash() throws Exception {
        // existsByEmail returns false (race condition window), but save() throws constraint violation
        when(waitlistRepo.existsByEmail("race@example.com")).thenReturn(false);
        doThrow(new DataIntegrityViolationException("unique constraint")).when(waitlistRepo).save(any());

        mockMvc.perform(post("/waitlist")
                        .param("email", "race@example.com"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/waitlist*"))
                .andExpect(flash().attribute("joined", true));
    }
}
