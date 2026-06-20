package com.emailmessenger.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * The inbox (`/threads`) and per-sender chat (`/senders`) were replaced by the
 * unified texting-style chats at `/chats`. These bounces keep old bookmarks,
 * emailed deep-links, and existing post-action redirects working — everything
 * funnels into the one chats experience.
 */
@Controller
class ChatRedirectController {

    @GetMapping({"/threads", "/threads/**", "/senders"})
    String toChats() {
        return "redirect:/chats";
    }
}
