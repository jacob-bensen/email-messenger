package com.emailmessenger.web;

import com.emailmessenger.auth.UserActivityService;
import com.emailmessenger.auth.UserService;
import com.emailmessenger.billing.BillingBanner;
import com.emailmessenger.billing.BillingBannerService;
import com.emailmessenger.billing.BillingService;
import com.emailmessenger.billing.TrialConversionNudgeService;
import com.emailmessenger.domain.EmailThread;
import com.emailmessenger.domain.User;
import com.emailmessenger.repository.EmailThreadRepository;
import com.emailmessenger.service.Conversation;
import com.emailmessenger.service.ReplyService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

@Controller
class ThreadController {

    private static final int PAGE_SIZE = 20;

    private final EmailThreadRepository threadRepository;
    private final ThreadViewService threadViewService;
    private final ReplyService replyService;
    private final UserService userService;
    private final BillingBannerService billingBannerService;
    private final BillingService billingService;
    private final OnboardingService onboardingService;
    private final TrialConversionNudgeService trialConversionNudgeService;
    private final ThreadSearchService threadSearchService;
    private final SenderGroupService senderGroupService;
    private final SavedSearchService savedSearchService;
    private final UserActivityService userActivityService;
    private final Clock clock;

    ThreadController(EmailThreadRepository threadRepository,
                     ThreadViewService threadViewService,
                     ReplyService replyService,
                     UserService userService,
                     BillingBannerService billingBannerService,
                     BillingService billingService,
                     OnboardingService onboardingService,
                     TrialConversionNudgeService trialConversionNudgeService,
                     ThreadSearchService threadSearchService,
                     SenderGroupService senderGroupService,
                     SavedSearchService savedSearchService,
                     UserActivityService userActivityService,
                     Clock clock) {
        this.threadRepository = threadRepository;
        this.threadViewService = threadViewService;
        this.replyService = replyService;
        this.userService = userService;
        this.billingBannerService = billingBannerService;
        this.billingService = billingService;
        this.onboardingService = onboardingService;
        this.trialConversionNudgeService = trialConversionNudgeService;
        this.threadSearchService = threadSearchService;
        this.senderGroupService = senderGroupService;
        this.savedSearchService = savedSearchService;
        this.userActivityService = userActivityService;
        this.clock = clock;
    }

    @GetMapping("/threads")
    String listThreads(@RequestParam(defaultValue = "0") int page,
                       @RequestParam(name = "q", required = false) String query,
                       @RequestParam(name = "from", required = false) String fromSender,
                       @RequestParam(name = "since", required = false) String since,
                       @RequestParam(name = "unread", defaultValue = "false") boolean unread,
                       @RequestParam(name = "attachments", defaultValue = "false") boolean attachments,
                       @RequestParam(name = "s", required = false) Long savedSearchId,
                       Principal principal,
                       Model model) {
        User owner = userService.requireByEmail(principal.getName());
        userActivityService.recordInboxVisit(owner);
        BillingBanner banner = billingBannerService.bannerFor(owner).orElse(null);
        model.addAttribute("billingBanner", banner);
        model.addAttribute("hasBilling", billingService.hasManagedBilling(owner));
        model.addAttribute("emailUnverified", !owner.isEmailVerified());
        if (banner != null && banner.isSubscriptionEnded()) {
            return "threads";
        }
        if (savedSearchId != null) {
            savedSearchService.markViewed(owner, savedSearchId, LocalDateTime.now(clock));
        }
        String trimmedQuery = query == null ? "" : query.trim();
        String trimmedFrom = (fromSender == null || fromSender.isBlank()) ? null : fromSender.trim();
        ThreadFilters filters = ThreadFilters.parse(since, unread, attachments, clock);
        PageRequest pageRequest = PageRequest.of(Math.max(0, page), PAGE_SIZE);
        Page<EmailThread> threads;
        if (trimmedQuery.isEmpty() && trimmedFrom == null && !filters.isActive()) {
            threads = threadRepository.findByOwnerOrderByUpdatedAtDesc(owner, pageRequest);
        } else {
            ThreadSearchService.Result result =
                    threadSearchService.search(owner, trimmedQuery, trimmedFrom, filters, pageRequest);
            threads = result.page();
            if (result.showBodySearchUpgradeNag()) {
                model.addAttribute("bodySearchUpgradeNag", true);
            }
        }
        model.addAttribute("threads", threads);
        model.addAttribute("searchQuery", trimmedQuery);
        model.addAttribute("activeSender", trimmedFrom);
        model.addAttribute("activeFilters", filters);
        List<SenderGroupService.SenderGroup> senderGroups = senderGroupService.topSenders(owner);
        model.addAttribute("senderGroups", senderGroups);
        model.addAttribute("hasAnyThreads", !senderGroups.isEmpty());
        List<SavedSearchView> savedSearches = savedSearchService.viewsFor(owner);
        model.addAttribute("savedSearches", savedSearches);
        model.addAttribute("hasActiveSearchToSave",
                !trimmedQuery.isEmpty() || trimmedFrom != null || filters.isActive());
        if (trimmedQuery.isEmpty() && trimmedFrom == null && !filters.isActive()
                && threads.getTotalElements() == 0) {
            model.addAttribute("onboarding", onboardingService.checklistFor(owner));
        }
        trialConversionNudgeService.nudgeFor(owner)
                .ifPresent(n -> model.addAttribute("trialConversionNudge", n));
        return "threads";
    }

    @GetMapping("/threads/{id}")
    String viewConversation(@PathVariable Long id, Principal principal, Model model) {
        User owner = userService.requireByEmail(principal.getName());
        Conversation conversation = threadViewService.getConversation(id, owner);
        model.addAttribute("conversation", conversation);
        model.addAttribute("replyForm", new ReplyForm());
        model.addAttribute("billingBanner", billingBannerService.bannerFor(owner).orElse(null));
        return "conversation";
    }

    @PostMapping("/threads/{id}/reply")
    String reply(@PathVariable Long id,
                 @Valid @ModelAttribute("replyForm") ReplyForm replyForm,
                 BindingResult bindingResult,
                 Principal principal,
                 RedirectAttributes redirectAttributes,
                 Model model) {
        User owner = userService.requireByEmail(principal.getName());
        if (bindingResult.hasErrors()) {
            Conversation conversation = threadViewService.getConversation(id, owner);
            model.addAttribute("conversation", conversation);
            return "conversation";
        }
        EmailThread thread = threadRepository.findByIdAndOwner(id, owner)
                .orElseThrow(NoSuchElementException::new);
        replyService.sendReply(id, thread.getSubject(), replyForm.getBody());
        redirectAttributes.addFlashAttribute("successMessage", "Reply sent successfully.");
        return "redirect:/threads/" + id;
    }
}
