# WEB.md — Thymeleaf website

Templates: `src/main/resources/templates` (fragments in `fragments/`, e.g. `live-refresh.html`).

| Route | Auth | Controller | Notes |
|---|---|---|---|
| `/`, `/pricing`, `/demo` | public | PricingController, DemoController | landing/pricing/demo conversation |
| `/privacy` `/terms` `/refund` | public | LegalController | |
| `/login`, `/register`, `/auth/google/start` | public | AuthController, OAuthStartController | |
| `/password/forgot`, `/password/reset` | public | PasswordResetController | |
| `/verify-email`, `/verify-email/resend` | public / auth | EmailVerificationController | |
| `/chats` | auth | ConversationController | list, params `page,q,mailbox,filter`; ConversationListService |
| `/chats/new` GET/POST | auth | ConversationController | compose, multipart attachments |
| `/chats/{key}` , `/chats/{key}/pane` | auth | ConversationController | ChatService.buildFor/markRead |
| `POST /chats/{key}/send` | auth | ConversationController | ReplyForm → OutboundMessageService/ReplyService |
| `POST /chats/refresh` | auth | ConversationController | JSON `{imported:n}` |
| `GET /chats/events` | auth | ChatEventsController | SSE `new-mail` |
| `/mailboxes/**`, `/mailboxes/gmail/**` | auth | MailboxController, GmailConnectController | connect/sync/delete/signature/preferences |
| `/searches`, `/searches/{id}/delete` | auth | SavedSearchController | |
| `/account/**` | auth | AccountController | |
| `/billing/**` | auth (webhook public) | BillingController, BillingWebhookController | |
| `/team/invite**` | auth | TeamInviteController | |
| `/digest/opt-out` | public | DigestController | |
| `/admin/**` | admin | AdminRevenueController | |
| `/threads/**`, `/senders` | auth | ChatRedirectController | legacy redirects to /chats |
| PWA/SEO | public | PwaController, SeoController | manifest, sw.js, icons, robots, sitemap |
