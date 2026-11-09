# WEB.md — Thymeleaf website (2026-11-09)

Templates: `src/main/resources/templates` (+ `fragments/`, `admin/`, `billing/`, `mailboxes/`, `password/`, `team/`, `digest/`).
All authenticated routes use the session; services named are the ones the controller delegates to.

| Route(s) | Controller | Template | Services |
|---|---|---|---|
| `GET /`, `/pricing`, `/demo` (public) | PricingController, DemoController | landing, pricing | DemoConversationService |
| `/privacy /terms /refund` (public) | LegalController | legal | — |
| `/login`, `/register`, `/auth/google/start` | AuthController, OAuthStartController | login, register | UserService, OAuth2ProvisioningService |
| `/password/forgot`, `/password/reset` | PasswordResetController | password/* | PasswordResetService |
| `/verify-email`, `POST /verify-email/resend` | EmailVerificationController | verify-email | EmailVerificationService |
| `GET /chats`, `/chats/{key}`, `/chats/{key}/pane`, `/chats/new` | ConversationController | chat, compose, fragments/chat-pane | ConversationListService, ChatService, MailAccountService |
| `POST /chats/new`, `/chats/{key}/send`, `/chats/refresh` | ConversationController | — | OutboundMessageService, ReplyService, MailboxPollingService |
| `GET /chats/events` (SSE) | ChatEventsController | — | ChatEventStream |
| `/threads`, `/threads/**`, `/senders` | ChatRedirectController | redirects to /chats | — |
| `/mailboxes`, `/new`, `{id}/sync`, `preferences`, `{id}/signature`, `{id}/delete` | MailboxController | mailboxes/* | MailAccountService, MailboxPollingService |
| `/mailboxes/gmail/connect|reconnect/{id}|callback` | GmailConnectController | — | GmailOAuthClient |
| `POST /searches`, `/searches/{id}/delete` | SavedSearchController | — | SavedSearchService |
| `/account`, `/account/{notifications,appearance,privacy}`, `POST /account/{password,email}` | AccountController | account-* | AccountService |
| `/billing/checkout|portal|success|cancel|cancel-subscription`, `POST /billing/webhook` | BillingController, BillingWebhookController | billing/* | BillingService |
| `/team/invite`, `/team/invite/accept` | TeamInviteController | team/* | TeamInviteService |
| `/digest/opt-out` | DigestController | digest/* | — |
| `/admin/revenue`, `/admin/retention/win-back`, ... | AdminRevenueController | admin/* | — |
| `/robots.txt /sitemap.xml /images/og-card.png`, PWA (`/manifest.webmanifest`, `/sw.js`, `/offline`, icons) | SeoController, PwaController | — | — |

Shared fragments: `fragments/` (live-refresh, chat-pane, nav). Model advice: `NavModelAdvice`, `SiteModelAdvice`.
