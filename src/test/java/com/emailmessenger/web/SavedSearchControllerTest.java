package com.emailmessenger.web;

import com.emailmessenger.auth.UserService;
import com.emailmessenger.billing.StripeCheckoutGateway;
import com.emailmessenger.billing.StripePortalGateway;
import com.emailmessenger.domain.Plan;
import com.emailmessenger.domain.SavedSearch;
import com.emailmessenger.domain.Subscription;
import com.emailmessenger.domain.User;
import com.emailmessenger.repository.SavedSearchRepository;
import com.emailmessenger.repository.SubscriptionRepository;
import com.emailmessenger.repository.UserRepository;
import com.emailmessenger.service.ReplyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Transactional
class SavedSearchControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired UserService userService;
    @Autowired UserRepository userRepository;
    @Autowired SavedSearchRepository savedSearchRepository;
    @Autowired SubscriptionRepository subscriptions;

    @MockBean StripeCheckoutGateway gateway;
    @MockBean StripePortalGateway portalGateway;
    @MockBean ReplyService replyService;

    private User owner;

    @BeforeEach
    void setUp() {
        userService.register("saver@example.com", "password1", "Saver");
        owner = userRepository.findByEmail("saver@example.com").orElseThrow();
    }

    @Test
    void anonymousPostIsRedirectedToLogin() throws Exception {
        mockMvc.perform(post("/searches").with(csrf())
                        .param("name", "From Ada")
                        .param("from", "ada@example.com"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void postWithoutCsrfReturnsForbidden() throws Exception {
        mockMvc.perform(post("/searches").with(user("saver@example.com"))
                        .param("name", "From Ada")
                        .param("from", "ada@example.com"))
                .andExpect(status().isForbidden());
    }

    @Test
    void savePersistsAndRedirectsBackPreservingQueryString() throws Exception {
        mockMvc.perform(post("/searches")
                        .with(user("saver@example.com")).with(csrf())
                        .param("name", "Invoices from Ada")
                        .param("q", "invoice")
                        .param("from", "ada@example.com")
                        .param("since", "30d")
                        .param("unread", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(
                        "/threads?q=invoice&from=ada@example.com&since=30d&unread=true"))
                .andExpect(flash().attributeExists("savedSearchMessage"));

        List<SavedSearch> saved = savedSearchRepository.findByOwnerOrderByCreatedAtAsc(owner);
        assertThat(saved).hasSize(1);
        SavedSearch s = saved.get(0);
        assertThat(s.getName()).isEqualTo("Invoices from Ada");
        assertThat(s.getQuery()).isEqualTo("invoice");
        assertThat(s.getSenderEmail()).isEqualTo("ada@example.com");
        assertThat(s.getSincePreset()).isEqualTo("30d");
        assertThat(s.isRequireUnread()).isTrue();
        assertThat(s.isRequireAttachments()).isFalse();
    }

    @Test
    void freeUserSavingSecondSearchSeesUpgradeModalFlash() throws Exception {
        savedSearchRepository.save(new SavedSearch(owner, "First", null,
                "first@example.com", null, false, false));

        // PlanLimitExceededException propagates to GlobalExceptionHandler which
        // redirects to /threads with the upgradeModal flash attribute set.
        mockMvc.perform(post("/searches")
                        .with(user("saver@example.com")).with(csrf())
                        .param("name", "Second")
                        .param("from", "second@example.com"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/threads"))
                .andExpect(flash().attributeExists("upgradeModal"));

        assertThat(savedSearchRepository.countByOwner(owner)).isEqualTo(1);
    }

    @Test
    void personalUserCanSaveMultiple() throws Exception {
        Subscription sub = new Subscription(owner, "cus_x", "active");
        sub.setPlan(Plan.PERSONAL);
        subscriptions.save(sub);
        savedSearchRepository.save(new SavedSearch(owner, "First", null,
                "first@example.com", null, false, false));

        mockMvc.perform(post("/searches")
                        .with(user("saver@example.com")).with(csrf())
                        .param("name", "Second")
                        .param("from", "second@example.com"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("savedSearchMessage"));

        assertThat(savedSearchRepository.countByOwner(owner)).isEqualTo(2);
    }

    @Test
    void duplicateNameFlashesErrorWithoutCreating() throws Exception {
        savedSearchRepository.save(new SavedSearch(owner, "Duplicate", null,
                "ada@example.com", null, false, false));

        mockMvc.perform(post("/searches")
                        .with(user("saver@example.com")).with(csrf())
                        .param("name", "Duplicate")
                        .param("from", "different@example.com"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("savedSearchError"));

        assertThat(savedSearchRepository.countByOwner(owner)).isEqualTo(1);
    }

    @Test
    void emptyFiltersFlashesErrorWithoutCreating() throws Exception {
        mockMvc.perform(post("/searches")
                        .with(user("saver@example.com")).with(csrf())
                        .param("name", "Empty"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("savedSearchError"));

        assertThat(savedSearchRepository.countByOwner(owner)).isZero();
    }

    @Test
    void deleteRemovesEntryAndRedirectsBack() throws Exception {
        SavedSearch s = savedSearchRepository.save(new SavedSearch(owner, "Mine", null,
                "ada@example.com", "7d", false, false));

        mockMvc.perform(post("/searches/" + s.getId() + "/delete")
                        .with(user("saver@example.com")).with(csrf())
                        .param("q", "invoice"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/threads?q=invoice"))
                .andExpect(flash().attributeExists("savedSearchMessage"));

        assertThat(savedSearchRepository.findById(s.getId())).isEmpty();
    }

    @Test
    void deleteAcrossUsersReturns404() throws Exception {
        userService.register("stranger@example.com", "password1", "Stranger");
        User stranger = userRepository.findByEmail("stranger@example.com").orElseThrow();
        SavedSearch theirs = savedSearchRepository.save(new SavedSearch(stranger, "Theirs",
                null, "x@example.com", null, false, false));

        mockMvc.perform(post("/searches/" + theirs.getId() + "/delete")
                        .with(user("saver@example.com")).with(csrf()))
                .andExpect(status().isNotFound());

        assertThat(savedSearchRepository.findById(theirs.getId())).isPresent();
    }
}
