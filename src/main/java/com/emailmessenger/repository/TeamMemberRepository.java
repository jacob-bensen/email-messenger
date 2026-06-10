package com.emailmessenger.repository;

import com.emailmessenger.domain.Team;
import com.emailmessenger.domain.TeamMember;
import com.emailmessenger.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {

    Optional<TeamMember> findByTeamAndUser(Team team, User user);

    long countByTeam(Team team);
}
