# WEB.md — Thymeleaf website (2026-11-10)

| route | controller | template |
|---|---|---|
| `/`, `/pricing`, `/demo` | PricingController, DemoController | landing, pricing, chat demo |
| `/privacy` `/terms` `/refund` | LegalController | legal |
| `/login` `/register` `/auth/google/start` | AuthController, OAuthStartController | login, register |
| `/verify-email`, `/password/forgot|reset` | EmailVerificationController, PasswordResetController | verify-email, password/* |
| `/chats`, `/chats/new`, `/chats/{key}`, `/chats/{key}/pane`, `/chats/{key}/send`, `/chats/refresh`, `/chats/events` | ConversationController, ChatEventsController | chat, compose, fragments/chat-pane |
| `/mailboxes[/new|/{id}/sync|/{id}/delete|/{id}/signature|/preferences]`, `/mailboxes/gmail/*` | MailboxController, GmailConnectController | mailboxes/* |
| `/searches` | SavedSearchController | (fragment of chat) |
| `/account[/notifications|/appearance|/privacy|/password|/email]` | AccountController | account-* |
| `/billing/checkout|portal|success|cancel|cancel-subscription` | BillingController | billing/* |
| `/team/invite[/accept]` | TeamInviteController | team/* |
| `/digest/opt-out` | DigestController | digest/opt-out |
| `/admin/revenue` (+ POST actions) | AdminRevenueController | admin/revenue |
| `/threads/**`, `/senders` | ChatRedirectController | redirects to /chats |
Shared fragments: `fragments/{app-nav,chat-pane,cookie-banner,install-banner,live-refresh,seo,settings-nav,sw-register}`.
