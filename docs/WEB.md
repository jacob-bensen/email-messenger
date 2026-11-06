# Website (Thymeleaf) — page inventory

Templates in `src/main/resources/templates`; shared fragments in `fragments/`
(`app-nav`, `settings-nav`, `chat-pane`, `live-refresh`, `seo`, `cookie-banner`,
`install-banner`, `sw-register`).

| Route | Template | Controller | Notes |
|---|---|---|---|
| `GET /`, `/pricing` | landing, pricing | `PricingController` | public |
| `GET /demo` | chat (demo data) | `DemoController` | public, `DemoConversationService` |
| `/login`, `/register`, `/auth/google/start` | login, register | `AuthController`, `OAuthStartController` | public |
| `/password/forgot`, `/password/reset` | password/* | `PasswordResetController` | public |
| `/verify-email`, `POST /verify-email/resend` | verify-email | `EmailVerificationController` | |
| `GET /chats` (`page,q,mailbox,filter`) | chat | `ConversationController` | main workspace |
| `GET /chats/{key}`, `/chats/{key}/pane` | chat, fragments/chat-pane | `ConversationController` | marks read |
| `GET/POST /chats/new` | compose | `ConversationController` | |
| `POST /chats/{key}/send` | chat | `ConversationController` → `OutboundMessageService`/`ReplyService` | multipart attachments |
| `POST /chats/refresh`, `GET /chats/events` (SSE) | – | `ConversationController`, `ChatEventsController` | live updates |
| `/mailboxes`, `/new`, `/{id}/sync|signature|delete`, `/preferences` | mailboxes/* | `MailboxController` | |
| `/mailboxes/gmail/connect|reconnect/{id}|callback` | – | `GmailConnectController` | |
| `POST /searches`, `/searches/{id}/delete` | – | `SavedSearchController` | |
| `/account`, `/account/{notifications,appearance,privacy,password,email}` | account-* | `AccountController` | |
| `/team/invite`, `/team/invite/accept` | team/* | `TeamInviteController` | |
| `/billing/checkout|portal|success|cancel|cancel-subscription` | billing/* | `BillingController` | |
| `POST /billing/webhook` | – | `BillingWebhookController` | Stripe, no CSRF |
| `/digest/opt-out` | digest/opt-out | `DigestController` | public |
| `/privacy`, `/terms`, `/refund` | legal | `LegalController` | public |
| `/admin/revenue`, … | admin/revenue | `AdminRevenueController` | operator only |
| `/manifest.webmanifest`, `/sw.js`, `/offline`, icons | – | `PwaController` | PWA shell |
| `/robots.txt`, `/sitemap.xml`, `/images/og-card.png` | – | `SeoController` | |
| `/threads/**`, `/senders` | – | `ChatRedirectController` | legacy redirects to /chats |
