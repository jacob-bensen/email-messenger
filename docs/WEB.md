# Website (Thymeleaf)

The public website is rendered by Spring MVC controllers under
`com.emailmessenger.web`, `.admin`, `.auth`, `.billing`, `.digest`, and
`.team`. Views live under `src/main/resources/templates/`.

## Ground rules

- Views never call domain code directly. Controllers pull data from a
  service (which the mobile API under `/api/*` shares); templates render
  the resulting view model.
- Shared layout, header, footer, and PWA scaffolding are Thymeleaf
  fragments under `templates/fragments/` — do NOT copy them per page.
- Business rules never diverge between web and API. If a check is
  correct on the website, the mobile API must inherit it by calling the
  same service.

## Page inventory (as of 2026-08-29)

Landing / marketing:
- `GET /` — `landing.html` (hero, feature grid, pricing teaser).
- `GET /pricing` — `pricing.html`.
- `GET /demo` — `DemoController` → `demo` view.
- `GET /privacy`, `/terms`, `/refund` — `LegalController` → `legal.html`
  (source under `src/main/resources/legal/`).

Auth:
- `GET/POST /login` — `AuthController` → `login.html`.
- `GET/POST /register` — `AuthController` → `register.html`.
- `GET /verify-email`, `POST /verify-email/resend` — `verify-email.html`.
- `GET/POST /password/forgot`, `/password/reset` — `PasswordResetController`.
- `GET /auth/google/start`, OAuth callback via Spring Security's default
  handler.

Account:
- `GET /account` — `account.html`.
- `GET /account/appearance` — `account-appearance.html`.
- `GET /account/notifications` — `account-notifications.html`.
- `GET /account/privacy` — `account-privacy.html`.

Mailboxes:
- `GET /mailboxes` — connected accounts.
- `GET /mailboxes/connect`, `POST /mailboxes` — IMAP-password add flow.
- `GET /mailboxes/gmail/start`, `/mailboxes/gmail/callback`,
  `/mailboxes/gmail/reconnect/{id}` — Gmail OAuth.

Chats (the /chats texting-style workspace):
- `GET /chats` — conversation list, keyed by participant-set
  `conversationKey`.
- `GET /chats/{key}` — merged timeline + compose box.
- `POST /chats/{key}/reply`, `POST /chats/{key}/attachments`.
- `POST /chats/refresh` — `@ResponseBody`, returns unread counts.
- `GET /chats/events` — Server-Sent Events for live in-place updates.

Compose:
- `GET /compose`, `POST /compose` — start a new conversation.

Billing:
- `GET /pricing`, `GET/POST /billing/*` — Stripe Checkout + portal.
- `POST /billing/webhook` — `@RestController`, Stripe signature-verified.

Admin (gated by `ADMIN_EMAILS`):
- `GET /admin/revenue` — churn card, retention queue, win-back CTA.
- `POST /admin/retention/win-back` — one-shot win-back send.

Digest:
- `GET /digest/opt-out` — one-click unsubscribe from operator digests.

PWA:
- `GET /manifest.webmanifest`, `/sw.js`, `/offline` — PWA shell.

## When adding a page

1. Add the controller under the package that owns the domain (`web`,
   `admin`, `billing`, `team`, `digest`, `auth`).
2. If the page needs a service method that doesn't exist, add it to the
   service — never inline the query in the controller.
3. If the mobile app will need the same data, add the JSON version to
   `com.emailmessenger.api` in the SAME commit, or add a follow-up to
   `INTERNAL_TODO.md`.
4. Update this file's page inventory.
