# WEB

Server-rendered Thymeleaf website served from the same Spring Boot
process as the REST API. Users interact with ConexusMail primarily
through this site today; the mobile client
(`../conexus-mail-mobile`) is a companion surface built on the same
service layer.

## Templates

`src/main/resources/templates/`:

| template                    | route(s) served by                                  | purpose |
| --------------------------- | --------------------------------------------------- | ------- |
| `landing.html`              | `SeoController`                                     | marketing landing page (`/`) |
| `pricing.html`              | `PricingController` (`/pricing`)                    | plan comparison, checkout entry |
| `login.html`, `register.html` | `AuthController`                                  | form login + Google OIDC entry |
| `verify-email.html`         | `EmailVerificationController`                       | one-time link landing |
| `password/*.html`           | `PasswordResetController` (`/password/forgot`, `/password/reset`) | reset flow |
| `chat.html`                 | `ConversationController` (`/chats/**`)              | IM-style conversation workspace |
| `conversation.html`         | `ConversationController` (`/threads/{id}`)          | legacy per-thread view |
| `compose.html`              | `ConversationController` / `OutboundMessageService` | new outbound compose |
| `mailboxes/*.html`          | `MailboxController` (`/mailboxes/**`)               | IMAP + Gmail OAuth mailbox connect |
| `account*.html`             | `AccountController` (`/account/**`)                 | profile, appearance, notifications, privacy |
| `admin/*.html`              | admin dashboards (`/admin/revenue` etc.)            | operator metrics + retention actions |
| `team/*.html`               | team controllers                                    | team invite + membership |
| `digest/*.html`             | digest opt-out landing                              | one-click opt-out from digest emails |
| `billing/*.html`            | Stripe checkout return pages                        | success + cancel flows |
| `legal.html`                | `LegalController` (`/privacy`, `/terms`, `/refund`) | legal pages sourced from `resources/legal/` |
| `fragments/*.html`          | shared includes                                     | nav bar, live-refresh SSE hooks, site chrome |

## Route → service mapping

Web controllers delegate immediately to services; templates receive
view models. The rule (`docs/BACKEND.md`, "Website / API relationship"):
**no business logic in controllers or templates**. Every mutation is a
call into a `*Service` class in `com.emailmessenger.service`,
`.auth`, `.billing`, `.email`, `.team`, `.digest`, `.activation`,
`.reengagement`, or `.admin`.

Notable pairs:

- `ConversationController` + `ChatService` / `ConversationListService`
  / `ConversationService` — the `/chats` workspace and its per-thread
  drill-down. Any mobile chat endpoint added under `docs/API.md`
  reuses these services verbatim.
- `MailboxController` + `MailboxProvider` implementations — IMAP and
  Gmail OAuth mailbox lifecycle. Mobile mailbox connect will call
  the same services.
- `AccountController` + `AccountService` — user account settings.
- `OutboundMessageService` — the SMTP send path; `ReplyForm` on the web
  and any future `POST /api/v1/chats/{key}/reply` on the API both
  funnel through this one method.

## Shared cross-cutting

- `NavModelAdvice`, `SiteModelAdvice` — populate site-wide model
  attributes on every rendered page.
- `GlobalExceptionHandler` — maps common exceptions
  (413 upload-too-large, 4xx form violations) to friendly views. The
  REST API needs a companion handler that returns the JSON error
  envelope described in `docs/API.md`; this is on the tracked list.
- `SecurityConfig` — the single source of URL-level authorization.
  `/api/v1/**` public endpoints are opened here; authenticated API
  paths will be opened once bearer-token auth lands.

## PWA shell

`PwaController` serves `/manifest.webmanifest`, `/sw.js`, and the
`/offline` fallback. The service worker is minimal — it caches static
assets and the offline page; the app itself is server-rendered on each
request. This is unrelated to the native mobile client.

## Server-Sent Events

`ChatEventStream` and `ChatEventsController` publish `NewMailEvent`s
over SSE to the browser (`/chats/events`) so the workspace can update
in place when a poll or IDLE cycle imports new mail. The mobile client
will get an analogous push channel via a separate mechanism (planned
Web Push / FCM); the SSE endpoint is browser-only.
