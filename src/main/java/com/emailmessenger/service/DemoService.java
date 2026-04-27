package com.emailmessenger.service;

import com.emailmessenger.domain.EmailThread;
import com.emailmessenger.domain.Participant;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DemoService {

    public record DemoCatalogEntry(int id, String subject, int messageCount, LocalDateTime updatedAt) {}

    private static final Participant SARAH  = new Participant("sarah@acmecorp.com",  "Sarah Chen");
    private static final Participant MARCUS = new Participant("marcus@acmecorp.com", "Marcus Rivera");
    private static final Participant DIANA  = new Participant("diana@agency.com",    "Diana Walsh");
    private static final Participant JAMES  = new Participant("james@agency.com",    "James Park");
    private static final Participant YOU    = new Participant("you@company.com",     "You");

    public List<DemoCatalogEntry> listThreads() {
        LocalDateTime yesterday = LocalDateTime.now().minusDays(1).withHour(16).withMinute(7).withSecond(0).withNano(0);
        LocalDateTime todayLate = LocalDateTime.now().withHour(11).withMinute(50).withSecond(0).withNano(0);
        return List.of(
            new DemoCatalogEntry(1, "Re: Q3 marketing campaign results", 3, yesterday),
            new DemoCatalogEntry(2, "Onboarding new team members this Thursday", 5, todayLate)
        );
    }

    public Conversation getConversation(int id) {
        return switch (id) {
            case 1 -> buildConv1();
            case 2 -> buildConv2();
            default -> null;
        };
    }

    private Conversation buildConv1() {
        LocalDateTime t1 = LocalDateTime.now().minusDays(1).withHour(9).withMinute(15).withSecond(0).withNano(0);
        LocalDateTime t2 = LocalDateTime.now().minusDays(1).withHour(11).withMinute(42).withSecond(0).withNano(0);
        LocalDateTime t3 = LocalDateTime.now().minusDays(1).withHour(16).withMinute(7).withSecond(0).withNano(0);

        EmailThread thread = new EmailThread("Re: Q3 marketing campaign results", null);
        thread.setMessageCount(3);

        List<BubbleRun> runs = List.of(
            new BubbleRun(SARAH, List.of(
                new BubbleMessage(null,
                    "<p>Hey Marcus! Just finished reviewing the Q3 numbers. Our email campaign hit a " +
                    "<strong>4.2% click-through rate</strong> — up from 2.8% last quarter.</p>" +
                    "<p>The personalized subject lines made a huge difference. Worth scaling for Q4.</p>",
                    t1, List.of())
            )),
            new BubbleRun(MARCUS, List.of(
                new BubbleMessage(null,
                    "<p>That's fantastic! The segmented list approach clearly worked. Did you see the " +
                    "conversion rate on the landing page?</p>" +
                    "<p>I'm showing <strong>18.3%</strong> from email traffic — almost double the organic baseline.</p>",
                    t2, List.of())
            )),
            new BubbleRun(SARAH, List.of(
                new BubbleMessage(null,
                    "<p>Exactly. I'll put together a full breakdown for the team meeting on Thursday. " +
                    "Can you pull the revenue attribution numbers from Stripe?</p>" +
                    "<p>This will make a compelling case for doubling the content budget next year.</p>",
                    t3, List.of())
            ))
        );
        return new Conversation(thread, runs);
    }

    private Conversation buildConv2() {
        LocalDateTime t1 = LocalDateTime.now().withHour(8).withMinute(55).withSecond(0).withNano(0);
        LocalDateTime t2 = LocalDateTime.now().withHour(9).withMinute(33).withSecond(0).withNano(0);
        LocalDateTime t3 = LocalDateTime.now().withHour(9).withMinute(36).withSecond(0).withNano(0);
        LocalDateTime t4 = LocalDateTime.now().withHour(10).withMinute(12).withSecond(0).withNano(0);
        LocalDateTime t5 = LocalDateTime.now().withHour(11).withMinute(50).withSecond(0).withNano(0);

        EmailThread thread = new EmailThread("Onboarding new team members this Thursday", null);
        thread.setMessageCount(5);

        List<BubbleRun> runs = List.of(
            new BubbleRun(DIANA, List.of(
                new BubbleMessage(null,
                    "<p>Hi all — just confirmed: Priya and Leo join us Thursday. " +
                    "I'll send calendar invites for the 10am welcome session shortly.</p>",
                    t1, List.of())
            )),
            new BubbleRun(JAMES, List.of(
                new BubbleMessage(null,
                    "<p>Great! I've set up their Slack accounts and added them to the relevant channels. " +
                    "Their equipment should arrive by Wednesday.</p>",
                    t2, List.of()),
                new BubbleMessage(null,
                    "<p>Should I send the employee handbook PDF ahead of time, or go through it live on Thursday?</p>",
                    t3, List.of())
            )),
            new BubbleRun(DIANA, List.of(
                new BubbleMessage(null,
                    "<p>Send it ahead — gives them a chance to read at their own pace. " +
                    "Also include the <code>onboarding-2026</code> Notion doc link.</p>",
                    t4, List.of())
            )),
            new BubbleRun(YOU, List.of(
                new BubbleMessage(null,
                    "<p>Done! Sent both links to Priya and Leo. See everyone Thursday!</p>",
                    t5, List.of())
            ))
        );
        return new Conversation(thread, runs);
    }
}
