# WEB — Thymeleaf website

Templates: `src/main/resources/templates`; shared fragments in `templates/fragments`
(`app-nav`, `settings-nav`, `chat-pane`, `live-refresh`, `cookie-banner`, `install-banner`, `seo`, `sw-register`).

## Public
`/` and `/pricing` (PricingController, landing.html/pricing.html), `/demo` (DemoController → DemoConversationService),
`/login`, `/register`, `/auth/google/start`, `/password/forgot`, `/password/reset`, `/verify-email`,
`/privacy` `/terms` `/refund` (LegalController), `/robots.txt` `/sitemap.xml` (SeoController),
PWA: `/manifest.webmanifest`, `/sw.js`, `/offline`, `/icons/*` (PwaController), `/digest/opt-out`, `/billing/cancel`.

## Authenticated
| Route | Controller | Notes |
|---|---|---|
| `GET /chats` | ConversationController | list + empty pane; params `page,q,mailbox,filter` |
| `GET /chats/{key}`, `/chats/{key}/pane` | ConversationController | open chat (full page / fragment) |
| `GET/POST /chats/new` | ConversationController | compose |
| `POST /chats/{key}/send` | ConversationController | multipart send (see BACKEND.md drift note) |
| `POST /chats/refresh`, `GET /chats/events` | ConversationController, ChatEventsController | JSON / SSE |
| `/threads`, `/senders` | ChatRedirectController | legacy → `/chats` |
| `/mailboxes` (+`/new`, `/{id}/sync`, `/{id}/signature`, `/{id}/delete`, `/preferences`) | MailboxController | |
| `/mailboxes/gmail/{connect,reconnect/{id},callback}` | GmailConnectController | Gmail OAuth |
| `/searches`, `/searches/{id}/delete` | SavedSearchController | |
| `/account`, `/account/{notifications,appearance,privacy,password,email}` | AccountController | |
| `/billing/{checkout,portal,success,cancel-subscription}` | BillingController | `/billing/webhook` public, Stripe-signed |
| `/team/invite`, `/team/invite/accept` | TeamInviteController | |
| `/admin/revenue`, `/admin/revenue/reconcile-billing-period`, `/admin/retention/win-back` | AdminRevenueController | operator only (`AdminAuthorizer`) |
