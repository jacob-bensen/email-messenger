package com.emailmessenger.web;

import com.emailmessenger.domain.User;
import com.emailmessenger.repository.EmailThreadRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Builds the "drill by sender" sidebar surfaced on `/threads`: each row
 * is a participant who has sent at least one message into one of the
 * user's threads, ranked by distinct thread count so the people the
 * user hears from most show up first.
 */
@Service
public class SenderGroupService {

    static final int DEFAULT_LIMIT = 8;

    private final EmailThreadRepository threads;

    SenderGroupService(EmailThreadRepository threads) {
        this.threads = threads;
    }

    @Transactional(readOnly = true)
    public List<SenderGroup> topSenders(User owner) {
        return topSenders(owner, DEFAULT_LIMIT);
    }

    @Transactional(readOnly = true)
    public List<SenderGroup> topSenders(User owner, int limit) {
        int safeLimit = Math.max(1, limit);
        return threads.topSenders(owner, PageRequest.of(0, safeLimit)).stream()
                .map(SenderGroup::from)
                .toList();
    }

    public record SenderGroup(String email, String displayName, long threadCount,
                              String label, String initials) {
        static SenderGroup from(EmailThreadRepository.SenderGroupRow row) {
            String name = (row.getDisplayName() != null && !row.getDisplayName().isBlank())
                    ? row.getDisplayName().trim()
                    : row.getEmail();
            return new SenderGroup(row.getEmail(), row.getDisplayName(), row.getThreadCount(),
                    name, initialsFor(row.getDisplayName(), row.getEmail()));
        }

        private static String initialsFor(String displayName, String email) {
            String source = (displayName != null && !displayName.isBlank()) ? displayName : email;
            if (source == null || source.isBlank()) return "?";
            if (!source.contains(" ") && source.contains("@")) {
                source = source.substring(0, source.indexOf('@'));
            }
            String[] parts = source.trim().split("\\s+");
            if (parts.length == 0 || parts[0].isBlank()) return "?";
            if (parts.length >= 2 && !parts[1].isBlank()) {
                return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase();
            }
            return parts[0].substring(0, 1).toUpperCase();
        }
    }
}
