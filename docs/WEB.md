# WEB (Thymeleaf site)

Templates: `src/main/resources/templates`; shared pieces in `fragments/` (e.g. `live-refresh.html`).

| Area | Routes | Controller | Template |
|---|---|---|---|
| Marketing | `/`, `/pricing`, `/demo`, `/privacy`, `/terms`, `/refund` | PricingController, DemoController, LegalController | landing, pricing, legal |
| Auth | `/login`, `/register`, `/auth/google/start`, `/password/forgot|reset`, `/verify-email[/resend]` | auth.* | login, register, password/*, verify-email |
| Chats | `/chats`, `/chats/new`, `/chats/{key}`, `/chats/{key}/pane`, `/chats/{key}/send`, `/chats/refresh`, `/chats/events` | ConversationController, ChatEventsController | chat, compose |
| Mailboxes | `/mailboxes[/new|/{id}/sync|/{id}/signature|/{id}/delete|/preferences]`, `/mailboxes/gmail/{connect|reconnect/{id}|callback}` | MailboxController, GmailConnectController | mailboxes/* |
| Searches | `/searches`, `/searches/{id}/delete` | SavedSearchController | (in chat) |
| Account | `/account`, `/account/{notifications|appearance|privacy|password|email}` | AccountController | account-* |
| Billing | `/billing/{checkout|portal|success|cancel|cancel-subscription|webhook}` | billing.* | billing/* |
| Teams | `/team/invite[/accept]` | TeamInviteController | team/* |
| Admin | `/admin/revenue`, `/admin/retention/win-back` | AdminRevenueController | admin/* |
| PWA/SEO | `/manifest.webmanifest`, `/sw.js`, `/offline`, `/icons/*`, `/robots.txt`, `/sitemap.xml` | PwaController, SeoController | — |
| Legacy | `/threads/**`, `/senders` → redirect to `/chats` | ChatRedirectController | — |
