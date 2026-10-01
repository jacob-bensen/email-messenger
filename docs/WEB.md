# WEB.md — Thymeleaf website

Templates in `src/main/resources/templates`; shared fragments in `fragments/`
(`app-nav`, `settings-nav`, `chat-pane`, `live-refresh`, `seo`, `cookie-banner`, `install-banner`, `sw-register`).
Global model attributes come from `NavModelAdvice` and `SiteModelAdvice`.

## Public pages
| Route | Template / handler | Notes |
|---|---|---|
| `/` , `/pricing` | `landing`, `pricing` (`PricingController`) | |
| `/demo` | `conversation`/demo (`DemoController`, `DemoConversationService`) | |
| `/login`, `/register` | `login`, `register` (`AuthController`) | `UserService` |
| `/password/forgot`, `/password/reset` | `password/*` | `PasswordResetService` |
| `/verify-email`, `/verify-email/resend` | `verify-email` | `EmailVerificationService` |
| `/privacy`, `/terms`, `/refund` | `legal` (`LegalController`) | texts in `resources/legal` |
| `/digest/opt-out` | `digest/opt-out` | |
| `/robots.txt`, `/sitemap.xml`, `/images/og-card.png` | `SeoController` | |
| `/manifest.webmanifest`, `/sw.js`, `/offline`, `/icons/*` | `PwaController` | PWA shell |

## Authenticated pages
| Route | Handler → services |
|---|---|
| `/chats`, `/chats/{key}`, `/chats/{key}/pane`, `/chats/new` | `ConversationController` → `ConversationListService`, `ChatService`, `MailAccountService`, `MailboxPollingService` |
| `POST /chats/{key}/send`, `POST /chats/new` | → `OutboundMessageService` / `ReplyService` |
| `/threads`, `/threads/**`, `/senders` | `ChatRedirectController` (legacy redirects to `/chats`) |
| `/mailboxes` (+ `/new`, `/{id}/sync`, `/{id}/signature`, `/{id}/delete`, `/preferences`) | `MailboxController` → `MailAccountService` |
| `/mailboxes/gmail/{connect,reconnect/{id},callback}` | `GmailConnectController` → `GmailOAuthClient` |
| `/searches` (+ `/{id}/delete`) | `SavedSearchController` → `SavedSearchService` |
| `/account`, `/account/{notifications,appearance,privacy,password,email}` | `AccountController` → `AccountService` |
| `/billing/{checkout,portal,success,cancel,cancel-subscription}` | `BillingController` → `BillingService` |
| `/team/invite`, `/team/invite/accept` | `TeamInviteController` |
| `/admin/revenue`, `/admin/revenue/reconcile-billing-period`, `/admin/retention/win-back` | `AdminRevenueController` → admin metrics services, `WinBackOutreachService` (operator allow-list `ADMIN_EMAILS`) |

Live updates: `chat.html` consumes `/chats/events` (SSE) through `fragments/live-refresh.html` and
morphs the rail + open pane in place.
