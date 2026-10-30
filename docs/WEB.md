# Thymeleaf website

Templates: `src/main/resources/templates` (`chat.html` workspace, `fragments/`
incl. `chat-pane` and `live-refresh`).

| Area | Routes | Controller |
|---|---|---|
| Public | `/`, `/pricing`, `/demo`, `/privacy`, `/terms`, `/refund`, `/robots.txt`, `/sitemap.xml`, `/images/og-card.png` | Pricing/Demo/Legal/SeoController |
| PWA | `/manifest.webmanifest`, `/sw.js`, `/offline`, `/icons/*`, `/apple-touch-icon.png` | PwaController |
| Auth | `/login`, `/register`, `/auth/google/start`, `/verify-email[/resend]`, `/password/forgot`, `/password/reset` | AuthController et al. |
| Account | `/account`, `/account/{notifications,appearance,privacy,password,email}` | AccountController |
| Chats | `/chats`, `/chats/new`, `/chats/{key}`, `/chats/{key}/pane`, `/chats/{key}/send`, `/chats/refresh`, `/chats/events` | ConversationController, ChatEventsController |
| Legacy | `/threads/**`, `/senders` → redirect to `/chats` | ChatRedirectController |
| Mailboxes | `/mailboxes[/new,/{id}/sync,/preferences,/{id}/signature,/{id}/delete]`, `/mailboxes/gmail/{connect,reconnect/{id},callback}` | MailboxController, GmailConnectController |
| Searches | `/searches`, `/searches/{id}/delete` | SavedSearchController |
| Billing | `/billing/{checkout,portal,success,cancel,cancel-subscription}` | BillingController |
| Teams | `/team/invite[/accept]` | TeamInviteController |
| Digest | `/digest/opt-out` | DigestController |
| Admin | `/admin/revenue`, `/admin/revenue/reconcile-billing-period`, `/admin/retention/win-back` | AdminRevenueController |
