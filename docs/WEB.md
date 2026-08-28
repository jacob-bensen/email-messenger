# Website (Thymeleaf)

Server-rendered pages under `src/main/resources/templates/`. Every page
goes through the same service layer as the `/api/v1/**` JSON API — no
duplicated business logic.

## Public pages (unauthenticated)

| Path                       | Controller               | Template                     |
|----------------------------|--------------------------|------------------------------|
| `/`                        | `PricingController`      | `landing.html`               |
| `/pricing`                 | `PricingController`      | `pricing.html`               |
| `/demo`                    | `DemoController`         | `demo.html`                  |
| `/login`                   | `AuthController`         | `auth/login.html`            |
| `/register`                | `AuthController`         | `auth/register.html`         |
| `/password/forgot`         | `PasswordResetController`| `auth/password-forgot.html`  |
| `/password/reset`          | `PasswordResetController`| `auth/password-reset.html`   |
| `/verify-email`            | `EmailVerificationController` | `auth/verify-email.html` |
| `/privacy`, `/terms`, `/refund` | `LegalController`   | `legal/*.html`               |
| `/digest/opt-out`          | `DigestController`       | `digest/opt-out.html`        |
| `/robots.txt`, `/sitemap.xml` | `SeoController`       | (plain text / XML)           |

## In-app pages (authenticated)

| Path                          | Controller                 | Purpose                                   |
|-------------------------------|----------------------------|-------------------------------------------|
| `/chats`                      | `ConversationController`   | Chats workspace (rail + open pane).       |
| `/chats/{key}`                | `ConversationController`   | One conversation.                         |
| `/chats/{key}/pane`           | `ConversationController`   | HTMX-style pane fragment.                 |
| `/chats/{key}/send`           | `ConversationController`   | Reply from the pane.                      |
| `/chats/new`                  | `ConversationController`   | Compose a new chat.                       |
| `/chats/refresh`              | `ConversationController`   | Manual refresh.                           |
| `/chats/events`               | `ChatEventsController`     | SSE stream of new-message events.         |
| `/threads`, `/threads/**`     | `ChatRedirectController`   | Legacy redirect → `/chats`.               |
| `/mailboxes` (+ subpaths)     | `MailboxController`        | Mailbox connect / list / delete / sync.   |
| `/mailboxes/gmail/*`          | `GmailConnectController`   | Gmail OAuth connect + reconnect.          |
| `/searches` (+ subpaths)      | `SavedSearchController`    | Saved searches.                           |
| `/account` (+ subpaths)       | `AccountController`        | Profile, appearance, email, notifications,|
|                               |                            | password, privacy, senders, preferences.  |
| `/billing/*`                  | `BillingController`        | Checkout, portal, cancel.                 |
| `/team/invite*`               | `TeamInviteController`     | Team invite send/accept.                  |
| `/admin/*`                    | `Admin*Controller`         | Internal admin dashboards.                |
| `/manifest.webmanifest`, `/sw.js`, `/offline`, `/icons/**` | `PwaController` | PWA shell. |

## Shared fragments

Under `src/main/resources/templates/fragments/`. Notable ones:

- `layout.html` — the outer HTML skeleton.
- `nav.html` — top navigation (populated by `NavModelAdvice`).
- `live-refresh.html` — SSE-driven partial refresh helpers for the
  chats workspace.

## Conventions

- Constructor injection only.
- Package-private controllers by default.
- Redirects use `redirect:/...` return strings.
- URL-encode any user-supplied `RequestParam` before splicing into a
  redirect target (see `PricingController` for the pattern).
