package com.emailmessenger.web;

import com.emailmessenger.auth.UserActivityService;
import com.emailmessenger.auth.UserService;
import com.emailmessenger.billing.BillingBanner;
import com.emailmessenger.billing.BillingBannerService;
import com.emailmessenger.billing.BillingService;
import com.emailmessenger.billing.PlanLimitKind;
import com.emailmessenger.billing.PlanLimitService;
import com.emailmessenger.billing.TrialConversionNudge;
import com.emailmessenger.billing.TrialConversionNudgeService;
import com.emailmessenger.billing.UpgradeModal;
import com.emailmessenger.domain.EmailThread;
import com.emailmessenger.domain.Plan;
import com.emailmessenger.domain.User;
import com.emailmessenger.repository.EmailThreadRepository;
import com.emailmessenger.domain.ThreadNote;
import com.emailmessenger.service.Conversation;
import com.emailmessenger.service.ReplyService;
import com.emailmessenger.team.NoteMentionService;
import com.emailmessenger.team.ThreadAccessService;
import com.emailmessenger.team.ThreadNoteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import java.security.Principal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@ExtendWith(MockitoExtension.class)
class ThreadControllerTest {

    @Mock EmailThreadRepository threadRepository;
    @Mock ThreadViewService threadViewService;
    @Mock ReplyService replyService;
    @Mock UserService userService;
    @Mock BillingBannerService billingBannerService;
    @Mock BillingService billingService;
    @Mock OnboardingService onboardingService;
    @Mock PlanLimitService planLimitService;
    @Mock TrialConversionNudgeService trialConversionNudgeService;
    @Mock ThreadSearchService threadSearchService;
    @Mock SenderGroupService senderGroupService;
    @Mock SavedSearchService savedSearchService;
    @Mock UserActivityService userActivityService;
    @Mock ThreadNoteService threadNoteService;
    @Mock ThreadAccessService threadAccessService;
    @Mock NoteMentionService noteMentionService;

    MockMvc mockMvc;

    private final User owner = new User("owner@example.com", "hash", "Owner");
    private final Principal principal = () -> "owner@example.com";

    private static final Clock CLOCK =
            Clock.fixed(Instant.parse("2026-06-06T12:00:00Z"), ZoneOffset.UTC);

    @BeforeEach
    void setUp() {
        ThreadController controller = new ThreadController(
                threadRepository, threadViewService, replyService, userService,
                billingBannerService, billingService, onboardingService,
                planLimitService, trialConversionNudgeService, threadSearchService,
                senderGroupService, savedSearchService, userActivityService,
                threadNoteService, threadAccessService, noteMentionService, CLOCK);
        lenient().when(userService.requireByEmail("owner@example.com")).thenReturn(owner);
        lenient().when(billingBannerService.bannerFor(owner)).thenReturn(Optional.empty());
        lenient().when(onboardingService.checklistFor(owner))
                .thenReturn(new OnboardingChecklist(false, 0L, false, false));
        // Default the plan to PERSONAL so the existing checklist scenarios don't
        // accidentally surface a nudge unless a test opts in to FREE.
        lenient().when(planLimitService.currentPlan(owner)).thenReturn(Plan.PERSONAL);
        lenient().when(trialConversionNudgeService.nudgeFor(owner)).thenReturn(Optional.empty());
        lenient().when(senderGroupService.topSenders(owner)).thenReturn(List.of());
        lenient().when(savedSearchService.viewsFor(owner)).thenReturn(List.of());
        lenient().when(threadNoteService.canAccessNotes(owner)).thenReturn(false);
        lenient().when(threadNoteService.canAccessNotesOn(any(EmailThread.class), eq(owner))).thenReturn(false);
        lenient().when(threadNoteService.notesFor(any(EmailThread.class), eq(owner))).thenReturn(List.of());
        lenient().when(noteMentionService.candidatesForThread(any(EmailThread.class), eq(owner)))
                .thenReturn(List.of());
        // By default the principal IS the owner of any thread the controller renders;
        // teammate-viewer tests opt in by overriding this stub.
        lenient().when(threadAccessService.isOwner(any(EmailThread.class), eq(owner))).thenReturn(true);
        // Prefix/suffix prevents InternalResourceViewResolver from producing a path that
        // matches the request URL (which would cause a circular-dispatch error in standalone mode).
        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/WEB-INF/templates/");
        viewResolver.setSuffix(".html");
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setViewResolvers(viewResolver)
                .build();
    }

    @Test
    void listThreadsReturnsThreadsViewWithModel() throws Exception {
        Page<EmailThread> empty = new PageImpl<>(List.of());
        when(threadRepository.findByOwnerOrderByUpdatedAtDesc(eq(owner), any(Pageable.class)))
                .thenReturn(empty);

        mockMvc.perform(get("/threads").principal(principal))
                .andExpect(status().isOk())
                .andExpect(view().name("threads"))
                .andExpect(model().attributeExists("threads"));
    }

    @Test
    void listThreadsRecordsInboxVisitForOwner() throws Exception {
        Page<EmailThread> empty = new PageImpl<>(List.of());
        when(threadRepository.findByOwnerOrderByUpdatedAtDesc(eq(owner), any(Pageable.class)))
                .thenReturn(empty);

        mockMvc.perform(get("/threads").principal(principal))
                .andExpect(status().isOk());

        verify(userActivityService).recordInboxVisit(owner);
    }

    @Test
    void listThreadsNegativePageClampsToZero() throws Exception {
        Page<EmailThread> empty = new PageImpl<>(List.of());
        when(threadRepository.findByOwnerOrderByUpdatedAtDesc(eq(owner), any(Pageable.class)))
                .thenReturn(empty);

        mockMvc.perform(get("/threads").principal(principal).param("page", "-5"))
                .andExpect(status().isOk())
                .andExpect(view().name("threads"));
    }

    @Test
    void viewConversationReturnsConversationView() throws Exception {
        EmailThread thread = new EmailThread(owner, "Test Subject", "<root@test>");
        Conversation conv = new Conversation(thread, List.of());
        when(threadViewService.getConversation(1L, owner)).thenReturn(conv);

        mockMvc.perform(get("/threads/1").principal(principal))
                .andExpect(status().isOk())
                .andExpect(view().name("conversation"))
                .andExpect(model().attributeExists("conversation", "replyForm"));
    }

    @Test
    void viewConversationWithUnknownIdReturns404() throws Exception {
        when(threadViewService.getConversation(999L, owner)).thenThrow(new NoSuchElementException());

        mockMvc.perform(get("/threads/999").principal(principal))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error"));
    }

    @Test
    void replyWithEmptyBodyShowsValidationErrorAndConversationView() throws Exception {
        EmailThread thread = new EmailThread(owner, "Test", "<root@test>");
        Conversation conv = new Conversation(thread, List.of());
        when(threadViewService.getConversation(1L, owner)).thenReturn(conv);

        mockMvc.perform(post("/threads/1/reply").principal(principal).param("body", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("conversation"))
                .andExpect(model().attributeHasFieldErrors("replyForm", "body"));

        verify(replyService, never()).sendReply(anyLong(), anyString(), anyString());
    }

    @Test
    void replyWithValidBodyRedirectsWithSuccessFlash() throws Exception {
        EmailThread thread = new EmailThread(owner, "Test Subject", "<root@test>");
        when(threadRepository.findByIdAndOwner(1L, owner)).thenReturn(Optional.of(thread));
        doNothing().when(replyService).sendReply(anyLong(), anyString(), anyString());

        mockMvc.perform(post("/threads/1/reply").principal(principal)
                        .param("body", "Thanks for your message!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/threads/1"));
    }

    @Test
    void replyToOtherUsersThreadReturns404() throws Exception {
        when(threadRepository.findByIdAndOwner(42L, owner)).thenReturn(Optional.empty());

        mockMvc.perform(post("/threads/42/reply").principal(principal)
                        .param("body", "Trying to reply to someone else's thread"))
                .andExpect(status().isNotFound());

        verify(replyService, never()).sendReply(anyLong(), anyString(), anyString());
    }

    @Test
    void trialingUserSeesBannerOnInbox() throws Exception {
        Page<EmailThread> empty = new PageImpl<>(List.of());
        when(threadRepository.findByOwnerOrderByUpdatedAtDesc(eq(owner), any(Pageable.class)))
                .thenReturn(empty);
        when(billingBannerService.bannerFor(owner))
                .thenReturn(Optional.of(BillingBanner.trialEnding(7)));

        mockMvc.perform(get("/threads").principal(principal))
                .andExpect(status().isOk())
                .andExpect(view().name("threads"))
                .andExpect(model().attribute("billingBanner",
                        new BillingBanner(BillingBanner.Kind.TRIAL_ENDING, 7, null)))
                .andExpect(model().attributeExists("threads"));
    }

    @Test
    void canceledUserGetsLockoutWithoutLoadingThreads() throws Exception {
        when(billingBannerService.bannerFor(owner))
                .thenReturn(Optional.of(BillingBanner.subscriptionEnded()));

        mockMvc.perform(get("/threads").principal(principal))
                .andExpect(status().isOk())
                .andExpect(view().name("threads"))
                .andExpect(model().attribute("billingBanner", notNullValue()))
                .andExpect(model().attribute("threads", nullValue()));

        verify(threadRepository, never())
                .findByOwnerOrderByUpdatedAtDesc(any(User.class), any(Pageable.class));
    }

    @Test
    void upgradeModalFlashAttributeIsRenderedOnInbox() throws Exception {
        Page<EmailThread> empty = new PageImpl<>(List.of());
        when(threadRepository.findByOwnerOrderByUpdatedAtDesc(eq(owner), any(Pageable.class)))
                .thenReturn(empty);
        UpgradeModal modal = new UpgradeModal(Plan.FREE, PlanLimitKind.THREAD_COUNT,
                500L, 500L, Plan.PERSONAL, "$9", "$7", "$84");

        mockMvc.perform(get("/threads").principal(principal).flashAttr("upgradeModal", modal))
                .andExpect(status().isOk())
                .andExpect(view().name("threads"))
                .andExpect(model().attribute("upgradeModal", modal));
    }

    @Test
    void emptyInboxExposesOnboardingChecklist() throws Exception {
        Page<EmailThread> empty = new PageImpl<>(List.of());
        when(threadRepository.findByOwnerOrderByUpdatedAtDesc(eq(owner), any(Pageable.class)))
                .thenReturn(empty);
        OnboardingChecklist checklist = new OnboardingChecklist(false, 0L, false, false);
        when(onboardingService.checklistFor(owner)).thenReturn(checklist);

        mockMvc.perform(get("/threads").principal(principal))
                .andExpect(status().isOk())
                .andExpect(view().name("threads"))
                .andExpect(model().attribute("onboarding", checklist));
    }

    @Test
    void nonEmptyInboxStillExposesOnboardingChecklistWhileIncomplete() throws Exception {
        EmailThread thread = new EmailThread(owner, "Hello", "<a@b>");
        Page<EmailThread> populated = new PageImpl<>(List.of(thread));
        when(threadRepository.findByOwnerOrderByUpdatedAtDesc(eq(owner), any(Pageable.class)))
                .thenReturn(populated);
        OnboardingChecklist incomplete = new OnboardingChecklist(true, 1L, false, false);
        when(onboardingService.checklistFor(owner)).thenReturn(incomplete);

        mockMvc.perform(get("/threads").principal(principal))
                .andExpect(status().isOk())
                .andExpect(view().name("threads"))
                .andExpect(model().attribute("onboarding", incomplete));
    }

    @Test
    void completedChecklistIsNotExposedSoProgressBarDisappears() throws Exception {
        EmailThread thread = new EmailThread(owner, "Hello", "<a@b>");
        Page<EmailThread> populated = new PageImpl<>(List.of(thread));
        when(threadRepository.findByOwnerOrderByUpdatedAtDesc(eq(owner), any(Pageable.class)))
                .thenReturn(populated);
        OnboardingChecklist done = new OnboardingChecklist(true, 25L, true, true);
        when(onboardingService.checklistFor(owner)).thenReturn(done);

        mockMvc.perform(get("/threads").principal(principal))
                .andExpect(status().isOk())
                .andExpect(view().name("threads"))
                .andExpect(model().attribute("onboarding", nullValue()));
    }

    @Test
    void freeUserPastTenThreadsExposesPersonalUpgradeNudge() throws Exception {
        EmailThread thread = new EmailThread(owner, "Hello", "<a@b>");
        Page<EmailThread> populated = new PageImpl<>(List.of(thread));
        when(threadRepository.findByOwnerOrderByUpdatedAtDesc(eq(owner), any(Pageable.class)))
                .thenReturn(populated);
        OnboardingChecklist checklist = new OnboardingChecklist(true, 12L, false, false);
        when(onboardingService.checklistFor(owner)).thenReturn(checklist);
        when(planLimitService.currentPlan(owner)).thenReturn(Plan.FREE);

        mockMvc.perform(get("/threads").principal(principal))
                .andExpect(status().isOk())
                .andExpect(view().name("threads"))
                .andExpect(model().attribute("onboarding", checklist))
                .andExpect(model().attribute("onboardingNudge", notNullValue()));
    }

    @Test
    void freeUserAfterSavedSearchSurfacesPersonalSavedSearchNudge() throws Exception {
        EmailThread thread = new EmailThread(owner, "Hello", "<a@b>");
        Page<EmailThread> populated = new PageImpl<>(List.of(thread));
        when(threadRepository.findByOwnerOrderByUpdatedAtDesc(eq(owner), any(Pageable.class)))
                .thenReturn(populated);
        OnboardingChecklist checklist = new OnboardingChecklist(true, 12L, true, false);
        when(onboardingService.checklistFor(owner)).thenReturn(checklist);
        when(planLimitService.currentPlan(owner)).thenReturn(Plan.FREE);

        org.springframework.test.web.servlet.MvcResult result = mockMvc.perform(get("/threads").principal(principal))
                .andExpect(status().isOk())
                .andExpect(model().attribute("onboardingNudge", notNullValue()))
                .andReturn();
        OnboardingNudge nudge = (OnboardingNudge) result.getModelAndView()
                .getModel().get("onboardingNudge");
        org.assertj.core.api.Assertions.assertThat(nudge.trigger()).isEqualTo("step3");
        org.assertj.core.api.Assertions.assertThat(nudge.upgradeTarget()).isEqualTo(Plan.PERSONAL);
    }

    @Test
    void freeUserAfterTeammateInvitedSurfacesTeamPlanNudgeEvenWhenChecklistComplete() throws Exception {
        EmailThread thread = new EmailThread(owner, "Hello", "<a@b>");
        Page<EmailThread> populated = new PageImpl<>(List.of(thread));
        when(threadRepository.findByOwnerOrderByUpdatedAtDesc(eq(owner), any(Pageable.class)))
                .thenReturn(populated);
        // All 4 steps complete — `onboarding` is NOT exposed, but the Team
        // nudge keeps the card visible so the Free user sees the upsell at
        // the moment they've just felt the sharing-inbox feature gap.
        OnboardingChecklist done = new OnboardingChecklist(true, 25L, true, true);
        when(onboardingService.checklistFor(owner)).thenReturn(done);
        when(planLimitService.currentPlan(owner)).thenReturn(Plan.FREE);

        org.springframework.test.web.servlet.MvcResult result = mockMvc.perform(get("/threads").principal(principal))
                .andExpect(status().isOk())
                .andExpect(model().attribute("onboarding", nullValue()))
                .andExpect(model().attribute("onboardingNudge", notNullValue()))
                .andReturn();
        OnboardingNudge nudge = (OnboardingNudge) result.getModelAndView()
                .getModel().get("onboardingNudge");
        org.assertj.core.api.Assertions.assertThat(nudge.trigger()).isEqualTo("step4");
        org.assertj.core.api.Assertions.assertThat(nudge.upgradeTarget()).isEqualTo(Plan.TEAM);
    }

    @Test
    void paidUserDoesNotGetUpgradeNudgeEvenAtSameProgress() throws Exception {
        EmailThread thread = new EmailThread(owner, "Hello", "<a@b>");
        Page<EmailThread> populated = new PageImpl<>(List.of(thread));
        when(threadRepository.findByOwnerOrderByUpdatedAtDesc(eq(owner), any(Pageable.class)))
                .thenReturn(populated);
        OnboardingChecklist checklist = new OnboardingChecklist(true, 12L, true, false);
        when(onboardingService.checklistFor(owner)).thenReturn(checklist);
        when(planLimitService.currentPlan(owner)).thenReturn(Plan.PERSONAL);

        mockMvc.perform(get("/threads").principal(principal))
                .andExpect(status().isOk())
                .andExpect(model().attribute("onboardingNudge", nullValue()));
    }

    @Test
    void trialConversionNudgeIsExposedOnInboxWhenServiceReturnsOne() throws Exception {
        Page<EmailThread> empty = new PageImpl<>(List.of());
        when(threadRepository.findByOwnerOrderByUpdatedAtDesc(eq(owner), any(Pageable.class)))
                .thenReturn(empty);
        TrialConversionNudge nudge = new TrialConversionNudge(
                "Personal", "personal", 2L, "$9", "$7", "$84",
                "mailim-trial-nudge-2026-06-07-d2");
        when(trialConversionNudgeService.nudgeFor(owner)).thenReturn(Optional.of(nudge));

        mockMvc.perform(get("/threads").principal(principal))
                .andExpect(status().isOk())
                .andExpect(view().name("threads"))
                .andExpect(model().attribute("trialConversionNudge", nudge));
    }

    @Test
    void trialConversionNudgeIsNotSurfacedDuringLockout() throws Exception {
        when(billingBannerService.bannerFor(owner))
                .thenReturn(Optional.of(BillingBanner.subscriptionEnded()));

        mockMvc.perform(get("/threads").principal(principal))
                .andExpect(status().isOk())
                .andExpect(model().attribute("trialConversionNudge", nullValue()));

        verify(trialConversionNudgeService, never()).nudgeFor(any(User.class));
    }

    @Test
    void searchQueryParamRoutesThroughThreadSearchService() throws Exception {
        Page<EmailThread> empty = new PageImpl<>(List.of());
        when(threadSearchService.search(eq(owner), eq("planning"), eq(null), any(ThreadFilters.class), any(Pageable.class)))
                .thenReturn(new ThreadSearchService.Result(empty, false));

        mockMvc.perform(get("/threads").principal(principal).param("q", "planning"))
                .andExpect(status().isOk())
                .andExpect(view().name("threads"))
                .andExpect(model().attribute("searchQuery", "planning"))
                .andExpect(model().attribute("threads", notNullValue()))
                .andExpect(model().attribute("bodySearchUpgradeNag", nullValue()));

        verify(threadRepository, never())
                .findByOwnerOrderByUpdatedAtDesc(any(User.class), any(Pageable.class));
    }

    @Test
    void bodyOnlyMatchOnFreePlanExposesUpgradeNag() throws Exception {
        Page<EmailThread> empty = new PageImpl<>(List.of());
        when(threadSearchService.search(eq(owner), eq("invoice"), eq(null), any(ThreadFilters.class), any(Pageable.class)))
                .thenReturn(new ThreadSearchService.Result(empty, true));

        mockMvc.perform(get("/threads").principal(principal).param("q", "invoice"))
                .andExpect(status().isOk())
                .andExpect(view().name("threads"))
                .andExpect(model().attribute("searchQuery", "invoice"))
                .andExpect(model().attribute("bodySearchUpgradeNag", is(true)));
    }

    @Test
    void blankSearchQueryFallsBackToFullList() throws Exception {
        Page<EmailThread> empty = new PageImpl<>(List.of());
        when(threadRepository.findByOwnerOrderByUpdatedAtDesc(eq(owner), any(Pageable.class)))
                .thenReturn(empty);

        mockMvc.perform(get("/threads").principal(principal).param("q", "   "))
                .andExpect(status().isOk())
                .andExpect(view().name("threads"))
                .andExpect(model().attribute("searchQuery", ""));

        verify(threadSearchService, never())
                .search(any(User.class), anyString(), any(), any(ThreadFilters.class), any(Pageable.class));
        verify(threadRepository, never())
                .search(any(User.class), anyString(), any(), any(), anyBoolean(), anyBoolean(), any(Pageable.class));
    }

    @Test
    void searchWithNoResultsStillShowsOnboardingProgressWhileIncomplete() throws Exception {
        Page<EmailThread> empty = new PageImpl<>(List.of());
        when(threadSearchService.search(eq(owner), eq("nope"), eq(null), any(ThreadFilters.class), any(Pageable.class)))
                .thenReturn(new ThreadSearchService.Result(empty, false));
        OnboardingChecklist incomplete = new OnboardingChecklist(true, 5L, false, false);
        when(onboardingService.checklistFor(owner)).thenReturn(incomplete);

        mockMvc.perform(get("/threads").principal(principal).param("q", "nope"))
                .andExpect(status().isOk())
                .andExpect(view().name("threads"))
                .andExpect(model().attribute("onboarding", incomplete));
    }

    @Test
    void senderRailIsExposedOnInbox() throws Exception {
        Page<EmailThread> empty = new PageImpl<>(List.of());
        when(threadRepository.findByOwnerOrderByUpdatedAtDesc(eq(owner), any(Pageable.class)))
                .thenReturn(empty);
        List<SenderGroupService.SenderGroup> groups = List.of(
                new SenderGroupService.SenderGroup("ada@acme.com", "Ada Lovelace", 4L, "Ada Lovelace", "AL"));
        when(senderGroupService.topSenders(owner)).thenReturn(groups);

        mockMvc.perform(get("/threads").principal(principal))
                .andExpect(status().isOk())
                .andExpect(model().attribute("senderGroups", groups))
                .andExpect(model().attribute("activeSender", nullValue()));
    }

    @Test
    void fromParamRoutesThroughSenderOnlyFilter() throws Exception {
        Page<EmailThread> empty = new PageImpl<>(List.of());
        when(threadSearchService.search(eq(owner), eq(""), eq("ada@acme.com"), any(ThreadFilters.class), any(Pageable.class)))
                .thenReturn(new ThreadSearchService.Result(empty, false));

        mockMvc.perform(get("/threads").principal(principal).param("from", "ada@acme.com"))
                .andExpect(status().isOk())
                .andExpect(view().name("threads"))
                .andExpect(model().attribute("activeSender", "ada@acme.com"))
                .andExpect(model().attribute("searchQuery", ""));

        verify(threadRepository, never())
                .findByOwnerOrderByUpdatedAtDesc(any(User.class), any(Pageable.class));
    }

    @Test
    void combinedQueryAndFromGoesThroughSearchServiceWithBoth() throws Exception {
        Page<EmailThread> empty = new PageImpl<>(List.of());
        when(threadSearchService.search(eq(owner), eq("planning"), eq("ada@acme.com"), any(ThreadFilters.class), any(Pageable.class)))
                .thenReturn(new ThreadSearchService.Result(empty, false));

        mockMvc.perform(get("/threads").principal(principal)
                        .param("q", "planning")
                        .param("from", "ada@acme.com"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("activeSender", "ada@acme.com"))
                .andExpect(model().attribute("searchQuery", "planning"));
    }

    @Test
    void blankFromParamFallsBackToFullList() throws Exception {
        Page<EmailThread> empty = new PageImpl<>(List.of());
        when(threadRepository.findByOwnerOrderByUpdatedAtDesc(eq(owner), any(Pageable.class)))
                .thenReturn(empty);

        mockMvc.perform(get("/threads").principal(principal).param("from", "  "))
                .andExpect(status().isOk())
                .andExpect(model().attribute("activeSender", nullValue()));

        verify(threadSearchService, never())
                .search(any(User.class), anyString(), any(), any(ThreadFilters.class), any(Pageable.class));
    }

    @Test
    void senderFilterActiveStillShowsOnboardingProgressWhileIncomplete() throws Exception {
        Page<EmailThread> empty = new PageImpl<>(List.of());
        when(threadSearchService.search(eq(owner), eq(""), eq("ada@acme.com"), any(ThreadFilters.class), any(Pageable.class)))
                .thenReturn(new ThreadSearchService.Result(empty, false));
        OnboardingChecklist incomplete = new OnboardingChecklist(true, 12L, false, false);
        when(onboardingService.checklistFor(owner)).thenReturn(incomplete);

        mockMvc.perform(get("/threads").principal(principal).param("from", "ada@acme.com"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("onboarding", incomplete));
    }

    @Test
    void unreadChipParamSendsAFiltersWithUnreadTrue() throws Exception {
        Page<EmailThread> empty = new PageImpl<>(List.of());
        org.mockito.ArgumentCaptor<ThreadFilters> captor =
                org.mockito.ArgumentCaptor.forClass(ThreadFilters.class);
        when(threadSearchService.search(eq(owner), eq(""), eq(null), captor.capture(), any(Pageable.class)))
                .thenReturn(new ThreadSearchService.Result(empty, false));

        mockMvc.perform(get("/threads").principal(principal).param("unread", "true"))
                .andExpect(status().isOk())
                .andExpect(view().name("threads"));

        ThreadFilters f = captor.getValue();
        org.assertj.core.api.Assertions.assertThat(f.requireUnread()).isTrue();
        org.assertj.core.api.Assertions.assertThat(f.requireAttachments()).isFalse();
        org.assertj.core.api.Assertions.assertThat(f.since()).isNull();

        verify(threadRepository, never())
                .findByOwnerOrderByUpdatedAtDesc(any(User.class), any(Pageable.class));
    }

    @Test
    void attachmentsChipParamSendsFiltersWithAttachmentsTrue() throws Exception {
        Page<EmailThread> empty = new PageImpl<>(List.of());
        org.mockito.ArgumentCaptor<ThreadFilters> captor =
                org.mockito.ArgumentCaptor.forClass(ThreadFilters.class);
        when(threadSearchService.search(eq(owner), eq(""), eq(null), captor.capture(), any(Pageable.class)))
                .thenReturn(new ThreadSearchService.Result(empty, false));

        mockMvc.perform(get("/threads").principal(principal).param("attachments", "true"))
                .andExpect(status().isOk());

        org.assertj.core.api.Assertions.assertThat(captor.getValue().requireAttachments()).isTrue();
    }

    @Test
    void sinceChipParamResolvesPresetThroughClock() throws Exception {
        Page<EmailThread> empty = new PageImpl<>(List.of());
        org.mockito.ArgumentCaptor<ThreadFilters> captor =
                org.mockito.ArgumentCaptor.forClass(ThreadFilters.class);
        when(threadSearchService.search(eq(owner), eq(""), eq(null), captor.capture(), any(Pageable.class)))
                .thenReturn(new ThreadSearchService.Result(empty, false));

        mockMvc.perform(get("/threads").principal(principal).param("since", "7d"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("activeFilters", notNullValue()));

        ThreadFilters f = captor.getValue();
        org.assertj.core.api.Assertions.assertThat(f.sincePreset()).isEqualTo("7d");
        // 2026-06-06T12:00:00Z minus 7 days
        org.assertj.core.api.Assertions.assertThat(f.since())
                .isEqualTo(java.time.LocalDateTime.of(2026, 5, 30, 12, 0));
    }

    @Test
    void unknownSinceValueIsIgnored() throws Exception {
        Page<EmailThread> empty = new PageImpl<>(List.of());
        when(threadRepository.findByOwnerOrderByUpdatedAtDesc(eq(owner), any(Pageable.class)))
                .thenReturn(empty);

        mockMvc.perform(get("/threads").principal(principal).param("since", "evil_payload"))
                .andExpect(status().isOk());

        verify(threadSearchService, never())
                .search(any(User.class), anyString(), any(), any(ThreadFilters.class), any(Pageable.class));
    }

    @Test
    void filterChipsActiveStillShowsOnboardingProgressWhileIncomplete() throws Exception {
        Page<EmailThread> empty = new PageImpl<>(List.of());
        when(threadSearchService.search(eq(owner), eq(""), eq(null), any(ThreadFilters.class), any(Pageable.class)))
                .thenReturn(new ThreadSearchService.Result(empty, false));
        OnboardingChecklist incomplete = new OnboardingChecklist(true, 12L, false, false);
        when(onboardingService.checklistFor(owner)).thenReturn(incomplete);

        mockMvc.perform(get("/threads").principal(principal).param("unread", "true"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("onboarding", incomplete));
    }

    @Test
    void conversationViewIncludesBillingBannerAttribute() throws Exception {
        EmailThread thread = new EmailThread(owner, "Test Subject", "<root@test>");
        Conversation conv = new Conversation(thread, List.of());
        when(threadViewService.getConversation(1L, owner)).thenReturn(conv);
        when(billingBannerService.bannerFor(owner))
                .thenReturn(Optional.of(BillingBanner.trialEnding(2)));

        mockMvc.perform(get("/threads/1").principal(principal))
                .andExpect(status().isOk())
                .andExpect(view().name("conversation"))
                .andExpect(model().attribute("billingBanner",
                        is(new BillingBanner(BillingBanner.Kind.TRIAL_ENDING, 2, null))));
    }

    @Test
    void savedSearchesAreExposedOnInbox() throws Exception {
        Page<EmailThread> empty = new PageImpl<>(List.of());
        when(threadRepository.findByOwnerOrderByUpdatedAtDesc(eq(owner), any(Pageable.class)))
                .thenReturn(empty);
        List<SavedSearchView> views = List.of(
                new SavedSearchView(1L, "From Ada", null, "ada@example.com",
                        "30d", false, true, 7L, 2L));
        when(savedSearchService.viewsFor(owner)).thenReturn(views);

        mockMvc.perform(get("/threads").principal(principal))
                .andExpect(status().isOk())
                .andExpect(model().attribute("savedSearches", views))
                .andExpect(model().attribute("hasActiveSearchToSave", is(false)));
    }

    @Test
    void savedSearchIdParamMarksViewedBeforeRendering() throws Exception {
        Page<EmailThread> empty = new PageImpl<>(List.of());
        when(threadSearchService.search(eq(owner), eq(""), eq("ada@example.com"),
                any(ThreadFilters.class), any(Pageable.class)))
                .thenReturn(new ThreadSearchService.Result(empty, false));

        mockMvc.perform(get("/threads").principal(principal)
                        .param("from", "ada@example.com")
                        .param("s", "42"))
                .andExpect(status().isOk())
                .andExpect(view().name("threads"));

        verify(savedSearchService).markViewed(eq(owner), eq(42L),
                eq(java.time.LocalDateTime.ofInstant(CLOCK.instant(), java.time.ZoneOffset.UTC)));
    }

    @Test
    void omittedSavedSearchIdParamDoesNotCallMarkViewed() throws Exception {
        Page<EmailThread> empty = new PageImpl<>(List.of());
        when(threadRepository.findByOwnerOrderByUpdatedAtDesc(eq(owner), any(Pageable.class)))
                .thenReturn(empty);

        mockMvc.perform(get("/threads").principal(principal))
                .andExpect(status().isOk());

        verify(savedSearchService, never()).markViewed(any(), any(), any());
    }

    @Test
    void hasActiveSearchToSaveIsTrueWhenAnyFilterActive() throws Exception {
        Page<EmailThread> empty = new PageImpl<>(List.of());
        when(threadSearchService.search(eq(owner), eq(""), eq("ada@example.com"),
                any(ThreadFilters.class), any(Pageable.class)))
                .thenReturn(new ThreadSearchService.Result(empty, false));

        mockMvc.perform(get("/threads").principal(principal).param("from", "ada@example.com"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("hasActiveSearchToSave", is(true)));
    }

    @Test
    void teamPlanOwnerSeesNotesAndCanPostFlagOnConversationView() throws Exception {
        EmailThread thread = new EmailThread(owner, "Subject", "<root@test>");
        Conversation conv = new Conversation(thread, List.of());
        when(threadViewService.getConversation(1L, owner)).thenReturn(conv);
        when(threadNoteService.canAccessNotesOn(thread, owner)).thenReturn(true);
        when(threadNoteService.notesFor(thread, owner)).thenReturn(List.of());

        mockMvc.perform(get("/threads/1").principal(principal))
                .andExpect(status().isOk())
                .andExpect(view().name("conversation"))
                .andExpect(model().attribute("isThreadOwner", is(true)))
                .andExpect(model().attribute("canPostTeamNote", is(true)))
                .andExpect(model().attribute("teamNotes", notNullValue()))
                .andExpect(model().attribute("teamNotesUpgradeNudge", nullValue()));
    }

    @Test
    void freePlanOwnerSeesUpgradeNudgeAndEmptyNotesOnConversationView() throws Exception {
        EmailThread thread = new EmailThread(owner, "Subject", "<root@test>");
        Conversation conv = new Conversation(thread, List.of());
        when(threadViewService.getConversation(1L, owner)).thenReturn(conv);
        when(threadNoteService.canAccessNotesOn(thread, owner)).thenReturn(false);
        when(threadNoteService.notesFor(thread, owner)).thenReturn(List.of());

        mockMvc.perform(get("/threads/1").principal(principal))
                .andExpect(status().isOk())
                .andExpect(view().name("conversation"))
                .andExpect(model().attribute("isThreadOwner", is(true)))
                .andExpect(model().attribute("canPostTeamNote", is(false)))
                .andExpect(model().attribute("teamNotesUpgradeNudge", is(true)));
    }

    @Test
    void teammateViewerSeesNotesPanelWithoutUpgradeNudge() throws Exception {
        User mailboxOwner = new User("mailbox@example.com", "hash", "Mailbox");
        EmailThread thread = new EmailThread(mailboxOwner, "Subject", "<root@test>");
        Conversation conv = new Conversation(thread, List.of());
        when(threadViewService.getConversation(9L, owner)).thenReturn(conv);
        when(threadAccessService.isOwner(thread, owner)).thenReturn(false);
        when(threadNoteService.canAccessNotesOn(thread, owner)).thenReturn(true);
        when(threadNoteService.notesFor(thread, owner)).thenReturn(List.of());

        mockMvc.perform(get("/threads/9").principal(principal))
                .andExpect(status().isOk())
                .andExpect(view().name("conversation"))
                .andExpect(model().attribute("isThreadOwner", is(false)))
                .andExpect(model().attribute("canPostTeamNote", is(true)))
                .andExpect(model().attribute("teamNotesUpgradeNudge", nullValue()));
    }

    @Test
    void teammateViewerWithoutNotesAccessSeesNoUpgradeNudge() throws Exception {
        User mailboxOwner = new User("mailbox@example.com", "hash", "Mailbox");
        EmailThread thread = new EmailThread(mailboxOwner, "Subject", "<root@test>");
        Conversation conv = new Conversation(thread, List.of());
        when(threadViewService.getConversation(9L, owner)).thenReturn(conv);
        when(threadAccessService.isOwner(thread, owner)).thenReturn(false);
        when(threadNoteService.canAccessNotesOn(thread, owner)).thenReturn(false);
        when(threadNoteService.notesFor(thread, owner)).thenReturn(List.of());

        mockMvc.perform(get("/threads/9").principal(principal))
                .andExpect(status().isOk())
                .andExpect(model().attribute("isThreadOwner", is(false)))
                .andExpect(model().attribute("canPostTeamNote", is(false)))
                .andExpect(model().attribute("teamNotesUpgradeNudge", nullValue()));
    }

    @Test
    void postNoteRedirectsBackToThreadWithFlash() throws Exception {
        EmailThread thread = new EmailThread(owner, "Subject", "<root@test>");
        when(threadAccessService.findAccessibleThread(7L, owner)).thenReturn(Optional.of(thread));
        when(threadNoteService.post(eq(thread), eq(owner), eq("Heads-up for the team")))
                .thenReturn(ThreadNoteService.PostResult.posted(new ThreadNote(thread, null, owner, "Heads-up for the team")));

        mockMvc.perform(post("/threads/7/note").principal(principal)
                        .param("body", "Heads-up for the team"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/threads/7"));

        verify(threadNoteService).post(eq(thread), eq(owner), eq("Heads-up for the team"));
    }

    @Test
    void postNoteOnInaccessibleThreadReturns404() throws Exception {
        when(threadAccessService.findAccessibleThread(42L, owner)).thenReturn(Optional.empty());

        mockMvc.perform(post("/threads/42/note").principal(principal)
                        .param("body", "Trying to add a note to a thread we can't reach"))
                .andExpect(status().isNotFound());

        verify(threadNoteService, never()).post(any(EmailThread.class), any(User.class), anyString());
    }

    @Test
    void postNoteByTeammateGoesThroughTeamScopedAccess() throws Exception {
        User mailboxOwner = new User("mailbox@example.com", "hash", "Mailbox");
        EmailThread thread = new EmailThread(mailboxOwner, "Subject", "<root@test>");
        when(threadAccessService.findAccessibleThread(11L, owner)).thenReturn(Optional.of(thread));
        when(threadNoteService.post(eq(thread), eq(owner), eq("Looping you in")))
                .thenReturn(ThreadNoteService.PostResult.posted(new ThreadNote(thread, null, owner, "Looping you in")));

        mockMvc.perform(post("/threads/11/note").principal(principal)
                        .param("body", "Looping you in"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/threads/11"));

        verify(threadNoteService).post(eq(thread), eq(owner), eq("Looping you in"));
    }
}
