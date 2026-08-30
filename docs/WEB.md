# WEB.md — ConexusMail website (Thymeleaf)

The backend serves the marketing site, auth flows, chats workspace,
account settings, admin console, billing, and the PWA shell from the same
Spring Boot process that will host the mobile-facing REST API.

**Shared-service invariant:** web controllers and (future) REST
controllers MUST call the same service layer. Never fork business logic
into a controller or a template.

## Page inventory

Templates live in `src/main/resources/templates/`.

### Marketing & pricing (`PricingController`, `DemoController`, `SeoController`, `LegalController`)

| Route | Template | Purpose |
| --- | --- | --- |
| `/` | `landing.html` | Public landing page. Uses `LandingProperties`/`LandingVideo`. |
| `/pricing` | `pricing.html` | Plan matrix + CTA. |
| `/demo` | `demo` template referenced by `DemoController` | Live demo of the chat UI (`DemoConversationService`). |
| `/privacy`, `/terms`, `/refund` | `legal.html` (parameterized) | Static legal pages, content in `src/main/resources/legal/`. |
| `/robots.txt`, `/sitemap.xml`, `/images/og-card.png` | rendered inline | SEO. |

### Auth (`AuthController`, `EmailVerificationController`, `PasswordResetController`, `OAuthStartController`)

| Route | Template |
| --- | --- |
| `/login` | `login.html` |
| `/register` | `register.html` |
| `/verify-email`, `/verify-email/resend` | `verify-email.html` |
| `/password/forgot`, `/password/reset` | `password/*.html` |
| `/auth/google/start` → OIDC | (redirect) |

### Account settings (`AccountController`)

| Route | Template |
| --- | --- |
| `/account` | `account.html` |
| `/account/notifications` | `account-notifications.html` |
| `/account/appearance` | `account-appearance.html` |
| `/account/privacy` | `account-privacy.html` |

Password / email change POSTs are on the same controller and re-render
the appropriate page with flash messages.

### Chats workspace (`ConversationController`, `ChatEventsController`, `ChatRedirectController`)

| Route | Template | Notes |
| --- | --- | --- |
| `/chats` | `chat.html` | Full-page workspace (rail + open pane). |
| `/chats/new` | `compose.html` | New chat composer. |
| `/chats/{key}` | `chat.html` | Same page with a specific conversation open. |
| `/chats/{key}/pane` | `fragments/chat-pane.html` | AJAX pane refresh. |
| `/chats/{key}/send` | (redirect) | Handled by `ReplyService`. |
| `/chats/refresh` | (302) | Kicks a poll then redirects. |
| `/chats/events` | text/event-stream | SSE for live updates. |
| `/threads`, `/threads/**`, `/senders` | 302 → `/chats` | Legacy URL keep-alives. |

`fragments/live-refresh.html` wires the SSE stream to in-place morphs of
the rail and the open pane so new mail lands without a reload.

### Mailboxes (`MailboxController`, `GmailConnectController`)

| Route | Template |
| --- | --- |
| `/mailboxes` | `mailboxes/index.html` |
| `/mailboxes/new` | `mailboxes/new.html` |

Form posts for add, sync, preferences, signature, delete return to the
list. Gmail flows are: `/mailboxes/gmail/connect` → Google → `/callback`
(or `/reconnect/{id}` for the weekly refresh dance while OAuth is still in
Testing).

### Team invites (`TeamInviteController`)

| Route | Template |
| --- | --- |
| `/team/invite`, `/team/invite/accept` | `team/*.html` |

### Billing (`BillingController`, `BillingWebhookController`)

| Route | Template | Notes |
| --- | --- | --- |
| `POST /billing/checkout`, `/billing/portal` | (redirect to Stripe) | |
| `/billing/success` | `billing/success.html` | Post-checkout landing. |
| `/billing/cancel` | (redirect) | |
| `/billing/cancel-subscription` | `billing/cancel-subscription.html` | Confirm before cancelling. |
| `POST /billing/webhook` | JSON | Stripe → server; the only `@RestController`. |

### Digest, PWA, Admin

- `/digest/opt-out` — `DigestController`, one-click unsubscribe.
- `/manifest.webmanifest`, `/sw.js`, `/offline`, PWA icons — `PwaController`.
- `/admin/revenue` and admin POSTs — `AdminRevenueController`, gated on
  the `admin.emails` allowlist.

## Shared fragments

`templates/fragments/` — `app-nav.html`, `chat-pane.html`,
`cookie-banner.html`, `install-banner.html`, `live-refresh.html`,
`seo.html`, `settings-nav.html`, `sw-register.html`.

## Model-attribute advice

`NavModelAdvice` injects nav state, `OnboardingChecklist` /
`OnboardingNudge` (via `OnboardingService`) add first-run guidance.
`BillingBannerService` surfaces trial / past-due warnings across pages.

## Static assets

`src/main/resources/static/` — CSS, JS, service-worker source, PWA icons.
