# Website (Thymeleaf)

The Thymeleaf-rendered website IS the ConexusMail product UI today —
the mobile client does not exist yet. Pages funnel every state change
through the same `service/` and feature-service classes a future mobile
JSON API will use, so logic never forks between the two.

## Templates

Under `src/main/resources/templates/`:

### Public / marketing

| Template                    | Route(s)                                | Notes                                    |
| --------------------------- | --------------------------------------- | ---------------------------------------- |
| `landing.html`              | `GET /`                                 | Hero + features + pricing snapshot.      |
| `pricing.html`              | `GET /pricing`                          | Plans, driven by `PricingController`.    |
| `legal.html` (+ fragments)  | `GET /privacy`, `/terms`, `/refund`     | `LegalController` + `LegalProperties`.   |
| SEO / PWA                   | `GET /robots.txt`, `/sitemap.xml`,      | `SeoController`, `PwaController`.        |
|                             | `/manifest.webmanifest`, `/sw.js`,      |                                          |
|                             | `/offline`, `/icons/*`, `/apple-touch…` |                                          |

### Auth

| Template                | Route(s)                                                                                | Controller                    |
| ----------------------- | --------------------------------------------------------------------------------------- | ----------------------------- |
| `login.html`            | `GET /login`                                                                            | `AuthController`              |
| `register.html`         | `GET /register`, `POST /register`                                                       | `AuthController`              |
| `verify-email.html`     | `GET /verify-email`, `POST /verify-email/resend`                                        | `EmailVerificationController` |
| `password/forgot.html`  | `GET /password/forgot`, `POST /password/forgot`                                         | `PasswordResetController`     |
| `password/reset.html`   | `GET /password/reset`, `POST /password/reset`                                           | `PasswordResetController`     |
| (redirect)              | `GET /auth/google/start`, `GET /callback`                                               | `OAuthStartController`        |

### Account

| Template                        | Route(s)                                                                             | Controller        |
| ------------------------------- | ------------------------------------------------------------------------------------ | ----------------- |
| `account.html`                  | `GET /account`, `POST /account/email`, `POST /account/password`                      | `AccountController` |
| `account-appearance.html`       | `GET /account/appearance`, `POST /account/appearance`                                | `AccountController` |
| `account-notifications.html`    | `GET /account/notifications`, `POST /preferences`                                    | `AccountController` |
| `account-privacy.html`          | `GET /account/privacy`                                                               | `AccountController` |

### Mailboxes

| Template               | Route(s)                                                                    | Controller             |
| ---------------------- | --------------------------------------------------------------------------- | ---------------------- |
| `mailboxes/index.html` | `GET /mailboxes`, `POST /mailboxes/{id}/sync`, `POST /mailboxes/{id}/delete`, `POST /mailboxes/{id}/signature` | `MailboxController` |
| `mailboxes/new.html`   | `GET /mailboxes/new`, `POST /mailboxes` (IMAP-password)                     | `MailboxController`    |
| (redirects)            | `GET /mailboxes/gmail/connect`, `GET /mailboxes/gmail/reconnect/{id}`       | `GmailConnectController` |

### Chats (product core)

| Template                | Route(s)                                                       | Controller                                 |
| ----------------------- | -------------------------------------------------------------- | ------------------------------------------ |
| `chat.html`             | `GET /chats`, `GET /chats/{key}`                               | `ConversationController` + `ChatService`   |
| `fragments/chat-pane.html` | `GET /chats/{key}/pane`                                     | `ConversationController` (HTMX-style morph) |
| `compose.html`          | `GET /chats/new`, `POST /chats/new`                            | `ConversationController`                   |
| (send)                  | `POST /chats/{key}/send`, `POST /chats/refresh`                | `ConversationController`                   |
| (SSE)                   | `GET /chats/events` (`text/event-stream`)                      | `ChatEventStream` + `ChatEventsController` |
| (legacy redirects)      | `GET /threads`, `/threads/**`, `/senders`                      | `ChatRedirectController`                   |

### Billing

| Template                            | Route(s)                                                                                                  | Controller                  |
| ----------------------------------- | --------------------------------------------------------------------------------------------------------- | --------------------------- |
| `billing/success.html`              | `GET /billing/success`                                                                                    | `BillingController`         |
| `billing/cancel-subscription.html`  | `GET /billing/cancel`, `GET /billing/cancel-subscription`, `POST /billing/cancel-subscription`            | `BillingController`         |
| (POST-only)                         | `POST /billing/checkout`, `POST /billing/portal`                                                          | `BillingController`         |
| (webhook)                           | `POST /billing/webhook`                                                                                   | `BillingWebhookController` (`@RestController`) |

### Team

| Template            | Route(s)                                                                          | Controller                |
| ------------------- | --------------------------------------------------------------------------------- | ------------------------- |
| `team/invite.html`  | `GET /team/invite`, `POST /team/invite`, `POST /team/invite/{id}/delete`          | `TeamInviteController`    |
| `team/accept.html`  | `GET /team/invite/accept`, `POST /team/invite/accept`                             | `TeamInviteController`    |

### Digest / admin

| Template                    | Route(s)                                                              | Controller                                       |
| --------------------------- | --------------------------------------------------------------------- | ------------------------------------------------ |
| `digest/opt-out.html`       | `GET /digest/opt-out`                                                 | `DigestController`                               |
| `admin/revenue.html`        | `GET /admin/revenue`, `POST /admin/revenue/reconcile-billing-period`, `POST /admin/retention/win-back` | `AdminRevenueController` (allowlist-gated)       |

### Shared fragments (`templates/fragments/`)

- `app-nav.html`, `settings-nav.html` — top-nav / settings-side-nav.
- `chat-pane.html` — the swappable chat pane returned by
  `/chats/{key}/pane`.
- `live-refresh.html` — the SSE-driven morph client script.
- `sw-register.html`, `install-banner.html` — PWA install glue.
- `cookie-banner.html`, `seo.html` — shared page furniture.

## Shared-service rule

Every mutation on a page above goes through a service class in the
feature package (for example `MailboxController` → `MailAccountService`,
`ConversationController` → `ChatService` / `ReplyService`, billing
controllers → `BillingService`). When the mobile REST API in `API.md`
starts landing, its new controllers MUST reuse those same services —
never re-implement business logic in a JSON controller.

## Live-refresh flow

`fragments/live-refresh.html` subscribes to `GET /chats/events` (SSE).
On each `NewMailEvent` it either updates the chats-list rail or (if the
event matches the currently open chat) fetches
`GET /chats/{key}/pane` and morphs the pane in place. This replaces
what used to be a full page reload on new mail (see the "Live delivery,
part 1" roadmap item in `CLAUDE.md`).

## Adding a page

1. Route under an existing feature controller if one fits; add a new
   controller only when the URL family is new.
2. Reuse (or create in) the feature's service class — no business
   logic in the controller.
3. Add the template under `templates/<feature>/` and, if it wants the
   product chrome, include `fragments/app-nav.html` and (for chat-
   adjacent pages) `fragments/live-refresh.html`.
4. Add a slice test (`@WebMvcTest`) or MVC test for the new route.
