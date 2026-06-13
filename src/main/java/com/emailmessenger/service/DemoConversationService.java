package com.emailmessenger.service;

import com.emailmessenger.domain.EmailThread;
import com.emailmessenger.domain.Message;
import com.emailmessenger.domain.Participant;
import com.emailmessenger.domain.User;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.LocalTime;

/**
 * Builds a hard-coded, no-database demo conversation so anonymous Product Hunt
 * / Twitter visitors can see the IM-style chat view in five seconds without
 * connecting a mailbox. Drives the same {@link ConversationService} pipeline
 * the real inbox uses — bubble grouping, quoted-reply stripping, markdown
 * rendering — so what visitors see is what they'd get with their own mail.
 */
@Service
public class DemoConversationService {

    private static final User DEMO_OWNER = new User("you@mailaim.app", "n/a", "You");

    private static final Participant ALEX = new Participant("alex.lee@mailaim.app", "Alex Lee");
    private static final Participant SAM  = new Participant("sam.patel@mailaim.app", "Sam Patel");
    private static final Participant MAYA = new Participant("maya.chen@mailaim.app", "Maya Chen");

    private final ConversationService conversationService;

    DemoConversationService(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    public Conversation buildDemo() {
        return buildDemo(LocalDate.now(ZoneOffset.UTC));
    }

    public Conversation buildDemo(LocalDate today) {
        LocalDate yesterday = today.minusDays(1);
        EmailThread thread = new EmailThread(DEMO_OWNER, "Launch checklist — Tuesday demo", "<demo-root@mailaim.app>");

        addPlain(thread, ALEX, yesterday, LocalTime.of(9, 41),
                "Quick one — can you ship the pricing page today?");

        addPlain(thread, ALEX, yesterday, LocalTime.of(9, 42),
                "Two things blocking launch:\n"
                        + "\n"
                        + "- pricing page live at `/pricing`\n"
                        + "- Stripe webhook receiving events\n"
                        + "\n"
                        + "The rest can wait.");

        // Quoted-reply block at the bottom is the exact thing MailIM strips —
        // visitors see only the new content, not the wall of >'s.
        addPlain(thread, SAM, yesterday, LocalTime.of(9, 55),
                "Already in review. Pricing page should be live within the hour.\n"
                        + "\n"
                        + "On Mon, Jun 3, Alex Lee wrote:\n"
                        + "> Two things blocking launch:\n"
                        + "> - pricing page live at `/pricing`\n"
                        + "> - Stripe webhook receiving events\n"
                        + "> The rest can wait.");

        addPlain(thread, SAM, yesterday, LocalTime.of(10, 34),
                "Live now: **mailaim.app/pricing**. Webhook handler ships after lunch.");

        addPlain(thread, ALEX, today, LocalTime.of(9, 3),
                "Approved. Let's ship.");

        addPlain(thread, MAYA, today, LocalTime.of(11, 20),
                "Coordinating the Product Hunt post for Tuesday 12:01 AM PT.\n"
                        + "\n"
                        + "Need from each maker:\n"
                        + "\n"
                        + "- 140-char tagline\n"
                        + "- 3 screenshots of the conversation view\n"
                        + "- 1-sentence bio");

        addPlain(thread, SAM, today, LocalTime.of(11, 32),
                "Tagline draft: **Your inbox, as a chat.** Screenshots tonight.");

        return conversationService.buildConversation(thread);
    }

    private static void addPlain(EmailThread thread, Participant sender,
                                 LocalDate day, LocalTime time, String body) {
        thread.addMessage(new Message(thread, sender, thread.getSubject(),
                body, null, LocalDateTime.of(day, time)));
    }
}
