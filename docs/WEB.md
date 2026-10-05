# WEB — Thymeleaf site (inventory as of 2026-10-05)

All routes below are `@Controller` (HTML) unless noted. Auth column: P = public,
A = authenticated session. All POSTs need a CSRF token (except webhook).

| Route | Auth | Template | Notes |
|---|---|---|---|
| `GET /`, `/pricing`, `/demo` | P | `landing`, `pricing`, `chat` (demo data) | `PricingController`, `DemoController` |
| `GET /login`, `GET/POST /register` | P | `login`, `register` | `AuthController`; `POST /login` handled by Spring Security |
| `GET /auth/google/start` | P | redirect | OAuth intent store |
| `GET/POST /password/forgot`, `/password/reset` | P | `password/*` | |
| `GET /verify-email`, `POST /verify-email/resend` | P / A | `verify-email` | |
| `GET /chats` | A | `chat` | rail + empty pane; params `page`, `q`, `mailbox`, `filter` |
| `GET /chats/{key}` | A | `chat` | opens conversation, marks read |
| `GET /chats/{key}/pane` | A | `fragments/chat-pane` | HTML fragment |
| `POST /chats/{key}/send` | A | redirect | multipart: `body`, `subject`, `asReply`, `attachments[]`, `mailbox` |
| `GET/POST /chats/new` | A | `compose` | `to`, `subject`, `body`, `attachments[]`, `mailbox` |
| `POST /chats/refresh` | A | **JSON** `{"imported":int}` | only JSON endpoint besides webhook-less actuator |
| `GET /chats/events` | A | SSE | `text/event-stream` |
| `GET /mailboxes`, `/mailboxes/new`; `POST /mailboxes`, `/{id}/sync`, `/preferences`, `/{id}/signature`, `/{id}/delete` | A | `mailboxes/*` | |
| `GET /mailboxes/gmail/connect`, `/reconnect/{id}`, `/callback` | A | redirect | Gmail OAuth |
| `POST /searches`, `/searches/{id}/delete` | A | redirect | saved searches |
| `GET /account`, `/account/notifications`, `/account/appearance`, `/account/privacy`; `POST /account/appearance`, `/account/password`, `/account/email` | A | `account*` | |
| `POST /billing/checkout`, `/billing/portal`; `GET /billing/success`, `/billing/cancel`; `GET/POST /billing/cancel-subscription` | A (`/billing/cancel` P) | `billing/*` | Stripe |
| `POST /billing/webhook` | P (signature) | JSON in | CSRF-exempt |
| `GET/POST /team/invite`, `/team/invite/accept` | A | `team/*` | |
| `GET /digest/opt-out` | P | `digest/opt-out` | |
| `GET /admin/revenue`; `POST /admin/revenue/reconcile-billing-period`, `/admin/retention/win-back` | A (admin allow-list) | `admin/revenue` | |
| `/privacy`, `/terms`, `/refund` | P | `legal` | |
| `/manifest.webmanifest`, `/sw.js`, `/offline`, `/icons/*`, `/robots.txt`, `/sitemap.xml` | P | generated | PWA + SEO |

Legacy `/threads/**`, `/senders` redirect to `/chats` (`ChatRedirectController`).

Shared fragments: `app-nav`, `settings-nav`, `chat-pane`, `live-refresh`,
`install-banner`, `cookie-banner`, `seo`, `sw-register`.
