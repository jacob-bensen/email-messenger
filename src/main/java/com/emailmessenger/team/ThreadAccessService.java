package com.emailmessenger.team;

import com.emailmessenger.domain.EmailThread;
import com.emailmessenger.domain.Team;
import com.emailmessenger.domain.User;
import com.emailmessenger.repository.EmailThreadRepository;
import com.emailmessenger.repository.TeamMemberRepository;
import com.emailmessenger.repository.TeamRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Shared-inbox visibility check. M1 of EPIC-16 scoped thread access to the
 * mailbox owner; M2 widens it: any member of the owner's team can follow a
 * shared link to a thread and reach the conversation view + notes panel.
 *
 * <p>Reply still uses {@code findByIdAndOwner} on the controller (SMTP
 * credentials are owner-only), so this only governs read access and notes.
 */
@Service
public class ThreadAccessService {

    private final EmailThreadRepository threads;
    private final TeamRepository teams;
    private final TeamMemberRepository teamMembers;

    ThreadAccessService(EmailThreadRepository threads,
                        TeamRepository teams,
                        TeamMemberRepository teamMembers) {
        this.threads = threads;
        this.teams = teams;
        this.teamMembers = teamMembers;
    }

    @Transactional(readOnly = true)
    public Optional<EmailThread> findAccessibleThread(Long id, User viewer) {
        return threads.findById(id).filter(t -> isAccessibleTo(t, viewer));
    }

    @Transactional(readOnly = true)
    public boolean isAccessibleTo(EmailThread thread, User viewer) {
        if (isOwner(thread, viewer)) {
            return true;
        }
        return ownerTeamFor(thread)
                .map(team -> teamMembers.existsByTeamAndUser(team, viewer))
                .orElse(false);
    }

    public boolean isOwner(EmailThread thread, User viewer) {
        Long ownerId = thread.getOwner().getId();
        return ownerId != null && viewer != null && ownerId.equals(viewer.getId());
    }

    private Optional<Team> ownerTeamFor(EmailThread thread) {
        return teams.findByOwnerUser(thread.getOwner());
    }
}
