# WEB.md — Thymeleaf website

Templates in `src/main/resources/templates`; shared pieces in `templates/fragments`
(e.g. `live-refresh.html` for SSE-driven in-place updates). Controllers are package-private
`@Controller`s under `web/`, `auth/`, `billing/`, `team/`, `digest/`, `admin/`.

| Area | Routes (controller) |
|---|---|
| Public | `/`, `/pricing` (PricingController); `/demo`; `/privacy` `/terms` `/refund` (Legal); `/robots.txt` `/sitemap.xml` `/images/og-card.png` (Seo) |
| PWA | `/manifest.webmanifest`, `/icons/*`, `/apple-touch-icon.png`, `/sw.js`, `/offline` (PwaController) |
| Auth | `/login`, `/register`, `/auth/google/start`, `/verify-email`(+`/resend`), `/password/forgot`, `/password/reset`, `/logout` |
| Account | `/account`, `/account/notifications`, `/account/appearance`, `/account/privacy`, POST `/account/password`, `/account/email` |
| Chats | `/chats`, `/chats/new`, POST `/chats/refresh`, `/chats/{key}`, `/chats/{key}/pane`, POST `/chats/{key}/send`, SSE `/chats/events`; legacy `/threads/**`, `/senders` redirect to /chats |
| Mailboxes | `/mailboxes` (+`/new`, POST `/{id}/sync`, `/preferences`, `/{id}/signature`, `/{id}/delete`); Gmail: `/mailboxes/gmail/connect`, `/reconnect/{id}`, `/callback` |
| Saved searches | POST `/searches`, `/searches/{id}/delete` |
| Billing | POST `/billing/checkout`, `/billing/portal`; `/billing/success`, `/billing/cancel`; `/billing/cancel-subscription` (GET+POST); POST `/billing/webhook` (Stripe, JSON) |
| Teams | `/team/invite` (GET+POST), `/team/invite/accept` (GET+POST) |
| Digest | `/digest/opt-out` |
| Admin | `/admin/revenue`, POST `/admin/revenue/reconcile-billing-period`, `/admin/retention/win-back` |

Service reuse (controller → service) was not audited line-by-line this run: TBD in INTERNAL_TODO.md.
