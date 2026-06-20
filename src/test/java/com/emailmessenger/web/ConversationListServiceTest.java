package com.emailmessenger.web;

import com.emailmessenger.domain.EmailThread;
import com.emailmessenger.domain.Message;
import com.emailmessenger.domain.Participant;
import com.emailmessenger.domain.RecipientType;
import com.emailmessenger.domain.User;
import com.emailmessenger.email.OwnerAddressService;
import com.emailmessenger.repository.EmailThreadRepository;
import com.emailmessenger.repository.MessageRepository;
import com.emailmessenger.repository.ParticipantRepository;
import com.emailmessenger.repository.UserRepository;
import com.emailmessenger.service.ConversationKeyService;
import com.emailmessenger.service.ConversationListItem;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises the participant-set grouping queries end-to-end against H2: a 1:1
 * and a group conversation (each spanning the threads that share its key) must
 * collapse into one row apiece, ordered most-recent first, with the right
 * labels, preview, and unread flag.
 */
@SpringBootTest
@Transactional
class ConversationListServiceTest {

    @Autowired ConversationListService listService;
    @Autowired ConversationKeyService keyService;
    @Autowired OwnerAddressService ownerAddressService;
    @Autowired UserRepository users;
    @Autowired EmailThreadRepository threads;
    @Autowired MessageRepository messages;
    @Autowired ParticipantRepository participants;

    @PersistenceContext EntityManager em;

    private User owner;
    private Participant me;

    @BeforeEach
    void setUp() {
        owner = users.save(new User("owner@chat.com", "hash", "Owner"));
        me = participants.save(new Participant("owner@chat.com", "Owner"));
    }

    @Test
    void collapsesThreadsByPeopleAndOrdersMostRecentFirst() {
        Participant ada = participants.save(new Participant("ada@acme.com", "Ada Lovelace"));
        Participant bob = participants.save(new Participant("bob@acme.com", "Bob"));

        // 1:1 with Ada across TWO subjects → one conversation. Older activity.
        // Both threads read, so the conversation reads as fully caught up.
        inbound("Invoice", ada, List.of(me), "About the invoice", false,
                LocalDateTime.of(2026, 1, 1, 9, 0));
        inbound("Lunch", ada, List.of(me), "Lunch Friday?", false,
                LocalDateTime.of(2026, 1, 2, 9, 0));
        // Group with Ada + Bob → a separate conversation. Newer activity.
        inbound("Project", ada, List.of(me, bob), "Kickoff Monday", true,
                LocalDateTime.of(2026, 1, 3, 9, 0));

        Page<ConversationListItem> page = listService.list(owner, PageRequest.of(0, 20));

        assertThat(page.getTotalElements()).isEqualTo(2);
        List<ConversationListItem> items = page.getContent();
        // Group is most recent → first.
        assertThat(items.get(0).group()).isTrue();
        assertThat(items.get(0).title()).contains("Ada").contains("Bob");
        assertThat(items.get(0).preview()).contains("Kickoff Monday");
        assertThat(items.get(0).unread()).isTrue();

        // 1:1 with Ada → one row spanning both subjects (threadCount 2).
        assertThat(items.get(1).group()).isFalse();
        assertThat(items.get(1).title()).isEqualTo("Ada Lovelace");
        assertThat(items.get(1).threadCount()).isEqualTo(2);
        assertThat(items.get(1).preview()).contains("Lunch Friday");
        // Latest of the two threads was read → no unread on the conversation.
        assertThat(items.get(1).unread()).isFalse();
    }

    @Test
    void outboundLatestMessageIsPreviewedAsYou() {
        Participant ada = participants.save(new Participant("ada@acme.com", "Ada"));
        inbound("Q", ada, List.of(me), "A question", false,
                LocalDateTime.of(2026, 1, 1, 9, 0));
        outboundReply("Q", ada, "My answer", LocalDateTime.of(2026, 1, 1, 10, 0));

        ConversationListItem item = listService.list(owner, PageRequest.of(0, 20)).getContent().get(0);
        assertThat(item.preview()).startsWith("You: ").contains("My answer");
    }

    private void inbound(String subject, Participant sender, List<Participant> to, String body,
                         boolean unread, LocalDateTime at) {
        EmailThread thread = threads.save(new EmailThread(owner, subject, "<" + subject + "@x>"));
        Message m = new Message(thread, sender, subject, body, null, at);
        for (Participant p : to) {
            m.addRecipient(p, RecipientType.TO);
        }
        thread.addMessage(messages.save(m), unread);
        if (!unread) {
            thread.markRead();
        }
        finalizeThread(thread, at);
    }

    private void outboundReply(String subject, Participant correspondent, String body, LocalDateTime at) {
        EmailThread thread = threads.findThreadsByConversationKey(
                owner, keyService.keyFor(List.of(correspondent.getEmail()),
                        ownerAddressService.addressesFor(owner))).get(0);
        Message m = new Message(thread, me, "Re: " + subject, body, null, at);
        m.markOutbound();
        m.addRecipient(correspondent, RecipientType.TO);
        thread.addMessage(messages.save(m));
        thread.markRead();
        finalizeThread(thread, at);
    }

    private void finalizeThread(EmailThread thread, LocalDateTime at) {
        thread.setConversationKey(
                keyService.compute(thread, ownerAddressService.addressesFor(owner)));
        threads.saveAndFlush(thread);
        // Force updatedAt so ordering is deterministic (addMessage stamps "now").
        em.createQuery("update EmailThread t set t.updatedAt = :ts where t.id = :id")
                .setParameter("ts", at).setParameter("id", thread.getId()).executeUpdate();
        em.flush();
        em.clear();
    }
}
