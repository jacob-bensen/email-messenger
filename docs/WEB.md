# WEB — Thymeleaf Site

The `email-messenger` app ships a public marketing site and the signed-in
web workspace on the same Spring Boot instance that serves the mobile
REST API. Every page here MUST reuse the same service layer as the
corresponding REST endpoint in `docs/API.md` — no duplicated business
logic between a controller and a template.

## Layout & fragments

- `templates/fragments/` — layout, nav, footer, live-refresh SSE
  wiring, PWA install banner. Include with `th:replace` /
  `th:insert` rather than copying markup.
- `static/css/` — vanilla CSS (no framework). Dark mode via
  `prefers-color-scheme`.
- `static/js/` — small vanilla modules. The chat pane uses
  hyperscript-style incremental DOM swapping over SSE, not a SPA
  framework.

## Public pages

| Route | Template | Controller | Purpose |
| --- | --- | --- | --- |
| `/` | `landing.html` | `SeoController` | Marketing home |
| `/pricing` | `pricing.html` | `PricingController` | Plans + Stripe CTA |
| `/demo` | (demo view) | `DemoController` | Interactive product tour |
| `/privacy`, `/terms`, `/refund` | `legal.html` | `LegalController` | Markdown-rendered legal |
| `/login`, `/register` | `login.html`, `register.html` | `AuthController` | Form auth |
| `/verify-email` | `verify-email.html` | `EmailVerificationController` | Post-signup verify |
| `/password/forgot`, `/password/reset` | `password/*.html` | `PasswordResetController` | Password reset flow |
| `/auth/google/start` | (redirect) | `OAuthStartController` | Google OIDC entry |
| `/digest/opt-out` | (page) | `DigestController` | Digest unsubscribe |
| `/manifest.webmanifest`, `/sw.js`, `/offline` | (static + template) | `PwaController` | PWA shell |

## Signed-in workspace

| Route | Template | Controller | Purpose |
| --- | --- | --- | --- |
| `/chats`, `/chats/{conversationKey}` | `chat.html` | `ChatService`, `ConversationController` | IM-style workspace |
| `/chats/events` | (SSE) | `ChatEventStream`, `ChatEventsController` | Live new-mail feed |
| `/threads/{id}` | `conversation.html` | `ConversationController` | Single thread fallback |
| `/compose` | `compose.html` | `ConversationController` | New outbound message |
| `/mailboxes` | `mailboxes/*.html` | `MailboxController`, `GmailConnectController` | Connect IMAP / Gmail |
| `/saved-searches` | (in chat.html rail) | `SavedSearchController` | List, create, delete saved searches |
| `/account` | `account.html` | `AccountController` | Profile |
| `/account/appearance`, `/account/notifications`, `/account/privacy` | `account-*.html` | `AccountController` | Prefs |
| `/team`, `/team/invites` | `team/*.html` | `TeamInviteController` | Team plan |
| `/digest/opt-out` | (page) | `DigestController` | Digest prefs |

## Admin

| Route | Template | Gate |
| --- | --- | --- |
| `/admin/revenue` | `admin/revenue.html` | `ADMIN_EMAILS` contains signed-in user |

## Billing

Stripe checkout is a redirect handled by `PlanCheckoutSuccessHandler`;
the return URL renders a Thymeleaf confirmation page under
`templates/billing/`. `/billing/webhook` is the ONE JSON endpoint
(`BillingWebhookController`, `@RestController`) — do not route Stripe
callbacks through Thymeleaf.

## SEO

- `SeoController` also serves `/robots.txt` and `/sitemap.xml`.
- Landing hero video is configured through `LandingProperties` /
  `LandingVideo`.

## PWA

- `manifest.webmanifest`, `sw.js`, and offline shell live under
  `PwaController`.
- Web Push (VAPID) is roadmap in `CLAUDE.md` — not implemented.

## Model advice

- `NavModelAdvice` — populates nav-user info on every signed-in view.
- `SiteModelAdvice` — populates site-wide branding on every view.

## Contract with the mobile client

When a web page and a mobile screen deliver the same feature (chats,
mailbox connect, saved searches, account) they MUST call the same
service methods. If a mobile REST endpoint needs a shape the web page
does not, add it to the DTO layer — never fork the service.
