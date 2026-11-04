# Thymeleaf website

Templates: `src/main/resources/templates` (+ `fragments/`, `admin/`, `billing/`,
`digest/`, `mailboxes/`, `password/`, `team/`). Static assets under `static/`.

## Route inventory (derived from `@*Mapping`; auth = session required unless "public")

| Route | Auth | Notes |
|---|---|---|
| `GET /`, `/pricing`, `/demo`, `/privacy`, `/terms`, `/refund`, `/robots.txt`, `/sitemap.xml` | public | landing, legal, SEO |
| `GET/POST /login`, `/register`, `/password/forgot`, `/password/reset`, `GET /verify-email`, `POST /verify-email/resend`, `GET /auth/google/start`, `POST /logout` | public (resend: auth) | auth flows |
| `GET /chats` (`page`,`q`,`mailbox`,`filter`), `GET /chats/new`, `POST /chats/new`, `GET /chats/{key}`, `GET /chats/{key}/pane`, `POST /chats/{key}/send`, `POST /chats/refresh` (JSON), `GET /chats/events` (SSE) | auth | main workspace (`chat.html`, `compose.html`) |
| `GET/POST /mailboxes`, `/mailboxes/new`, `/{id}/sync`, `/{id}/signature`, `/{id}/delete`, `/mailboxes/preferences`, `/mailboxes/gmail/*` (`/connect`, `/callback`, `/reconnect/{id}`) | auth | mailbox management |
| `/searches/**` | auth | saved searches |
| `GET /account`, `/account/appearance`, `/account/notifications`, `/account/privacy`; `POST /account/{appearance,email,password}` | auth | settings |
| `POST /billing/checkout`, `/billing/portal`, `/billing/cancel-subscription`, `GET /billing/success`, `/billing/cancel`, `POST /billing/webhook` | mixed (webhook public, CSRF-exempt, Stripe-signed) | Stripe |
| `GET/POST /team/invite`, `/team/invite/accept` | auth | teams |
| `GET /digest/opt-out` | public | digest unsubscribe |
| `GET /admin/revenue`, `POST /admin/retention/win-back`, `POST /admin/revenue/reconcile-billing-period` | admin | `AdminAuthorizer` |
| `/manifest.webmanifest`, `/sw.js`, `/offline`, icons | public | PWA shell |
| `/threads`, `/threads/**`, `/senders` | auth | legacy redirects to `/chats` (`ChatRedirectController`) |

Shared layout/fragments: `templates/fragments/` (e.g. `live-refresh.html`).
Service use per page: see `docs/BACKEND.md`; exact per-template model contents TBD.
