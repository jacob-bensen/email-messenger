package com.emailmessenger.web;

import com.emailmessenger.auth.UserActivityService;
import com.emailmessenger.auth.UserService;
import com.emailmessenger.billing.BillingBanner;
import com.emailmessenger.billing.BillingBannerService;
import com.emailmessenger.billing.BillingService;
import com.emailmessenger.billing.PlanLimitService;
import com.emailmessenger.billing.TrialConversionNudgeService;
import com.emailmessenger.domain.EmailThread;
import com.emailmessenger.domain.User;
import com.emailmessenger.email.MailboxPollingService;
import com.emailmessenger.repository.EmailThreadRepository;
import com.emailmessenger.service.Conversation;
import com.emailmessenger.service.OutgoingAttachment;
import com.emailmessenger.service.ReplyService;
import com.emailmessenger.team.NoteMentionService;
import com.emailmessenger.team.ThreadAccessService;
import com.emailmessenger.team.ThreadNoteService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.security.Principal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private final PlanLimitService planLimitService;
    private final TrialConversionNudgeService trialConversionNudgeService;
    private final ThreadSearchService threadSearchService;
    private final SenderGroupService senderGroupService;
    private final SavedSearchService savedSearchService;
    private final UserActivityService userActivityService;
    private final ThreadNoteService threadNoteService;
    private final ThreadAccessService threadAccessService;
    private final NoteMentionService noteMentionService;
    private final MailboxPollingService mailboxPollingService;
    private final OutboundMessageService outboundMessageService;
    private final Clock clock;

    ThreadController(EmailThreadRepository threadRepository,
                     ThreadViewService threadViewService,
                     ReplyService replyService,
                     UserService userService,
                     BillingBannerService billingBannerService,
                     BillingService billingService,
                     OnboardingService onboardingService,
                     PlanLimitService planLimitService,
                     TrialConversionNudgeService trialConversionNudgeService,
                     ThreadSearchService threadSearchService,
                     SenderGroupService senderGroupService,
                     SavedSearchService savedSearchService,
                     UserActivityService userActivityService,
                     ThreadNoteService threadNoteService,
                     ThreadAccessService threadAccessService,
                     NoteMentionService noteMentionService,
                     MailboxPollingService mailboxPollingService,
                     OutboundMessageService outboundMessageService,
                     Clock clock) {
        this.threadRepository = threadRepository;
        this.threadViewService = threadViewService;
        this.replyService = replyService;
        this.userService = userService;
        this.billingBannerService = billingBannerService;
        this.billingService = billingService;
        this.onboardingService = onboardingService;
        this.planLimitService = planLimitService;
        this.trialConversionNudgeService = trialConversionNudgeService;
        this.threadSearchService = threadSearchService;
        this.senderGroupService = senderGroupService;
        this.savedSearchService = savedSearchService;
        this.userActivityService = userActivityService;
        this.threadNoteService = threadNoteService;
        this.threadAccessService = threadAccessService;
        this.noteMentionService = noteMentionService;
        this.mailboxPollingService = mailboxPollingService;
        this.outboundMessageService = outboundMessageService;
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
        // Kick a non-blocking refresh of this user's due mailboxes so opening the
        // inbox pulls fresh mail right away; only accounts past their next-poll
        // time are touched, so this won't re-poll what the scheduler just did.
        mailboxPollingService.refreshDueForUserAsync(owner.getId());
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
        // When no single sender is filtered ("Everyone"), label each thread with
        // its most recent correspondent so the list shows who it's from.
        model.addAttribute("threadSenders", latestSenderLabels(trimmedFrom, threads));
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
        OnboardingChecklist checklist = onboardingService.checklistFor(owner);
        // Collapse the panel once the essential steps are done — saving a search
        // and inviting a teammate are optional and shouldn't keep it pinned open.
        if (!checklist.coreStepsComplete()) {
            model.addAttribute("onboarding", checklist);
        }
        OnboardingNudge.from(planLimitService.currentPlan(owner), checklist)
                .ifPresent(n -> model.addAttribute("onboardingNudge", n));
        trialConversionNudgeService.nudgeFor(owner)
                .ifPresent(n -> model.addAttribute("trialConversionNudge", n));
        return "threads";
    }

    @GetMapping("/threads/{id}")
    String viewConversation(@PathVariable Long id, Principal principal, Model model) {
        User viewer = userService.requireByEmail(principal.getName());
        populateConversationModel(id, viewer, model);
        model.addAttribute("replyForm", new ReplyForm());
        return "conversation";
    }

    // Shared by the GET view and the reply-validation-error re-render so both
    // paths supply every attribute conversation.html reads. demoMode in
    // particular must always be present: the template uses `!demoMode`, and
    // SpEL throws on `!null` rather than treating it as false.
    private void populateConversationModel(Long id, User viewer, Model model) {
        Conversation conversation = threadViewService.getConversation(id, viewer);
        EmailThread thread = conversation.thread();
        boolean isOwner = threadAccessService.isOwner(thread, viewer);
        model.addAttribute("demoMode", false);
        model.addAttribute("conversation", conversation);
        model.addAttribute("isThreadOwner", isOwner);
        model.addAttribute("billingBanner", billingBannerService.bannerFor(viewer).orElse(null));
        boolean canPostNote = threadNoteService.canAccessNotesOn(thread, viewer);
        model.addAttribute("teamNotes", threadNoteService.notesFor(thread, viewer));
        model.addAttribute("canPostTeamNote", canPostNote);
        model.addAttribute("teamNoteForm", new ThreadNoteForm());
        model.addAttribute("teamMentionCandidates",
                canPostNote ? noteMentionService.candidatesForThread(thread, viewer) : List.of());
        // Only the owner sees the upgrade-to-Team CTA — a teammate viewing the
        // thread can't upgrade for someone else; we just hide the notes panel.
        if (isOwner && !canPostNote) {
            model.addAttribute("teamNotesUpgradeNudge", true);
        }
    }

    @PostMapping("/threads/{id}/note")
    String postNote(@PathVariable Long id,
                    @RequestParam(value = "body", required = false) String body,
                    Principal principal,
                    RedirectAttributes redirectAttributes) {
        User viewer = userService.requireByEmail(principal.getName());
        EmailThread thread = threadAccessService.findAccessibleThread(id, viewer)
                .orElseThrow(NoSuchElementException::new);
        ThreadNoteService.PostResult result = threadNoteService.post(thread, viewer, body);
        switch (result.outcome()) {
            case POSTED -> redirectAttributes.addFlashAttribute("noteFlash", "posted");
            case GATED -> redirectAttributes.addFlashAttribute("noteFlash", "gated");
            case BLANK -> redirectAttributes.addFlashAttribute("noteFlash", "blank");
            case TOO_LONG -> redirectAttributes.addFlashAttribute("noteFlash", "tooLong");
        }
        return "redirect:/threads/" + id;
    }

    @PostMapping("/threads/{id}/reply")
    String reply(@PathVariable Long id,
                 @Valid @ModelAttribute("replyForm") ReplyForm replyForm,
                 BindingResult bindingResult,
                 @RequestParam(value = "attachments", required = false) MultipartFile[] attachments,
                 Principal principal,
                 RedirectAttributes redirectAttributes,
                 Model model) {
        User owner = userService.requireByEmail(principal.getName());
        boolean hasAttachment = attachments != null
                && Arrays.stream(attachments).anyMatch(f -> f != null && !f.isEmpty());
        // A reply needs either text or at least one attachment.
        if (replyForm.getBody().isBlank() && !hasAttachment) {
            bindingResult.rejectValue("body", "reply.empty", "Add a message or an attachment.");
        }
        if (bindingResult.hasErrors()) {
            // replyForm is already in the model as the bound @ModelAttribute,
            // carrying the validation errors the template renders.
            populateConversationModel(id, owner, model);
            return "conversation";
        }
        EmailThread thread = threadRepository.findByIdAndOwner(id, owner)
                .orElseThrow(NoSuchElementException::new);
        List<OutgoingAttachment> outgoing = toOutgoingAttachments(attachments);
        replyService.sendReply(id, thread.getSubject(), replyForm.getBody(), outgoing);
        // Persist the sent reply so it appears in the conversation as a "you" bubble.
        outboundMessageService.recordReply(id, owner, replyForm.getBody(), outgoing);
        redirectAttributes.addFlashAttribute("successMessage", "Reply sent successfully.");
        return "redirect:/threads/" + id;
    }

    // Convert uploaded files into web-agnostic attachments, skipping the empty
    // file part browsers send when no file is chosen. Oversized uploads are
    // rejected upstream by the multipart size limits (handled in
    // GlobalExceptionHandler), so this only sees what's within bounds.
    // Most-recent inbound sender per thread, keyed by thread id, for the inbox
    // "from" line. Empty when a single sender is already the filter, since every
    // row would then carry the same name.
    private Map<Long, String> latestSenderLabels(String activeSender, Page<EmailThread> threads) {
        Map<Long, String> labels = new HashMap<>();
        if (activeSender != null || !threads.hasContent()) {
            return labels;
        }
        List<Long> ids = threads.getContent().stream().map(EmailThread::getId).toList();
        for (EmailThreadRepository.ThreadSenderRow row : threadRepository.latestInboundSenders(ids)) {
            String label = (row.getDisplayName() != null && !row.getDisplayName().isBlank())
                    ? row.getDisplayName() : row.getEmail();
            labels.putIfAbsent(row.getThreadId(), label);
        }
        return labels;
    }

    private List<OutgoingAttachment> toOutgoingAttachments(MultipartFile[] files) {
        if (files == null) {
            return List.of();
        }
        List<OutgoingAttachment> out = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            String name = StringUtils.cleanPath(
                    file.getOriginalFilename() == null ? "attachment" : file.getOriginalFilename());
            try {
                out.add(new OutgoingAttachment(name, file.getContentType(), file.getBytes()));
            } catch (IOException e) {
                throw new UncheckedIOException("Could not read uploaded attachment", e);
            }
        }
        return out;
    }
}
