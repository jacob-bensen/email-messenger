# WEB — Thymeleaf pages

Server-rendered pages of the public ConexusMail site. Each page uses the same
service layer as the (future) REST API — controllers are thin, business logic lives
in `service/`, `auth/`, `email/`, `billing/`, etc. When a mobile endpoint is added
for behavior a page already exposes, both paths MUST call the same service method;
never fork logic between web and API.

## Layout & shared fragments

- Base layout: `templates/fragments/layout.html` (nav, footer, meta).
- Live-refresh fragment: `templates/fragments/live-refresh.html` — wires the
  browser to `/chats/events` (SSE) and morphs the rail + open chat pane in place.
- Nav model advice: `web/NavModelAdvice.java` (per-request nav state).
- Site model advice: `web/SiteModelAdvice.java` (brand strings, footer copy).

## Page inventory

| Route | Template | Controller → Service(s) |
|---|---|---|
| `/` | `landing.html` | `PricingController` → `AccountService`, `SiteProperties` |
| `/pricing` | `pricing.html` | `PricingController` → billing catalog |
| `/demo` | (demo view) | `DemoController` → `DemoConversationService` |
| `/login` | `login.html` | `AuthController` → Spring Security |
| `/register` | `register.html` | `AuthController` → `UserService`, `EmailVerificationService` |
| `/verify-email` | `verify-email.html` | `EmailVerificationController` → `EmailVerificationService` |
| `/password/forgot`, `/password/reset` | `password/*.html` | `PasswordResetController` → `PasswordResetService` |
| `/account`, `/account/appearance`, `/account/notifications`, `/account/privacy` | `account*.html` | `AccountController` → `AccountService`, `AppearanceSettings` |
| `/chats` | `chat.html` | `ConversationController` → `ConversationListService`, `ChatService` |
| `/chats/{key}` | `chat.html` (with open pane) | `ConversationController` → `ConversationService`, `IMTransformService` |
| `/chats/{key}/pane` | `fragments/chat-pane.html` | `ConversationController` → `ConversationService` |
| `/chats/{key}/send` | (redirect) | `ConversationController` → `ReplyService`, `OutboundMessageService` |
| `/chats/new` | `compose.html` | `ConversationController` → `OutboundMessageService` |
| `/mailboxes`, `/mailboxes/new`, `/mailboxes/{id}/*`, `/mailboxes/preferences` | `mailboxes/*.html` | `MailboxController` → mailbox services, IMAP |
| `/mailboxes/gmail/connect`, `/reconnect/{id}`, `/callback` | (redirects) | `GmailConnectController` → Google OAuth |
| `/searches`, `/searches/{id}/delete` | (redirect) | `SavedSearchController` → `SavedSearchService` |
| `/team/invite`, `/team/invite/accept` | `team/*.html` | `TeamInviteController` → team services |
| `/billing/checkout`, `/portal`, `/success`, `/cancel`, `/cancel-subscription` | `billing/*.html` | `BillingController` → Stripe |
| `/digest/opt-out` | `digest/opt-out.html` | `DigestController` → `DigestEmailPreference` |
| `/admin/revenue`, `/admin/revenue/*`, `/admin/retention/*` | `admin/*.html` | `AdminRevenueController` → admin metrics |
| `/privacy`, `/terms`, `/refund` | `legal.html` (+ per-doc content) | `LegalController` → `LegalProperties` |
| `/manifest.webmanifest`, `/sw.js`, `/icons/*`, `/offline` | (raw responses) | `PwaController` |

## Live event stream

`GET /chats/events` produces `text/event-stream`. Events:

- `newMail` — payload includes `conversationKey`. The browser refetches the rail
  and the open pane fragment (if that key is showing).

`ChatEventStream` holds one `SseEmitter` per session. `MailboxPollingService` and
`ImapIdleService` publish `NewMailEvent` on the Spring context; the controller
subscribes and forwards.

## Convention reminders

- Controllers are package-private; widen only when a class in another package needs
  them.
- Redirects use `RedirectAttributes` (`flashAttributes`) for success/error banners.
- Never call an entity or repository from a Thymeleaf template — assemble a view
  model (record) in the controller/service and pass that in.
