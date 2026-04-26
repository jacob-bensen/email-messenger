package com.emailmessenger.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "participants")
public class Participant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 254)
    private String email;

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected Participant() {}

    public Participant(String email, String displayName) {
        this.email = email;
        this.displayName = displayName;
    }

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getDisplayName() { return displayName; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setDisplayName(String displayName) { this.displayName = displayName; }

    /**
     * Returns 1–2 uppercase initials for avatar fallback.
     * "Alice Bob" → "AB", "alice@test.com" → "A", "Charlie" → "C".
     */
    public String initials() {
        String name = (displayName != null && !displayName.isBlank()) ? displayName : email;
        // For bare email addresses, use only the local part before @
        if (!name.contains(" ") && name.contains("@")) {
            name = name.substring(0, name.indexOf('@'));
        }
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 0 || parts[0].isBlank()) return "?";
        if (parts.length >= 2 && !parts[1].isBlank()) {
            return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase();
        }
        return parts[0].substring(0, 1).toUpperCase();
    }
}
