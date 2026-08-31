# Website (Thymeleaf)

The public site + product web app is server-rendered Thymeleaf. It shares
the service layer with the REST API (`docs/API.md`) so the two never drift
in behavior. Do not duplicate business logic in a controller or template —
if you find yourself doing that, extract a `service/` class first and call
it from both `web/` and `api/`.

## Layouts & fragments

`src/main/resources/templates/fragments/` holds the reusable pieces:

- `layout.html` — outer chrome (head, nav, footer).
- `nav.html` — top nav; populated by `web/NavModelAdvice.java`.
- `chat-pane.html`, `chat-rail.html` — the /chats workspace fragments.
- `live-refresh.html` — SSE glue that morphs new messages into the DOM.
- `flash.html` — flash messages.

`web/SiteModelAdvice.java` and `web/NavModelAdvice.java` populate
model attributes on every request (site branding, current user, plan
badge, active nav tab).

## Page inventory

Legend: **Route** → template — controller — main services used.

### Marketing / legal / SEO

- `GET /` → `landing.html` — `PricingController` — `LandingProperties`.
- `GET /pricing` → `pricing.html` — `PricingController` —
  `BillingService`.
- `GET /demo` → `conversation.html` — `DemoController` —
  `DemoConversationService`.
- `GET /privacy|/terms|/refund` → `legal.html` — `LegalController` —
  `LegalProperties`.
- `GET /robots.txt|/sitemap.xml|/images/og-card.png` — `SeoController`.

### Auth

- `GET|POST /login` — `AuthController`.
- `GET|POST /register` — `AuthController` — `UserService`.
- `GET /auth/google/start` — `OAuthStartController` (stashes plan/UTM
  intent, redirects to `/oauth2/authorization/google`).
- `GET|POST /password/forgot`, `GET|POST /password/reset` —
  `PasswordResetController`.
- `GET /verify-email`, `POST /verify-email/resend` —
  `EmailVerificationController`.

### Account

- `GET /account`, `GET /account/notifications`,
  `GET|POST /account/appearance`, `GET /account/privacy` —
  `AccountController`.
- `POST /account/password`, `POST /account/email` — password / email
  change flows.

### Chats workspace

`web/ConversationController` + `web/ChatService` +
`service/ConversationService`, `service/ConversationKeyService`,
`web/ConversationListService`, `web/OutboundMessageService`,
`service/ReplyService`.

- `GET /chats` → `chat.html` — the rail-and-pane workspace.
- `GET /chats/new`, `POST /chats/new` — compose a first message.
- `GET /chats/{key}` → `chat.html` — open a specific conversation.
- `GET /chats/{key}/pane` — fragment (`chat-pane :: pane`), used by the
  SSE morph.
- `POST /chats/{key}/send` — reply through `ReplyService`.
- `GET /chats/events` — SSE stream (`ChatEventStream`); browser only.
- `POST /chats/refresh` — foreground poll; returns `{"imported": N}`.
- Legacy redirects `/threads`, `/threads/**`, `/senders` → `/chats`
  (`ChatRedirectController`).

### Mailboxes

- `GET /mailboxes`, `GET /mailboxes/new`, `POST /mailboxes`,
  `POST /mailboxes/{id}/sync`, `POST /mailboxes/preferences`,
  `POST /mailboxes/{id}/signature`, `POST /mailboxes/{id}/delete` —
  `MailboxController` — `MailAccountService`, `MailboxPollingService`.
- `GET /mailboxes/gmail/connect`, `GET /mailboxes/gmail/reconnect/{id}`,
  `GET /mailboxes/gmail/callback` — `GmailConnectController` — Google
  OAuth flow.

### Saved searches

- `POST /searches`, `POST /searches/{id}/delete` —
  `SavedSearchController` — `SavedSearchService`.

### Billing

- `POST /billing/checkout`, `POST /billing/portal`,
  `GET /billing/success`, `GET /billing/cancel`,
  `GET|POST /billing/cancel-subscription` — `BillingController` —
  `BillingService`, Stripe SDK.
- `POST /billing/webhook` — `BillingWebhookController` — verifies Stripe
  signature; unauthenticated, CSRF-exempt.

### Digest / teams / admin

- `GET /digest/opt-out?token=…` — `DigestController`.
- `GET|POST /team/invite`, `GET|POST /team/invite/accept` —
  `TeamInviteController`.
- `GET /admin/revenue`, `POST /admin/revenue/reconcile-billing-period`,
  `POST /admin/retention/win-back` — `AdminRevenueController` (guarded
  by `AdminAuthorizer.requireAdmin`).

### PWA

- `GET /manifest.webmanifest`, `/icons/icon-192.png`,
  `/icons/icon-512.png`, `/icons/icon-512-maskable.png`,
  `/apple-touch-icon.png`, `/sw.js`, `/offline` — `PwaController`.

## Styling

- `src/main/resources/static/css/` — dark-mode-first, IM bubble UI.
- `src/main/resources/static/js/` — minimal vanilla JS, mostly the SSE
  morph in `chat.html`.

## Not part of the mobile API

Everything on this page is browser-facing (session + CSRF). Do not call
these endpoints from the Flutter client — use `/api/v1/**` instead.
