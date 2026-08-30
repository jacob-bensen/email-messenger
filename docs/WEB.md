# WEB — Thymeleaf pages

The public website and the authenticated web app are one Spring Boot
process. Templates live under `src/main/resources/templates/`, and are
rendered by controllers under `com.emailmessenger.web`, `.auth`,
`.billing`, `.team`, `.digest`, and `.admin`. All controllers call into
the services in `com.emailmessenger.service` (and package-owned services
like `BillingService`, `TeamInviteService`) — never re-implement business
logic in a controller or template.

## Page inventory (as of 2026-08-30)

Marketing / public (no login required):

- `/` — landing (`PricingController#index`, `landing.html`)
- `/pricing` — plan comparison (`PricingController#pricing`,
  `pricing.html`)
- `/demo` — sample IM view (`DemoController`, uses
  `DemoConversationService`; `templates/*` includes `chat.html`)
- `/privacy`, `/terms`, `/refund` — legal (`LegalController`,
  `templates/legal.html` + Markdown under `resources/legal/`)
- `/robots.txt`, `/sitemap.xml`, `/images/og-card.png` —
  `SeoController`

Auth:

- `/login`, `/register` — `AuthController`, `login.html`, `register.html`
- `/verify-email`, `/verify-email/resend` — `EmailVerificationController`,
  `verify-email.html`
- `/password/forgot`, `/password/reset` — `PasswordResetController`,
  `templates/password/`
- `/auth/google/start` — `OAuthStartController` bounces to
  `spring-security-oauth2-client`; callback is the Spring default
  `/login/oauth2/code/google`.

Account settings:

- `/account`, `/account/notifications`, `/account/appearance`,
  `/account/privacy`, `/account/password`, `/account/email` —
  `AccountController`, `account*.html`

Mailboxes:

- `/mailboxes`, `/mailboxes/new`, `POST /mailboxes`, `/mailboxes/{id}/sync`,
  `/mailboxes/preferences`, `/mailboxes/{id}/signature`,
  `/mailboxes/{id}/delete` — `MailboxController`,
  `templates/mailboxes/`
- `/mailboxes/gmail/connect`, `/mailboxes/gmail/reconnect/{id}`,
  `/mailboxes/gmail/callback` — `GmailConnectController`

Chats (the IM view):

- `/chats` — chat rail + open pane (`ConversationController#index`,
  `chat.html`)
- `/chats/new`, `POST /chats/new` — compose new conversation
- `POST /chats/refresh` — force poll and re-render
- `/chats/{key}`, `/chats/{key}/pane` — open one chat (full + fragment)
- `POST /chats/{key}/send` — inline reply (calls `ReplyService`)
- `/chats/events` — Server-Sent Events (`ChatEventStream`,
  `ChatEventsController`) that push new-mail refresh signals
- Legacy `/threads`, `/senders` — `ChatRedirectController` bounces to
  `/chats`
- Templates: `chat.html`, `conversation.html`, `compose.html`, and
  fragments under `templates/fragments/`.

Saved searches:

- `POST /searches`, `POST /searches/{id}/delete` —
  `SavedSearchController`, integrated into the chats rail via
  `SavedSearchCountService`.

Team:

- `/team/invite`, `POST /team/invite`, `/team/invite/accept`,
  `POST /team/invite/accept` — `TeamInviteController`,
  `templates/team/`

Digest:

- `/digest/opt-out?token=...` — one-click unsubscribe link
  (`DigestController`, `templates/digest/`).

Billing (Stripe):

- `POST /billing/checkout` — Stripe Checkout Session
- `POST /billing/portal` — Stripe Customer Portal
- `GET /billing/success`, `GET /billing/cancel`,
  `GET /billing/cancel-subscription`,
  `POST /billing/cancel-subscription` — flow pages / actions
- `POST /billing/webhook` — Stripe webhook, JSON in (this one is a
  `@RestController`; every other billing endpoint is a `@Controller`
  serving HTML)
- Templates: `templates/billing/`

Admin (allowlisted by `admin.emails` in `application.yml`):

- `/admin/revenue` — dashboard
- `POST /admin/revenue/reconcile-billing-period`
- `POST /admin/retention/win-back`
- Templates: `templates/admin/`

PWA / SEO / SW:

- `/manifest.webmanifest`, `/sw.js`, `/offline`, `/icons/icon-192.png`,
  `/icons/icon-512.png`, `/icons/icon-512-maskable.png`,
  `/apple-touch-icon.png` — `PwaController`

## Shared fragments and advice

- `SiteModelAdvice` (`@ControllerAdvice`) — puts the site-config
  properties on every model.
- `NavModelAdvice` (`@ControllerAdvice`) — puts the current user, plan
  gate, and onboarding checklist on every authenticated model.
- `GlobalExceptionHandler` (`@ControllerAdvice`) — maps unexpected
  exceptions to `error.html`; validation / auth errors bubble to the
  Spring-Security default paths.
- `templates/fragments/` holds shared header, footer, plan-gate banners,
  live-refresh snippets, and the chat-list rail.

## Website vs. mobile rule

Any behavior that both the website and the mobile client will surface
MUST live in `com.emailmessenger.service` (or the domain-owning
`service`-suffixed class), NOT in a controller or a template. When
a mobile REST endpoint is added, it and the equivalent Thymeleaf action
must both call into the same service method — no forking of business
logic between the web and API paths.
