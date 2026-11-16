package com.emailmessenger.api;

import com.emailmessenger.auth.UserService;
import com.emailmessenger.domain.Attachment;
import com.emailmessenger.domain.EmailThread;
import com.emailmessenger.domain.MailAccount;
import com.emailmessenger.domain.Message;
import com.emailmessenger.domain.Participant;
import com.emailmessenger.domain.RecipientType;
import com.emailmessenger.domain.User;
import com.emailmessenger.email.OwnerAddressService;
import com.emailmessenger.repository.EmailThreadRepository;
import com.emailmessenger.repository.MailAccountRepository;
import com.emailmessenger.repository.MessageRepository;
import com.emailmessenger.repository.ParticipantRepository;
import com.emailmessenger.service.ConversationKeyService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ApiConversationIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserService userService;
    @Autowired ConversationKeyService keyService;
    @Autowired OwnerAddressService ownerAddressService;
    @Autowired EmailThreadRepository threads;
    @Autowired MessageRepository messages;
    @Autowired ParticipantRepository participants;
    @Autowired MailAccountRepository mailAccounts;
    @PersistenceContext EntityManager em;

    private User owner;
    private Participant me;
    private String token;

    @BeforeEach
    void setUp() throws Exception {
        owner = userService.register("ada@example.com", "correct-horse", "Ada Lovelace");
        me = participants.save(new Participant("ada@example.com", "Ada Lovelace"));
        String json = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ApiDtos.LoginRequest("ada@example.com", "correct-horse", null))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        token = objectMapper.readTree(json).get("token").asText();
    }

    @Test
    void listsConversationsMostRecentFirstWithCounts() throws Exception {
        Participant bob = participants.save(new Participant("bob@acme.com", "Bob Builder"));
        Participant cy = participants.save(new Participant("cy@acme.com", "Cy"));
        inbound("Old", bob, List.of(me), "older message", false, LocalDateTime.of(2026, 1, 1, 9, 0));
        inbound("New", cy, List.of(me), "newest message", true, LocalDateTime.of(2026, 1, 2, 9, 30));

        mockMvc.perform(get("/api/v1/conversations").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.items[0].title").value("Cy"))
                .andExpect(jsonPath("$.items[0].initials").value("C"))
                .andExpect(jsonPath("$.items[0].unread").value(true))
                .andExpect(jsonPath("$.items[0].preview").value("newest message"))
                .andExpect(jsonPath("$.items[0].lastActivityAt").value("2026-01-02T09:30:00Z"))
                .andExpect(jsonPath("$.items[0].key").value(
                        keyService.keyFor(List.of("cy@acme.com"), ownerAddressService.addressesFor(owner))))
                .andExpect(jsonPath("$.items[0].group").value(false))
                .andExpect(jsonPath("$.items[0].memberCount").value(1))
                .andExpect(jsonPath("$.items[0].threadCount").value(1))
                .andExpect(jsonPath("$.items[0].senderEmail").value("cy@acme.com"))
                .andExpect(jsonPath("$.items[1].title").value("Bob Builder"))
                .andExpect(jsonPath("$.items[1].initials").value("BB"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(30))
                .andExpect(jsonPath("$.totalItems").value(2))
                .andExpect(jsonPath("$.hasNext").value(false))
                .andExpect(jsonPath("$.counts.all").value(2))
                .andExpect(jsonPath("$.counts.unread").value(1))
                .andExpect(jsonPath("$.counts.awaiting").value(2))
                .andExpect(jsonPath("$.counts.attachments").value(0));
    }

    @Test
    void filtersSearchesAndPages() throws Exception {
        Participant bob = participants.save(new Participant("bob@acme.com", "Bob"));
        Participant cy = participants.save(new Participant("cy@acme.com", "Cy"));
        inbound("Lunch", bob, List.of(me), "pizza?", false, LocalDateTime.of(2026, 1, 1, 9, 0));
        inbound("Report", cy, List.of(me), "numbers", true, LocalDateTime.of(2026, 1, 2, 9, 0));

        mockMvc.perform(get("/api/v1/conversations?filter=UNREAD").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].title").value("Cy"))
                .andExpect(jsonPath("$.counts.all").value(2));

        mockMvc.perform(get("/api/v1/conversations?q=pizza").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].title").value("Bob"));

        mockMvc.perform(get("/api/v1/conversations?size=1&page=0").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.totalItems").value(2))
                .andExpect(jsonPath("$.hasNext").value(true));
    }

    @Test
    void scopesToAMailboxOnlyWhenTheUserHasSeveral() throws Exception {
        MailAccount personal = mailAccounts.save(new MailAccount(owner, "imap.example.com", 993, true,
                "ada@example.com", "ciphertext"));
        MailAccount work = mailAccounts.save(new MailAccount(owner, "imap.acme.com", 993, true,
                "work@acme.com", "ciphertext"));
        Participant workAddress = participants.save(new Participant("work@acme.com", "Work"));
        Participant bob = participants.save(new Participant("bob@acme.com", "Bob"));
        Participant cy = participants.save(new Participant("cy@acme.com", "Cy"));
        inbound("Work", bob, List.of(workAddress), "work", false, LocalDateTime.of(2026, 1, 1, 9, 0));
        inbound("Home", cy, List.of(me), "home", false, LocalDateTime.of(2026, 1, 2, 9, 0));

        mockMvc.perform(get("/api/v1/conversations?mailboxId=" + work.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].title").value("Bob"))
                .andExpect(jsonPath("$.items[0].accounts", hasSize(0)))
                .andExpect(jsonPath("$.counts.all").value(1));

        mockMvc.perform(get("/api/v1/conversations?mailboxId=" + personal.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.items[0].title").value("Cy"));

        mockMvc.perform(get("/api/v1/conversations").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.items[1].accounts[0]").value("work@acme.com"));
    }

    @Test
    void anotherUsersMailboxIsNotFound() throws Exception {
        User other = userService.register("eve@example.com", "correct-horse", "Eve");
        MailAccount theirs = mailAccounts.save(new MailAccount(other, "imap.example.com", 993, true,
                "eve@example.com", "ciphertext"));

        mockMvc.perform(get("/api/v1/conversations?mailboxId=" + theirs.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("not_found"));
    }

    @Test
    void rejectsBadParameters() throws Exception {
        for (String query : List.of("filter=starred", "size=0", "size=101", "page=-1", "page=abc", "mailboxId=x")) {
            mockMvc.perform(get("/api/v1/conversations?" + query).header("Authorization", "Bearer " + token))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value("validation_failed"));
        }
    }

    @Test
    void requiresABearerToken() throws Exception {
        mockMvc.perform(get("/api/v1/conversations"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("unauthorized"));
    }

    @Test
    void anEmptyMailboxIsAnEmptyPage() throws Exception {
        mockMvc.perform(get("/api/v1/conversations").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(0)))
                .andExpect(jsonPath("$.totalItems").value(0))
                .andExpect(jsonPath("$.hasNext").value(false));
    }

    @Test
    void returnsOneConversationAsChronologicalRunsWithoutMarkingItRead() throws Exception {
        Participant bob = participants.save(new Participant("bob@acme.com", "Bob Builder"));
        EmailThread lunch = threads.save(new EmailThread(owner, "Lunch", "<lunch@x>"));
        Message ask = message(lunch, bob, me, "pizza at noon?", LocalDateTime.of(2026, 1, 1, 9, 0));
        ask.setMessageIdHeader("<ask@x>");
        ask.addAttachment(new Attachment(ask, "menu.pdf", "application/pdf", 2048L, null));
        Message again = message(lunch, bob, me, "or sushi", LocalDateTime.of(2026, 1, 1, 9, 1));
        Message reply = message(lunch, me, bob, "pizza!", LocalDateTime.of(2026, 1, 1, 9, 5));
        reply.markOutbound();
        reply.setInReplyTo("<ask@x>");
        save(lunch, true, ask, again, reply);
        EmailThread report = threads.save(new EmailThread(owner, "Report", "<report@x>"));
        save(report, false, message(report, bob, me, "numbers attached", LocalDateTime.of(2026, 1, 2, 8, 0)));
        String key = keyService.keyFor(List.of("bob@acme.com"), ownerAddressService.addressesFor(owner));

        mockMvc.perform(get("/api/v1/conversations/" + key).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.key").value(key))
                .andExpect(jsonPath("$.title").value("Bob Builder"))
                .andExpect(jsonPath("$.initials").value("BB"))
                .andExpect(jsonPath("$.group").value(false))
                .andExpect(jsonPath("$.threadCount").value(2))
                .andExpect(jsonPath("$.members", hasSize(1)))
                .andExpect(jsonPath("$.members[0].email").value("bob@acme.com"))
                .andExpect(jsonPath("$.members[0].label").value("Bob Builder"))
                .andExpect(jsonPath("$.runs", hasSize(3)))
                .andExpect(jsonPath("$.runs[0].outbound").value(false))
                .andExpect(jsonPath("$.runs[0].sender.email").value("bob@acme.com"))
                .andExpect(jsonPath("$.runs[0].thread.subject").value("Lunch"))
                .andExpect(jsonPath("$.runs[0].messages", hasSize(2)))
                .andExpect(jsonPath("$.runs[0].messages[0].sentAt").value("2026-01-01T09:00:00Z"))
                .andExpect(jsonPath("$.runs[0].messages[0].bodyText").value("pizza at noon?"))
                .andExpect(jsonPath("$.runs[0].messages[0].replyToId").value(nullValue()))
                .andExpect(jsonPath("$.runs[0].messages[0].attachments[0].filename").value("menu.pdf"))
                .andExpect(jsonPath("$.runs[0].messages[0].attachments[0].mimeType").value("application/pdf"))
                .andExpect(jsonPath("$.runs[0].messages[0].attachments[0].sizeBytes").value(2048))
                .andExpect(jsonPath("$.runs[0].messages[1].bodyText").value("or sushi"))
                .andExpect(jsonPath("$.runs[1].outbound").value(true))
                .andExpect(jsonPath("$.runs[1].messages[0].bodyText").value("pizza!"))
                .andExpect(jsonPath("$.runs[1].messages[0].replyToId").value(ask.getId()))
                .andExpect(jsonPath("$.runs[2].thread.subject").value("Report"));

        mockMvc.perform(get("/api/v1/conversations?filter=unread").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.items", hasSize(1)));
    }

    @Test
    void markReadClearsUnreadAndIsIdempotent() throws Exception {
        Participant bob = participants.save(new Participant("bob@acme.com", "Bob"));
        inbound("Lunch", bob, List.of(me), "pizza?", true, LocalDateTime.of(2026, 1, 1, 9, 0));
        inbound("Report", bob, List.of(me), "numbers", true, LocalDateTime.of(2026, 1, 2, 9, 0));
        String key = keyService.keyFor(List.of("bob@acme.com"), ownerAddressService.addressesFor(owner));

        for (int i = 0; i < 2; i++) {
            mockMvc.perform(post("/api/v1/conversations/" + key + "/read")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isNoContent());
        }

        mockMvc.perform(get("/api/v1/conversations").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.items[0].unread").value(false))
                .andExpect(jsonPath("$.counts.unread").value(0));
    }

    @Test
    void markReadInOneMailboxLeavesTheOtherUnread() throws Exception {
        MailAccount personal = mailAccounts.save(new MailAccount(owner, "imap.example.com", 993, true,
                "ada@example.com", "ciphertext"));
        mailAccounts.save(new MailAccount(owner, "imap.acme.com", 993, true, "work@acme.com", "ciphertext"));
        Participant workAddress = participants.save(new Participant("work@acme.com", "Work"));
        Participant bob = participants.save(new Participant("bob@acme.com", "Bob"));
        inbound("Work", bob, List.of(workAddress), "work", true, LocalDateTime.of(2026, 1, 1, 9, 0));
        inbound("Home", bob, List.of(me), "home", true, LocalDateTime.of(2026, 1, 2, 9, 0));
        String key = keyService.keyFor(List.of("bob@acme.com"), ownerAddressService.addressesFor(owner));

        mockMvc.perform(get("/api/v1/conversations/" + key + "?mailboxId=" + personal.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.threadCount").value(1))
                .andExpect(jsonPath("$.runs[0].messages[0].bodyText").value("home"));

        mockMvc.perform(post("/api/v1/conversations/" + key + "/read?mailboxId=" + personal.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/conversations").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.items[0].unread").value(true));
    }

    @Test
    void unknownOrForeignConversationIsNotFound() throws Exception {
        User other = userService.register("eve@example.com", "correct-horse", "Eve");
        Participant bob = participants.save(new Participant("bob@acme.com", "Bob"));
        Participant eve = participants.save(new Participant("eve@example.com", "Eve"));
        EmailThread theirs = threads.save(new EmailThread(other, "Secret", "<secret@x>"));
        Message m = message(theirs, bob, eve, "for eve only", LocalDateTime.of(2026, 1, 1, 9, 0));
        theirs.addMessage(messages.save(m), true);
        theirs.setConversationKey(keyService.compute(theirs, ownerAddressService.addressesFor(other)));
        threads.saveAndFlush(theirs);
        String theirKey = theirs.getConversationKey();

        for (String key : List.of(theirKey, "0123456789abcdef0123456789abcdef")) {
            mockMvc.perform(get("/api/v1/conversations/" + key).header("Authorization", "Bearer " + token))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error.code").value("not_found"));
            mockMvc.perform(post("/api/v1/conversations/" + key + "/read")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error.code").value("not_found"));
        }
        em.clear();
        assertThat(threads.findById(theirs.getId()).orElseThrow().isUnread()).isTrue();
    }

    @Test
    void conversationEndpointsRequireABearerToken() throws Exception {
        mockMvc.perform(get("/api/v1/conversations/0123456789abcdef0123456789abcdef"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/conversations/0123456789abcdef0123456789abcdef/read"))
                .andExpect(status().isUnauthorized());
    }

    private Message message(EmailThread thread, Participant from, Participant to, String body, LocalDateTime at) {
        Message m = new Message(thread, from, thread.getSubject(), body, null, at);
        m.addRecipient(to, RecipientType.TO);
        return m;
    }

    private void save(EmailThread thread, boolean unread, Message... batch) {
        for (Message m : batch) {
            thread.addMessage(messages.save(m), unread);
        }
        if (!unread) {
            thread.markRead();
        }
        thread.setConversationKey(keyService.compute(thread, ownerAddressService.addressesFor(owner)));
        threads.saveAndFlush(thread);
        em.flush();
        em.clear();
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
        thread.setConversationKey(keyService.compute(thread, ownerAddressService.addressesFor(owner)));
        threads.saveAndFlush(thread);
        em.flush();
        em.clear();
    }
}
