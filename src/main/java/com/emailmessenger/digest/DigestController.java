package com.emailmessenger.digest;

import com.emailmessenger.domain.DigestEmailPreference;
import com.emailmessenger.repository.DigestEmailPreferenceRepository;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;

/**
 * Unauthenticated opt-out endpoint for the weekly digest. The token in
 * the URL is the same value the user sees in the email footer; a missing
 * or unknown token renders a generic "invalid link" page rather than
 * leaking whether the token existed.
 */
@Controller
class DigestController {

    private final DigestEmailPreferenceRepository preferences;

    DigestController(DigestEmailPreferenceRepository preferences) {
        this.preferences = preferences;
    }

    @GetMapping("/digest/opt-out")
    @Transactional
    String optOut(@RequestParam(value = "token", required = false) String token, Model model) {
        Optional<DigestEmailPreference> match = (token == null || token.isBlank())
                ? Optional.empty()
                : preferences.findByOptOutToken(token.trim());
        if (match.isEmpty()) {
            model.addAttribute("status", "invalid");
            return "digest/opt-out";
        }
        DigestEmailPreference prefs = match.get();
        if (!prefs.isOptedOut()) {
            prefs.setOptedOut(true);
            preferences.save(prefs);
        }
        model.addAttribute("status", "ok");
        model.addAttribute("email", prefs.getUser().getEmail());
        return "digest/opt-out";
    }
}
