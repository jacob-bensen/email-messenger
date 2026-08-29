# Website (Thymeleaf) page inventory

The backend serves the marketing site, product surface, account pages, and
billing/legal pages as server-rendered Thymeleaf views. Every page below
delegates to the same service layer that the (planned) mobile REST API
will use — no page owns business logic directly.

Templates live under `src/main/resources/templates/`, layouts and reusable
bits under `templates/fragments/`, static assets under
`src/main/resources/static/`.

## Layouts and fragments

- `fragments/layout.html` — the site chrome (nav, footer, dark-mode
  toggle).
- `fragments/nav.html` — top navigation, populated by `NavModelAdvice`.
- `fragments/site.html` — marketing-site outer shell, populated by
  `SiteModelAdvice`.
- `fragments/chat-pane.html` — the conversation pane fragment returned by
  `GET /chats/{key}/pane` for in-place swapping.
- `fragments/live-refresh.html` — the client-side JS that consumes the
  `/chats/events` SSE stream and morphs the DOM.
- `fragments/billing-banner.html`, `fragments/onboarding-nudge.html`,
  `fragments/error.html` — smaller reusables.

## Public marketing / legal

| Route | Template | Controller | Purpose |
|---|---|---|---|
| `GET /` | `landing.html` | `PricingController` (yes, index lives on the pricing controller) | Landing page. |
| `GET /pricing` | `pricing.html` | `PricingController` | Plan pricing table. |
| `GET /demo` | (inline) | `DemoController` | Interactive product demo. |
| `GET /privacy` | `legal.html` | `LegalController` | Privacy policy. |
| `GET /terms` | `legal.html` | `LegalController` | Terms of service. |
| `GET /refund` | `legal.html` | `LegalController` | Refund policy. |
| `GET /robots.txt` | inline | `SeoController` | Robots. |
| `GET /sitemap.xml` | inline | `SeoController` | Sitemap. |
| `GET /images/og-card.png` | binary | `SeoController` | Open Graph card. |

## Auth

| Route | Template | Controller | Purpose |
|---|---|---|---|
| `GET /login` | `login.html` | `AuthController` | Login form + Google button. |
| `POST /login` | — | Spring Security | Form-login credentials check. Rate-limited by `LoginThrottleFilter`. |
| `GET /register` | `register.html` | `AuthController` | Signup form. |
| `POST /register` | — | `AuthController` | Creates user, auto-logs-in, optionally redirects to Stripe Checkout for a chosen plan. |
| `GET /auth/google/start` | — | `OAuthStartController` | Starts Google OIDC with a plan/billing intent stored on the session. |
| `GET /verify-email?token=` | — | `EmailVerificationController` | Consumes a one-time token. |
| `POST /verify-email/resend` | — | `EmailVerificationController` | Resends the verification email. |
| `GET /password/forgot` | `password/forgot.html` | `PasswordResetController` | Request form. |
| `POST /password/forgot` | — | `PasswordResetController` | Sends a reset email. |
| `GET /password/reset?token=` | `password/reset.html` | `PasswordResetController` | New-password form. |
| `POST /password/reset` | — | `PasswordResetController` | Consumes the token, sets the password. |
| `POST /logout` | — | Spring Security | Clears session and remember-me cookies. |

## Account settings

| Route | Template | Controller |
|---|---|---|
| `GET /account` | `account.html` | `AccountController` — profile + billing summary. |
| `GET /account/appearance` | `account-appearance.html` | `AccountController`. |
| `POST /account/appearance` | — | `AccountController` — theme, density, accent color. |
| `GET /account/notifications` | `account-notifications.html` | `AccountController`. |
| `GET /account/privacy` | `account-privacy.html` | `AccountController`. |
| `POST /account/password` | — | `AccountController` — change password. |
| `POST /account/email` | — | `AccountController` — change login email. |

## Mailboxes

All under `MailboxController` (base path `/mailboxes`).

| Route | Template | Purpose |
|---|---|---|
| `GET /mailboxes` | `mailboxes/list.html` | List connected mailboxes. |
| `GET /mailboxes/new` | `mailboxes/new.html` | IMAP-connect form. |
| `POST /mailboxes` | — | Create an IMAP mailbox. |
| `POST /mailboxes/{id}/sync` | — | Sync now. |
| `POST /mailboxes/preferences` | — | Global mail preferences. |
| `POST /mailboxes/{id}/signature` | — | Update per-account signature. |
| `POST /mailboxes/{id}/delete` | — | Disconnect. |
| `GET /mailboxes/gmail/connect` | — | Start Gmail OAuth (`GmailConnectController`). |
| `GET /mailboxes/gmail/reconnect/{id}` | — | Re-issue an expired refresh token. |
| `GET /mailboxes/gmail/callback` | — | OAuth callback. |

## Chats workspace (the app surface)

All under `ConversationController`.

| Route | Template | Purpose |
|---|---|---|
| `GET /chats` | `chat.html` | List of chats (the sidebar) with an empty pane. |
| `GET /chats/{key}` | `chat.html` | Same layout with the pane populated for one conversation. |
| `GET /chats/{key}/pane` | `fragments/chat-pane.html` | Just the pane fragment, for in-place swaps. |
| `GET /chats/new` | `compose.html` | Blank-slate compose. |
| `POST /chats/new` | — | Start a new conversation. |
| `POST /chats/{key}/send` | — | Send a message in an existing chat. |
| `POST /chats/refresh` | JSON | Foreground refresh heartbeat (`@ResponseBody`). |
| `GET /chats/events` | SSE | Live-update stream. |

Legacy `/threads` and `/senders` URLs redirect to `/chats` via
`ChatRedirectController`.

## Billing

Under `BillingController` and `BillingWebhookController`.

| Route | Purpose |
|---|---|
| `POST /billing/checkout` | Start Stripe Checkout for the selected plan. |
| `POST /billing/portal` | Redirect to Stripe Customer Portal. |
| `GET /billing/success` | Post-Checkout success landing. |
| `GET /billing/cancel` | Post-Checkout cancel landing. |
| `GET /billing/cancel-subscription` | Cancel-flow confirmation page. |
| `POST /billing/cancel-subscription` | Actually cancel. |
| `POST /billing/webhook` | Stripe webhook (signed). |

## Teams

Under `TeamInviteController`.

| Route | Purpose |
|---|---|
| `GET /team/invite` | Invite form. |
| `POST /team/invite` | Send an invite. |
| `GET /team/invite/accept?token=` | Accept-invite page. |
| `POST /team/invite/accept` | Accept an invite. |

## Digest

| Route | Purpose |
|---|---|
| `GET /digest/opt-out?token=` | Unsubscribe from digest emails via a one-click link (`DigestController`). |

## Admin

Restricted to admin users only.

| Route | Purpose |
|---|---|
| `GET /admin/revenue` | Revenue / metrics dashboard. |
| `POST /admin/revenue/reconcile-billing-period` | Recompute billing-period totals. |
| `POST /admin/retention/win-back` | Trigger a win-back email batch. |

## PWA / static

| Route | Purpose |
|---|---|
| `GET /manifest.webmanifest` | PWA manifest. |
| `GET /sw.js` | Service worker. |
| `GET /offline` | Offline shell. |
| `GET /icons/*.png`, `/apple-touch-icon.png` | App icons. |
| `GET /error` | Global error page (see `GlobalExceptionHandler`). |

## Saved searches

Under `SavedSearchController` (base path `/saved-searches`).

| Route | Purpose |
|---|---|
| `POST /saved-searches/{id}/delete` | Delete one. |

Other CRUD verbs are exercised through the chats-list UI, which posts to
the same controller with query params — inventory these when the mobile
API needs them.
