# WEB (Thymeleaf site)

Templates: `src/main/resources/templates`. Shared fragments in `fragments/`
(`app-nav`, `chat-pane`, `live-refresh`, `settings-nav`, `seo`, `cookie-banner`,
`install-banner`, `sw-register`). Global model attributes via `NavModelAdvice` and
`SiteModelAdvice`.

| Route(s) | Controller | Template | Auth |
|---|---|---|---|
| `/`, `/pricing` | `PricingController` | `landing`, `pricing` | public |
| `/demo` | `DemoController` | `chat` (demo data) | public |
| `/login`, `/register` | `AuthController` | `login`, `register` | public |
| `/password/forgot`, `/password/reset` | `PasswordResetController` | `password/*` | public |
| `/verify-email`, `/verify-email/resend` | `EmailVerificationController` | `verify-email` | public / user |
| `/auth/google/start` | `OAuthStartController` | — | public |
| `/chats`, `/chats/new`, `/chats/{key}`, `/chats/{key}/pane`, `/chats/{key}/send`, `/chats/refresh`, `/chats/events` | `ConversationController`, `ChatEventsController` | `chat`, `compose`, `fragments/chat-pane` | user |
| `/threads`, `/threads/**`, `/senders` | `ChatRedirectController` | redirect to `/chats` | user |
| `/mailboxes`, `/mailboxes/new`, `/{id}/sync`, `/preferences`, `/{id}/signature`, `/{id}/delete` | `MailboxController` | `mailboxes/*` | user |
| `/mailboxes/gmail/connect`, `/reconnect/{id}`, `/callback` | `GmailConnectController` | — | user |
| `/searches`, `/searches/{id}/delete` | `SavedSearchController` | — | user |
| `/account`, `/account/notifications`, `/account/appearance`, `/account/privacy`, `/account/password`, `/account/email` | `AccountController` | `account*` | user |
| `/billing/checkout`, `/portal`, `/success`, `/cancel`, `/cancel-subscription` | `BillingController` | `billing/*` | user (`/billing/cancel` public) |
| `/billing/webhook` | `BillingWebhookController` (`@RestController`) | — | Stripe signature |
| `/team/invite`, `/team/invite/accept` | `TeamInviteController` | `team/*` | user |
| `/digest/opt-out` | `DigestController` | `digest/opt-out` | public |
| `/admin/revenue` (+ POST reconcile / win-back) | `AdminRevenueController` | `admin/revenue` | operator (`AdminAuthorizer`) |
| `/privacy`, `/terms`, `/refund` | `LegalController` | `legal` | public |
| `/robots.txt`, `/sitemap.xml`, `/images/og-card.png` | `SeoController` | — | public |
| `/manifest.webmanifest`, `/sw.js`, `/offline`, icons | `PwaController` | `offline` | public |

All controllers are package-private and delegate to services; no REST layer shares
them yet, so there is currently no web/API logic duplication (see INTERNAL_TODO).
