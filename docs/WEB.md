# WEB.md — Thymeleaf website

Templates live in `src/main/resources/templates`; shared fragments in `fragments/`.

| Route | Auth | Controller | Template |
|---|---|---|---|
| `/` | public | `DemoController`/landing | `landing.html` |
| `/pricing` | public | `PricingController` | `pricing.html` |
| `/demo` | public | `DemoController` (`DemoConversationService`) | `chat.html`/demo |
| `/privacy` `/terms` `/refund` | public | `LegalController` | `legal.html` |
| `/login` `/register` `/auth/google/start` | public | `AuthController`, `OAuthStartController` | `login.html`, `register.html` |
| `/password/forgot` `/password/reset` | public | `PasswordResetController` | `password/*` |
| `/verify-email` (+`/resend`) | public / auth | `EmailVerificationController` | `verify-email.html` |
| `/chats` | auth | `ConversationController` | `chat.html` (list + empty pane) |
| `/chats/{key}` , `/chats/{key}/pane` | auth | `ConversationController` | `chat.html` (open chat; `/pane` is the in-place live-refresh fragment) |
| `/chats/new` (GET/POST) | auth | `ConversationController` | `compose.html` |
| `/chats/{key}/send` POST | auth | `ConversationController` | redirect |
| `/chats/refresh` POST | auth | `ConversationController` | JSON |
| `/chats/events` | auth | `ChatEventsController` | SSE |
| `/threads`, `/threads/**`, `/senders` | auth | `ChatRedirectController` | redirects to `/chats` |
| `/mailboxes` (+`/new`, `/{id}/sync`, `/{id}/signature`, `/{id}/delete`, `/preferences`) | auth | `MailboxController` | `mailboxes/*` |
| `/mailboxes/gmail/{connect,reconnect/{id},callback}` | auth | `GmailConnectController` | redirects |
| `/searches` (+`/{id}/delete`) | auth | `SavedSearchController` | redirect |
| `/account`, `/account/{notifications,appearance,privacy,password,email}` | auth | `AccountController` | `account*.html` |
| `/billing/{checkout,portal,success,cancel,cancel-subscription}` | auth (`cancel`, webhook public) | `BillingController` | `billing/*` |
| `/team/invite` (+`/accept`) | auth | `TeamInviteController` | `team/*` |
| `/digest/opt-out` | public | `DigestController` | `digest/*` |
| `/admin/revenue`, `/admin/retention/win-back` | admin | `AdminRevenueController` | `admin/*` |
| `/manifest.webmanifest`, `/sw.js`, `/offline`, icons | public | `PwaController` | PWA shell |
| `/robots.txt`, `/sitemap.xml`, `/images/og-card.png` | public | `SeoController` | — |

Shared model advice: `NavModelAdvice`, `SiteModelAdvice`. Live updates: `fragments/live-refresh.html`.
