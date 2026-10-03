# WEB — Thymeleaf site inventory

Templates in `src/main/resources/templates`; shared pieces in `fragments/` (e.g. `chat-pane`,
`live-refresh`). Controllers are package-private in `web/`, `auth/`, `billing/`, `team/`, `digest/`, `admin/`.

| Route | Template / result | Controller → services |
|---|---|---|
| `/`, `/pricing` | `landing`, `pricing` | `PricingController` |
| `/demo` | demo chat | `DemoController` → `DemoConversationService` |
| `/login`, `/register`, `/verify-email` (+`/resend`) | auth pages | `AuthController`, `EmailVerificationController` → `UserService`, `EmailVerificationService` |
| `/password/forgot`, `/password/reset` | password reset | `PasswordResetController` |
| `/auth/google/start` | OIDC kickoff | `OAuthStartController` |
| `/chats` (q, mailbox, filter=ALL/UNREAD/AWAITING/ATTACHMENTS, page) | `chat` (list, empty pane) | `ConversationController` → `ConversationListService`, `MailAccountService`, `MailboxPollingService` |
| `/chats/{key}` | `chat` (list + open pane; marks read) | `ChatService.buildFor/markRead` |
| `/chats/{key}/pane` | HTML fragment | same |
| `/chats/new` GET/POST | `compose` | `ReplyService.sendNewEmail`, `OutboundMessageService.recordNewEmail` |
| `/chats/{key}/send` POST | redirect | `ReplyService`, `OutboundMessageService` |
| `/chats/refresh` POST (JSON), `/chats/events` (SSE) | non-HTML | `MailboxPollingService`, `ChatEventStream` |
| `/threads/**`, `/senders` | redirect to `/chats` | `ChatRedirectController` |
| `/mailboxes` (+`/new`, `/{id}/sync`, `/{id}/signature`, `/{id}/delete`, `/preferences`) | `mailboxes/*` | `MailboxController` → `MailAccountService` |
| `/mailboxes/gmail/{connect,reconnect/{id},callback}` | Gmail OAuth | `GmailConnectController` |
| `/searches` POST, `/searches/{id}/delete` | redirect | `SavedSearchController` |
| `/account`, `/account/{notifications,appearance,privacy,password,email}` | `account*` | `AccountController` |
| `/billing/{checkout,portal,success,cancel,cancel-subscription}`, `/billing/webhook` | Stripe flow | `BillingController`, `BillingWebhookController` |
| `/team/invite`, `/team/invite/accept` | `team/*` | `TeamInviteController` |
| `/digest/opt-out` | `digest/*` | `DigestController` |
| `/admin/revenue` (+POST actions) | `admin/revenue` | `AdminRevenueController` (allowlist) |
| `/privacy`, `/terms`, `/refund` | `legal` | `LegalController` |
| `/manifest.webmanifest`, `/sw.js`, `/offline`, icons | PWA | `PwaController` |
| `/robots.txt`, `/sitemap.xml`, `/images/og-card.png` | SEO | `SeoController` |
