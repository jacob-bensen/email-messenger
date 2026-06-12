package com.emailmessenger.team;

import com.emailmessenger.domain.Team;
import com.emailmessenger.domain.TeamMember;
import com.emailmessenger.domain.TeamMemberRole;
import com.emailmessenger.domain.User;
import com.emailmessenger.repository.TeamMemberRepository;
import com.emailmessenger.repository.TeamRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TeamService {

    private final TeamRepository teams;
    private final TeamMemberRepository teamMembers;

    TeamService(TeamRepository teams, TeamMemberRepository teamMembers) {
        this.teams = teams;
        this.teamMembers = teamMembers;
    }

    /**
     * Returns the team owned by {@code owner}, lazy-creating one on
     * first use. The owner is also inserted as a {@link TeamMemberRole#OWNER}
     * row so {@code team_members.count(team)} always reflects the true
     * size of the team.
     */
    @Transactional
    public Team findOrCreateOwnedTeam(User owner) {
        return teams.findByOwnerUser(owner).orElseGet(() -> {
            Team created = teams.save(new Team(defaultName(owner), owner));
            teamMembers.save(new TeamMember(created, owner, TeamMemberRole.OWNER));
            return created;
        });
    }

    private static String defaultName(User owner) {
        String name = owner.getDisplayName();
        String label = (name != null && !name.isBlank()) ? name.trim() : owner.getEmail();
        String candidate = label + "'s team";
        return candidate.length() > 120 ? candidate.substring(0, 120) : candidate;
    }
}
