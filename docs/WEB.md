# ConexusMail website (Thymeleaf)

Server-rendered pages served by the same Spring Boot app as the REST API.
Templates live in `src/main/resources/templates/`. Shared layout fragments
sit in `templates/fragments/`.

Every page-serving controller MUST delegate to a service class (in
`service/`, `auth/`, `email/`, `billing/`, `team/`, `digest/`,
`activation/`, `admin/`, or `reengagement/`) — never encode business logic
inside a `@Controller` method or a Thymeleaf template. The mobile REST API
will invoke the same services.

## Page inventory

Public (no session required):

| Route                | Controller                    | Template               | Notes |
| -------------------- | ----------------------------- | ---------------------- | ----- |
| `/`                  | (landing)                     | `landing.html`         | Marketing landing. |
| `/pricing`           | `PricingController`           | `pricing.html`         | Plan comparison + Stripe checkout. |
| `/demo`              | `DemoController`              | (demo view)            | Live demo (`DemoConversationService`). |
| `/login`             | `AuthController`              | `login.html`           | Form login + "Continue with Google". |
| `/register`          | `AuthController`              | `register.html`        | Account creation. |
| `/verify-email`      | `EmailVerificationController` | `verify-email.html`    | Confirm email link. |
| `/password/forgot`   | `PasswordResetController`     | `password/…`           | Request reset email. |
| `/password/reset`    | `PasswordResetController`     | `password/…`           | Token-scoped reset. |
| `/privacy`           | `LegalController`             | `legal.html` (privacy) | |
| `/terms`             | `LegalController`             | `legal.html` (terms)   | |
| `/refund`            | `LegalController`             | `legal.html` (refund)  | |
| `/digest/opt-out`    | `DigestController`            | digest view            | Token-scoped opt-out link. |
| `/billing/webhook`   | `BillingWebhookController`    | (no view)              | Stripe webhook — raw JSON in, no CSRF. |
| `/robots.txt`, `/sitemap.xml` | `SeoController`      | text/plain / XML       | |
| `/manifest.webmanifest`, `/sw.js`, `/offline`, `/icons/*`, `/apple-touch-icon.png` | `PwaController` | | PWA shell. |
| `/actuator/health`   | Spring Actuator               | (JSON)                 | Kept public for platform liveness probes. |

Authenticated:

| Route                    | Controller                   | Template          | Notes |
| ------------------------ | ---------------------------- | ----------------- | ----- |
| `/chats`                 | `ConversationController`     | `chat.html`       | Texting-style workspace; keyed by conversationKey. |
| `/chats/{key}`           | `ConversationController`     | `chat.html`       | Open a specific conversation. |
| `/chats/{key}/pane`      | `ConversationController`     | `fragments/*`     | Right-pane fragment refresh. |
| `/chats/{key}/send`      | `ConversationController`     | (redirect)        | Post a reply. Delegates to `ReplyService`. |
| `/chats/new`             | `ConversationController`     | `compose.html`    | New conversation composer. |
| `/chats/events`          | `ChatEventsController`       | text/event-stream | SSE for live delivery — `NewMailEvent` fanout. |
| `/chats/refresh`         | `ChatRedirectController`     | (redirect)        | Legacy redirect. |
| `/threads`, `/threads/**`| (see controller)             | conversation view | Legacy per-thread view. |
| `/mailboxes`             | `MailboxController`          | `mailboxes/…`     | List + add IMAP account. |
| `/mailboxes/gmail`       | `GmailConnectController`     | (OAuth start)     | Start Gmail OAuth. |
| `/mailboxes/gmail/callback` | `GmailConnectController`  | (redirect)        | OAuth callback. |
| `/mailboxes/gmail/reconnect/{id}` | `GmailConnectController` | (redirect)   | Weekly reconnect while OAuth is in Testing mode. |
| `/mailboxes/{id}/delete` | `MailboxController`          | (redirect)        | Remove account. |
| `/mailboxes/{id}/signature` | `MailboxController`       | (fragment)        | Update saved signature. |
| `/mailboxes/{id}/sync`   | `MailboxController`          | (redirect)        | Force a poll. |
| `/searches`              | `SavedSearchController`      | (saved-search list) | |
| `/senders`               | (see controller)             | (senders view)    | |
| `/preferences`           | (see controller)             | account settings  | |
| `/account`               | `AccountController`          | `account.html`    | Profile, appearance, notifications, privacy. |
| `/account/appearance`    | `AccountController`          | `account-appearance.html` | Theme + accent settings. |
| `/account/notifications` | `AccountController`          | `account-notifications.html` | Digest opt-in + (future) push opt-in. |
| `/account/privacy`       | `AccountController`          | `account-privacy.html` | Data export / deletion. |
| `/team/invite/…`         | `TeamInviteController`       | `team/…`          | Send + accept team invites. |
| `/billing/…`             | `BillingController`          | `billing/…`       | Stripe checkout flows. |
| `/admin/revenue`         | `AdminRevenueController`     | `admin/…`         | Owner-only revenue view. |

## Shared layout

- `fragments/head.html` — meta, PWA manifest link, theme boot.
- `fragments/nav.html` — top navigation (`NavModelAdvice` populates).
- `fragments/live-refresh.html` — the SSE client for `/chats/events`.
- `fragments/footer.html` — footer + legal links.
- `NavModelAdvice` and `SiteModelAdvice` inject shared model attributes.

## Do-not-duplicate contract

If you find yourself writing business logic in a `@Controller` or a
Thymeleaf `th:with=…` expression, stop. Move it into a service so the same
call is reused by the future `/api/v1/*` endpoint that covers the same
feature.
