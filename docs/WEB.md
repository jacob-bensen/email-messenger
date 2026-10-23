# WEB (Thymeleaf)

Templates: `src/main/resources/templates`. Shared fragments: `fragments/{app-nav,settings-nav,chat-pane,live-refresh,seo,sw-register,install-banner,cookie-banner}.html`.

| Route | Auth | Template / notes |
|---|---|---|
| `/` | public | `landing` |
| `/pricing`, `/demo`, `/privacy`, `/terms`, `/refund` | public | pricing, demo, legal |
| `/login`, `/register`, `/password/forgot`, `/password/reset`, `/verify-email` | public | auth pages |
| `/chats`, `/chats/{key}`, `/chats/{key}/pane`, `/chats/new`, `POST /chats/{key}/send`, `POST /chats/refresh` | user | `chat`, `compose`, `fragments/chat-pane` |
| `/chats/events` | user | SSE new-mail signal |
| `/mailboxes/**`, `/mailboxes/gmail/**` | user | connect/sync/delete mailboxes, `POST /{id}/signature` |
| `/searches` | user | saved searches |
| `/account`, `/account/{appearance,notifications,privacy}` | user | settings |
| `/billing/{checkout,portal,success,cancel,cancel-subscription,webhook}` | mixed | Stripe |
| `/team/invite`, `/team/invite/accept` | user | teams |
| `/admin/revenue` | admin | metrics |
| `/manifest.webmanifest`, `/sw.js`, `/offline`, `/icons/**` | public | PWA |

Legacy `/threads/**`, `/senders` redirect to `/chats` (`ChatRedirectController`).
Services used by pages: `ChatService`, `ConversationListService`, `ReplyService`/`OutboundMessageService`, `MailAccountService`, `BillingService`, `SavedSearchService`.
