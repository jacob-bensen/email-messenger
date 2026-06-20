package com.emailmessenger.web;

import com.emailmessenger.auth.UserService;
import com.emailmessenger.domain.EmailThread;
import com.emailmessenger.domain.User;
import com.emailmessenger.email.MailboxPollingService;
import com.emailmessenger.repository.EmailThreadRepository;
import com.emailmessenger.service.ChatConversation;
import com.emailmessenger.service.ChatMember;
import com.emailmessenger.service.OutgoingAttachment;
import com.emailmessenger.service.ReplyService;
import com.emailmessenger.billing.BillingBannerService;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The texting-style chats experience: a list of conversations (people and
 * groups, most-recent first) at {@code /chats}, and a single conversation's
 * unified timeline + compose box at {@code /chats/{key}}. Conversations are
 * keyed by the set of people involved — see
 * {@link com.emailmessenger.service.ConversationKeyService}.
 */
@Controller
class ConversationController {

    private static final int PAGE_SIZE = 30;
    private static final int RAIL_SIZE = 50;

    private final ConversationListService conversationListService;
    private final ChatService chatService;
    private final EmailThreadRepository threadRepository;
    private final ReplyService replyService;
    private final OutboundMessageService outboundMessageService;
    private final UserService userService;
    private final BillingBannerService billingBannerService;
    private final MailboxPollingService mailboxPollingService;

    ConversationController(ConversationListService conversationListService,
                           ChatService chatService,
                           EmailThreadRepository threadRepository,
                           ReplyService replyService,
                           OutboundMessageService outboundMessageService,
                           UserService userService,
                           BillingBannerService billingBannerService,
                           MailboxPollingService mailboxPollingService) {
        this.conversationListService = conversationListService;
        this.chatService = chatService;
        this.threadRepository = threadRepository;
        this.replyService = replyService;
        this.outboundMessageService = outboundMessageService;
        this.userService = userService;
        this.billingBannerService = billingBannerService;
        this.mailboxPollingService = mailboxPollingService;
    }

    @GetMapping("/chats")
    String list(@RequestParam(defaultValue = "0") int page,
                @RequestParam(name = "q", required = false) String query,
                Principal principal, Model model) {
        User owner = userService.requireByEmail(principal.getName());
        // Non-blocking refresh of due mailboxes, mirroring the old inbox.
        mailboxPollingService.refreshDueForUserAsync(owner.getId());
        String trimmedQuery = query == null ? "" : query.trim();
        Page<com.emailmessenger.service.ConversationListItem> conversations =
                conversationListService.list(owner, trimmedQuery, PageRequest.of(Math.max(0, page), PAGE_SIZE));
        model.addAttribute("conversations", conversations);
        model.addAttribute("searchQuery", trimmedQuery);
        model.addAttribute("billingBanner", billingBannerService.bannerFor(owner).orElse(null));
        model.addAttribute("emailUnverified", !owner.isEmailVerified());
        return "chats";
    }

    @GetMapping("/chats/{key}")
    String chat(@PathVariable String key, Principal principal, Model model) {
        User owner = userService.requireByEmail(principal.getName());
        if (!populateChat(owner, key, model)) {
            return "redirect:/chats";
        }
        // Secondary rail: the other conversations, collapsed beside the main nav,
        // with the open one marked active so you can switch between chats.
        model.addAttribute("replyForm", new ReplyForm());
        return "chat";
    }

    /**
     * Sends a chat message. By default it goes out as a brand-new email with the
     * given (optional) subject; ticking "reply" instead continues the
     * conversation's most recent thread, keeping its subject and threading
     * headers. Either way it goes to the conversation's participants.
     */
    @PostMapping("/chats/{key}/send")
    String send(@PathVariable String key,
                @Valid @ModelAttribute("replyForm") ReplyForm replyForm,
                BindingResult bindingResult,
                @RequestParam(value = "subject", required = false) String subject,
                @RequestParam(value = "asReply", defaultValue = "false") boolean asReply,
                @RequestParam(value = "attachments", required = false) MultipartFile[] attachments,
                Principal principal,
                RedirectAttributes redirectAttributes,
                Model model) {
        User owner = userService.requireByEmail(principal.getName());
        boolean hasAttachment = attachments != null
                && Arrays.stream(attachments).anyMatch(f -> f != null && !f.isEmpty());
        if (replyForm.getBody().isBlank() && !hasAttachment) {
            bindingResult.rejectValue("body", "reply.empty", "Add a message or an attachment.");
        }
        if (bindingResult.hasErrors()) {
            if (!populateChat(owner, key, model)) {
                return "redirect:/chats";
            }
            return "chat";
        }

        ChatConversation conversation = chatService.buildFor(owner, key);
        if (conversation == null) {
            return "redirect:/chats";
        }
        List<String> recipients = conversation.members().stream().map(ChatMember::email).toList();
        if (recipients.isEmpty()) {
            redirectAttributes.addFlashAttribute("sendError",
                    "This conversation has no one to send to.");
            return "redirect:/chats/" + key;
        }
        List<OutgoingAttachment> outgoing = toOutgoingAttachments(attachments);
        String from = owner.getEmail();
        List<EmailThread> threads = threadRepository.findThreadsByConversationKey(owner, key);

        if (asReply && !threads.isEmpty()) {
            // Continue the most recently active thread so the email keeps its subject.
            EmailThread target = threads.get(0);
            replyService.sendReply(target.getId(), target.getSubject(), replyForm.getBody(),
                    outgoing, recipients, from);
            outboundMessageService.recordReply(target.getId(), owner, replyForm.getBody(), outgoing);
            redirectAttributes.addFlashAttribute("successMessage", "Reply sent.");
        } else {
            String subj = (subject == null || subject.isBlank()) ? "(no subject)" : subject.trim();
            replyService.sendNewEmail(subj, replyForm.getBody(), outgoing, recipients, from);
            outboundMessageService.recordNewEmail(owner, key, subj, replyForm.getBody(),
                    recipients, outgoing);
            redirectAttributes.addFlashAttribute("successMessage", "Message sent.");
        }
        return "redirect:/chats/" + key;
    }

    private boolean populateChat(User owner, String key, Model model) {
        ChatConversation conversation = chatService.buildFor(owner, key);
        if (conversation == null) {
            return false;
        }
        model.addAttribute("chat", conversation);
        model.addAttribute("billingBanner", billingBannerService.bannerFor(owner).orElse(null));
        // Secondary rail: the other conversations, with the open one active.
        model.addAttribute("conversations",
                conversationListService.list(owner, PageRequest.of(0, RAIL_SIZE)));
        model.addAttribute("activeKey", key);
        return true;
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
