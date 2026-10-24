# Website (Thymeleaf)

Templates: `src/main/resources/templates`. Controllers are package-private in
`web/`, `auth/`, `billing/`, `team/`, `digest/`, `admin/`.

| Route(s) | Template | Controller → services |
|---|---|---|
| `/`, `/pricing` | `landing`, `pricing` | `PricingController` |
| `/demo` | `chat` (demo data) | `DemoController` → `DemoConversationService` |
| `/login`, `/register`, `/auth/google/start` | `login`, `register` | `AuthController`, `OAuthStartController` → `UserService` |
| `/verify-email` | `verify-email` | `EmailVerificationController` |
| `/password/forgot`, `/password/reset` | `password/*` | `PasswordResetController` |
| `/chats`, `/chats/{key}`, `/chats/{key}/pane`, `/chats/new`, `/chats/{key}/send` | `chat`, `compose`, `fragments/chat-pane` | `ConversationController` → `ChatService`, `ConversationListService`, `ReplyService`, `MailAccountService` |
| `/chats/refresh`, `/chats/events` | JSON / SSE | `ConversationController`, `ChatEventsController` |
| `/threads/**`, `/senders` | redirect to `/chats` | `ChatRedirectController` |
| `/mailboxes/**`, `/mailboxes/gmail/**` | `mailboxes/*` | `MailboxController`, `GmailConnectController` → `MailAccountService` |
| `/searches/**` | (POST only) | `SavedSearchController` |
| `/account/**` | `account*` | `AccountController` → `AccountService` |
| `/billing/**` | `billing/*` | `BillingController` → `BillingService` |
| `/team/invite**` | `team/*` | `TeamInviteController` → `TeamInviteService` |
| `/digest/opt-out` | `digest/opt-out` | `DigestController` |
| `/admin/revenue` | `admin/revenue` | `AdminRevenueController` |
| `/privacy`, `/terms`, `/refund` | `legal` | `LegalController` |
| `/manifest.webmanifest`, `/sw.js`, `/offline`, icons | PWA | `PwaController` |
| `/robots.txt`, `/sitemap.xml` | SEO | `SeoController` |

Shared fragments (`templates/fragments`): `app-nav`, `settings-nav`, `chat-pane`,
`live-refresh`, `seo`, `cookie-banner`, `install-banner`, `sw-register`.
