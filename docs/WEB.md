# Thymeleaf website

All pages are server-rendered from `src/main/resources/templates`. Cookie-session
auth; CSRF on. Public vs authenticated is decided in `SecurityConfig`.

## Public
| Route | Template / controller | Notes |
|---|---|---|
| `/`, `/pricing` | `landing`, `pricing` — `PricingController` | |
| `/demo` | `DemoController` → `DemoConversationService` | sample chat |
| `/login`, `/register`, `/auth/google/start` | `AuthController`, `OAuthStartController` | |
| `/password/forgot`, `/password/reset` | `PasswordResetController` | |
| `/verify-email`, `/verify-email/resend` | `EmailVerificationController` | |
| `/privacy`, `/terms`, `/refund` | `LegalController` | |
| `/digest/opt-out` | `DigestController` | |
| `/robots.txt`, `/sitemap.xml`, `/images/og-card.png` | `SeoController` | |
| `/manifest.webmanifest`, `/icons/*`, `/sw.js`, `/offline` | `PwaController` | PWA shell |
| `/billing/cancel`, `/billing/webhook` | `BillingController`, `BillingWebhookController` | |

## Authenticated
| Route | Controller → services |
|---|---|
| `GET /chats` (`q`, `mailbox`, `filter`, `page`) | `ConversationController` → `ConversationListService` |
| `GET/POST /chats/new` | → `ReplyService`, `OutboundMessageService`, `ConversationKeyService` |
| `GET /chats/{key}`, `/chats/{key}/pane` (fragment) | → `ChatService` (marks read) |
| `POST /chats/{key}/send` (`asReply`, `subject`, `attachments`) | → `ReplyService`, `OutboundMessageService` |
| `POST /chats/refresh`, `GET /chats/events` | see API.md §1 |
| `/threads`, `/threads/**`, `/senders` | `ChatRedirectController` (legacy redirects) |
| `/mailboxes`, `/new`, `/{id}/sync`, `/preferences`, `/{id}/signature`, `/{id}/delete` | `MailboxController` → `MailAccountService` |
| `/mailboxes/gmail/connect`, `/reconnect/{id}`, `/callback` | `GmailConnectController` |
| `POST /searches`, `/searches/{id}/delete` | `SavedSearchController` |
| `/account`, `/account/notifications`, `/account/appearance`, `/account/privacy`, `POST /account/password`, `/account/email` | `AccountController` |
| `/billing/checkout`, `/portal`, `/success`, `/cancel-subscription` | `BillingController` → `BillingService` |
| `/team/invite`, `/team/invite/accept` | `TeamInviteController` |
| `/admin/revenue`, `POST /admin/revenue/reconcile-billing-period`, `/admin/retention/win-back` | `AdminRevenueController` (allow-listed emails; else 404) |

## Shared fragments / advice
`fragments/`: `app-nav`, `settings-nav`, `chat-pane`, `live-refresh` (SSE →
in-place morph), `seo`, `cookie-banner`, `install-banner`, `sw-register`.
`NavModelAdvice` / `SiteModelAdvice` inject nav and site-wide model data.
`GlobalExceptionHandler` renders the error page.
