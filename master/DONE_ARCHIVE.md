# Done Archive

Tasks completed in prior sessions, moved out of `INTERNAL_TODO.md` to keep the
active backlog focused. Format mirrors `INTERNAL_TODO.md`.

---

## Archived 2026-04-28 (Run #18)

- [x] DONE [HEALTH] [S] Cookie consent banner — completion date: 2026-04-28 (Run #18). `templates/fragments/cookie-banner.html` (`th:fragment="banner"`), `static/js/cookie-banner.js` (localStorage `mailim.cookieConsent.v1`, accessible-banner pattern, role=region + aria-label), `.cookie-banner` CSS (light + dark mode), included on all 9 public templates (index, pricing, demo, waitlist, privacy, terms, refund, threads, conversation). 7 new integration tests. Unblocks EU/GDPR market. (EPIC-2)

## Archived 2026-04-28 (Run #17 and earlier)

These items are kept for historical context. Each was completed before the
EPICS.md split, so most are tagged retrospectively.

- [x] DONE [CORE] Rewrite README.md into proper README
- [x] DONE [CORE] [L] Scaffold Maven project: pom.xml, mvnw, application.yml, EmailMessengerApplication.java
- [x] DONE [CORE] [M] Add all Spring Boot starters: web, thymeleaf, data-jpa, validation, mail, flyway, postgresql, h2, testcontainers
- [x] DONE [CORE] [M] Add Flyway migration V1__init.sql: EmailThread, Message, Participant, Attachment, MessageRecipient tables with indexes
- [x] DONE [CORE] [M] Implement domain entities (EmailThread, Message, Participant, MessageRecipient, Attachment) and Spring Data repositories
- [x] DONE [CORE] [L] Email-import service: parse RFC 822 via Jakarta Mail, build threads from Message-ID / In-Reply-To / References
- [x] DONE [CORE] [M] IM transform: IMTransformService (stripQuotes + renderMarkdown), ConversationService (BubbleRun grouping), Conversation/BubbleRun/BubbleMessage view model records
- [x] DONE [HEALTH] [M] Sanitize HTML email bodies: jsoup 1.17.2 added; ConversationService.buildBodyHtml calls Jsoup.clean(bodyHtml, Safelist.relaxed()) — closes CRITICAL XSS vector
- [x] DONE [UX] [S] Participant initials utility: added initials() method to Participant entity
- [x] DONE [HEALTH] [S] EmailImportService: wrap MessagingException and IOException in EmailImportException (unchecked)
- [x] DONE [HEALTH] [S] Add global exception handler: GlobalExceptionHandler handles 404/409/502/500; disables Whitelabel error page
- [x] DONE [CORE] [L] Thymeleaf templates: threads.html, conversation.html, main.css, ThreadController, ThreadViewService, ReplyService
- [x] DONE [HEALTH] [S] Add input validation for all web form objects: ReplyForm @NotBlank + @Size; ThreadController uses @Valid
- [x] DONE [UX] [S] Thread list empty state, conversation empty state, reply button primary CTA
- [x] DONE [UX] [S] Bubble body HTML rendering: th:utext with sanitization contract comment
- [x] DONE [CORE] [M] CSS for the IM look: day separators, dark mode, refined bubbles, header-nav and msg-count classes
- [x] DONE [UX] [S] Keyboard shortcuts: j/k navigate, Enter open, r reply, Esc cancel
- [x] DONE [CORE] [M] IMAP polling job: ImapPollingJob (@ConditionalOnProperty, @Scheduled), ImapPollingProperties (@ConfigurationProperties), feature flag app.imap.polling.enabled
- [x] DONE [GROWTH] [S] Static pricing page at /pricing: PricingController + pricing.html with annual/monthly toggle, plan comparison, feature matrix, FAQ section, OG/meta tags
- [x] DONE [GROWTH] [S] Demo mode: DemoService (2 realistic conversations), DemoController (GET /demo + GET /demo/{id}), demo.html with CTA; conversation.html isDemo flag
- [x] DONE [GROWTH] [S] Waitlist email capture at /waitlist: WaitlistEntry JPA entity, V2__waitlist.sql migration, WaitlistController, waitlist.html (form / success / already-joined states)
- [x] DONE [UX] [S] Pricing page CTAs (Personal, Team) updated from /threads to /waitlist
- [x] DONE [UX] [S] Pricing page privacy/TOS links fixed: /privacy and /terms stub pages created
- [x] DONE [UX] [S] Demo conversation banner "Connect your own mailbox" updated to "Join the waitlist →"
- [x] DONE [UX] [S] Threads empty state CTA updated from non-existent /settings/mailboxes to /waitlist
- [x] DONE [GROWTH] [M] SEO-friendly landing page at /: LandingController + index.html with hero, feature grid, pricing preview, JSON-LD SoftwareApplication schema
- [x] DONE [GROWTH] [S] Waitlist count social proof on landing page hero and waitlist page
- [x] DONE [UX] [S] Multiple nav dead-ends fixed across waitlist.html, demo.html, threads.html
- [x] DONE [CORE] [M] Dockerfile + docker-compose.yml (app + postgres): multi-stage build, non-root user, depends_on health check, .env.example
- [x] DONE [CORE] [S] GitHub Actions CI: build, test, cache Maven deps; uploads Surefire reports on failure
- [x] DONE [UX] [S] Add footer/nav to /privacy and /terms pages
- [x] DONE [UX] [S] Fix pricing page brand link from /threads to /
- [x] DONE [UX] [S] Waitlist success state CTA updated to "Try the live demo →"
- [x] DONE [UX] [S] Add site footer to waitlist.html and demo.html
- [x] DONE [CORE] [S] Health-check endpoint at GET /health: HealthController returns 200 JSON `{"status":"UP"}`; HEALTHCHECK in Dockerfile + docker-compose.yml
- [x] DONE [UX] [S] Landing page "How it works" 3-step section
- [x] DONE [HEALTH] [S] noindex meta on private pages (threads.html, conversation.html)
- [x] DONE [UX] [S] Pricing page Free plan CTA fixed: from /threads to /waitlist
- [x] DONE [HEALTH] [S] WaitlistController bug fix: `waitlistCount` not added to model on validation error
- [x] DONE [GROWTH] [S] Refund policy stub page at /refund (LegalController + refund.html), linked from all 6 footers
- [x] DONE [GROWTH] [S] Gzip compression: server.compression.enabled + mime-types + min-response-size: 1024 in application.yml
- [x] DONE [HEALTH] [S] Security response headers: SecurityHeadersFilter (X-Frame-Options, X-Content-Type-Options, Referrer-Policy)
- [x] DONE [UX] [S] Refund Policy link added to all footers; pricing FAQ updated to mention refund policy
- [x] DONE [CORE] [L] Integration tests with Testcontainers (Postgres) + GreenMail (SMTP/IMAP): EmailImportIntegrationTest (6 tests, PG container, skips gracefully when Docker unavailable) + GreenMailSmtpImapIntegrationTest (4 tests, full SMTP→import pipeline). GreenMail 2.1.2 dep added. RequiresDocker ExecutionCondition prevents spurious CI failures.
- [x] DONE [GROWTH] [S] Trust microcopy on pricing page CTAs and landing hero
- [x] DONE [GROWTH] [S] Objection-handling FAQ on /pricing (confirmed present)
