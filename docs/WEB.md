# WEB.md — Thymeleaf website

Templates: `src/main/resources/templates`. Shared fragments in `fragments/`:
`app-nav`, `settings-nav`, `chat-pane`, `live-refresh`, `cookie-banner`,
`install-banner`, `sw-register`, `seo`. Model attributes common to all pages are
supplied by `NavModelAdvice` and `SiteModelAdvice`.

## Page inventory
| Route | Auth | Controller → service | Template |
|---|---|---|---|
| `/`, `/pricing` | public | `PricingController` | `landing`, `pricing` |
| `/demo` | public | `DemoController` → `DemoConversationService` | `chat` (demo) |
| `/privacy` `/terms` `/refund` | public | `LegalController` | `legal` |
| `/login` `/register` | public | `AuthController` → `UserService` | `login`, `register` |
| `/password/forgot` `/password/reset` | public | `PasswordResetController` | `password/*` |
| `/verify-email`, `/verify-email/resend` | public/auth | `EmailVerificationController` | `verify-email` |
| `/chats` `/chats/{key}` `/chats/{key}/pane` `/chats/new` | user | `ConversationController` → `ChatService`, `ConversationListService`, `MailAccountService` | `chat`, `compose`, `fragments/chat-pane` |
| `POST /chats/{key}/send` | user | `ConversationController` → `OutboundMessageService`/`ReplyService` | redirect |
| `/mailboxes`, `/mailboxes/new`, `/mailboxes/gmail/*` | user | `MailboxController`, `GmailConnectController` → `MailAccountService` | `mailboxes/*` |
| `POST /searches`, `/searches/{id}/delete` | user | `SavedSearchController` → `SavedSearchService` | redirect |
| `/account`, `/account/{notifications,appearance,privacy}` | user | `AccountController` → `AccountService` | `account*` |
| `/billing/*` | user | `BillingController` → `BillingService` | `billing/*` |
| `/team/invite`, `/team/invite/accept` | user | `TeamInviteController` → `TeamInviteService` | `team/*` |
| `/digest/opt-out` | public | `DigestController` | `digest/*` |
| `/admin/revenue` (+ POST actions) | operator | `AdminRevenueController` → admin metrics services | `admin/revenue` |
| `/manifest.webmanifest`, `/sw.js`, `/icons/*`, `/offline` | public | `PwaController` | — |
| `/robots.txt`, `/sitemap.xml`, `/images/og-card.png` | public | `SeoController` | — |

Live updates: `fragments/live-refresh.html` opens `GET /chats/events` (SSE) and
morphs the rail and open pane in place.
