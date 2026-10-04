# WEB — Thymeleaf website

Templates: `src/main/resources/templates` (+ `fragments/`, `admin/`, `billing/`,
`mailboxes/`, `password/`, `team/`, `digest/`).

| Route | Controller | Template | Notes |
|---|---|---|---|
| `GET /`, `/pricing` | PricingController | landing, pricing | public |
| `GET /demo` | DemoController | chat (demo data) | `DemoConversationService` |
| `GET /login`, `/register`, `POST /register` | AuthController | login, register | |
| `GET /auth/google/start` | OAuthStartController | — | OIDC |
| `GET/POST /password/forgot`, `/password/reset` | PasswordResetController | password/* | |
| `GET /verify-email`, `POST /verify-email/resend` | EmailVerificationController | verify-email | |
| `GET /chats`, `/chats/new`, `/chats/{key}`, `/chats/{key}/pane` | ConversationController | chat, fragments/chat-pane | list + open pane |
| `POST /chats/new`, `/chats/{key}/send`, `/chats/refresh` | ConversationController | — | send uses `ReplyService`; refresh returns `{"imported":n}` |
| `GET /chats/events` | ChatEventsController | SSE | `ChatEventStream` |
| `GET /mailboxes`, `/new`; `POST /mailboxes`, `/{id}/sync`, `/preferences`, `/{id}/signature`, `/{id}/delete` | MailboxController | mailboxes/* | |
| `GET /mailboxes/gmail/connect`, `/reconnect/{id}`, `/callback` | GmailConnectController | — | |
| `POST /searches`, `/searches/{id}/delete` | SavedSearchController | — | |
| `GET /account`, `/account/notifications`, `/account/appearance`, `/account/privacy`; `POST /account/appearance`, `/password`, `/email` | AccountController | account-* | |
| `POST /billing/checkout`, `/portal`, `/cancel-subscription`; `GET /billing/success`, `/cancel`, `/cancel-subscription`; `POST /billing/webhook` | Billing*Controller | billing/* | webhook is JSON |
| `GET/POST /team/invite`, `/team/invite/accept` | TeamInviteController | team/* | |
| `GET /admin/revenue`; `POST /admin/revenue/reconcile-billing-period`, `/admin/retention/win-back` | AdminRevenueController | admin/* | operator only |
| `/privacy`, `/terms`, `/refund`, `/robots.txt`, `/sitemap.xml`, PWA assets (`/manifest.webmanifest`, `/sw.js`, icons, `/offline`), `/digest/opt-out` | Legal/Seo/Pwa/Digest controllers | legal, … | |

Legacy `/threads/**`, `/senders` redirect to `/chats` (ChatRedirectController).
