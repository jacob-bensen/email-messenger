# WEB — ConexusMail Thymeleaf site

Server-rendered pages under `src/main/resources/templates/`. All HTML
controllers live in `com.emailmessenger.web`, `com.emailmessenger.auth`,
`com.emailmessenger.billing`, `com.emailmessenger.team`,
`com.emailmessenger.digest`, and `com.emailmessenger.admin`. They MUST
reuse the same service beans the (future) mobile API will use — never
fork business logic between a controller and a template partial.

Auth: session-cookie login via Spring Security. `SecurityConfig` declares
the public paths; everything else requires an authenticated session. See
`BACKEND.md` for the full matcher list.

Fragments and layouts live under `templates/fragments/` and are pulled in
by pages via `th:replace`.

## Page inventory

### Marketing / public

| Route         | View                | Controller                    | Notes                                      |
| ------------- | ------------------- | ----------------------------- | ------------------------------------------ |
| GET `/`       | `landing`           | `web/PricingController`       | Redirects signed-in users to `/threads`    |
| GET `/pricing`| `pricing`           | `web/PricingController`       | Plan comparison                            |
| GET `/demo`   | `conversation`      | `web/DemoController`          | Synthetic conversation showcase            |
| GET `/privacy`| `legal`             | `web/LegalController`         | Content from `src/main/resources/legal/`   |
| GET `/terms`  | `legal`             | `web/LegalController`         | Same                                       |
| GET `/refund` | `legal`             | `web/LegalController`         | Same                                       |

Legal content is overridable per-page via env: `MARKETING_LEGAL_PRIVACY`,
`MARKETING_LEGAL_TERMS`, `MARKETING_LEGAL_REFUND` (Spring resource
locators; see `master/MASTER_ACTIONS.md`).

### Auth

| Route                          | View                | Controller                        |
| ------------------------------ | ------------------- | --------------------------------- |
| GET `/login`                   | `login`             | `auth/AuthController`             |
| POST `/login`                  | (Spring Security)   | —                                 |
| POST `/logout`                 | (Spring Security)   | —                                 |
| GET `/register`                | `register`          | `auth/AuthController`             |
| POST `/register`               | redirect            | `auth/AuthController`             |
| GET `/auth/google/start`       | redirect            | `auth/OAuthStartController`       |
| `/oauth2/authorization/google` | (Spring Security)   | —                                 |
| GET `/password/forgot`         | `password/forgot`   | `auth/PasswordResetController`    |
| POST `/password/forgot`        | same                | `auth/PasswordResetController`    |
| GET `/password/reset`          | `password/reset`    | `auth/PasswordResetController`    |
| POST `/password/reset`         | redirect            | `auth/PasswordResetController`    |
| GET `/verify-email`            | `verify-email`      | `auth/EmailVerificationController`|
| POST `/verify-email/resend`    | redirect            | `auth/EmailVerificationController`|

### Account (authenticated)

| Route                              | View                    | Controller               |
| ---------------------------------- | ----------------------- | ------------------------ |
| GET `/account`                     | `account`               | `auth/AccountController` |
| GET `/account/notifications`       | `account-notifications` | `auth/AccountController` |
| GET `/account/appearance`          | `account-appearance`    | `auth/AccountController` |
| POST `/account/appearance`         | redirect                | `auth/AccountController` |
| GET `/account/privacy`             | `account-privacy`       | `auth/AccountController` |
| POST `/account/password`           | redirect                | `auth/AccountController` |
| POST `/account/email`              | redirect (invalidates session) | `auth/AccountController` |

### Chats (the IM-style workspace, authenticated)

| Route                          | View / fragment                | Controller                    |
| ------------------------------ | ------------------------------ | ----------------------------- |
| GET `/threads`, `/threads/**`, `/senders` | redirect to `/chats` | `web/ChatRedirectController`  |
| GET `/chats`                   | `chat`                         | `web/ConversationController`  |
| GET `/chats/new`               | `compose`                      | `web/ConversationController`  |
| POST `/chats/new`              | redirect to `/chats/{key}`     | `web/ConversationController`  |
| GET `/chats/{key}`             | `chat`                         | `web/ConversationController`  |
| GET `/chats/{key}/pane`        | `fragments/chat-pane :: pane`  | `web/ConversationController`  |
| POST `/chats/{key}/send`       | redirect                       | `web/ConversationController`  |
| POST `/chats/refresh`          | JSON `{imported: N}` (`@ResponseBody`) | `web/ConversationController` |
| GET `/chats/events`            | SSE stream                     | `web/ChatEventsController`    |

Live updates: `chat.html` subscribes to `/chats/events` and swaps rail
rows + the open conversation pane in place via the fragment endpoint,
wired through `templates/fragments/live-refresh.html`.

### Mailboxes

| Route                                       | View                | Controller                    |
| ------------------------------------------- | ------------------- | ----------------------------- |
| GET `/mailboxes`                            | `mailboxes/index`   | `web/MailboxController`       |
| GET `/mailboxes/new`                        | `mailboxes/new`     | `web/MailboxController`       |
| POST `/mailboxes`                           | redirect            | `web/MailboxController`       |
| POST `/mailboxes/{id}/sync`                 | redirect            | `web/MailboxController`       |
| POST `/mailboxes/preferences`               | redirect            | `web/MailboxController`       |
| POST `/mailboxes/{id}/signature`            | redirect            | `web/MailboxController`       |
| POST `/mailboxes/{id}/delete`               | redirect            | `web/MailboxController`       |
| GET `/mailboxes/gmail/connect`              | external redirect   | `web/GmailConnectController`  |
| GET `/mailboxes/gmail/reconnect/{id}`       | external redirect   | `web/GmailConnectController`  |
| GET `/mailboxes/gmail/callback`             | redirect            | `web/GmailConnectController`  |

Gmail reconnect exists because the OAuth consent screen is still in
Testing mode, where Google expires refresh tokens after 7 days
(`master/MASTER_ACTIONS.md`; roadmap: "Publish the Google OAuth consent
screen").

### Saved searches

| Route                                    | View     | Controller                     |
| ---------------------------------------- | -------- | ------------------------------ |
| POST `/searches`                         | redirect | `web/SavedSearchController`    |
| POST `/searches/{id}/delete`             | redirect | `web/SavedSearchController`    |

### Billing (Stripe)

| Route                                    | View                     | Controller                   |
| ---------------------------------------- | ------------------------ | ---------------------------- |
| POST `/billing/checkout`                 | external Stripe redirect | `billing/BillingController`  |
| POST `/billing/portal`                   | external Stripe redirect | `billing/BillingController`  |
| GET `/billing/success`                   | `billing/success`        | `billing/BillingController`  |
| GET `/billing/cancel`                    | redirect                 | `billing/BillingController`  |
| GET `/billing/cancel-subscription`       | `billing/cancel-subscription` | `billing/BillingController` |
| POST `/billing/cancel-subscription`      | external Stripe redirect | `billing/BillingController`  |
| POST `/billing/webhook`                  | text/plain response      | `billing/BillingWebhookController` (`@RestController`) |

### Team

| Route                                    | View                | Controller                     |
| ---------------------------------------- | ------------------- | ------------------------------ |
| GET `/team/invite`                       | `team/invite`       | `team/TeamInviteController`    |
| POST `/team/invite`                      | redirect            | `team/TeamInviteController`    |
| GET `/team/invite/accept`                | `team/accept`       | `team/TeamInviteController`    |
| POST `/team/invite/accept`               | redirect            | `team/TeamInviteController`    |

### Digest

| Route                                    | View               | Controller             |
| ---------------------------------------- | ------------------ | ---------------------- |
| GET `/digest/opt-out`                    | `digest/opt-out`   | `digest/DigestController` — token in URL |

### Admin (operator-only, gated by `ADMIN_EMAILS`)

| Route                                              | View            | Controller                       |
| -------------------------------------------------- | --------------- | -------------------------------- |
| GET `/admin/revenue`                               | `admin/revenue` | `admin/AdminRevenueController`   |
| POST `/admin/revenue/reconcile-billing-period`     | redirect        | `admin/AdminRevenueController`   |
| POST `/admin/retention/win-back`                   | redirect        | `admin/AdminRevenueController`   |

### PWA / SEO shell

See `API.md` §1.4 — these serve non-view resources (manifest, service
worker, offline shell, icons, `robots.txt`, `sitemap.xml`, OG card).

## Shared fragments and layouts

- `fragments/live-refresh.html` — SSE-driven rail + pane morph glue.
- `fragments/chat-pane.html` — the `pane` fragment served by
  `GET /chats/{key}/pane`.
- Other fragments (header, footer, plan-badge, avatar, day-separator)
  live alongside under `templates/fragments/` — enumerate as they get
  touched; keep this list accurate.

## Editing rules

- Any change that alters what the user can do must go through the service
  layer, not the controller or the template. When a mobile-facing JSON
  version of the same action lands, both entry points must call the exact
  same service method.
- Never introduce a Thymeleaf-only branch of logic ("hide this in the
  template if unpaid") — enforce the rule in the service.
- CSRF: every mutating form must render Spring Security's `_csrf` token.
