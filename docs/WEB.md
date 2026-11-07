# WEB.md — Thymeleaf site

Templates in `src/main/resources/templates`; fragments in `templates/fragments`
(`app-nav`, `settings-nav`, `chat-pane`, `live-refresh`, `seo`, `cookie-banner`, `install-banner`, `sw-register`).

| Area | Routes | Controller | Template(s) |
|---|---|---|---|
| Marketing | `/`, `/pricing`, `/demo`, `/privacy`, `/terms`, `/refund` | `PricingController`, `DemoController`, `LegalController` | `landing`, `pricing`, `legal` |
| Auth | `/login`, `/register`, `/auth/google/start`, `/password/forgot`, `/password/reset`, `/verify-email[/resend]` | `auth/*Controller` | `login`, `register`, `password/*`, `verify-email` |
| Chats (core) | `GET /chats`, `/chats/{key}`, `/chats/{key}/pane`, `/chats/new`; `POST /chats/new`, `/chats/{key}/send`, `/chats/refresh`; SSE `/chats/events` | `ConversationController`, `ChatEventsController` | `chat`, `compose`, `fragments/chat-pane` |
| Legacy | `/threads/**`, `/senders` | `ChatRedirectController` | redirects to `/chats` |
| Mailboxes | `/mailboxes` (list/new/sync/preferences/signature/delete), `/mailboxes/gmail/{connect,reconnect/{id},callback}` | `MailboxController`, `GmailConnectController` | `mailboxes/*` |
| Saved searches | `POST /searches`, `/searches/{id}/delete` | `SavedSearchController` | (inline in chat) |
| Account | `/account`, `/account/{notifications,appearance,privacy,password,email}` | `AccountController` | `account*` |
| Billing | `/billing/{checkout,portal,success,cancel,cancel-subscription,webhook}` | `BillingController`, `BillingWebhookController` | `billing/*` |
| Team | `/team/invite[/accept]` | `TeamInviteController` | `team/*` |
| Digest | `/digest/opt-out` | `DigestController` | `digest/*` |
| Admin | `/admin/revenue`, `/admin/revenue/reconcile-billing-period`, `/admin/retention/win-back` | `admin/*` | `admin/*` |
| PWA/SEO | `/manifest.webmanifest`, `/sw.js`, `/offline`, `/icons/**`, `/robots.txt`, `/sitemap.xml` | `PwaController`, `SeoController` | — |

Services used by chats: `ChatService`, `ConversationListService`, `ReplyService`, `OutboundMessageService`,
`MailAccountService`, `MailboxPollingService`, `OwnerAddressService`, `ConversationKeyService`.
Known gap: `ConversationController` holds send-flow logic itself (see INTERNAL_TODO #2).
