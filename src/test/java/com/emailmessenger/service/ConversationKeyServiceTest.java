package com.emailmessenger.service;

import com.emailmessenger.domain.EmailThread;
import com.emailmessenger.domain.Message;
import com.emailmessenger.domain.Participant;
import com.emailmessenger.domain.RecipientType;
import com.emailmessenger.domain.User;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ConversationKeyServiceTest {

    private static final LocalDateTime T = LocalDateTime.of(2026, 1, 1, 9, 0);
    private static final Set<String> OWNER = Set.of("me@acme.com");

    private final ConversationKeyService svc = new ConversationKeyService();

    private final User owner = new User("me@acme.com", "h", null);

    private EmailThread thread() {
        return new EmailThread(owner, "Subject", "<root@x>");
    }

    private Message inbound(EmailThread t, Participant sender) {
        Message m = new Message(t, sender, "Subject", "body", null, T);
        t.addMessage(m);
        return m;
    }

    @Test
    void oneToOneKeyIsJustTheCorrespondent() {
        EmailThread t = thread();
        inbound(t, new Participant("ada@acme.com", "Ada"));

        assertThat(svc.compute(t, OWNER))
                .isEqualTo(svc.keyFor(List.of("ada@acme.com"), OWNER))
                .isNotBlank();
    }

    @Test
    void ownerAddressesAreExcludedFromTheKey() {
        EmailThread t = thread();
        Message m = inbound(t, new Participant("ada@acme.com", "Ada"));
        m.addRecipient(new Participant("me@acme.com", "Me"), RecipientType.TO);

        // Owner on the To line doesn't change the conversation: still just Ada.
        assertThat(svc.compute(t, OWNER)).isEqualTo(svc.keyFor(List.of("ada@acme.com"), OWNER));
    }

    @Test
    void keyIsIndependentOfCaseAndOrderAndWhichSideSent() {
        EmailThread a = thread();
        Message m1 = inbound(a, new Participant("ada@acme.com", "Ada"));
        m1.addRecipient(new Participant("BOB@acme.com", "Bob"), RecipientType.CC);

        // Same people, different casing/order, and the owner as sender (outbound).
        EmailThread b = thread();
        Message out = new Message(b, new Participant("me@acme.com", "Me"), "Subject", "x", null, T);
        out.markOutbound();
        out.addRecipient(new Participant("Bob@ACME.com", "Bob"), RecipientType.TO);
        out.addRecipient(new Participant("Ada@acme.com", "Ada"), RecipientType.TO);
        b.addMessage(out);

        assertThat(svc.compute(a, OWNER)).isEqualTo(svc.compute(b, OWNER));
    }

    @Test
    void groupKeyDiffersFromEachMemberOneToOne() {
        EmailThread group = thread();
        Message m = inbound(group, new Participant("ada@acme.com", "Ada"));
        m.addRecipient(new Participant("bob@acme.com", "Bob"), RecipientType.TO);

        String groupKey = svc.compute(group, OWNER);
        assertThat(groupKey).isNotEqualTo(svc.keyFor(List.of("ada@acme.com"), OWNER));
        assertThat(groupKey).isNotEqualTo(svc.keyFor(List.of("bob@acme.com"), OWNER));
        assertThat(groupKey).isEqualTo(svc.keyFor(List.of("ada@acme.com", "bob@acme.com"), OWNER));
    }

    @Test
    void bccDoesNotChangeMembership() {
        EmailThread withBcc = thread();
        Message m = inbound(withBcc, new Participant("ada@acme.com", "Ada"));
        m.addRecipient(new Participant("secret@acme.com", "Secret"), RecipientType.BCC);

        assertThat(svc.compute(withBcc, OWNER)).isEqualTo(svc.keyFor(List.of("ada@acme.com"), OWNER));
    }

    @Test
    void threadWithOnlyTheOwnerGetsAStableSelfKey() {
        EmailThread t = thread();
        Message out = new Message(t, new Participant("me@acme.com", "Me"), "Subject", "x", null, T);
        out.markOutbound();
        t.addMessage(out);

        assertThat(svc.compute(t, OWNER)).isEqualTo(svc.keyFor(List.of(), OWNER)).isNotBlank();
    }
}
