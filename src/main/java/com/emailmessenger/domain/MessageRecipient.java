package com.emailmessenger.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "message_recipients")
public class MessageRecipient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "message_id", nullable = false)
    private Message message;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "participant_id", nullable = false)
    private Participant participant;

    @Enumerated(EnumType.STRING)
    @Column(name = "recipient_type", nullable = false, length = 3)
    private RecipientType recipientType;

    protected MessageRecipient() {}

    public MessageRecipient(Message message, Participant participant, RecipientType recipientType) {
        this.message = message;
        this.participant = participant;
        this.recipientType = recipientType;
    }

    public Long getId() { return id; }
    public Message getMessage() { return message; }
    public Participant getParticipant() { return participant; }
    public RecipientType getRecipientType() { return recipientType; }
}
