# Website (Thymeleaf)

Templates: `src/main/resources/templates`; shared fragments in `templates/fragments`
(`app-nav`, `settings-nav`, `chat-pane`, `live-refresh`, `seo`, `cookie-banner`,
`install-banner`, `sw-register`). Global model attributes come from `NavModelAdvice` and
`SiteModelAdvice`; errors from `GlobalExceptionHandler` → `error.html`.

## Pages

| Route(s) | Template | Controller / service | Auth |
|---|---|---|---|
| `/`, `/pricing`, `/demo` | `landing`, `pricing` | `PricingController`, `DemoController` (`DemoConversationService`) | public |
| `/privacy`, `/terms`, `/refund` | `legal` | `LegalController` | public |
| `/login`, `/register` | `login`, `register` | `AuthController`, `UserService` | public |
| `/password/forgot`, `/password/reset` | `password/*` | `PasswordResetController` | public |
| `/verify-email`, `/verify-email/resend` | `verify-email` | `EmailVerificationController` | public / user |
| `/chats`, `/chats/{key}`, `/chats/{key}/pane`, `/chats/new`, `/chats/{key}/send`, `/chats/refresh`, `/chats/events` | `chat`, `compose`, `fragments/chat-pane` | `ConversationController`, `ConversationListService`, `ChatService`, `ReplyService`, `OutboundMessageService`, `ChatEventStream` | user |
| `/threads/**`, `/senders` | — | `ChatRedirectController` → `/chats` | user |
| `/mailboxes/**`, `/mailboxes/gmail/**` | `mailboxes/*` | `MailboxController`, `GmailConnectController`, `MailAccountService` | user |
| `/searches` | — | `SavedSearchController` | user |
| `/account`, `/account/notifications|appearance|privacy|password|email` | `account*` | `AccountController`, `AccountService` | user |
| `/billing/**` | `billing/*` | `BillingController`, `BillingService` | user (`/billing/cancel`, `/billing/webhook` public) |
| `/team/invite`, `/team/invite/accept` | `team/*` | `TeamInviteController` | user |
| `/digest/**` | `digest/*` | `DigestController` | user (`/digest/opt-out` public) |
| `/admin/revenue`, … | `admin/*` | `AdminRevenueController` | admin allow-list |
| `/manifest.webmanifest`, `/sw.js`, `/offline`, icons | — | `PwaController` | public |
| `/robots.txt`, `/sitemap.xml`, `/images/og-card.png` | — | `SeoController` | public |

The `/chats` workspace updates in place via the SSE stream and HTML fragment swaps
(`live-refresh.html`); the `/pane` endpoint returns HTML, not JSON, and is not part of the
mobile contract.
