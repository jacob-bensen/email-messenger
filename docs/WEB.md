# WEB (Thymeleaf site)

Templates: `src/main/resources/templates`. Fragments: `fragments/` (app-nav,
chat-pane, cookie-banner, install-banner, live-refresh, seo, settings-nav,
sw-register).

| Area | Routes (controller) | Templates |
|---|---|---|
| Public | `/`, `/pricing`, `/demo`, `/privacy`, `/terms`, `/refund` (Landing/Pricing/Demo/Legal controllers), `/robots.txt`, `/sitemap.xml` (SeoController) | landing, pricing, legal |
| Auth | `/login`, `/register`, `/auth/google/start`, `/password/forgot`, `/password/reset`, `/verify-email[/resend]` | login, register, password/*, verify-email |
| Chats workspace | `/chats`, `/chats/new`, `/chats/{key}`, `/chats/{key}/pane`, `/chats/{key}/send`, `/chats/refresh`, `/chats/events` (ConversationController, ChatEventsController); `/threads/**`, `/senders` redirect to chats | chat, compose, conversation, fragments/chat-pane |
| Mailboxes | `/mailboxes[/new|/{id}/sync|/{id}/signature|/{id}/delete|/preferences]`, `/mailboxes/gmail/{connect,callback,reconnect/{id}}` | mailboxes/* |
| Saved searches | `/searches/**` | (in chat) |
| Account | `/account`, `/account/{email,password,appearance,notifications,privacy}` | account-* |
| Billing | `/billing/{checkout,portal,success,cancel,cancel-subscription,webhook}` | billing/* |
| Team | `/team/invite`, `/team/invite/accept` | team/* |
| Digest | `/digest/opt-out` | digest/opt-out |
| Admin | `/admin/revenue`, `/admin/retention/win-back`, `/admin/revenue/reconcile-billing-period` | admin/revenue |
| PWA | `/manifest.webmanifest`, `/sw.js`, `/offline`, icons | |

Services used by the chats pages: `ChatService`, `ConversationListService`,
`OutboundMessageService`, `MailboxPollingService`, `MailAccountService`,
`BillingBannerService`, `OwnerAddressService`, `ConversationKeyService`.
