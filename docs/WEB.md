# Website (Thymeleaf)

Templates: `src/main/resources/templates`. Shared fragments in `templates/fragments`
(`app-nav`, `settings-nav`, `chat-pane`, `live-refresh`, `seo`, `cookie-banner`,
`install-banner`, `sw-register`).

## Public pages
| Route | Controller | Template |
|---|---|---|
| `GET /`, `/pricing` | `PricingController` | `landing`, `pricing` |
| `GET /demo` | `DemoController` (`DemoConversationService`) | `chat` (demo data) |
| `GET /privacy`, `/terms`, `/refund` | `LegalController` | `legal` |
| `GET /login`, `/register`, `POST /register` | `AuthController` | `login`, `register` |
| `GET/POST /password/forgot`, `/password/reset` | `PasswordResetController` | `password/*` |
| `GET /verify-email`, `POST /verify-email/resend` | `EmailVerificationController` | `verify-email` |
| `GET /auth/google/start` | `OAuthStartController` | redirect |
| `GET /digest/opt-out` | `DigestController` | `digest/opt-out` |
| `/robots.txt`, `/sitemap.xml`, `/images/og-card.png` | `SeoController` | generated |
| `/manifest.webmanifest`, `/sw.js`, `/offline`, icons | `PwaController` | generated |

## Authenticated pages
| Route | Controller | Services |
|---|---|---|
| `GET /chats` (`q`, `mailbox`, `filter`, `page`) | `ConversationController` | `ConversationListService`, `MailAccountService`, `MailboxPollingService` |
| `GET /chats/{key}`, `/chats/{key}/pane` | `ConversationController` | `ChatService.markRead`, `ConversationService` |
| `GET/POST /chats/new` | `ConversationController` | `ReplyService` |
| `POST /chats/{key}/send` (multipart) | `ConversationController` | `ReplyService`, `PlanLimitService` |
| `POST /chats/refresh` → JSON `{imported}` | `ConversationController` | `MailboxPollingService` |
| `GET /chats/events` (SSE) | `ChatEventsController` | `ChatEventStream` |
| `/threads/**`, `/senders` | `ChatRedirectController` | legacy redirects to `/chats` |
| `/mailboxes`, `/new`, `/{id}/sync|signature|delete`, `/preferences` | `MailboxController` | `MailAccountService` |
| `/mailboxes/gmail/connect|reconnect/{id}|callback` | `GmailConnectController` | `GmailOAuthClient` |
| `POST /searches`, `/searches/{id}/delete` | `SavedSearchController` | saved-search repo |
| `/account`, `/account/{notifications,appearance,privacy}`, `POST /account/{password,email,appearance}` | `AccountController` | `AccountService` |
| `/billing/{checkout,portal,success,cancel,cancel-subscription}` | `BillingController` | `BillingService` |
| `/team/invite`, `/team/invite/accept` | `TeamInviteController` | team services |
| `/admin/revenue` + POST actions | `AdminRevenueController` | `*MetricsService` (operator only) |
