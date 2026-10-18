# WEB.md — Thymeleaf website

Templates: `src/main/resources/templates`. Fragments: `fragments/` (`app-nav`,
`settings-nav`, `chat-pane`, `live-refresh`, `seo`, `cookie-banner`,
`install-banner`, `sw-register`). Global model advice: `NavModelAdvice`,
`SiteModelAdvice`; errors: `GlobalExceptionHandler` + `error.html`.

## Public pages
| Route | Controller | Template |
|---|---|---|
| `/`, `/pricing` | `PricingController` | `landing`, `pricing` |
| `/demo` | `DemoController` (`DemoConversationService`) | `chat` |
| `/privacy`, `/terms`, `/refund` | `LegalController` | `legal` |
| `/login`, `/register` | `AuthController` | `login`, `register` |
| `/auth/google/start` | `OAuthStartController` | redirect |
| `/password/forgot`, `/password/reset` | `PasswordResetController` | `password/*` |
| `/verify-email`, `/verify-email/resend` | `EmailVerificationController` | `verify-email` |
| `/digest/opt-out` | `DigestController` | `digest/opt-out` |
| `/robots.txt`, `/sitemap.xml`, `/images/og-card.png` | `SeoController` | — |
| `/manifest.webmanifest`, `/sw.js`, `/offline`, icons | `PwaController` | `offline` |

## Authenticated pages
| Route | Controller | Services |
|---|---|---|
| `GET /chats` (`q`, `mailbox`, `filter`, `page`) | `ConversationController` | `ConversationListService`, `MailboxPollingService`, `MailAccountService` |
| `GET /chats/new`, `POST /chats/new` | same | `ReplyService`, `OutboundMessageService` |
| `GET /chats/{key}`, `/chats/{key}/pane` | same | `ChatService` (marks read) |
| `POST /chats/{key}/send` | same | `ReplyService`, `OutboundMessageService` (**logic in controller**) |
| `POST /chats/refresh` (JSON `{imported}`), `GET /chats/events` (SSE) | same / `ChatEventsController` | `MailboxPollingService`, `ChatEventStream` |
| `/threads/**`, `/senders` | `ChatRedirectController` | redirect to `/chats` |
| `/mailboxes`, `/new`, `/{id}/sync`, `/preferences`, `/{id}/signature`, `/{id}/delete` | `MailboxController` | `MailAccountService`, `PlanLimitService` |
| `/mailboxes/gmail/connect`, `/reconnect/{id}`, `/callback` | `GmailConnectController` | `GmailOAuthClient` |
| `POST /searches`, `/searches/{id}/delete` | `SavedSearchController` | `SavedSearchService` |
| `/account`, `/account/notifications|appearance|privacy`, `POST /account/password|email|appearance` | `AccountController` | `AccountService` |
| `/team/invite`, `/team/invite/accept` | `TeamInviteController` | `TeamInviteService` |
| `/billing/checkout|portal|success|cancel|cancel-subscription` | `BillingController` | `BillingService` |
| `/admin/revenue` + POST reconcile / win-back | `AdminRevenueController` | metrics services, `AdminAuthorizer` |

All forms rely on Spring Security CSRF tokens (webhook and H2 console exempt).
