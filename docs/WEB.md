# Thymeleaf website

Templates: `src/main/resources/templates` (account-*.html, chat.html, compose.html, conversation.html, landing.html,
pricing.html, legal.html, login/register/verify-email, plus dirs admin/, billing/, digest/, mailboxes/, password/, team/,
fragments/ — e.g. `fragments/live-refresh.html`).

| Route | Controller | Notes |
|---|---|---|
| `/`, `/pricing`, `/demo` | PricingController, DemoController | public |
| `/privacy` `/terms` `/refund` | LegalController | public |
| `/login` `/register` `/auth/google/start` | auth.AuthController, OAuthStartController | |
| `/password/forgot|reset`, `/verify-email[/resend]` | PasswordResetController, EmailVerificationController | |
| `/account`, `/account/{notifications,appearance,privacy,password,email}` | AccountController | |
| `/chats`, `/chats/new`, `/chats/{key}`, `/chats/{key}/pane`, `/chats/{key}/send` | ConversationController | main workspace |
| `/chats/refresh` (JSON), `/chats/events` (SSE) | ConversationController, ChatEventsController | |
| `/mailboxes[...]`, `/mailboxes/gmail/{connect,reconnect/{id},callback}` | MailboxController, GmailConnectController | |
| `/searches` | SavedSearchController | |
| `/team/invite[/accept]` | TeamInviteController | |
| `/billing/{checkout,portal,success,cancel,cancel-subscription,webhook}` | BillingController, BillingWebhookController | |
| `/admin/revenue...` | AdminRevenueController | admin only |
| `/manifest.webmanifest`, `/sw.js`, `/offline`, icons, `/robots.txt`, `/sitemap.xml` | PwaController, SeoController | |
| `/threads/**`, `/senders` | ChatRedirectController | legacy redirects to /chats |

Controllers delegate to services (`ChatService`, `ConversationService`, `ReplyService`, `OutboundMessageService`, ...).
