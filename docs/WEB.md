# WEB — the Thymeleaf website

Every user-facing HTML page rendered by the backend. Routes here are
Thymeleaf-served; JSON/REST endpoints for the mobile client live in
`docs/API.md`.

## Page inventory

### Marketing / public

| Route | Template | Controller | Purpose |
|---|---|---|---|
| `GET /` | `landing.html` | `PricingController` | Landing page with hero, features, plan tiles. |
| `GET /pricing` | `pricing.html` | `PricingController` | Plan comparison + monthly/annual toggle. |
| `GET /demo` | (redirect) | `DemoController` | Redirects to the demo video URL. |
| `GET /privacy` `/terms` `/refund` | `legal.html` | `LegalController` | Legal pages driven by `LegalProperties`. |
| `GET /robots.txt` `/sitemap.xml` `/images/og-card.png` | — | `SeoController` | SEO / OG assets. |

### Auth

| Route | Template | Controller | Purpose |
|---|---|---|---|
| `GET /login` | `login.html` | `AuthController` | Form login + Google button. |
| `GET /register` `POST /register` | `register.html` | `AuthController` | New account creation. |
| `GET /verify-email` | `verify-email.html` | `EmailVerificationController` | Consume email verification token. |
| `POST /verify-email/resend` | — | `EmailVerificationController` | Resend the verification email. |
| `GET /auth/google/start` | (OAuth redirect) | `OAuthStartController` | Kicks Google OIDC with a stored `OAuthIntent`. |
| `GET /password/forgot` `POST /password/forgot` | `password/forgot.html` | `PasswordResetController` | Request reset email. |
| `GET /password/reset` `POST /password/reset` | `password/reset.html` | `PasswordResetController` | Consume token, set new password. |

### Chats workspace

| Route | Template | Controller | Purpose |
|---|---|---|---|
| `GET /chats` | `chat.html` | `ConversationController` | Conversation list + Dashboard/Hub view. |
| `GET /chats/{key}` | `chat.html` | `ConversationController` | List + open conversation pane. |
| `GET /chats/{key}/pane` | `fragments/chat-pane :: pane` | `ConversationController` | Just the pane, for in-place swaps. |
| `POST /chats/{key}/send` | (redirect) | `ConversationController` | Send reply or new-thread message. |
| `GET /chats/new` `POST /chats/new` | `compose.html` | `ConversationController` | Compose a brand-new conversation. |
| `POST /chats/refresh` | JSON | `ConversationController` | Foreground poll heartbeat. Returns `{ imported: N }`. |
| `GET /chats/events` | SSE | `ChatEventsController` | Live new-mail stream (`text/event-stream`). |
| `GET /threads/**` `/senders` | (redirect) | `ChatRedirectController` | Legacy URLs → `/chats`. |

### Mailboxes

| Route | Template | Controller | Purpose |
|---|---|---|---|
| `GET /mailboxes/new` | `mailboxes/new.html` | `MailboxController` | Connect an IMAP mailbox (password). |
| `POST /mailboxes/{id}/sync` | (redirect) | `MailboxController` | Manual "Sync now" trigger. |
| `POST /mailboxes/preferences` | (redirect) | `MailboxController` | Update user's mail preferences. |
| `POST /mailboxes/{id}/signature` | (redirect) | `MailboxController` | Update per-mailbox signature. |
| `POST /mailboxes/{id}/delete` | (redirect) | `MailboxController` | Disconnect a mailbox. |
| `GET /mailboxes/gmail/connect` | (OAuth redirect) | `GmailConnectController` | Start Gmail OAuth. |
| `GET /mailboxes/gmail/reconnect/{id}` | (OAuth redirect) | `GmailConnectController` | Re-issue OAuth for an existing mailbox. |
| `GET /mailboxes/gmail/callback` | (redirect) | `GmailConnectController` | OAuth code exchange. |

### Account / settings

| Route | Template | Controller | Purpose |
|---|---|---|---|
| `GET /account` | `account.html` | `AccountController` | Overview + billing summary. |
| `GET /account/notifications` | `account-notifications.html` | `AccountController` | Digest + notification prefs. |
| `GET /account/appearance` `POST /account/appearance` | `account-appearance.html` | `AccountController` | Theme + density. |
| `GET /account/privacy` `POST /account/password` `POST /account/email` | `account-privacy.html` | `AccountController` | Password + login-email change. |

### Billing

| Route | Template | Controller | Purpose |
|---|---|---|---|
| `POST /billing/checkout` | (Stripe redirect) | `BillingController` | Create Stripe Checkout session. |
| `POST /billing/portal` | (Stripe redirect) | `BillingController` | Open Stripe Customer Portal. |
| `GET /billing/success` | `billing/success.html` | `BillingController` | Post-checkout landing. |
| `GET /billing/cancel` | (redirect) | `BillingController` | Post-checkout cancel landing. |
| `GET /billing/cancel-subscription` `POST /billing/cancel-subscription` | `billing/cancel-subscription.html` | `BillingController` | Cancellation flow. |
| `POST /billing/webhook` | JSON | `BillingWebhookController` | Stripe webhook receiver. |

### Teams / saved searches / digests

| Route | Template | Controller | Purpose |
|---|---|---|---|
| `GET /team/invite` `POST /team/invite` | `team/invite.html` | `TeamInviteController` | Send a team invite. |
| `GET /team/invite/accept` `POST /team/invite/accept` | `team/accept.html` | `TeamInviteController` | Accept an invite. |
| `GET /searches` `POST /searches` `POST /searches/{id}/delete` | (fragment) | `SavedSearchController` | Manage saved searches. |
| `GET /digest/opt-out` | `digest/opt-out.html` | `DigestController` | Unsubscribe from digests. |

### Admin (gated by `AdminAuthorizer`)

| Route | Template | Controller | Purpose |
|---|---|---|---|
| `GET /admin/revenue` | `admin/revenue.html` | `AdminRevenueController` | Revenue, churn, funnel, win-back metrics. |
| `POST /admin/revenue/reconcile-billing-period` | (redirect) | `AdminRevenueController` | Backfill billing-period rows. |
| `POST /admin/retention/win-back` | (redirect) | `AdminRevenueController` | Send a win-back email to a canceled customer. |

### PWA shell

| Route | Controller | Purpose |
|---|---|---|
| `GET /manifest.webmanifest` | `PwaController` | Web app manifest. |
| `GET /sw.js` | `PwaController` | Service worker. |
| `GET /offline` | `PwaController` | Offline fallback page. |
| `GET /icons/*.png` `/apple-touch-icon.png` | `PwaController` | PWA icons. |

## Shared fragments

Under `templates/fragments/`:

- `app-nav.html` — top nav shared by every logged-in page. Model
  attributes come from `NavModelAdvice`.
- `chat-pane.html` — the open-conversation pane (timeline, reply box,
  signatures). Used by `GET /chats/{key}` full page AND `GET /chats/{key}/pane`
  fragment swap.
- `live-refresh.html` — inline JS that consumes `/chats/events` SSE and
  morphs the rail + pane in place when new mail arrives.
- `settings-nav.html` — settings sub-nav.
- `cookie-banner.html`, `install-banner.html`, `sw-register.html`,
  `seo.html` — cross-cutting includes.

## Service layer used by the pages

Every page delegates to `@Service` beans; controllers stay thin. The
key ones the mobile API MUST also use (never fork):

- `UserService` — resolve authenticated `User` from the principal.
- `MailAccountService` — list/create/delete mailboxes.
- `MailboxPollingService` — foreground refresh + scheduled polling.
- `ConversationListService` — paginated conversation list + counts.
- `ChatService` — build one open conversation, mark-read.
- `ConversationKeyService` — deterministic conversation-key hashing.
- `ReplyService` — SMTP send (reply into thread, or new email).
- `OutboundMessageService` — record the outbound copy for the chat rail.
- `PlanLimitService` — every quota-bearing mutation gates through here.
- `BillingService` / `BillingBannerService` — plan state + upsell banners.

**Any new mobile endpoint MUST call these same services.** Duplicating
business logic in a JSON controller is a bug.
