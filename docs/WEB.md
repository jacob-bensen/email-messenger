# Website — Thymeleaf pages

The public and signed-in website served directly by this backend. Views
live in `src/main/resources/templates/`; each is rendered by a controller
under `com.emailmessenger.web`. Shared layout and fragments live in
`templates/fragments/`.

## Page inventory

| Route | Template | Controller | Auth |
|---|---|---|---|
| `/` | `landing.html` | `SeoController` / landing wiring | public |
| `/pricing` | `pricing.html` | `PricingController` | public |
| `/demo` | (chat demo shell) | `DemoController` + `DemoConversationService` | public |
| `/login`, `/register` | `login.html`, `register.html` | `AuthController` | public |
| `/password/forgot`, `/password/reset` | `password/…` | `PasswordResetController` | public |
| `/verify-email` | `verify-email.html` | `EmailVerificationController` | public |
| `/privacy`, `/terms`, `/refund` | `legal.html` | `LegalController` | public |
| `/chats`, `/chats/{key}` | `chat.html`, `conversation.html` | `ConversationController`, `ChatRedirectController`, `ChatService` | required |
| `/chats/events` | SSE stream | `ChatEventsController` + `ChatEventStream` | required |
| `/compose` | `compose.html` | `ConversationController` (reply/new mail) | required |
| `/mailboxes/**` | `mailboxes/*` | `MailboxController`, `GmailConnectController` | required |
| `/account`, `/account/*` | `account*.html` | `AccountController` | required |
| `/billing/**` (checkout, portal, cancel) | `billing/*` | `BillingController` | required |
| `/billing/webhook` | — | `BillingWebhookController` | public (Stripe-signed) |
| `/digest/opt-out`, `/digest/*` | `digest/*` | (see `digest/`) | mixed |
| `/team/**` | `team/*` | `TeamInviteController` | required |
| `/admin/**` | `admin/*` | `AdminRevenueController` etc. | admin only |

Static assets (`/css/**`, `/js/**`, `/images/**`, `/favicon.ico`,
`/manifest.webmanifest`, `/sw.js`, `/icons/**`, `/apple-touch-icon.png`,
`/offline`) and `/actuator/health` are public. See
`auth/SecurityConfig.java` for the authoritative permit list.

## Shared services

Web controllers do NOT own business logic. They compose services from
`service/`, `email/`, `billing/`, `auth/`, `team/`, `admin/`. Any new
mobile-facing REST endpoint added under `api/v1/` MUST call the same
service methods — never fork logic between web and API paths (see
`docs/BACKEND.md` "Website ↔ API discipline").

## PWA

`sw.js` and `manifest.webmanifest` under `static/` back the "Add to home
screen" installable web app. Live chat updates use SSE at `/chats/events`
and morph the DOM in place (see `fragments/live-refresh.html`); Web Push
is on the roadmap.
