# WEB.md — Thymeleaf website

Templates: `src/main/resources/templates/`; shared fragments in `fragments/`
(`app-nav`, `settings-nav`, `chat-pane`, `live-refresh`, `seo`, `cookie-banner`, `install-banner`, `sw-register`).

| Route | Controller | Template | Notes |
|---|---|---|---|
| `GET /`, `/pricing` | PricingController | landing, pricing | public |
| `GET /demo` | DemoController | chat (demo data) | public |
| `/login`, `/register` | AuthController | login, register | |
| `/password/forgot`, `/password/reset` | PasswordResetController | password/* | |
| `/verify-email`, `POST /verify-email/resend` | EmailVerificationController | verify-email | |
| `/auth/google/start` | OAuthStartController | — | redirect |
| `GET /chats` (`page,q,mailbox,filter`) | ConversationController | chat | list + open pane |
| `GET/POST /chats/new` | ConversationController | compose | multipart attachments |
| `POST /chats/refresh` | ConversationController | JSON `{imported}` | manual import |
| `GET /chats/{key}`, `/chats/{key}/pane` | ConversationController | chat, fragments/chat-pane | |
| `POST /chats/{key}/send` | ConversationController | redirect | `body, subject, asReply, attachments` |
| `GET /chats/events` | ChatEventsController | SSE | `NewMailEvent` |
| `/mailboxes` (+`/new`, `POST /`, `/{id}/sync`, `/preferences`, `/{id}/signature`, `/{id}/delete`) | MailboxController | mailboxes/* | |
| `/mailboxes/gmail/{connect,reconnect/{id},callback}` | GmailConnectController | — | OAuth |
| `POST /searches`, `/searches/{id}/delete` | SavedSearchController | redirect | |
| `/account`, `/account/{notifications,appearance,privacy}`, `POST /account/{password,email,appearance}` | AccountController | account-* | |
| `/billing/{checkout,portal,success,cancel,cancel-subscription}` | BillingController | billing/* | |
| `POST /billing/webhook` | BillingWebhookController | — | Stripe, CSRF-exempt |
| `/team/invite`, `/team/invite/accept` | TeamInviteController | team/* | |
| `/digest/opt-out` | DigestController | digest/opt-out | public |
| `/admin/revenue`, `POST /admin/revenue/reconcile-billing-period`, `/admin/retention/win-back` | AdminRevenueController | admin/revenue | admin only |
| `/privacy`,`/terms`,`/refund` | LegalController | legal | |
| `/robots.txt`,`/sitemap.xml`,`/images/og-card.png` | SeoController | — | |
| `/manifest.webmanifest`, icons, `/sw.js`, `/offline` | PwaController | — | PWA shell |
| `/threads/**`, `/senders` | ChatRedirectController | — | legacy redirects |
