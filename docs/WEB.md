# WEB

Inventory of the Thymeleaf-rendered website + web app served by the backend.
Every page here lives at an unversioned path (the `/api/v1` namespace is
reserved for the mobile REST surface, see `docs/API.md`).

Business logic is in the service layer — controllers assemble view models and
call services. When the mobile REST API lands it **must** call the same
services; never fork logic between a Thymeleaf controller and a REST
controller.

## Layouts & fragments

- `templates/fragments/` — shared header, footer, chat rail, live-refresh SSE
  hook (`live-refresh.html`), etc.
- `NavModelAdvice` + `SiteModelAdvice` — global model attributes injected on
  every request.

## Public / marketing

| Route | Controller | Template | Purpose |
| --- | --- | --- | --- |
| `GET /` | `PricingController` | `landing.html` | Landing page. |
| `GET /pricing` | `PricingController` | `pricing.html` | Plan comparison + CTA. |
| `GET /demo` | `DemoController` | *inline* | Video demo shell. |
| `GET /privacy`, `/terms`, `/refund` | `LegalController` | `legal.html` | Legal pages rendered from `resources/legal/*.md`. |
| `GET /robots.txt`, `/sitemap.xml`, `/images/og-card.png` | `SeoController` | *inline* | SEO. |

## Auth

| Route | Controller | Purpose |
| --- | --- | --- |
| `GET /login` | `AuthController` | Login form (Google OIDC button also here). |
| `GET/POST /register` | `AuthController` | Registration. |
| `GET /verify-email`, `POST /verify-email/resend` | `EmailVerificationController` | Email verification handshake. |
| `GET/POST /password/forgot`, `GET/POST /password/reset` | `PasswordResetController` | Password reset. |
| `GET /auth/google/start` | `OAuthStartController` | Kicks off Google OIDC with intent tracking. |

## Account

| Route | Controller | Purpose |
| --- | --- | --- |
| `GET /account` | `AccountController` | Account overview + billing summary. |
| `GET /account/notifications` | `AccountController` | Digest + push opt-in. |
| `GET/POST /account/appearance` | `AccountController` | Theme + accent-color prefs. |
| `GET /account/privacy` | `AccountController` | Privacy controls. |
| `POST /account/password` | `AccountController` | Change password. |
| `POST /account/email` | `AccountController` | Change email (triggers re-verification). |

## Mailboxes

| Route | Controller | Purpose |
| --- | --- | --- |
| `GET /mailboxes` | `MailboxController` | Mailbox list. |
| `GET /mailboxes/new`, `POST /mailboxes` | `MailboxController` | Add IMAP-password mailbox. |
| `POST /mailboxes/{id}/sync` | `MailboxController` | Force a sync. |
| `POST /mailboxes/{id}/signature`, `POST /mailboxes/{id}/delete` | `MailboxController` | Per-mailbox actions. |
| `POST /mailboxes/preferences` | `MailboxController` | Import prefs. |
| `GET /mailboxes/gmail/connect`, `/reconnect/{id}`, `/callback` | `GmailConnectController` | Gmail OAuth mailbox flow. |

## Chat workspace

| Route | Controller | Purpose |
| --- | --- | --- |
| `GET /chats` | `ConversationController` | Chats rail + open pane. |
| `GET /chats/{key}` | `ConversationController` | One conversation (full page). |
| `GET /chats/{key}/pane` | `ConversationController` | Pane fragment (morph-in for live updates). |
| `POST /chats/{key}/send` | `ConversationController` | Send a reply. |
| `GET /chats/new`, `POST /chats/new` | `ConversationController` | Compose new. |
| `POST /chats/refresh` | `ConversationController` | Force poll. |
| `GET /chats/events` | `ChatEventsController` | SSE stream (`text/event-stream`); publishes `NewMailEvent`s so the open pane can morph in place. |
| `GET /threads`, `/threads/**`, `/senders` | `ChatRedirectController` | 301 legacy paths → `/chats`. |

## Saved searches

| Route | Controller | Purpose |
| --- | --- | --- |
| `POST /searches`, `POST /searches/{id}/delete` | `SavedSearchController` | Create / delete a saved search. |

## Teams

| Route | Controller | Purpose |
| --- | --- | --- |
| `GET/POST /team/invite` | `TeamInviteController` | Send invite. |
| `GET/POST /team/invite/accept` | `TeamInviteController` | Accept invite. |

## Billing

| Route | Controller | Purpose |
| --- | --- | --- |
| `POST /billing/checkout` | `BillingController` | Kick off Stripe Checkout. |
| `POST /billing/portal` | `BillingController` | Stripe Billing Portal redirect. |
| `GET /billing/success`, `/billing/cancel` | `BillingController` | Post-checkout landing pages. |
| `GET/POST /billing/cancel-subscription` | `BillingController` | In-app cancellation flow (with reason picker, EPIC-17). |
| `POST /billing/webhook` | `BillingWebhookController` | Stripe webhook (`@RestController`; the only non-HTML endpoint today). |

## Digest

| Route | Controller | Purpose |
| --- | --- | --- |
| `GET /digest/opt-out` | `DigestController` | Opt-out link included in every digest email. |

## Admin

| Route | Controller | Purpose |
| --- | --- | --- |
| `GET /admin/revenue` | `AdminRevenueController` | Ops dashboard (MRR, churn, at-risk retention, win-back). Gated by `ADMIN_EMAILS`. |
| `POST /admin/revenue/reconcile-billing-period` | `AdminRevenueController` | Manual reconcile action. |
| `POST /admin/retention/win-back` | `AdminRevenueController` | Per-row "Send win-back" CTA (EPIC-18 M1). |

## PWA

| Route | Controller | Purpose |
| --- | --- | --- |
| `GET /manifest.webmanifest` | `PwaController` | Web App Manifest. |
| `GET /icons/icon-192.png`, `/icons/icon-512.png`, `/icons/icon-512-maskable.png`, `/apple-touch-icon.png` | `PwaController` | PWA icons. |
| `GET /sw.js` | `PwaController` | Service worker (offline shell + push once wired). |
| `GET /offline` | `PwaController` | Offline fallback page. |

## Related docs

- `docs/BACKEND.md` — module map and stack.
- `docs/API.md` — REST contract for the mobile client (currently TBD).
