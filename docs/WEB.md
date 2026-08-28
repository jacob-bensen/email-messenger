# WEB

Server-rendered marketing site and logged-in web app (Thymeleaf). This
file is the page inventory for the *website* half of the backend; the
mobile client's contract is in [`API.md`](API.md).

All pages are Thymeleaf views under
`src/main/resources/templates/`. Controllers live in
`com.emailmessenger.web`, `.auth`, `.billing`, `.team`, `.digest`, and
`.admin`. Shared fragments (nav, chat pane, PWA install banner,
service-worker registration, SEO head, live-refresh JS, cookie banner)
live in `templates/fragments/`.

Every controller listed here delegates state changes to the services
under `com.emailmessenger.service` (chat/reply), `.email` (mailbox
sync/send), `.billing` (Stripe), and `.auth` (accounts). The rule from
[`BACKEND.md`](BACKEND.md) applies: the JSON API must call the same
service methods — no forked logic per surface.

## Public / anonymous

| Route | Controller | Template | Notes |
| :-- | :-- | :-- | :-- |
| `GET /` | `PricingController` | `landing.html` | Marketing landing; hero + demo embed placeholder. |
| `GET /pricing` | `PricingController` | `pricing.html` | Plans + Stripe Checkout entry points. |
| `GET /demo` | `DemoController` | (chat) | Read-only walkthrough backed by `DemoConversationService`. |
| `GET /login` | `AuthController` | `login.html` | Form + Google button (button hidden if OAuth not configured). |
| `GET /register` / `POST /register` | `AuthController` | `register.html` | Email + password signup; triggers verification mail. |
| `GET /password/forgot` / `POST /password/forgot` | `PasswordResetController` | `password/forgot.html` | Sends reset email. |
| `GET /password/reset` / `POST /password/reset` | `PasswordResetController` | `password/reset.html` | Token-consuming form. |
| `GET /verify-email` / `POST /verify-email/resend` | `EmailVerificationController` | `verify-email.html` | Verify by token; resend nudge. |
| `GET /privacy` / `/terms` / `/refund` | `LegalController` | `legal.html` | Env-overridable boilerplate under `src/main/resources/legal/`. |
| `GET /robots.txt` / `/sitemap.xml` / `/images/og-card.png` | `SeoController` | inline | SEO / OpenGraph. |

## PWA shell

| Route | Controller | Media type |
| :-- | :-- | :-- |
| `GET /manifest.webmanifest` | `PwaController` | `application/manifest+json` |
| `GET /sw.js` | `PwaController` | `application/javascript` |
| `GET /icons/icon-192.png`, `/icons/icon-512.png`, `/icons/icon-512-maskable.png`, `/apple-touch-icon.png` | `PwaController` | `image/png` |
| `GET /offline` | `PwaController` | inline HTML — service-worker fallback shell. |

## Authenticated web app

### Chats (the workspace)

| Route | Controller | Template |
| :-- | :-- | :-- |
| `GET /chats` | `ConversationController` | `chat.html` (rail + open pane) |
| `GET /chats/new` / `POST /chats/new` | `ConversationController` | `compose.html` |
| `POST /chats/refresh` | `ConversationController` | rail fragment |
| `GET /chats/{key}` | `ConversationController` | `chat.html` (with `{key}` selected) |
| `GET /chats/{key}/pane` | `ConversationController` | `fragments/chat-pane.html` (HTMX-style morph) |
| `POST /chats/{key}/send` | `ConversationController` | fragment update |
| `GET /chats/events` | `ChatEventsController` | `text/event-stream` — subscribes the tab to `NewMailEvent` via `ChatEventStream` |

Legacy redirect: `GET /threads`, `/threads/**`, `/senders` →
`ChatRedirectController` (all now redirect into `/chats`).

### Mailboxes

| Route | Controller |
| :-- | :-- |
| `GET  /mailboxes` | `MailboxController` |
| `GET  /mailboxes/new` | `MailboxController` — IMAP password connect form |
| `POST /mailboxes` | `MailboxController` — create IMAP mailbox |
| `POST /mailboxes/preferences` | `MailboxController` — per-user mail prefs |
| `POST /mailboxes/{id}/sync` | `MailboxController` — force-pull now |
| `POST /mailboxes/{id}/signature` | `MailboxController` |
| `POST /mailboxes/{id}/delete` | `MailboxController` |
| `GET  /mailboxes/gmail/connect` | `GmailConnectController` — start Gmail OAuth |
| `GET  /mailboxes/gmail/reconnect/{id}` | `GmailConnectController` — refresh-token expiry recovery (needed while consent screen is in Testing mode) |
| `GET  /mailboxes/gmail/callback` | `GmailConnectController` — OAuth callback |

### Account settings

| Route | Controller | Template |
| :-- | :-- | :-- |
| `GET /account` | `AccountController` | `account.html` |
| `GET /account/notifications` | `AccountController` | `account-notifications.html` |
| `GET /account/appearance` / `POST /account/appearance` | `AccountController` | `account-appearance.html` |
| `GET /account/privacy` | `AccountController` | `account-privacy.html` |
| `POST /account/password` | `AccountController` |  |
| `POST /account/email` | `AccountController` |  |

### Auth adjacent

| Route | Controller |
| :-- | :-- |
| `GET /auth/google/start` | `OAuthStartController` |
| Spring Security's own `/login` POST handles form submits. |

### Team

| Route | Controller |
| :-- | :-- |
| `GET  /team/invite` / `POST /team/invite` | `TeamInviteController` |
| `GET  /team/invite/accept` / `POST /team/invite/accept` | `TeamInviteController` |

Team templates live under `templates/team/`.

### Saved searches

| Route | Controller |
| :-- | :-- |
| `POST /searches` | `SavedSearchController` |
| `POST /searches/{id}/delete` | `SavedSearchController` |

Saved-search chips render inside `chat.html`.

### Billing

| Route | Controller | Notes |
| :-- | :-- | :-- |
| `POST /billing/checkout` | `BillingController` | Stripe Checkout session. |
| `POST /billing/portal` | `BillingController` | Customer Billing Portal. |
| `GET  /billing/success` | `BillingController` | Return URL. |
| `GET  /billing/cancel` | `BillingController` | Return URL. |
| `GET  /billing/cancel-subscription` / `POST /billing/cancel-subscription` | `BillingController` | In-app cancel with reason picker. |
| `POST /billing/webhook` | `BillingWebhookController` | Stripe-signed. |

### Digest

| Route | Controller |
| :-- | :-- |
| `GET /digest/opt-out` | `DigestController` |

### Admin (operator-only, gated by `ADMIN_EMAILS`)

| Route | Controller |
| :-- | :-- |
| `GET  /admin/revenue` | `AdminRevenueController` |
| `POST /admin/revenue/reconcile-billing-period` | `AdminRevenueController` |
| `POST /admin/retention/win-back` | `AdminRevenueController` |

Admin templates live under `templates/admin/`.

## Shared fragments

Under `src/main/resources/templates/fragments/`:

- `app-nav.html` — top navigation for the logged-in shell.
- `settings-nav.html` — sub-nav for `/account/*`.
- `chat-pane.html` — the message pane rendered on `/chats/{key}` and
  morphed in place by `/chats/{key}/pane`.
- `live-refresh.html` — JS that subscribes to `/chats/events` (SSE)
  and swaps rail + pane fragments in place on `new-mail` events.
- `install-banner.html` — PWA "install this app" banner.
- `cookie-banner.html` — cookie consent.
- `sw-register.html` — registers `/sw.js`.
- `seo.html` — page-level SEO / OG head elements.

## CSS / static

`src/main/resources/static/css/` holds the chat styling (bubbles,
avatars, day separators, dark mode) and the site styling. The PWA
service worker is generated by `PwaController` rather than served as a
static file so it can inline the current asset hashes.
