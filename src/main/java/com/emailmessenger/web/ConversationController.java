package com.emailmessenger.web;

import com.emailmessenger.auth.UserService;
import com.emailmessenger.domain.EmailThread;
import com.emailmessenger.domain.MailAccount;
import com.emailmessenger.domain.User;
import com.emailmessenger.email.MailAccountService;
import com.emailmessenger.email.MailboxPollingService;
import com.emailmessenger.repository.EmailThreadRepository;
import com.emailmessenger.email.OwnerAddressService;
import com.emailmessenger.service.ChatConversation;
import com.emailmessenger.service.ChatMember;
import com.emailmessenger.service.ConversationKeyService;
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
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
    private final MailAccountService mailAccountService;
    private final OwnerAddressService ownerAddressService;
    private final ConversationKeyService conversationKeyService;

    ConversationController(ConversationListService conversationListService,
                           ChatService chatService,
                           EmailThreadRepository threadRepository,
                           ReplyService replyService,
                           OutboundMessageService outboundMessageService,
                           UserService userService,
                           BillingBannerService billingBannerService,
                           MailboxPollingService mailboxPollingService,
                           MailAccountService mailAccountService,
                           OwnerAddressService ownerAddressService,
                           ConversationKeyService conversationKeyService) {
        this.conversationListService = conversationListService;
        this.chatService = chatService;
        this.threadRepository = threadRepository;
        this.replyService = replyService;
        this.outboundMessageService = outboundMessageService;
        this.userService = userService;
        this.billingBannerService = billingBannerService;
        this.mailboxPollingService = mailboxPollingService;
        this.mailAccountService = mailAccountService;
        this.ownerAddressService = ownerAddressService;
        this.conversationKeyService = conversationKeyService;
    }

    @GetMapping("/chats")
    String list(@RequestParam(defaultValue = "0") int page,
                @RequestParam(name = "q", required = false) String query,
                @RequestParam(name = "mailbox", required = false) String mailboxParam,
                @RequestParam(name = "filter", required = false) String filterParam,
                Principal principal, Model model) {
        User owner = userService.requireByEmail(principal.getName());
        // Non-blocking refresh of due mailboxes, mirroring the old inbox.
        mailboxPollingService.refreshDueForUserAsync(owner.getId());
        String trimmedQuery = query == null ? "" : query.trim();
        List<MailAccount> accounts = mailAccountService.list(owner);
        // No mailbox param => the cross-account Dashboard (Hub); a param scopes to
        // that one account so its chats stay separate.
        MailAccount selected = selectMailbox(accounts, mailboxParam);
        String scope = selected != null ? address(selected) : null;
        ConversationListService.ChatFilter filter = parseFilter(filterParam);

        PageRequest pageable = PageRequest.of(Math.max(0, page), PAGE_SIZE);
        model.addAttribute("conversations",
                conversationListService.list(owner, scope, trimmedQuery, filter, pageable));
        model.addAttribute("chatCounts", conversationListService.counts(owner, scope));
        model.addAttribute("activeFilter", filter.name().toLowerCase(Locale.ROOT));
        model.addAttribute("isHub", selected == null);
        model.addAttribute("accountColors", accountColorMap(owner, accounts));
        model.addAttribute("fallbackAccount", fallbackAccount(owner, accounts));
        model.addAttribute("searchQuery", trimmedQuery);
        model.addAttribute("billingBanner", billingBannerService.bannerFor(owner).orElse(null));
        model.addAttribute("emailUnverified", !owner.isEmailVerified());
        applyMailboxModel(model, selected);
        return "chats";
    }

    private static ConversationListService.ChatFilter parseFilter(String raw) {
        if (raw != null) {
            for (ConversationListService.ChatFilter f : ConversationListService.ChatFilter.values()) {
                if (f.name().equalsIgnoreCase(raw.trim())) {
                    return f;
                }
            }
        }
        return ConversationListService.ChatFilter.ALL;
    }

    /**
     * Best-guess account for a conversation the headers can't attribute (e.g. it
     * arrived via a list/alias so the owner's address isn't a To/Cc participant):
     * the sole connected mailbox if there's exactly one, otherwise the account
     * email. Lets the Dashboard still tag every row instead of leaving a blank.
     */
    private static String fallbackAccount(User owner, List<MailAccount> accounts) {
        if (accounts.size() == 1) {
            String username = accounts.get(0).getUsername();
            if (username != null && !username.isBlank()) {
                return username.trim().toLowerCase(Locale.ROOT);
            }
        }
        return owner.getEmail() == null ? null : owner.getEmail().trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Stable color index per account address (owner's login plus each connected
     * mailbox), so the Dashboard can tint each conversation's account tag
     * consistently. Keyed by lowercased address.
     */
    private static Map<String, Integer> accountColorMap(User owner, List<MailAccount> accounts) {
        Map<String, Integer> colors = new LinkedHashMap<>();
        if (owner.getEmail() != null && !owner.getEmail().isBlank()) {
            colors.put(owner.getEmail().trim().toLowerCase(Locale.ROOT), colors.size());
        }
        for (MailAccount account : accounts) {
            String username = account.getUsername();
            if (username != null && !username.isBlank()) {
                colors.putIfAbsent(username.trim().toLowerCase(Locale.ROOT), colors.size());
            }
        }
        return colors;
    }

    @GetMapping("/chats/new")
    String compose(@RequestParam(name = "mailbox", required = false) String mailboxParam,
                   Principal principal, Model model) {
        User owner = userService.requireByEmail(principal.getName());
        MailAccount selected = selectMailbox(mailAccountService.list(owner), mailboxParam);
        applyMailboxModel(model, selected);
        return "compose";
    }

    /**
     * Starts a brand-new conversation: sends the email from the selected account
     * and records it pinned to the recipients' conversation key, so it lands in
     * (and opens) the chat with those people.
     */
    @PostMapping("/chats/new")
    String createMessage(@RequestParam(name = "to", required = false) String to,
                         @RequestParam(name = "subject", required = false) String subject,
                         @RequestParam(name = "body", required = false) String body,
                         @RequestParam(value = "attachments", required = false) MultipartFile[] attachments,
                         @RequestParam(name = "mailbox", required = false) String mailboxParam,
                         Principal principal,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        User owner = userService.requireByEmail(principal.getName());
        MailAccount selected = selectMailbox(mailAccountService.list(owner), mailboxParam);
        List<String> recipients = parseRecipients(to);
        boolean hasAttachment = attachments != null
                && Arrays.stream(attachments).anyMatch(f -> f != null && !f.isEmpty());
        String messageBody = body == null ? "" : body;

        if (recipients.isEmpty() || (messageBody.isBlank() && !hasAttachment)) {
            applyMailboxModel(model, selected);
            model.addAttribute("composeError", recipients.isEmpty()
                    ? "Add at least one valid recipient email address."
                    : "Add a message or an attachment.");
            model.addAttribute("toValue", to);
            model.addAttribute("subjectValue", subject);
            model.addAttribute("bodyValue", body);
            return "compose";
        }

        List<OutgoingAttachment> outgoing = toOutgoingAttachments(attachments);
        String from = selected != null ? selected.getUsername() : owner.getEmail();
        String subj = (subject == null || subject.isBlank()) ? "(no subject)" : subject.trim();
        String key = conversationKeyService.keyFor(recipients, ownerAddressService.addressesFor(owner));

        replyService.sendNewEmail(subj, messageBody, outgoing, recipients, from);
        outboundMessageService.recordNewEmail(owner, key, subj, messageBody, recipients, outgoing);
        redirectAttributes.addFlashAttribute("successMessage", "Message sent.");
        return "redirect:/chats/" + key + mailboxQuery(selected);
    }

    /** Splits a free-text recipient field on commas/semicolons/whitespace, keeping distinct addresses. */
    private static List<String> parseRecipients(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        for (String part : raw.split("[,;\\s]+")) {
            String email = part.trim();
            if (!email.isEmpty() && email.contains("@") && !out.contains(email)) {
                out.add(email);
            }
        }
        return out;
    }

    /**
     * Foreground heartbeat: the open chats/chat page pings this ~every minute so
     * connected mailboxes refresh on a 1-minute cadence while in use. Returns the
     * number of newly imported messages so the page only reloads when something
     * actually arrived. Synchronous (mirrors the manual "Sync now" path) so the
     * count is accurate.
     */
    @PostMapping("/chats/refresh")
    @ResponseBody
    Map<String, Integer> refresh(Principal principal) {
        User owner = userService.requireByEmail(principal.getName());
        int imported = mailboxPollingService.refreshActiveUserNow(owner.getId());
        return Map.of("imported", imported);
    }

    @GetMapping("/chats/{key}")
    String chat(@PathVariable String key,
                @RequestParam(name = "mailbox", required = false) String mailboxParam,
                Principal principal, Model model) {
        User owner = userService.requireByEmail(principal.getName());
        MailAccount selected = selectMailbox(mailAccountService.list(owner), mailboxParam);
        if (!populateChat(owner, selected, key, model)) {
            return "redirect:/chats" + mailboxQuery(selected);
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
                @RequestParam(value = "mailbox", required = false) String mailboxParam,
                Principal principal,
                RedirectAttributes redirectAttributes,
                Model model) {
        User owner = userService.requireByEmail(principal.getName());
        MailAccount selected = selectMailbox(mailAccountService.list(owner), mailboxParam);
        boolean hasAttachment = attachments != null
                && Arrays.stream(attachments).anyMatch(f -> f != null && !f.isEmpty());
        if (replyForm.getBody().isBlank() && !hasAttachment) {
            bindingResult.rejectValue("body", "reply.empty", "Add a message or an attachment.");
        }
        if (bindingResult.hasErrors()) {
            if (!populateChat(owner, selected, key, model)) {
                return "redirect:/chats" + mailboxQuery(selected);
            }
            return "chat";
        }

        ChatConversation conversation = chatFor(owner, selected, key);
        if (conversation == null) {
            return "redirect:/chats" + mailboxQuery(selected);
        }
        List<String> recipients = conversation.members().stream().map(ChatMember::email).toList();
        if (recipients.isEmpty()) {
            redirectAttributes.addFlashAttribute("sendError",
                    "This conversation has no one to send to.");
            return "redirect:/chats/" + key + mailboxQuery(selected);
        }
        List<OutgoingAttachment> outgoing = toOutgoingAttachments(attachments);
        // Send as the selected account so the message comes from that address.
        String from = selected != null ? selected.getUsername() : owner.getEmail();
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
        return "redirect:/chats/" + key + mailboxQuery(selected);
    }

    private boolean populateChat(User owner, MailAccount selected, String key, Model model) {
        ChatConversation conversation = chatFor(owner, selected, key);
        if (conversation == null) {
            return false;
        }
        model.addAttribute("chat", conversation);
        model.addAttribute("billingBanner", billingBannerService.bannerFor(owner).orElse(null));
        // Secondary rail: the other conversations (scoped to the same account),
        // with the open one active.
        PageRequest railPage = PageRequest.of(0, RAIL_SIZE);
        model.addAttribute("conversations", selected != null
                ? conversationListService.list(owner, address(selected), null, railPage)
                : conversationListService.list(owner, railPage));
        model.addAttribute("activeKey", key);
        applyMailboxModel(model, selected);
        return true;
    }

    private ChatConversation chatFor(User owner, MailAccount selected, String key) {
        return selected != null
                ? chatService.buildFor(owner, address(selected), key)
                : chatService.buildFor(owner, key);
    }

    /**
     * The account a view is scoped to: the one whose id matches
     * {@code mailboxParam}, or {@code null} when no (valid) mailbox is specified —
     * the cross-account Dashboard for the list, or the owner-wide timeline for a
     * single chat opened from it.
     */
    private static MailAccount selectMailbox(List<MailAccount> accounts, String mailboxParam) {
        Long id = parseId(mailboxParam);
        if (id == null) {
            return null;
        }
        for (MailAccount account : accounts) {
            if (account.getId().equals(id)) {
                return account;
            }
        }
        return null;
    }

    private static Long parseId(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(raw.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String address(MailAccount account) {
        return account.getUsername() == null ? "" : account.getUsername().trim().toLowerCase(Locale.ROOT);
    }

    private static String mailboxQuery(MailAccount selected) {
        return selected != null ? "?mailbox=" + selected.getId() : "";
    }

    /** Marks the active account so the sidebar highlights it and links carry it. */
    private static void applyMailboxModel(Model model, MailAccount selected) {
        if (selected != null) {
            model.addAttribute("navActiveMailboxId", selected.getId());
            model.addAttribute("selectedMailbox", selected.getUsername());
        }
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
