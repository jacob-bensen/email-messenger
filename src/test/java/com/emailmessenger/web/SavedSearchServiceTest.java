package com.emailmessenger.web;

import com.emailmessenger.auth.UserService;
import com.emailmessenger.billing.PlanLimitService;
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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class SavedSearchServiceTest {

    @Autowired SavedSearchService savedSearchService;
    @Autowired SavedSearchRepository repository;
    @Autowired UserService userService;
    @Autowired UserRepository users;
    @Autowired SubscriptionRepository subscriptions;
    @Autowired PlanLimitService planLimitService;

    @MockitoBean StripeCheckoutGateway gateway;
    @MockitoBean StripePortalGateway portalGateway;
    @MockitoBean ReplyService replyService;

    private User newUser(String email) {
        userService.register(email, "password1", "Test");
        return users.findByEmail(email).orElseThrow();
    }

    private void grantPersonal(User user) {
        Subscription sub = new Subscription(user, "cus_" + user.getId(), "active");
        sub.setPlan(Plan.PRO);
        subscriptions.save(sub);
    }

    @Test
    void createPersistsSavedSearchAndStripsBlanks() {
        User user = newUser("create@example.com");
        SavedSearch saved = savedSearchService.create(user, "  From Ada  ",
                "  invoice  ", "ada@example.com", "30D", false, true);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("From Ada");
        assertThat(saved.getQuery()).isEqualTo("invoice");
        assertThat(saved.getSenderEmail()).isEqualTo("ada@example.com");
        assertThat(saved.getSincePreset()).isEqualTo("30d");
        assertThat(saved.isRequireUnread()).isFalse();
        assertThat(saved.isRequireAttachments()).isTrue();
    }

    @Test
    void createWithBlankNameThrows() {
        User user = newUser("blank@example.com");

        assertThatThrownBy(() ->
                savedSearchService.create(user, "   ", "invoice", null, null, false, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name is required");
    }

    @Test
    void createWithNoFiltersThrows() {
        User user = newUser("empty@example.com");

        assertThatThrownBy(() ->
                savedSearchService.create(user, "Nothing", null, null, null, false, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least one filter");
    }

    @Test
    void createWithUnknownSincePresetDropsIt() {
        User user = newUser("badpreset@example.com");
        SavedSearch saved = savedSearchService.create(user, "Whatever", "invoice",
                null, "evil_payload", false, false);

        assertThat(saved.getSincePreset()).isNull();
        assertThat(saved.getQuery()).isEqualTo("invoice");
    }

    @Test
    void secondSavedSearchIsAllowedSincePaidFeaturesAreUnlocked() {
        User user = newUser("freecap@example.com");
        savedSearchService.create(user, "First", null, "ada@example.com", null, false, false);

        assertThatCode(() ->
                savedSearchService.create(user, "Second", null, "grace@example.com", null, false, false))
                .doesNotThrowAnyException();

        assertThat(savedSearchService.list(user)).hasSize(2);
    }

    @Test
    void proPlanAllowsManySavedSearches() {
        User user = newUser("paid@example.com");
        grantPersonal(user);

        for (int i = 0; i < 5; i++) {
            savedSearchService.create(user, "Search " + i, "term" + i,
                    null, null, false, false);
        }

        List<SavedSearch> all = savedSearchService.list(user);
        assertThat(all).hasSize(5);
    }

    @Test
    void createDuplicateNameForSameOwnerThrows() {
        User user = newUser("dup@example.com");
        grantPersonal(user);
        savedSearchService.create(user, "From Ada", null, "ada@example.com", null, false, false);

        assertThatThrownBy(() ->
                savedSearchService.create(user, "From Ada", "different", null, null, false, false))
                .isInstanceOf(SavedSearchService.DuplicateSavedSearchNameException.class);
    }

    @Test
    void duplicateCheckHappensBeforeCapCheck() {
        User user = newUser("dupbeforecap@example.com");
        savedSearchService.create(user, "Only one", null, "ada@example.com", null, false, false);
        // Free user with the cap full retrying the same name should hit the
        // duplicate-name error, not the upgrade modal.
        assertThatThrownBy(() ->
                savedSearchService.create(user, "Only one", null, "ada@example.com", null, false, false))
                .isInstanceOf(SavedSearchService.DuplicateSavedSearchNameException.class);
    }

    @Test
    void deleteRemovesEntryAndRefusesCrossUserId() {
        User user = newUser("del@example.com");
        SavedSearch saved = savedSearchService.create(user, "Mine", null,
                "ada@example.com", null, false, false);
        User stranger = newUser("stranger@example.com");

        assertThatThrownBy(() -> savedSearchService.delete(stranger, saved.getId()))
                .isInstanceOf(java.util.NoSuchElementException.class);

        savedSearchService.delete(user, saved.getId());
        assertThat(repository.findById(saved.getId())).isEmpty();
    }

    @Test
    void viewsForReturnsRailViewModels() {
        User user = newUser("views@example.com");
        savedSearchService.create(user, "From Ada", null, "ada@example.com", "7d", true, false);

        List<SavedSearchView> views = savedSearchService.viewsFor(user);
        assertThat(views).hasSize(1);
        SavedSearchView v = views.get(0);
        assertThat(v.name()).isEqualTo("From Ada");
        assertThat(v.senderEmail()).isEqualTo("ada@example.com");
        assertThat(v.sincePreset()).isEqualTo("7d");
        assertThat(v.requireUnread()).isTrue();
        assertThat(v.matches(null, "ada@example.com", "7d", true, false)).isTrue();
        assertThat(v.matches("invoice", "ada@example.com", "7d", true, false)).isFalse();
    }
}
