# Web (Thymeleaf) surface — email-messenger

The Thymeleaf website is the primary UI for ConexusMail today. All
pages share a common layout and are rendered server-side; the PWA
shell (`manifest.webmanifest`, `sw.js`) lets it install to the home
screen.

The website is authoritative for auth, billing, and mailbox connect
flows — the mobile client will not re-implement those; it will use the
shared REST API in `docs/API.md`.

## Page inventory

Grouped by controller. Services listed are the shared ones each
controller calls; NONE of these controllers put business rules in the
template.

### Public

- `/` `SeoController` → landing page (site-props, hero video).
- `/pricing` `PricingController` → plans (Free/Pro/Business).
- `/demo` `DemoController` → interactive demo of the chat UI.
- `/privacy`, `/terms`, `/refund` `LegalController` → legal pages
  loaded from `src/main/resources/legal/`.
- `/robots.txt`, `/sitemap.xml` `SeoController`.
- `/manifest.webmanifest`, `/sw.js`, `/offline`, `/icons/**`
  `PwaController`.

### Auth

- `/login`, `/register` `AuthController` → form login + register.
- `/auth/google/start` `OAuthStartController` → Google OIDC start.
- `/password/forgot`, `/password/reset` `PasswordResetController`.
- `/verify-email` `EmailVerificationController`.
- `/account`, `/account/notifications` `AccountController` (services:
  `UserService`, `DigestEmailPreferenceService`).

### Billing

- `/billing/**` `BillingController` (services: `BillingService`,
  `PlanCheckoutSuccessHandler`).
- `/billing/webhook` `BillingWebhookController` — Stripe webhook,
  CSRF-exempt, verifies signature via `STRIPE_WEBHOOK_SECRET`.

### Chats (the core workspace)

- `/chats` `ConversationController` → list of conversations (services:
  `ConversationListService`, `SavedSearchCountService`).
- `/chats/{conversationKey}` `ConversationController` → one
  conversation timeline + compose box (services: `ChatService`,
  `ReplyService`, `OutboundMessageService`, `IMTransformService`).
- `/chats/redirect/{threadId}` `ChatRedirectController` → resolve a
  legacy per-thread link into the current `conversationKey`.
- `/chats/events` `ChatEventsController` → SSE stream of `new-mail`
  events for live-refresh (backed by `ChatEventStream`).
- `/savedsearches/**` `SavedSearchController`.

### Mailbox connect

- `/mailboxes` `MailboxController` → list connected accounts.
- `/mailboxes/gmail/**` `GmailConnectController` → Gmail OAuth
  connect/reconnect.

### Digests

- `/digest/opt-out`, `/digest/**` `DigestController` (services:
  `DigestService`).

### Team

- `/team/**` `TeamInviteController` (services: `TeamService`,
  `TeamInviteService`).

### Admin

- `/admin/**` `AdminRevenueController` — internal metrics.

## Shared fragments / layouts

Under `src/main/resources/templates/`:

- `fragments/nav.html` — top nav; model attributes are contributed by
  `NavModelAdvice` and `SiteModelAdvice`.
- `fragments/live-refresh.html` — wires SSE events from
  `/chats/events` into in-place DOM morphs for the chat rail and open
  conversation pane.
- Layout: each page includes `fragments/head.html` and
  `fragments/footer.html` (site-wide styling in
  `src/main/resources/static/css/`).

## Style / static assets

- `src/main/resources/static/css/` — the IM-look chat bubbles, dark
  mode, dashboard.
- `src/main/resources/static/js/` — small vanilla-JS helpers; no SPA
  framework.
