# WEB — Thymeleaf website

Templates in `src/main/resources/templates`; shared fragments in `templates/fragments`
(`app-nav`, `settings-nav`, `chat-pane`, `live-refresh`, `seo`, `cookie-banner`, `install-banner`,
`sw-register`). CSS in `static/css`. PWA shell served by `PwaController` (`/manifest.webmanifest`, `/sw.js`, `/offline`, icons).

## Pages / routes
| Area | Routes | Controller |
|---|---|---|
| Marketing | `/`, `/pricing`, `/demo`, `/privacy`, `/terms`, `/refund`, `/robots.txt`, `/sitemap.xml` | `ChatRedirect`/landing, `PricingController`, `DemoController`, `LegalController`, `SeoController` |
| Auth | `/login`, `/register`, `/auth/google/start`, `/password/forgot|reset`, `/verify-email[/resend]` | `AuthController`, `OAuthStartController`, `PasswordResetController`, `EmailVerificationController` |
| Chats (core) | `/chats`, `/chats/new`, `/chats/{key}`, `/chats/{key}/pane` (fragment), `/chats/{key}/send`, `/chats/refresh`, `/chats/events` | `ConversationController`, `ChatEventsController`; services `ChatService`, `ConversationListService`, `OutboundMessageService` |
| Legacy | `/threads/**`, `/senders` → redirect to `/chats` | `ChatRedirectController` |
| Mailboxes | `/mailboxes/new`, `/{id}/sync|signature|delete`, `/preferences`, `/mailboxes/gmail/connect|reconnect/{id}|callback` | `MailboxController`, `GmailConnectController` |
| Account | `/account`, `/account/notifications|appearance|privacy|password|email` | `AccountController` |
| Billing | `/billing/checkout|portal|success|cancel|cancel-subscription|webhook` | `BillingController` |
| Teams / searches / digest | `/team/invite[/accept]`, `/searches/{id}/delete`, `/digest/opt-out` | `TeamInviteController`, `SavedSearchController`, `DigestController` |
| Admin | `/admin/revenue`, `/admin/revenue/reconcile-billing-period`, `/admin/retention/win-back` | `AdminRevenueController` (`AdminAuthorizer`) |

The chats workspace updates in place: `live-refresh.html` listens to the SSE stream and morphs the rail + open pane.
