# Website (Thymeleaf) — page inventory

Server-rendered pages under `src/main/resources/templates/`, driven by
the controllers in `com.emailmessenger.web`, `.auth`, `.admin`,
`.billing`, `.team`, and `.digest`. Every controller goes through the
same `service/` layer as the (future) REST API — no business logic
lives in a controller or template.

## Layouts and shared fragments

- `fragments/` — reusable pieces (nav, footer, chat rail, live-refresh
  wiring). Fragments are `th:fragment` blocks the page templates
  include with `th:insert`.
- `SiteModelAdvice`, `NavModelAdvice`, `SiteProperties` — populate
  common model attributes (nav state, site name, canonical URLs).
- `GlobalExceptionHandler` — renders `error.html` on unhandled
  exceptions.

## Public / unauthenticated

| Route | Template | Controller | Notes |
|---|---|---|---|
| `GET /` | `landing.html` | `PricingController.landing` | Marketing landing page. |
| `GET /pricing` | `pricing.html` | `PricingController.pricing` | Free / Pro / Business tiers. |
| `GET /demo` | (fragments) | `DemoController.demo` | Static demo of the IM view. |
| `GET /login` | `login.html` | `AuthController.loginPage` | Form + Google OAuth button. |
| `POST /login` | — | Spring Security form login | `loginProcessingUrl`. |
| `GET /register` | `register.html` | `AuthController.registerPage` | New signup. |
| `POST /register` | — | `AuthController.register` | Creates `User`, sends verify email. |
| `GET /logout` | — | Spring Security | Clears session + remember-me. |
| `GET /auth/google/start` | — | `OAuthStartController.start` | Kicks off OIDC flow. |
| `GET /password/forgot` / `POST` | `password/forgot.html` | `PasswordResetController` | Request reset email. |
| `GET /password/reset` / `POST` | `password/reset.html` | `PasswordResetController` | Consume token, set new password. |
| `GET /verify-email` | (redirect) | `EmailVerificationController.verify` | Consume verify token. |
| `POST /verify-email/resend` | — | `EmailVerificationController.resend` | Resend verify link. |
| `GET /privacy` / `/terms` / `/refund` | `legal.html` | `LegalController` | Legal pages. |
| `GET /digest/opt-out` | `digest/opt-out.html` | `DigestController` | Token-consumed unsub. |
| `GET /robots.txt` / `/sitemap.xml` / `/images/og-card.png` | — | `SeoController` | SEO. |

## Authenticated app

| Route | Template | Controller | Notes |
|---|---|---|---|
| `GET /chats` | `chat.html` (list mode) | `ConversationController.list` | Texting-style list. Query params: `page`, `q`, `mailbox`, `filter`. |
| `GET /chats/new` / `POST` | `compose.html` | `ConversationController` | Start a new chat. |
| `POST /chats/refresh` | — | `ConversationController.refresh` | Manual mailbox poll. |
| `GET /chats/{key}` | `chat.html` (with open pane) | `ConversationController.show` | Full-page view of one conversation. |
| `GET /chats/{key}/pane` | `fragments/chat-pane` | `ConversationController.pane` | HTMX-style pane fragment. |
| `POST /chats/{key}/send` | — | `ConversationController.send` | Send a reply. |
| `GET /chats/events` | (SSE) | `ChatEventStream` | Server-Sent Events for live rail + pane updates. |
| `GET /mailboxes` / `GET /mailboxes/new` | `mailboxes/*.html` | `MailboxController` | List / add mailbox. |
| `POST /mailboxes` | — | `MailboxController.create` | Add IMAP or Gmail-OAuth mailbox. |
| `POST /mailboxes/{id}/sync` | — | `MailboxController.sync` | Trigger a manual sync. |
| `POST /mailboxes/preferences` | — | `MailboxController.preferences` | Poll interval, IDLE opt-in. |
| `POST /mailboxes/{id}/signature` | — | `MailboxController.signature` | Set per-mailbox signature. |
| `POST /mailboxes/{id}/delete` | — | `MailboxController.delete` | Remove a mailbox. |
| `GET /mailboxes/gmail/connect` | — | `GmailConnectController.connect` | OAuth start (Gmail). |
| `GET /mailboxes/gmail/reconnect/{id}` | — | `GmailConnectController.reconnect` | Re-issue refresh token. |
| `GET /mailboxes/gmail/callback` | — | `GmailConnectController.callback` | OAuth callback. |
| `GET /account` | `account.html` | `AccountController.account` | Account overview. |
| `GET /account/notifications` | `account-notifications.html` | `AccountController` | Digest opt-in etc. |
| `GET /account/appearance` / `POST` | `account-appearance.html` | `AccountController` | Theme, accent. |
| `GET /account/privacy` | `account-privacy.html` | `AccountController` | Data export / delete stubs. |
| `POST /account/password` | — | `AccountController.password` | Change password. |
| `POST /account/email` | — | `AccountController.email` | Change email (re-verifies). |
| `GET /searches` / `POST` / `POST /{id}/delete` | (fragments) | `SavedSearchController` | Saved `/chats` filters. |
| `GET /team/invite` / `POST` | `team/invite.html` | `TeamInviteController` | Invite a teammate. |
| `GET /team/invite/accept` / `POST` | `team/invite-accept.html` | `TeamInviteController` | Accept an invite. |

## Billing

| Route | Notes |
|---|---|
| `POST /billing/checkout` | Redirect to Stripe Checkout. |
| `POST /billing/portal` | Redirect to Stripe billing portal. |
| `POST /billing/webhook` | Stripe webhook, CSRF-exempt, signature-verified. |

## Admin

| Route | Notes |
|---|---|
| `GET /admin/revenue` | Operator dashboard. |
| `POST /admin/revenue/reconcile-billing-period` | Nightly reconcile trigger. |
| `POST /admin/retention/win-back` | Send win-back email to a row. |

## PWA shell

| Route | Notes |
|---|---|
| `GET /manifest.webmanifest` | PWA manifest. |
| `GET /sw.js` | Service worker. |
| `GET /offline` | Offline fallback page. |
| `GET /icons/*`, `/apple-touch-icon.png` | Icon set. |

## Notes for mobile parity

- The chat list, chat pane, mailbox management, and reply send flows
  are the ones the mobile client will need first. When the REST API
  wraps them, the JSON endpoints reuse the same `ConversationListService`,
  `ChatService`, `ReplyService`, and `MailAccountService` — no duplicate
  logic in the API-layer controllers.
- The SSE stream at `/chats/events` is browser-only. The mobile push
  path is a separate roadmap item (VAPID Web Push for PWA; native
  APNs/FCM for the mobile client).
