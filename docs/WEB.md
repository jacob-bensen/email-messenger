# Website (Thymeleaf) inventory

Templates: `src/main/resources/templates`. Shared fragments: `fragments/` (`app-nav`, `settings-nav`, `chat-pane`, `live-refresh`, `seo`, `cookie-banner`, `install-banner`, `sw-register`).

| Route | Method | Auth | Controller → services | Template |
|---|---|---|---|---|
| `/`, `/pricing` | GET | public | PricingController | landing, pricing |
| `/demo` | GET | public | DemoController → DemoConversationService | chat |
| `/login`, `/register` | GET/POST | public | AuthController → UserService | login, register |
| `/auth/google/start` | GET | public | OAuthStartController | — |
| `/password/forgot`, `/password/reset` | GET/POST | public | PasswordResetController | password/* |
| `/verify-email`, `/verify-email/resend` | GET/POST | public / auth | EmailVerificationController | verify-email |
| `/privacy`, `/terms`, `/refund` | GET | public | LegalController | legal |
| `/robots.txt`, `/sitemap.xml`, `/images/og-card.png` | GET | public | SeoController | — |
| `/manifest.webmanifest`, `/sw.js`, `/offline`, `/icons/*` | GET | public | PwaController | — |
| `/chats` | GET | auth | ConversationController → ConversationListService | chat |
| `/chats/new` | GET/POST | auth | ConversationController → ReplyService | compose |
| `/chats/refresh` | POST | auth | → MailboxPollingService (returns JSON `{imported}`) | — |
| `/chats/{key}` | GET | auth | → chatService (marks read) | chat |
| `/chats/{key}/pane` | GET | auth | same, HTML fragment | fragments/chat-pane |
| `/chats/{key}/send` | POST (multipart) | auth | → ReplyService | chat |
| `/chats/events` | GET (SSE) | auth | ChatEventsController → ChatEventStream | — |
| `/mailboxes`, `/new`, `/{id}/sync`, `/preferences`, `/{id}/signature`, `/{id}/delete` | GET/POST | auth | MailboxController → MailAccountService | mailboxes/* |
| `/mailboxes/gmail/connect`, `/reconnect/{id}`, `/callback` | GET | auth | GmailConnectController | — |
| `/searches`, `/searches/{id}/delete` | POST | auth | SavedSearchController | — |
| `/account`, `/account/{notifications,appearance,privacy}`, `/account/password`, `/account/email` | GET/POST | auth | AccountController → AccountService | account-* |
| `/billing/checkout`, `/portal`, `/success`, `/cancel`, `/cancel-subscription` | GET/POST | auth (`/cancel` public) | BillingController → BillingService | billing/* |
| `/billing/webhook` | POST | public (Stripe signature) | BillingWebhookController | — |
| `/team/invite`, `/team/invite/accept` | GET/POST | auth | TeamInviteController | team/* |
| `/digest/opt-out` | GET | public | DigestController | digest/opt-out |
| `/admin/revenue`, `/admin/revenue/reconcile-billing-period`, `/admin/retention/win-back` | GET/POST | auth (admin) | AdminRevenueController | admin/revenue |
| `/threads`, `/threads/**`, `/senders` | GET | auth | ChatRedirectController (legacy → /chats) | — |
