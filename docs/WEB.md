# WEB (Thymeleaf website)

Server-rendered pages that make up ConexusMail's product website and
in-app UI. The website and any future REST/JSON API for a mobile client
MUST share the same service layer under `com.emailmessenger.service` /
`com.emailmessenger.admin` / `com.emailmessenger.billing` etc. — never
fork business logic into a controller or a template.

## Page inventory

Grouped by controller. Template paths are relative to
`src/main/resources/templates/`.

### Marketing / legal / assets
- `SeoController` — `landing.html`, `robots.txt`, `sitemap.xml`, OG card,
  favicons, PWA manifest, offline shell.
- `PwaController` — `sw.js`, PWA icons.
- `PricingController` — `pricing.html`.
- `DemoController` — `demo.html`.
- `LegalController` — `legal.html` (rendered for `/privacy`, `/terms`,
  `/refund`; source under `src/main/resources/legal/`).

### Auth
- `AuthController` — `login.html`, `register.html`.
- `AccountController` — `account.html`, `account-appearance.html`,
  `account-notifications.html`, `account-privacy.html`.
- `EmailVerificationController` — `verify-email.html`.
- `PasswordResetController` — `password/*.html`.
- `OAuthStartController` — Google OIDC entry point.

### Mailboxes
- `MailboxController` — under `templates/mailboxes/`.
- `GmailConnectController` — Gmail OAuth callback and reconnect.

### Chats (IM workspace, primary UX)
- `ConversationController` — `chat.html` and the `pane` fragment;
  publishes reply sends. Consumes `ChatEventStream` via the SSE endpoint
  on the page.
- `ChatRedirectController` — legacy `/threads` and `/senders` URLs.
- `ChatEventsController` — `/chats/events` SSE stream (no template).
- `SavedSearchController` — under `templates/` (saved search chips).

### Compose
- `chats/new` posts through `ConversationController`; uses `compose.html`.

### Teams
- `TeamInviteController` — under `templates/team/`.

### Billing
- `BillingController` — `billing/*.html` (success, cancel, portal
  redirects).
- `BillingWebhookController` — no template; JSON in.

### Admin
- `AdminRevenueController` — `admin/revenue.html`; retention actions.

### Digest
- `DigestController` — `digest/opt-out.html`.

## Shared fragments and layout

- `templates/fragments/` — layout scaffolding, navigation, live-refresh
  wiring (`fragments/live-refresh.html` powers the in-place chat updates).

## Static assets

- `static/css/` — theme + IM bubble styles.
- `static/` — JS, images, favicons.

## Service-layer coupling

Web controllers own request/response translation and Thymeleaf model
building only. Business logic lives in service classes (e.g.
`ChatConversationService`, `MailboxImportService`,
`WinBackOutreachService`, `ReplyService`, `AdminRevenueService`). When a
mobile REST endpoint is later added for one of these actions, it MUST
call the same service — never re-implement the logic in the REST layer.
