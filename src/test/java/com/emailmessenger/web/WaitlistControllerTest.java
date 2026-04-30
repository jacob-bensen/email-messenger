package com.emailmessenger.web;

import com.emailmessenger.domain.WaitlistEntry;
import com.emailmessenger.repository.WaitlistEntryRepository;
import com.emailmessenger.service.WaitlistReferralService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import java.lang.reflect.Field;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

class WaitlistControllerTest {

    private static final String BASE_URL = "https://test.example";

    private MockMvc mockMvc;
    private WaitlistEntryRepository waitlistRepo;
    private WaitlistReferralService referralService;

    @BeforeEach
    void setUp() {
        waitlistRepo = mock(WaitlistEntryRepository.class);
        referralService = mock(WaitlistReferralService.class);
        InternalResourceViewResolver resolver = new InternalResourceViewResolver();
        resolver.setPrefix("/WEB-INF/templates/");
        resolver.setSuffix(".html");
        mockMvc = MockMvcBuilders
                .standaloneSetup(new WaitlistController(waitlistRepo, referralService, BASE_URL))
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
                .andExpect(model().attribute("waitlistCount", 5L))
                .andExpect(model().attribute("baseUrl", BASE_URL));
    }

    @Test
    void getWaitlistWithRefParamPrefillsHiddenField() throws Exception {
        when(waitlistRepo.count()).thenReturn(0L);

        var result = mockMvc.perform(get("/waitlist").param("ref", "abc-123"))
                .andExpect(status().isOk())
                .andReturn();

        var form = result.getModelAndView().getModel().get("waitlistForm");
        assertThat(form).isInstanceOfSatisfying(WaitlistForm.class,
                f -> assertThat(f.getRef()).isEqualTo("abc-123"));
    }

    @Test
    void postWithValidEmailSavesAndRedirectsWithJoinedFlag() throws Exception {
        when(waitlistRepo.existsByEmail("new@example.com")).thenReturn(false);
        WaitlistEntry saved = entryWithToken("new@example.com", "tok-new", 0L);
        when(waitlistRepo.save(any())).thenReturn(saved);
        when(waitlistRepo.findByEmail("new@example.com")).thenReturn(Optional.of(saved));
        when(referralService.effectivePosition(saved)).thenReturn(7L);

        mockMvc.perform(post("/waitlist")
                        .param("email", "new@example.com"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/waitlist*"))
                .andExpect(flash().attribute("joined", true))
                .andExpect(flash().attribute("referralUrl", BASE_URL + "/waitlist?ref=tok-new"))
                .andExpect(flash().attribute("position", 7L))
                .andExpect(flash().attribute("referralsCount", 0));

        verify(waitlistRepo).save(any());
    }

    @Test
    void postWithRefCreditsReferrerService() throws Exception {
        when(waitlistRepo.existsByEmail("new@example.com")).thenReturn(false);
        WaitlistEntry saved = entryWithToken("new@example.com", "tok-new", 0L);
        when(waitlistRepo.save(any())).thenReturn(saved);
        when(waitlistRepo.findByEmail("new@example.com")).thenReturn(Optional.of(saved));

        mockMvc.perform(post("/waitlist")
                        .param("email", "new@example.com")
                        .param("ref", "referrer-token"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("joined", true));

        verify(referralService).creditReferrer("referrer-token", "new@example.com");
    }

    @Test
    void postWithoutRefStillCallsCreditReferrerWithEmptyString() throws Exception {
        when(waitlistRepo.existsByEmail("new@example.com")).thenReturn(false);
        when(waitlistRepo.save(any())).thenReturn(entryWithToken("new@example.com", "t", 0L));
        when(waitlistRepo.findByEmail(anyString())).thenReturn(Optional.empty());

        mockMvc.perform(post("/waitlist").param("email", "new@example.com"))
                .andExpect(status().is3xxRedirection());

        verify(referralService).creditReferrer("", "new@example.com");
    }

    @Test
    void postWithDuplicateEmailRedirectsWithAlreadyJoinedFlagAndSharesExistingToken() throws Exception {
        WaitlistEntry existing = entryWithToken("existing@example.com", "tok-old", 1L);
        when(waitlistRepo.existsByEmail("existing@example.com")).thenReturn(true);
        when(waitlistRepo.findByEmail("existing@example.com")).thenReturn(Optional.of(existing));
        when(referralService.effectivePosition(existing)).thenReturn(3L);

        mockMvc.perform(post("/waitlist")
                        .param("email", "existing@example.com"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/waitlist*"))
                .andExpect(flash().attribute("alreadyJoined", true))
                .andExpect(flash().attribute("referralUrl", BASE_URL + "/waitlist?ref=tok-old"))
                .andExpect(flash().attribute("position", 3L))
                .andExpect(flash().attribute("referralsCount", 0));

        verify(waitlistRepo, never()).save(any());
    }

    @Test
    void postWithBlankEmailReturnsFormWithErrors() throws Exception {
        mockMvc.perform(post("/waitlist")
                        .param("email", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("waitlist"))
                .andExpect(model().attributeHasFieldErrors("waitlistForm", "email"))
                .andExpect(model().attribute("baseUrl", BASE_URL));

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
        when(waitlistRepo.save(any())).thenReturn(entryWithToken("trimmed@example.com", "t", 0L));
        when(waitlistRepo.findByEmail("trimmed@example.com")).thenReturn(Optional.empty());

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
                .andExpect(model().attribute("waitlistCount", 17L))
                .andExpect(model().attribute("baseUrl", BASE_URL));
    }

    @Test
    void postWithConcurrentDuplicateSaveStillReturnsJoinedFlash() throws Exception {
        when(waitlistRepo.existsByEmail("race@example.com")).thenReturn(false);
        doThrow(new DataIntegrityViolationException("unique constraint")).when(waitlistRepo).save(any());
        when(waitlistRepo.findByEmail("race@example.com")).thenReturn(Optional.empty());

        mockMvc.perform(post("/waitlist")
                        .param("email", "race@example.com"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/waitlist*"))
                .andExpect(flash().attribute("joined", true));
    }

    @Test
    void baseUrlConfigStripsTrailingSlash() {
        WaitlistController c = new WaitlistController(waitlistRepo, referralService,
                "https://test.example/");
        // Indirect check: post and verify the share URL has no double slash.
        WaitlistEntry saved = entryWithToken("a@b.com", "tok", 0L);
        when(waitlistRepo.existsByEmail("a@b.com")).thenReturn(false);
        when(waitlistRepo.save(any())).thenReturn(saved);
        when(waitlistRepo.findByEmail("a@b.com")).thenReturn(Optional.of(saved));

        mockMvc = MockMvcBuilders.standaloneSetup(c).build();

        try {
            mockMvc.perform(post("/waitlist").param("email", "a@b.com"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(flash().attribute("referralUrl", "https://test.example/waitlist?ref=tok"));
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    private static WaitlistEntry entryWithToken(String email, String token, long id) {
        WaitlistEntry e = new WaitlistEntry(email);
        try {
            Field idField = WaitlistEntry.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(e, id);
            Field tokenField = WaitlistEntry.class.getDeclaredField("referralToken");
            tokenField.setAccessible(true);
            tokenField.set(e, token);
        } catch (ReflectiveOperationException ex) {
            throw new RuntimeException(ex);
        }
        return e;
    }
}
