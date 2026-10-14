# Thymeleaf website

Templates in `src/main/resources/templates`. Routes (from controller annotations):

| Area | Routes | Controller |
|---|---|---|
| Marketing | `/`, `/pricing`, `/demo`, `/privacy`, `/terms`, `/refund` | web/* |
| Auth | `/login`, `/register`, `/auth/google/start`, `/password/forgot`, `/password/reset`, `/verify-email[/resend]` | auth/* |
| Chats | `/chats`, `/chats/new`, `/chats/refresh`, `/chats/{key}`, `/chats/{key}/pane`, `/chats/{key}/send`, `/chats/events` (SSE) | ConversationController, ChatEventsController |
| Legacy | `/threads`, `/threads/**`, `/senders` → redirect to chats | ChatRedirectController |
| Mailboxes | `/mailboxes`, `/new`, `/{id}/sync`, `/preferences`, `/{id}/signature`, `/{id}/delete`; `/mailboxes/gmail/{connect,reconnect/{id},callback}` | MailboxController, GmailConnectController |
| Account | `/account`, `/account/{notifications,appearance,privacy,password,email}` | AccountController |
| Searches | `/searches`, `/searches/{id}/delete` | SavedSearchController |
| Billing | `/billing/{checkout,portal,success,cancel,cancel-subscription,webhook}` | billing/* |
| Teams | `/team/invite`, `/team/invite/accept` | TeamInviteController |
| Digest | `/digest/opt-out` | DigestController |
| Admin | `/admin/revenue`, `/admin/revenue/reconcile-billing-period`, `/admin/retention/win-back` | AdminRevenueController |
| PWA/SEO | `/manifest.webmanifest`, `/sw.js`, `/offline`, `/icons/**`, `/robots.txt`, `/sitemap.xml` | PwaController, SeoController |

Shared fragments live in `templates/fragments` (e.g. `live-refresh.html`).
Per-page service usage: TBD (not audited in detail).
