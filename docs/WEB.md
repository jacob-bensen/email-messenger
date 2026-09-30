# Website (Thymeleaf) inventory

Templates in `src/main/resources/templates`; fragments in `templates/fragments`
(`app-nav`, `settings-nav`, `chat-pane`, `live-refresh`, `seo`, `cookie-banner`,
`install-banner`, `sw-register`).

| Page | Routes | Controller | Services |
|---|---|---|---|
| Landing / pricing / demo | `/`, `/pricing`, `/demo` | `PricingController`, `DemoController` | `DemoConversationService`, billing/plan services |
| Auth | `/login`, `/register`, `/auth/google/start`, `/password/*`, `/verify-email` | `AuthController`, `PasswordResetController`, `EmailVerificationController`, `OAuthStartController` | `UserService`, `PasswordResetService`, `EmailVerificationService` |
| Chats (main app) | `/chats`, `/chats/new`, `/chats/{key}`, `/chats/{key}/pane`, `/chats/{key}/send`, `/chats/refresh`, `/chats/events` (SSE) | `ConversationController`, `ChatEventsController` | `ConversationListService`, `ChatService`, `OutboundMessageService`, `ConversationKeyService` |
| Mailboxes | `/mailboxes`, `/mailboxes/new`, `/mailboxes/{id}/sync\|signature\|delete`, `/mailboxes/gmail/*` | `MailboxController`, `GmailConnectController` | `MailAccountService`, `MailboxPollingService` |
| Saved searches | `/searches` | `SavedSearchController` | `SavedSearchService` |
| Account | `/account`, `/account/{notifications,appearance,privacy,password,email}` | `AccountController` | `AccountService` |
| Billing / team | `/billing/*`, `/team/invite*` | `BillingController`, `TeamInviteController` | `BillingService`, `TeamService` |
| Legal / SEO / PWA | `/privacy`, `/terms`, `/refund`, `/robots.txt`, `/sitemap.xml`, `/offline` | `LegalController`, `SeoController`, `PwaController` | — |
| Admin | `/admin/revenue` | `AdminRevenueController` | `*MetricsService` |

`/threads/**` and `/senders` redirect to `/chats` (`ChatRedirectController`).
