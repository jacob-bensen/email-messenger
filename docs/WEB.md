# WEB — Thymeleaf site

Templates: `src/main/resources/templates`; shared fragments in `fragments/`
(`app-nav`, `chat-pane`, `settings-nav`, `live-refresh`, `seo`, `cookie-banner`,
`install-banner`, `sw-register`).

## Routes (controller → template/behaviour)
| Area | Routes | Controller |
|---|---|---|
| Public | `/`, `/pricing`, `/demo`, `/privacy`, `/terms`, `/refund`, `/robots.txt`, `/sitemap.xml` | `PricingController`, `DemoController`, `LegalController`, `SeoController` |
| Auth | `/login`, `/register`, `/auth/google/start`, `/verify-email[/resend]`, `/password/forgot`, `/password/reset` | `AuthController`, `OAuthStartController`, `EmailVerificationController`, `PasswordResetController` |
| Chats (`chat.html`, `compose.html`) | `GET /chats`, `/chats/new`, `/chats/{key}`, `/chats/{key}/pane`, `POST /chats/new`, `/chats/{key}/send`, `/chats/refresh`, `GET /chats/events` (SSE); legacy `/threads/**`,`/senders` redirect | `ConversationController`, `ChatEventsController`, `ChatRedirectController` |
| Mailboxes | `/mailboxes`, `/mailboxes/new`, `POST /mailboxes`, `/{id}/sync`, `/{id}/signature`, `/{id}/delete`, `/preferences`; Gmail `/mailboxes/gmail/{connect,reconnect/{id},callback}` | `MailboxController`, `GmailConnectController` |
| Account | `/account`, `/account/{notifications,appearance,privacy,password,email}` | `AccountController` |
| Billing | `/billing/{checkout,portal,success,cancel,cancel-subscription,webhook}` | `BillingController`, `BillingWebhookController` |
| Teams | `/team/invite`, `/team/invite/accept` | `TeamInviteController` |
| Saved searches | `POST /searches`, `/searches/{id}/delete` | `SavedSearchController` |
| Digest | `/digest/opt-out` | `DigestController` |
| Admin | `/admin/revenue`, `/admin/revenue/reconcile-billing-period`, `/admin/retention/win-back` | `AdminRevenueController` |
| PWA | `/manifest.webmanifest`, `/sw.js`, `/offline`, icons | `PwaController` |

Controllers delegate to services (`ConversationListService`, `ChatService`,
`OutboundMessageService`, `MailAccountService`, `BillingService`, …). Page-model
assembly (e.g. `ConversationController.populateList`) lives in controllers — extract
into a service before reusing it for a JSON API.
