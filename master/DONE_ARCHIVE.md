# Done Archive

Tasks completed and rolled out of `INTERNAL_TODO.md`. Each line keeps the
original task description with the date it was marked done so we can
trace velocity and feature lineage.

---

## 2026-05-06 — Run #9
- [x] DONE [GROWTH] [S] Static pricing page at `/pricing`: plan comparison table
      (Free/Personal/Team/Enterprise) with feature matrix and CTA buttons; no auth
      required. (EPIC-01)

## 2026-04-26 — Run #8
- [x] DONE [CORE] [M] CSS for the IM look: day separators (JS-inserted, date-aware
      with Today/Yesterday labels), dark mode (@media prefers-color-scheme), refined
      bubble borders + hover shadow, header-nav and msg-count CSS classes replacing
      inline styles.
- [x] DONE [UX] [S] Keyboard shortcuts: j/k to navigate thread list with .kb-focus
      indicator, Enter to open, r to focus reply textarea, Esc to blur.

## 2026-04-26 — Run #7
- [x] DONE [CORE] [L] Thymeleaf templates: threads.html, conversation.html, main.css,
      ThreadController, ThreadViewService, ReplyService.
- [x] DONE [HEALTH] [S] Input validation for web form objects: ReplyForm has
      @NotBlank + @Size(max=100,000); ThreadController uses @Valid + BindingResult.
- [x] DONE [UX] [S] Thread list empty state.
- [x] DONE [UX] [S] Conversation view empty state.
- [x] DONE [UX] [S] Conversation view reply button visual prominence.
- [x] DONE [UX] [S] Bubble body HTML rendering via th:utext with sanitization comment.

## 2026-04-26 — Run #6
- [x] DONE [HEALTH] [S] Global exception handler (@ControllerAdvice) + error.html;
      handles 404 / 502 / 409 / 500.
- [x] DONE [HEALTH] [S] EmailImportService: wraps MessagingException + IOException
      in EmailImportException so checked mail-stack exceptions don't leak.

## 2026-04-26 — Run #5
- [x] DONE [HEALTH] [M] Sanitize HTML email bodies: jsoup 1.17.2 + Jsoup.clean with
      Safelist.relaxed() — closes critical XSS vector.
- [x] DONE [UX] [S] Participant.initials() utility.

## Earlier runs
- [x] DONE [CORE] [M] IM transform: IMTransformService (stripQuotes + renderMarkdown),
      ConversationService (BubbleRun grouping), view model records.
- [x] DONE [CORE] [L] Email-import service: parse RFC 822 via Jakarta Mail, build
      threads from Message-ID / In-Reply-To / References.
- [x] DONE [CORE] [M] Domain entities (EmailThread, Message, Participant,
      MessageRecipient, Attachment) and Spring Data repositories.
- [x] DONE [CORE] [M] Flyway V1__init.sql migration.
- [x] DONE [CORE] [M] Spring Boot starters: web, thymeleaf, data-jpa, validation, mail,
      flyway, postgresql, h2, testcontainers.
- [x] DONE [CORE] [L] Maven scaffold: pom.xml, mvnw, application.yml,
      EmailMessengerApplication.java.
- [x] DONE [CORE] Rewrite README into a proper project README.
