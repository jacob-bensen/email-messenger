# WEB.md — Thymeleaf site

Templates in `src/main/resources/templates`; shared fragments in `templates/fragments`
(`chat-pane`, `live-refresh`, …). Model attributes shared by many pages come from
`NavModelAdvice` / `SiteModelAdvice`.

| Page | Route(s) | Controller | Services |
|---|---|---|---|
| Landing / pricing / demo | `/`, `/pricing`, `/demo` | PricingController, DemoController | DemoConversationService |
| Legal | `/privacy` `/terms` `/refund` | LegalController | — |
| Chats workspace | `/chats`, `/chats/{key}`, `/chats/{key}/pane`, `/chats/new` | ConversationController | ConversationListService, ChatService, MailAccountService, BillingBannerService |
| Send | `POST /chats/{key}/send`, `POST /chats/new` | ConversationController | OutboundMessageService / ReplyService |
| Live updates | `GET /chats/events` (SSE), `POST /chats/refresh` | ChatEventsController, ConversationController | ChatEventStream, MailboxPollingService |
| Mailboxes | `/mailboxes`, `/mailboxes/new`, `/{id}/sync|signature|delete`, `/preferences`, `/mailboxes/gmail/*` | MailboxController, GmailConnectController | MailAccountService |
| Saved searches | `POST /searches`, `/searches/{id}/delete` | SavedSearchController | SavedSearchService |
| Auth | `/login`, `/register`, `/auth/google/start`, `/password/*`, `/verify-email*` | auth.* controllers | UserService, PasswordResetService, EmailVerificationService |
| Account | `/account`, `/account/{notifications,appearance,privacy,password,email}` | AccountController | AccountService |
| Billing | `/billing/{checkout,portal,success,cancel,cancel-subscription}` | BillingController | BillingService |
| Teams | `/team/invite`, `/team/invite/accept` | TeamInviteController | TeamInviteService |
| Admin | `/admin/revenue`, `/admin/retention/win-back` | AdminRevenueController | admin.* metrics services |
| Misc | `/digest/opt-out`, `/robots.txt`, `/sitemap.xml`, PWA (`/manifest.webmanifest`, `/sw.js`, `/offline`, icons) | DigestController, SeoController, PwaController | — |
| Legacy | `/threads/**`, `/senders` → redirect to `/chats` | ChatRedirectController | — |
