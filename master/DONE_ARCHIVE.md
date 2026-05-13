# Done Archive

Completed tasks moved out of INTERNAL_TODO.md, with completion date.

---

## 2026-05-13

- DONE [GROWTH] [S] Static pricing page at /pricing — Epic E2.
  MarketingController + pricing.html template with 4-tier plan grid
  (Free/Personal/Team/Enterprise), monthly/annual toggle (JS swaps via
  data-monthly/data-annual attributes, "Save 16%" badge), per-plan feature
  lists, "Most popular" highlight on Personal, 14-day trial CTA, FAQ section,
  and footer CTA. Header link from /threads added. Mobile + dark-mode supported.
  9 MarketingControllerTest cases cover routing, prices (monthly + annual),
  4-plan rendering, CTA wording, SEO description, and noscript-safety.

## 2026-04-26 (and earlier — see CHANGELOG.md for full detail)

- DONE [CORE] Rewrite README.md into proper README
- DONE [CORE] [L] Scaffold Maven project: pom.xml, mvnw, application.yml,
  EmailMessengerApplication.java
- DONE [CORE] [M] Add all Spring Boot starters: web, thymeleaf, data-jpa,
  validation, mail, flyway, postgresql, h2, testcontainers
- DONE [CORE] [M] Flyway migration V1__init.sql: EmailThread, Message,
  Participant, Attachment, MessageRecipient tables with indexes
- DONE [CORE] [M] Implement domain entities and Spring Data repositories
- DONE [CORE] [L] Email-import service: parse RFC 822 via Jakarta Mail, build
  threads from Message-ID / In-Reply-To / References
- DONE [CORE] [M] IM transform: IMTransformService (stripQuotes +
  renderMarkdown), ConversationService (BubbleRun grouping),
  Conversation/BubbleRun/BubbleMessage view model records
- DONE [HEALTH] [M] Sanitize HTML email bodies: jsoup 1.17.2 added;
  ConversationService.buildBodyHtml calls Jsoup.clean(..., Safelist.relaxed())
  — closes CRITICAL XSS vector
- DONE [UX] [S] Participant initials utility: added initials() method to
  Participant entity
- DONE [HEALTH] [S] EmailImportService: wrap MessagingException and
  IOException in EmailImportException (unchecked)
- DONE [HEALTH] [S] Add global exception handler: GlobalExceptionHandler
  (@ControllerAdvice) with error.html template
- DONE [CORE] [L] Thymeleaf templates: threads.html, conversation.html,
  main.css, ThreadController, ThreadViewService, ReplyService
- DONE [HEALTH] [S] Add input validation for all web form objects: ReplyForm
  has @NotBlank + @Size(max=100,000); ThreadController uses @Valid +
  BindingResult
- DONE [UX] [S] Thread list empty state with CTA
- DONE [UX] [S] Conversation view empty state
- DONE [UX] [S] Conversation view reply button as primary blue CTA
- DONE [UX] [S] Bubble body HTML rendering with sanitization contract comment
- DONE [CORE] [M] CSS for the IM look: day separators (JS-inserted, date-aware
  with Today/Yesterday labels), dark mode, refined bubble borders + hover
- DONE [UX] [S] Keyboard shortcuts: j/k navigate thread list, Enter opens,
  r focuses reply, Esc blurs
