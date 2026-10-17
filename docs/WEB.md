# Thymeleaf website

Templates in `src/main/resources/templates`; layouts via `fragments/`.

| Area | Routes | Controller | Template(s) |
|---|---|---|---|
| Marketing | `/`, `/pricing`, `/demo`, `/privacy`, `/terms`, `/refund` | `PricingController`, `DemoController`, `LegalController` | `landing`, `pricing`, `legal` |
| Auth | `/login`, `/register`, `/auth/google/start`, `/password/forgot|reset`, `/verify-email[/resend]` | `auth.*Controller` | `login`, `register`, `password/*`, `verify-email` |
| Chat workspace | `/chats`, `/chats/{key}`, `/chats/{key}/pane`, `/chats/{key}/send`, `/chats/new`, `/chats/refresh`, `/chats/events` | `ConversationController`, `ChatEventsController` | `chat`, `compose`, `fragments/chat-pane`, `fragments/live-refresh` |
| Mailboxes | `/mailboxes[/new|/{id}/sync|/preferences|/{id}/signature|/{id}/delete]`, `/mailboxes/gmail/connect|reconnect/{id}|callback` | `MailboxController`, `GmailConnectController` | `mailboxes/*` |
| Saved searches | `/searches`, `/searches/{id}/delete` | `SavedSearchController` | (in `chat`) |
| Account | `/account`, `/account/notifications|appearance|privacy|password|email` | `AccountController` | `account*` |
| Billing | `/billing/checkout|portal|success|cancel|cancel-subscription` | `BillingController` | `billing/*` |
| Team | `/team/invite[/accept]` | `TeamInviteController` | `team/*` |
| Admin | `/admin/revenue`, `/admin/retention/win-back` | `AdminRevenueController` | `admin/revenue` |
| Misc | `/digest/opt-out`, `/robots.txt`, `/sitemap.xml`, PWA icons, `/offline` | `DigestController`, `SeoController`, `PwaController` | |

Legacy `/threads/**`, `/senders` redirect to `/chats` (`ChatRedirectController`). Shared fragments: `app-nav`, `settings-nav`, `seo`, `cookie-banner`, `install-banner`, `sw-register`.
