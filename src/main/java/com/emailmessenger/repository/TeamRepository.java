package com.emailmessenger.repository;

import com.emailmessenger.domain.Team;
import com.emailmessenger.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TeamRepository extends JpaRepository<Team, Long> {

    Optional<Team> findByOwnerUser(User ownerUser);
}
