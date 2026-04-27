package com.emailmessenger.web;

import com.emailmessenger.domain.EmailThread;
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

import java.time.LocalDate;
import java.util.NoSuchElementException;

@Controller
class ThreadController {

    private static final int PAGE_SIZE = 20;

    private final EmailThreadRepository threadRepository;
    private final ThreadViewService threadViewService;
    private final ReplyService replyService;

    ThreadController(EmailThreadRepository threadRepository,
                     ThreadViewService threadViewService,
                     ReplyService replyService) {
        this.threadRepository = threadRepository;
        this.threadViewService = threadViewService;
        this.replyService = replyService;
    }

    @GetMapping("/threads")
    String listThreads(@RequestParam(defaultValue = "0") int page, Model model) {
        Page<EmailThread> threads = threadRepository.findAllByOrderByUpdatedAtDesc(
                PageRequest.of(Math.max(0, page), PAGE_SIZE));
        model.addAttribute("threads", threads);
        LocalDate today = LocalDate.now();
        model.addAttribute("today", today);
        model.addAttribute("yesterday", today.minusDays(1));
        return "threads";
    }

    @GetMapping("/threads/{id}")
    String viewConversation(@PathVariable Long id, Model model) {
        Conversation conversation = threadViewService.getConversation(id);
        model.addAttribute("conversation", conversation);
        model.addAttribute("replyForm", new ReplyForm());
        return "conversation";
    }

    @PostMapping("/threads/{id}/reply")
    String reply(@PathVariable Long id,
                 @Valid @ModelAttribute("replyForm") ReplyForm replyForm,
                 BindingResult bindingResult,
                 RedirectAttributes redirectAttributes,
                 Model model) {
        if (bindingResult.hasErrors()) {
            Conversation conversation = threadViewService.getConversation(id);
            model.addAttribute("conversation", conversation);
            return "conversation";
        }
        EmailThread thread = threadRepository.findById(id)
                .orElseThrow(NoSuchElementException::new);
        replyService.sendReply(id, thread.getSubject(), replyForm.getBody());
        redirectAttributes.addFlashAttribute("successMessage", "Reply sent successfully.");
        return "redirect:/threads/" + id;
    }
}
