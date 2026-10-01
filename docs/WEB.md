# WEB — Thymeleaf website

Inventory from `@*Mapping` annotations as of 2026-10-01. "Auth" = needs a logged-in session unless noted.

## Public (permitAll in `SecurityConfig`)
| Route | Controller | Template |
|---|---|---|
| `GET /`, `/pricing` | `PricingController` | `landing`, `pricing` |
| `GET /demo` | `DemoController` (`DemoConversationService`) | `conversation` |
| `GET/POST /login`, `GET/POST /register`, `GET /auth/google/start` | `AuthController`, `OAuthStartController` | `login`, `register` |
| `GET/POST /password/forgot`, `/password/reset` | `PasswordResetController` | `password/*` |
| `GET /verify-email` (POST `/verify-email/resend` needs auth) | `EmailVerificationController` | `verify-email` |
| `/privacy`, `/terms`, `/refund` | `LegalController` | `legal` |
| `/robots.txt`, `/sitemap.xml`, `/images/og-card.png` | `SeoController` | — |
| `/manifest.webmanifest`, `/icons/*`, `/sw.js`, `/offline` | `PwaController` | — |
| `GET /digest/opt-out` | `DigestController` | — |
| `POST /billing/webhook`, `GET /billing/cancel` | `BillingWebhookController`, `BillingController` | — |

## Authenticated
| Route | Controller | Services |
|---|---|---|
| `GET /chats` (`page,q,mailbox,filter=ALL|UNREAD|AWAITING|ATTACHMENTS`) | `ConversationController` | `ConversationListService`, `MailboxPollingService` |
| `GET /chats/{key}`, `/chats/{key}/pane` | same | `ChatService.markRead`, `ChatService` |
| `GET/POST /chats/new`, `POST /chats/{key}/send` | same | `ReplyService`, `OutboundMessageService`, `ConversationKeyService` |
| `POST /chats/refresh` (JSON `{imported:n}`), `GET /chats/events` (SSE) | `ConversationController`, `ChatEventsController` | `MailboxPollingService`, `ChatEventStream` |
| `/threads/**`, `/senders` | `ChatRedirectController` | redirects to `/chats` |
| `/mailboxes`, `/mailboxes/new`, `POST /mailboxes`, `/{id}/sync`, `/{id}/signature`, `/{id}/delete`, `/preferences` | `MailboxController` | `MailAccountService` |
| `/mailboxes/gmail/connect`, `/reconnect/{id}`, `/callback` | `GmailConnectController` | `GmailOAuthClient` |
| `POST /searches`, `/searches/{id}/delete` | `SavedSearchController` | `SavedSearchService` |
| `/account`, `/account/{notifications,appearance,privacy}`, `POST /account/{password,email,appearance}` | `AccountController` | `AccountService` |
| `POST /billing/checkout`, `/billing/portal`, `GET /billing/success`, `/billing/cancel-subscription` (+POST) | `BillingController` | `BillingService` |
| `/team/invite`, `/team/invite/accept` (GET/POST) | `TeamInviteController` | team services |
| `/admin/revenue`, `POST /admin/revenue/reconcile-billing-period`, `POST /admin/retention/win-back` | `AdminRevenueController` | admin metrics services; non-admins get 404 |

## Shared fragments (`templates/fragments`)
`app-nav`, `settings-nav`, `chat-pane`, `live-refresh` (SSE in-place updates), `cookie-banner`,
`install-banner`, `seo`, `sw-register`. Model advice: `NavModelAdvice`, `SiteModelAdvice`.

## Known issue
`ConversationController` holds list/scope logic privately; a REST layer cannot reuse it yet.
