package com.emailmessenger.web;

import com.emailmessenger.billing.PlanLimitService;
import com.emailmessenger.domain.SavedSearch;
import com.emailmessenger.domain.User;
import com.emailmessenger.repository.SavedSearchRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * CRUD for {@link SavedSearch} with plan-cap enforcement. Free users get one
 * saved search; paid plans are unlimited. {@link DuplicateSavedSearchNameException}
 * surfaces as a form-level error rather than a 500 so the user can pick a
 * different name without losing the rest of the filter context.
 */
@Service
public class SavedSearchService {

    static final int MAX_NAME_LENGTH = 80;
    static final int MAX_QUERY_LENGTH = 200;

    private final SavedSearchRepository repository;
    private final PlanLimitService planLimits;
    private final SavedSearchCountService countService;

    SavedSearchService(SavedSearchRepository repository,
                       PlanLimitService planLimits,
                       SavedSearchCountService countService) {
        this.repository = repository;
        this.planLimits = planLimits;
        this.countService = countService;
    }

    @Transactional(readOnly = true)
    public List<SavedSearch> list(User owner) {
        return repository.findByOwnerOrderByCreatedAtAsc(owner);
    }

    @Transactional(readOnly = true)
    public List<SavedSearchView> viewsFor(User owner) {
        return countService.viewsFor(owner, list(owner));
    }

    @Transactional
    public void markViewed(User owner, Long id, LocalDateTime when) {
        repository.findByIdAndOwner(id, owner).ifPresent(s -> {
            s.setLastViewedAt(when);
            repository.save(s);
        });
    }

    @Transactional
    public SavedSearch create(User owner, String name, String query, String senderEmail,
                              String sincePreset, boolean requireUnread, boolean requireAttachments) {
        String trimmedName = name == null ? "" : name.trim();
        if (trimmedName.isEmpty()) {
            throw new IllegalArgumentException("Saved search name is required");
        }
        if (trimmedName.length() > MAX_NAME_LENGTH) {
            trimmedName = trimmedName.substring(0, MAX_NAME_LENGTH);
        }
        String trimmedQuery = blankToNull(query);
        if (trimmedQuery != null && trimmedQuery.length() > MAX_QUERY_LENGTH) {
            trimmedQuery = trimmedQuery.substring(0, MAX_QUERY_LENGTH);
        }
        String normalizedSender = blankToNull(senderEmail);
        String normalizedPreset = normalizePreset(sincePreset);
        if (trimmedQuery == null && normalizedSender == null && normalizedPreset == null
                && !requireUnread && !requireAttachments) {
            throw new IllegalArgumentException("Saved search must include at least one filter");
        }
        Optional<SavedSearch> clash = repository.findByOwnerAndName(owner, trimmedName);
        if (clash.isPresent()) {
            throw new DuplicateSavedSearchNameException(trimmedName);
        }
        // Cap check after we know the input is good — a Free user hitting the
        // dupe path shouldn't also get the upgrade modal on the same submit.
        planLimits.enforceCanCreateSavedSearch(owner);
        SavedSearch entity = new SavedSearch(owner, trimmedName, trimmedQuery, normalizedSender,
                normalizedPreset, requireUnread, requireAttachments);
        return repository.save(entity);
    }

    @Transactional
    public void delete(User owner, Long id) {
        SavedSearch s = repository.findByIdAndOwner(id, owner)
                .orElseThrow(NoSuchElementException::new);
        repository.delete(s);
    }

    private static String blankToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static String normalizePreset(String preset) {
        if (preset == null) return null;
        String s = preset.trim().toLowerCase();
        return switch (s) {
            case "7d", "30d", "90d" -> s;
            default -> null;
        };
    }

    public static class DuplicateSavedSearchNameException extends RuntimeException {
        private final String name;
        public DuplicateSavedSearchNameException(String name) {
            super("A saved search named '" + name + "' already exists");
            this.name = name;
        }
        public String getName() { return name; }
    }
}
