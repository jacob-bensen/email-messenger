# WEB — Thymeleaf website (as implemented 2026-11-05)

All routes below require auth unless marked (public). CSRF token required on every POST.

| Area | Routes | Controller |
|---|---|---|
| Marketing (public) | `/`, `/pricing`, `/demo`, `/privacy`, `/terms`, `/refund`, `/robots.txt`, `/sitemap.xml`, `/images/og-card.png` | PricingController, DemoController, LegalController, SeoController |
| PWA (public) | `/manifest.webmanifest`, `/sw.js`, `/offline`, `/icons/*`, `/apple-touch-icon.png` | PwaController |
| Auth (public) | `/login`, `/register`, `/auth/google/start`, `/password/forgot`, `/password/reset`, `/verify-email`, `POST /verify-email/resend` | AuthController, OAuthStartController, PasswordResetController, EmailVerificationController |
| Chats workspace | `GET /chats` (params `page`,`q`,`mailbox`,`filter`), `/chats/new` (GET/POST), `POST /chats/refresh`, `GET /chats/{key}`, `GET /chats/{key}/pane` (HTML fragment), `POST /chats/{key}/send` (multipart), `GET /chats/events` (SSE); `/threads/**`,`/senders` redirect to /chats | ConversationController, ChatEventsController, ChatRedirectController |
| Mailboxes | `/mailboxes` (+`/new`, `POST /`, `/{id}/sync`, `/preferences`, `/{id}/signature`, `/{id}/delete`), `/mailboxes/gmail/{connect,reconnect/{id},callback}` | MailboxController, GmailConnectController |
| Saved searches | `POST /searches`, `POST /searches/{id}/delete` | SavedSearchController |
| Account | `/account`, `/account/notifications`, `/account/appearance`, `/account/privacy`, `POST /account/{password,email}` | AccountController |
| Billing | `POST /billing/{checkout,portal,cancel-subscription}`, `/billing/{success,cancel,cancel-subscription}`, `POST /billing/webhook` (public, Stripe signature) | BillingController, BillingWebhookController |
| Teams | `/team/invite` (GET/POST), `/team/invite/accept` (GET/POST) | TeamInviteController |
| Digest | `GET /digest/opt-out` (public) | DigestController |
| Admin | `/admin/revenue`, `POST /admin/revenue/reconcile-billing-period`, `POST /admin/retention/win-back` | AdminRevenueController |

Templates: `src/main/resources/templates/*.html`; shared fragments in `templates/fragments/`
(app-nav, chat-pane, live-refresh, settings-nav, seo, cookie/install banners, sw-register).
Conversation identity: `{key}` = participant-set `conversationKey` (V32).
