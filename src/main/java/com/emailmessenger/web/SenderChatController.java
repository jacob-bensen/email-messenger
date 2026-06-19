package com.emailmessenger.web;

import com.emailmessenger.auth.UserService;
import com.emailmessenger.billing.BillingBannerService;
import com.emailmessenger.domain.User;
import com.emailmessenger.service.SenderConversation;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;

/**
 * Renders "everything from this sender" as one IM-style chat that spans every
 * thread the address appears in. Reached from the sender filter on /threads.
 */
@Controller
class SenderChatController {

    private final UserService userService;
    private final SenderChatService senderChatService;
    private final BillingBannerService billingBannerService;
    private final SenderGroupService senderGroupService;

    SenderChatController(UserService userService,
                         SenderChatService senderChatService,
                         BillingBannerService billingBannerService,
                         SenderGroupService senderGroupService) {
        this.userService = userService;
        this.senderChatService = senderChatService;
        this.billingBannerService = billingBannerService;
        this.senderGroupService = senderGroupService;
    }

    @GetMapping("/senders")
    String viewSenderChat(@RequestParam("email") String email, Principal principal, Model model) {
        User owner = userService.requireByEmail(principal.getName());
        SenderConversation conversation = senderChatService.buildFor(owner, email);
        if (conversation == null) {
            return "redirect:/threads";
        }
        model.addAttribute("senderConversation", conversation);
        model.addAttribute("billingBanner", billingBannerService.bannerFor(owner).orElse(null));
        // Powers the persistent left rail; mark this sender active so it highlights.
        model.addAttribute("senderGroups", senderGroupService.topSenders(owner));
        model.addAttribute("activeSender", conversation.sender().email());
        return "sender-chat";
    }
}
