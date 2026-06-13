package com.emailmessenger.web;

import com.emailmessenger.auth.UserService;
import com.emailmessenger.domain.User;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;

import java.security.Principal;

/**
 * Save / delete endpoints for {@link com.emailmessenger.domain.SavedSearch}.
 * Reads come through {@link ThreadController} since they share the inbox
 * page. PlanLimitExceededException is intentionally not caught — it
 * propagates to the GlobalExceptionHandler which renders the upgrade modal
 * over /threads, the same path used for thread + mailbox caps.
 */
@Controller
@RequestMapping("/searches")
class SavedSearchController {

    private final SavedSearchService savedSearchService;
    private final UserService userService;

    SavedSearchController(SavedSearchService savedSearchService, UserService userService) {
        this.savedSearchService = savedSearchService;
        this.userService = userService;
    }

    @PostMapping
    String save(@RequestParam("name") String name,
                @RequestParam(name = "q", required = false) String query,
                @RequestParam(name = "from", required = false) String fromSender,
                @RequestParam(name = "since", required = false) String since,
                @RequestParam(name = "unread", defaultValue = "false") boolean unread,
                @RequestParam(name = "attachments", defaultValue = "false") boolean attachments,
                Principal principal,
                RedirectAttributes redirectAttributes) {
        User owner = userService.requireByEmail(principal.getName());
        try {
            savedSearchService.create(owner, name, query, fromSender, since, unread, attachments);
            redirectAttributes.addFlashAttribute("savedSearchMessage",
                    "Saved \"" + name.trim() + "\".");
        } catch (SavedSearchService.DuplicateSavedSearchNameException e) {
            redirectAttributes.addFlashAttribute("savedSearchError",
                    "You already have a saved search named \"" + e.getName() + "\".");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("savedSearchError", e.getMessage());
        }
        return "redirect:" + threadsUrl(query, fromSender, since, unread, attachments);
    }

    @PostMapping("/{id}/delete")
    String delete(@PathVariable Long id,
                  @RequestParam(name = "q", required = false) String query,
                  @RequestParam(name = "from", required = false) String fromSender,
                  @RequestParam(name = "since", required = false) String since,
                  @RequestParam(name = "unread", defaultValue = "false") boolean unread,
                  @RequestParam(name = "attachments", defaultValue = "false") boolean attachments,
                  Principal principal,
                  RedirectAttributes redirectAttributes) {
        User owner = userService.requireByEmail(principal.getName());
        savedSearchService.delete(owner, id);
        redirectAttributes.addFlashAttribute("savedSearchMessage", "Saved search deleted.");
        return "redirect:" + threadsUrl(query, fromSender, since, unread, attachments);
    }

    private static String threadsUrl(String query, String fromSender, String since,
                                     boolean unread, boolean attachments) {
        UriComponentsBuilder b = UriComponentsBuilder.fromPath("/threads");
        if (query != null && !query.isBlank()) b.queryParam("q", query.trim());
        if (fromSender != null && !fromSender.isBlank()) b.queryParam("from", fromSender.trim());
        if (since != null && !since.isBlank()) b.queryParam("since", since.trim());
        if (unread) b.queryParam("unread", "true");
        if (attachments) b.queryParam("attachments", "true");
        return b.build().toUriString();
    }
}
