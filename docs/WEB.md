# Website (Thymeleaf)

Templates: `src/main/resources/templates`. Controllers return view names; all state changes are form POSTs with CSRF.

| Route | Controller | Auth | Template |
|---|---|---|---|
| `/`, `/pricing` | PricingController | public | landing, pricing |
| `/demo` | DemoController | public | chat (demo data) |
| `/login`, `/register`, `/auth/google/start` | AuthController, OAuthStartController | public | login, register |
| `/password/forgot`, `/password/reset` | PasswordResetController | public | password/* |
| `/verify-email`, `/verify-email/resend` | EmailVerificationController | public / user | verify-email |
| `/privacy`, `/terms`, `/refund` | LegalController | public | legal |
| `/chats` (q, mailbox, filter, page) | ConversationController | user | chat |
| `/chats/{key}`, `/chats/{key}/pane` | ConversationController | user | chat / fragment |
| `/chats/new` GET/POST, `/chats/{key}/send` POST | ConversationController | user | compose, chat |
| `/chats/refresh` POST (JSON), `/chats/events` (SSE) | ConversationController, ChatEventsController | user | - |
| `/mailboxes`, `/mailboxes/new`, `/{id}/sync`, `/{id}/delete`, `/{id}/signature`, `/preferences` | MailboxController | user | mailboxes/* |
| `/mailboxes/gmail/connect`, `/reconnect/{id}`, `/callback` | GmailConnectController | user | redirect |
| `/searches`, `/searches/{id}/delete` | SavedSearchController | user | redirect |
| `/account`, `/account/{notifications,appearance,privacy,password,email}` | AccountController | user | account-* |
| `/billing/checkout`, `/portal`, `/success`, `/cancel`, `/cancel-subscription` | BillingController | user (`/cancel` public) | billing/* |
| `/team/invite`, `/team/invite/accept` | TeamInviteController | user | team/* |
| `/digest/opt-out` | DigestController | public | digest/* |
| `/admin/revenue`, `/admin/revenue/reconcile-billing-period`, `/admin/retention/win-back` | AdminRevenueController | admin | admin/* |
| PWA/SEO: `/manifest.webmanifest`, `/sw.js`, `/offline`, `/icons/**`, `/robots.txt`, `/sitemap.xml` | PwaController, SeoController | public | - |

Shared fragments: `templates/fragments` (incl. `live-refresh.html` for SSE-driven in-place updates).
