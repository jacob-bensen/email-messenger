# Thymeleaf website

Templates in `src/main/resources/templates`; shared pieces in `fragments/`
(e.g. `live-refresh.html`). Routes (from controllers, 2026-10-21):

| Area | Routes | Controller |
|---|---|---|
| Marketing/legal | `/`, `/pricing`, `/demo`, `/privacy`, `/terms`, `/refund`, `/robots.txt`, `/sitemap.xml` | Pricing/Demo/Legal/SeoController |
| Auth | `/login`, `/register`, `/auth/google/start`, `/password/forgot`, `/password/reset`, `/verify-email`, `/verify-email/resend` | auth/* |
| Chats | `/chats`, `/chats/new`, `/chats/refresh`, `/chats/{key}`, `/chats/{key}/pane`, `/chats/{key}/send`, `/chats/events`; legacy `/threads/**`,`/senders` redirect | ConversationController, ChatEventsController, ChatRedirectController |
| Mailboxes | `/mailboxes`, `/new`, `/{id}/sync`, `/preferences`, `/{id}/signature`, `/{id}/delete`, `/mailboxes/gmail/{connect,reconnect/{id},callback}` | MailboxController, GmailConnectController |
| Searches | `/searches`, `/searches/{id}/delete` | SavedSearchController |
| Account | `/account`, `/account/{notifications,appearance,privacy,password,email}` | AccountController |
| Billing | `/billing/{checkout,portal,success,cancel,cancel-subscription,webhook}` | BillingController |
| Team | `/team/invite`, `/team/invite/accept` | TeamInviteController |
| Digest/Admin | `/digest/opt-out`, `/admin/revenue`, `/admin/retention/win-back` | DigestController, AdminRevenueController |
| PWA | `/manifest.webmanifest`, `/sw.js`, `/offline`, icons | PwaController |
