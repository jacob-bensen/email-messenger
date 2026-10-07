# Thymeleaf website

Templates: `src/main/resources/templates`. Fragments: `app-nav`, `chat-pane`,
`cookie-banner`, `install-banner`, `live-refresh`, `seo`, `settings-nav`, `sw-register`.

| Area | Routes | Template(s) | Main services |
|---|---|---|---|
| Marketing | `/`, `/pricing`, `/demo`, `/privacy`, `/terms`, `/refund` | landing, pricing, conversation, legal | `PricingController`, `DemoConversationService` |
| Auth | `/login`, `/register`, `/auth/google/start`, `/verify-email[/resend]`, `/password/forgot`, `/password/reset` | login, register, verify-email, password/* | `UserService`, `EmailVerificationService`, `PasswordResetService` |
| Chats workspace | `GET /chats`, `/chats/new`, `/chats/{key}`, `/chats/{key}/pane`; `POST /chats/new`, `/chats/refresh`, `/chats/{key}/send`; SSE `/chats/events` | chat, compose, fragments/chat-pane | `ConversationListService`, `ChatService`, `OutboundMessageService`, `MailboxPollingService` |
| Mailboxes | `/mailboxes` (list/new/create), `/{id}/sync`, `/preferences`, `/{id}/signature`, `/{id}/delete`, `/mailboxes/gmail/{connect,reconnect/{id},callback}` | mailboxes/* | `MailAccountService`, `GmailOAuthClient` |
| Saved searches | `POST /searches`, `/searches/{id}/delete` | (in chat) | `SavedSearchService` |
| Account | `/account`, `/account/{notifications,appearance,privacy,password,email}` | account*.html | `AccountService` |
| Billing | `POST /billing/{checkout,portal,cancel-subscription}`, `/billing/{success,cancel,cancel-subscription}` | billing/* | `BillingService` |
| Teams | `/team/invite`, `/team/invite/accept` | team/* | `TeamInviteService` |
| Admin | `/admin/revenue`, `POST /admin/revenue/reconcile-billing-period`, `/admin/retention/win-back` | admin/revenue | `AdminAuthorizer`, `*MetricsService` |
| Misc | `/digest/opt-out`, legacy `/threads/**`,`/senders` redirect to `/chats`, PWA (`/manifest.webmanifest`, `/sw.js`, `/offline`, icons), `/robots.txt`, `/sitemap.xml` | — | — |

Controllers are thin and delegate to services; no divergent REST duplicate
exists because no REST API exists (see `API.md`).
