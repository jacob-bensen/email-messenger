# WEB.md — Thymeleaf website

Templates: `src/main/resources/templates`. Fragments: `fragments/{app-nav,chat-pane,cookie-banner,install-banner,live-refresh,seo,settings-nav,sw-register}.html`.

| Route | Controller | Template | Auth |
|---|---|---|---|
| `/` | landing | `landing` | public |
| `/pricing`, `/demo`, `/privacy`, `/terms`, `/refund` | Pricing/Demo/LegalController | `pricing`, `chat`(demo), `legal` | public |
| `/login`, `/register`, `/verify-email`, `/password/forgot`, `/password/reset` | auth.* | `login`, `register`, `verify-email`, `password/*` | public |
| `/auth/google/start` | OAuthStartController | redirect | public |
| `/chats`, `/chats/{key}`, `/chats/{key}/pane`, `/chats/new`, `POST /chats/{key}/send`, `POST /chats/refresh`, `/chats/events` (SSE) | ConversationController, ChatEventsController | `chat`, `compose`, `fragments/chat-pane` | user |
| `/mailboxes`, `/mailboxes/new`, `POST /mailboxes`, `/{id}/sync`, `/{id}/delete`, `/{id}/signature`, `/preferences`; `/mailboxes/gmail/connect`, `/callback`, `/reconnect/{id}` | MailboxController, GmailConnectController | `mailboxes/*` | user |
| `/searches` | SavedSearchController | (redirects/fragments) | user |
| `/account`, `/account/{appearance,notifications,privacy}`, `POST /account/{email,password,appearance}` | AccountController | `account*` | user |
| `/billing/checkout`, `/portal`, `/success`, `/cancel`, `/cancel-subscription`, `POST /billing/webhook` | Billing*Controller | `billing/*` | user (webhook: Stripe signature) |
| `/team/invite`, `/team/invite/accept` | TeamInviteController | `team/*` | user |
| `/digest/opt-out` | DigestController | — | public |
| `/admin/revenue`, `POST /admin/...` | AdminRevenueController | `admin/revenue` | admin |
| PWA/SEO: `/manifest.webmanifest`, `/sw.js`, `/offline`, `/icons/**`, `/robots.txt`, `/sitemap.xml` | PwaController, SeoController | — | public |

Controllers delegate to services (`ChatService`, `OutboundMessageService`, `MailAccountService`,
`BillingService`, `UserService`, ...). A future REST API must reuse these services, not controllers.
