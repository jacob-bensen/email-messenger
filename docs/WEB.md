# WEB.md — Thymeleaf website

Templates: `src/main/resources/templates`. Shared fragments: `fragments/{app-nav,settings-nav,chat-pane,live-refresh,seo,cookie-banner,install-banner,sw-register}.html`. Errors: `GlobalExceptionHandler` (plan-limit → upgrade modal) + `error.html`.

## Public (permitAll)
| Route | Controller | Template |
|---|---|---|
| `GET /` , `/pricing` | PricingController | landing, pricing |
| `GET /demo` | DemoController (`DemoConversationService`) | chat |
| `/login`, `GET/POST /register` | AuthController | login, register |
| `/password/forgot`, `/password/reset` (GET/POST) | PasswordResetController | password/* |
| `GET /verify-email` | EmailVerificationController | verify-email |
| `/privacy`, `/terms`, `/refund` | LegalController | legal |
| `GET /digest/opt-out` | DigestController | digest/opt-out |
| `/robots.txt`, `/sitemap.xml`, `/images/og-card.png` | SeoController | — |
| `/manifest.webmanifest`, `/sw.js`, `/offline`, icons | PwaController | — |
| `/auth/google/start` | OAuthStartController | redirect |
| `POST /billing/webhook`, `GET /billing/cancel` | Billing* | — |

## Authenticated
| Route | Controller | Services |
|---|---|---|
| `GET /chats`, `/chats/new`, `/chats/{key}`, `/chats/{key}/pane` | ConversationController | ChatService, ConversationListService, MailAccountService, MailboxPollingService |
| `POST /chats/new`, `/chats/{key}/send`, `/chats/refresh` | ConversationController | ReplyService, OutboundMessageService, ConversationKeyService |
| `GET /chats/events` (SSE) | ChatEventsController | ChatEventStream |
| `/threads`, `/threads/**`, `/senders` | ChatRedirectController | redirect to /chats |
| `GET /mailboxes`, `/mailboxes/new`; `POST /mailboxes`, `/{id}/sync`, `/{id}/signature`, `/{id}/delete`, `/preferences` | MailboxController | MailAccountService |
| `/mailboxes/gmail/connect`, `/reconnect/{id}`, `/callback` | GmailConnectController | GmailOAuthClient |
| `POST /searches`, `/searches/{id}/delete` | SavedSearchController | SavedSearchService |
| `/account`, `/account/{notifications,appearance,privacy}`, `POST /account/{password,email,appearance}` | AccountController | AccountService |
| `POST /billing/checkout`, `/portal`, `/cancel-subscription`; `GET /billing/success`, `/cancel-subscription` | BillingController | BillingService |
| `/team/invite`, `/team/invite/accept` | TeamInviteController | TeamInviteService |
| `/admin/revenue` + 2 POSTs | AdminRevenueController | admin metrics |
