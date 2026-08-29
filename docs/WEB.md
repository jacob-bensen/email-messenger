# WEB

Thymeleaf website + authenticated web app. This is the surface humans
see today; the mobile client will hit `/api/v1/*` (see `docs/API.md`)
against the SAME service layer once those endpoints exist.

## Layouts & shared fragments

Fragments live in `templates/fragments/`:

- `app-nav.html` — top nav for authenticated pages.
- `settings-nav.html` — left rail on `/account/*`.
- `chat-pane.html` — the conversation pane, reused by both the
  full-page chat view and the SSE morph target.
- `live-refresh.html` — SSE client wiring pulled into `chat.html`.
- `cookie-banner.html`, `install-banner.html` — consent + PWA prompts.
- `seo.html` — meta tags and JSON-LD.
- `sw-register.html` — service-worker registration.

Global model attributes come from `NavModelAdvice` (auth-derived nav
state) and `SiteModelAdvice` (branding/site properties).

## Page inventory

Grouped by controller. Auth requirement in parentheses: (P) public,
(A) authenticated, (O) operator.

### Public / marketing

- `GET /` — landing (`landing.html`) via `SeoController` / `DemoController`
  supplying video + testimonial data.
- `GET /pricing` — plan comparison (`pricing.html`).
- `GET /legal/terms`, `/legal/privacy`, `/legal/refund` — legal pages
  (`LegalController`).
- `GET /demo` — animated conversation demo (`DemoController`).
- `GET /robots.txt`, `/sitemap.xml` — `SeoController`.

### Auth (P)

- `GET /login`, `POST /login` — form login (`AuthController` +
  Spring Security).
- `GET /register`, `POST /register` — signup.
- `GET /verify-email`, `POST /verify-email/resend` — email confirmation
  (`EmailVerificationController`).
- `GET /password/forgot`, `POST /password/forgot`,
  `GET /password/reset`, `POST /password/reset` — password reset flow
  (`PasswordResetController`).
- `GET /auth/google/start` → Google OIDC handshake (`OAuthStartController`).

### Chats (A)

- `GET /chats` — participant-set-keyed conversation list
  (`ConversationController`).
- `GET /chats/{key}` — full conversation view (`chat.html`).
- `GET /chats/{key}/pane` — the pane fragment for SSE morphing.
- `POST /chats/{key}/send` — send a reply (`OutboundMessageService`).
- `GET /chats/new` — compose a new chat (`compose.html`).
- `GET /chats/refresh` — HX-target refresh of the rail.
- `GET /chats/events` — Server-Sent Events stream of `NewMailEvent`
  (`ChatEventStream`).
- `GET /threads`, `/threads/**` — legacy thread view redirects to
  `/chats` via `ChatRedirectController`.

### Mailboxes (A)

- `GET /mailboxes` — connected mailbox list (`MailboxController`).
- `GET/POST /mailboxes/connect` — connect an IMAP-password mailbox.
- `GET /mailboxes/gmail` — start Gmail OAuth (`GmailConnectController`).
- `GET /mailboxes/gmail/callback` — Google OAuth return.
- `GET /mailboxes/gmail/reconnect/{id}` — weekly reconnect prompt.
- `POST /mailboxes/{id}/sync`, `POST /mailboxes/{id}/delete`,
  `POST /mailboxes/{id}/signature`.

### Account (A)

- `GET /account`, `GET /account/email`, `GET /account/appearance`,
  `GET /account/notifications`, `GET /account/privacy`,
  `GET /account/password` — settings pages (`AccountController`,
  `AppearanceSettings`).
- `GET /account/preferences` — merged prefs.

### Billing (A)

- `GET /billing/checkout` — starts Stripe Checkout
  (`BillingController` → `StripeCheckoutGateway`).
- `GET /billing/success`, `/billing/cancel` — Stripe return URLs.
- `GET /billing/portal` — redirects to Stripe Billing Portal.
- `GET/POST /billing/cancel-subscription` — reason picker before
  the Portal (`CancellationReason`).
- `POST /billing/webhook` — Stripe webhook (`BillingWebhookController`,
  the sole `@RestController` in the tree).

### Search + teams (A)

- `GET /searches` — saved searches (`SavedSearchController`).
- `GET /senders` — sender-scoped view (`SenderView`).
- `POST /team/invite`, `POST /team/invite/accept` — team invites
  (`team/*Controller`).

### Digest opt-out (P via signed token)

- `GET /digest/opt-out` — one-click unsubscribe (`DigestOptOutController`).

### Admin / operator (O)

- `GET /admin/revenue` — revenue dashboard
  (`admin/AdminRevenueController`). Cards: MRR, churn, at-risk
  retention queue, team-plan adoption, onboarding funnel, trial-end
  conversion, win-back conversion.
- `POST /admin/retention/win-back` — per-row "Send win-back" CTA.
- `POST /admin/revenue/reconcile-billing-period` — one-shot backfill.

### PWA / static

- `GET /manifest.webmanifest`, `/sw.js`, `/offline`,
  `/icons/*`, `/apple-touch-icon.png`, `/images/og-card.png` — served
  by `PwaController` + Spring static resource handling.

## Service dependencies (per page)

Every non-trivial page must go through a service — controllers never
touch repositories directly for mutations. This is the same contract
the future REST API must honor.

| Page                    | Primary services                                          |
|-------------------------|-----------------------------------------------------------|
| `/chats`                | `ConversationListService`, `ConversationService`          |
| `/chats/{key}`          | `ConversationService`, `ChatService`, `IMTransformService`|
| `/chats/{key}/send`     | `OutboundMessageService`, `ReplyService`, `PlanLimitService`|
| `/mailboxes/*`          | `MailAccountService`, `GmailOAuthClient`, `ImapClient`    |
| `/account/*`            | `AccountService`, `UserService`, `AppearanceSettings`     |
| `/billing/*`            | `BillingService`, `PlanLimitService`, `StripeCheckoutGateway`|
| `/admin/revenue`        | `RevenueMetricsService`, `ChurnMetricsService`,           |
|                         | `AtRiskRetentionService`, `TeamAdoptionMetricsService`,    |
|                         | `WinBackConversionMetricsService`, `OnboardingFunnelMetricsService`|
| `/digest/opt-out`       | (via `digest` package)                                     |

Rule: when the mobile REST equivalent lands, it must depend on the
SAME service class listed here. If a mobile-only helper is needed,
extract it into `service/` or `web/` and let both surfaces call it —
never fork logic into the API controller.
