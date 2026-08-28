# WEB — Thymeleaf pages served by email-messenger

The backend also serves the marketing site, the auth surface, the chat
workspace, and the operator admin. All templates live in
`src/main/resources/templates`. Shared layout fragments live under
`templates/fragments/` (e.g. `live-refresh.html`, plan-badge fragments).

Every page must delegate business logic to the same service classes the
future JSON API uses (see `docs/API.md`). Controllers stay thin — never
duplicate rules in a template or a controller.

## Page inventory (routes → template → primary services)

### Public / marketing

| Route             | Template                | Services / notes                              |
|-------------------|-------------------------|-----------------------------------------------|
| `GET /`           | `home.html`             | Landing.                                      |
| `GET /pricing`    | `pricing.html`          | `PricingPlan` metadata.                       |
| `GET /demo`       | `demo.html`             | `DemoConversationService` sample chat.        |
| `GET /terms`      | `legal/terms.html`      | Static, read from `resources/legal/`.         |
| `GET /privacy`    | `legal/privacy.html`    | Static.                                       |
| `GET /refund`     | `legal/refund.html`     | Static.                                       |
| `GET /robots.txt` | inline                  | `SeoController`.                              |
| `GET /sitemap.xml`| inline                  | `SeoController`.                              |
| `GET /manifest.webmanifest`, `/sw.js`, `/offline`, icons | `PwaController` | PWA shell. |

### Auth + account

| Route                              | Template                       | Notes                          |
|------------------------------------|--------------------------------|--------------------------------|
| `GET /login`, `POST /login`        | `login.html`                   | Form login, CSRF.              |
| `GET /register`, `POST /register`  | `register.html`                | Creates `User`, sends verify.  |
| `GET /verify-email`, `POST /verify-email/resend` | `verify-email.html` | `EmailVerificationController`.|
| `GET /password/forgot`, `POST`     | `password/forgot.html`         | Sends reset link.              |
| `GET /password/reset`, `POST`      | `password/reset.html`          | Consumes reset token.          |
| `GET /auth/google/start`, `GET /callback` | (redirects)             | `OAuthStartController`.        |
| `GET /account`                     | `account/index.html`           | Profile + password + email.    |
| `POST /account/email`, `/account/password` | (redirect)             | `AccountController`.           |
| `GET /account/appearance`, `POST`  | `account/appearance.html`      | Theme + accent color.          |
| `GET /account/notifications`       | `account/notifications.html`   | Digest/push prefs.             |
| `GET /account/privacy`             | `account/privacy.html`         | Data export/delete.            |

### Mailboxes

`@RequestMapping("/mailboxes")` on `MailboxController` and
`@RequestMapping("/mailboxes/gmail")` on `GmailConnectController`.

| Route                                     | Template               | Notes                             |
|-------------------------------------------|------------------------|-----------------------------------|
| `GET /mailboxes`                          | `mailboxes/index.html` | List + per-account status.        |
| `GET /mailboxes/new`                      | `mailboxes/new.html`   | IMAP form.                        |
| `POST /mailboxes` (implicit via form)     | (redirect)             | Adds an IMAP account.             |
| `POST /mailboxes/{id}/sync`               | (redirect)             | Kicks a poll.                     |
| `POST /mailboxes/{id}/signature`          | (redirect)             | Updates parsed signature.         |
| `POST /mailboxes/{id}/delete`             | (redirect)             | Removes an account.               |
| `GET /mailboxes/gmail/connect`, `/callback` | (redirects)          | OAuth flow.                       |
| `GET /mailboxes/gmail/reconnect/{id}`     | (redirect)             | Refresh-token reconnect.          |

### Chats

`ConversationController`, `ChatEventsController`, `ChatRedirectController`.

| Route                              | Template / response          | Notes                              |
|------------------------------------|------------------------------|------------------------------------|
| `GET /chats`                       | `chats/index.html`           | Full chat workspace shell.         |
| `GET /chats/new`                   | `chats/new.html`             | Compose to arbitrary addresses.    |
| `POST /chats/new`                  | (redirect)                   | Creates first outbound message.    |
| `GET /chats/{key}`                 | `chats/index.html`           | Chat rail + open pane.             |
| `GET /chats/{key}/pane`            | `chats/pane.html` (fragment) | Morph-swapped by SSE.              |
| `POST /chats/{key}/send`           | (redirect)                   | `ReplyService.send()`.             |
| `POST /chats/refresh`              | (redirect)                   | Force a sync of current mailbox.   |
| `GET /chats/events`                | `text/event-stream`          | SSE — mirrors `/api/v1/events`.    |
| `GET /threads`, `/threads/**`, `/senders` | (redirect to `/chats`) | Legacy URL shim.                   |

### Searches

`@RequestMapping("/searches")`.

| Route                    | Template            | Notes                        |
|--------------------------|---------------------|------------------------------|
| `GET /searches`          | `searches/index.html` | Saved searches list.       |
| `POST /searches`         | (redirect)          | Create a saved search.       |
| `POST /searches/{id}/delete` | (redirect)      | Delete.                      |

### Teams

`TeamInviteController`.

| Route                           | Template           | Notes                            |
|---------------------------------|--------------------|----------------------------------|
| `GET /team/invite`, `POST`      | `team/invite.html` | Owner invites a teammate.        |
| `GET /team/invite/accept`, `POST` | `team/accept.html` | Invitee accepts (token URL).   |

### Billing

`BillingController` and `BillingWebhookController`.

| Route                                 | Template                       | Notes                              |
|---------------------------------------|--------------------------------|------------------------------------|
| `POST /billing/checkout`              | (redirect to Stripe)           | Starts Checkout Session.           |
| `POST /billing/portal`                | (redirect)                     | Opens customer portal.             |
| `GET /billing/success`                | `billing/success.html`         | Post-checkout landing.             |
| `GET /billing/cancel`                 | `billing/cancel.html`          | Aborted checkout.                  |
| `GET /billing/cancel-subscription`, `POST` | `billing/cancel-subscription.html` | Reason-collection cancel flow. |
| `POST /billing/webhook`               | `text/plain`                   | Stripe webhook, CSRF-exempt.       |

### Admin

`AdminRevenueController`.

| Route                                        | Template                    | Notes                            |
|----------------------------------------------|-----------------------------|----------------------------------|
| `GET /admin/revenue`                         | `admin/revenue.html`        | MRR, at-risk queue, win-back.    |
| `POST /admin/retention/win-back`             | (redirect)                  | Per-row send.                    |
| `POST /admin/revenue/reconcile-billing-period` | (redirect)                | Manual reconcile.                |

### Digest

`DigestController`.

| Route                        | Template               | Notes                             |
|------------------------------|------------------------|-----------------------------------|
| `GET /digest/opt-out`        | `digest/opt-out.html`  | One-click unsubscribe.            |
| `POST /preferences`          | (redirect)             | Save digest email preferences.    |

## Shared fragments

- `fragments/live-refresh.html` — SSE bootstrap for `/chats/events`.
- Plan badge / avatar / footer fragments live in `fragments/`.
- Static assets under `src/main/resources/static/`.

## Rule of thumb

If a page needs a new capability, add the method to a service in
`com.emailmessenger.service`, `.billing`, `.email`, or the relevant
package — then have BOTH the Thymeleaf controller and (later) the
`/api/v1/**` handler call it. Do not put logic in `.html` or in a
`@Controller` body.
