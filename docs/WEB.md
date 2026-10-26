# WEB.md — Thymeleaf website

Templates live in `src/main/resources/templates`. Controllers are
package-private classes; business logic lives in services.

## Public pages
`/` (landing), `/pricing`, `/demo`, `/privacy`, `/terms`, `/refund`,
`/login`, `/register`, `/password/forgot`, `/password/reset`,
`/verify-email`, `/digest/opt-out`, `/robots.txt`, `/sitemap.xml`,
PWA: `/manifest.webmanifest`, `/sw.js`, `/offline`, icons.

## Authenticated pages
| Route | Controller → services | Template |
|---|---|---|
| `GET /chats`, `/chats/{key}` | `ConversationController` → `ConversationListService`, `ChatService`, `MailAccountService` | `chat` |
| `GET /chats/{key}/pane` | same | `fragments/chat-pane` |
| `GET/POST /chats/new` | `ConversationController` → `OutboundMessageService` | `compose` |
| `POST /chats/{key}/send` | `ConversationController` → `OutboundMessageService`/`ReplyService` | redirect |
| `/mailboxes/**` | `MailboxController`, `GmailConnectController` → `MailAccountService` | `mailboxes/*` |
| `/searches` | `SavedSearchController` → `SavedSearchService` | redirect |
| `/account/**` | `AccountController` → `AccountService` | `account*` |
| `/billing/**` | `BillingController` → `BillingService` | `billing/*` |
| `/team/invite**` | `TeamInviteController` → `TeamInviteService` | `team/*` |
| `/admin/revenue` | `AdminRevenueController` → metrics services | `admin/revenue` |

Legacy `/threads/**`, `/senders` redirect to `/chats`.
Shared fragments: `fragments/app-nav`, `settings-nav`, `live-refresh`,
`seo`, `cookie-banner`, `install-banner`, `sw-register`.
Model advice: `NavModelAdvice`, `SiteModelAdvice`; errors: `GlobalExceptionHandler`.
