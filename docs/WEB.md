# WEB.md — Thymeleaf website inventory

The Thymeleaf pages under `src/main/resources/templates/` are served
by controllers in `com.emailmessenger.web` (and a few sibling
packages: `auth`, `billing`, `admin`, `team`). Both the web pages
and the REST API share the same service layer — never duplicate
business logic between a controller and a JSON endpoint.

## Route map (as of 2026-08-31)

Reverse-engineered from the controllers. Auth requirement is inferred
from `SecurityConfig`'s permitAll list.

### Public (no auth)

- `GET /` — `web/LandingController` → `landing.html`. Marketing page.
- `GET /pricing` — `web/PricingController` → `pricing.html`.
- `GET /demo` — `web/DemoController` → conversation demo without
  requiring a login.
- `GET /login`, `POST /login` — form login. `login.html`.
- `GET /register`, `POST /register` — `auth/AuthController` →
  `register.html`.
- `GET /auth/google/start` — `auth/OAuthStartController` — begins
  Google OIDC.
- `GET /password/forgot`, `POST /password/forgot`,
  `GET /password/reset`, `POST /password/reset` —
  `auth/PasswordResetController` → templates under `password/`.
- `GET /verify-email` — `auth/EmailVerificationController` →
  `verify-email.html`.
- `GET /privacy`, `GET /terms`, `GET /refund` —
  `web/LegalController` → `legal.html`.
- `GET /billing/cancel` — Stripe cancel-URL landing page.
- `POST /billing/webhook` — Stripe webhook receiver (CSRF-exempt).
- `GET /digest/opt-out` — one-click unsubscribe from digest emails.
- `GET /manifest.webmanifest`, `GET /sw.js`, `GET /offline` — PWA
  shell (`web/PwaController`).
- `GET /robots.txt`, `GET /sitemap.xml`, `GET /favicon.ico` — SEO
  and static assets (`web/SeoController`).

### Authenticated

- `GET /chats` — `web/ChatRedirectController` → the workspace root.
- `GET /chats/{conversationKey}` — `web/ConversationController` →
  `chat.html`. The IM-style bubble view.
- `GET /chats/events` — `web/ChatEventsController` — SSE stream of
  new-mail events for the current user.
- `GET /compose`, `POST /compose` — `web/ConversationController` →
  `compose.html`.
- `GET /account`, `POST /account/*` — `auth/AccountController` →
  `account.html`, `account-appearance.html`,
  `account-notifications.html`, `account-privacy.html`.
- `GET /mailboxes`, `POST /mailboxes/**` —
  `web/MailboxController`, `web/GmailConnectController` — connect,
  reconnect, or remove a mailbox.
- `GET /billing/**` — `billing/*` — Stripe checkout, portal, plan
  changes.
- `GET /team/**` — `team/*` — team creation, invites.
- `GET /admin/**` — operator-only. Non-admins get 404
  (`admin.emails` allowlist in `application.yml`).

## Shared fragments

Under `templates/fragments/`:

- `layout.html` — master shell (head, header, footer).
- `live-refresh.html` — SSE wiring the `/chats` pane uses.
- `nav.html` — the top navigation, model-provided by
  `web/NavModelAdvice`.
- Various small fragments for chat bubbles, mailbox cards, etc.

## Services each page uses (selected)

- `chat.html` — `web/ChatService`, `service/ConversationService`,
  `web/ChatEventStream`. This is the workspace's read model and is
  also what the future `GET /api/v1/chats/{conversationKey}`
  endpoint will call.
- `mailboxes/*.html` — `web/MailboxController` +
  `email/MailboxService` (connect / reconnect flows).
- `account*.html` — `auth/AccountService` for user prefs;
  `billing/*` for the plan summary.
- Landing / pricing — `web/LandingProperties`, `web/PricingController`
  read static config; no per-user services.

## SEO / PWA / marketing

Handled entirely on the backend: `robots.txt`, `sitemap.xml`,
manifest, service worker (`sw.js`), and open-graph tags in the
layout. The mobile app does not replace or duplicate any of this —
the marketing surface stays a browser experience.
