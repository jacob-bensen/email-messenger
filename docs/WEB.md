# WEB.md — Thymeleaf website

Templates in `src/main/resources/templates`; shared pieces in `fragments/`.

| Route | Controller | Auth | Template |
|---|---|---|---|
| `/`, `/pricing`, `/demo`, `/privacy`, `/terms`, `/refund` | Landing/Pricing/Demo/Legal | public | landing, pricing, conversation, legal |
| `/login`, `/register`, `/password/forgot`, `/password/reset`, `/verify-email` | Auth*, PasswordReset, EmailVerification | public | login, register, password/*, verify-email |
| `/chats`, `/chats/new`, `/chats/{key}`, `/chats/{key}/pane`, `POST /chats/{key}/send`, `POST /chats/new`, `POST /chats/refresh` | ConversationController | user | chat, compose |
| `/chats/events` | ChatEventsController (SSE) | user | — |
| `/mailboxes/**`, `/mailboxes/gmail/**` | MailboxController, GmailConnectController | user | mailboxes/* |
| `/searches/**` | SavedSearchController | user | — |
| `/account`, `/account/{appearance,notifications,privacy}`, `POST /account/{email,password}` | AccountController | user | account-* |
| `/billing/*`, `POST /billing/webhook` | BillingController, BillingWebhookController | mixed | billing/* |
| `/team/invite*` | TeamInviteController | user | team/* |
| `/admin/revenue`, admin POSTs | AdminRevenueController | admin | admin/* |
| `/manifest.webmanifest`, `/sw.js`, `/offline`, icons, `/robots.txt`, `/sitemap.xml` | PwaController, SeoController | public | — |

Business logic lives in services (`ChatService`, `ReplyService`,
`MailAccountService`, `BillingService`, ...); controllers do form binding and view
model assembly. Duplication audit: see `INTERNAL_TODO.md`.
