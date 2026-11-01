# WEB — Thymeleaf website

Templates: `src/main/resources/templates` (+ `fragments/`, `admin/`, `team/`, `mailboxes/`, `digest/`,
`billing/`, `password/`). Auth = session unless marked public.

## Public
| Route | Controller | Notes |
|---|---|---|
| `GET /` , `/pricing` | `PricingController` | landing / plans |
| `GET /demo` | `DemoController` | `DemoConversationService` sample chat |
| `GET/POST /login`, `/register`, `/auth/google/start` | `AuthController`, `OAuthStartController` | form + Google OIDC |
| `/password/forgot`, `/password/reset` | `PasswordResetController` | |
| `GET /verify-email`, `POST /verify-email/resend` | `EmailVerificationController` | resend needs auth |
| `/privacy`, `/terms`, `/refund` | `LegalController` | |
| `/robots.txt`, `/sitemap.xml`, `/images/og-card.png` | `SeoController` | |
| `/manifest.webmanifest`, `/sw.js`, `/offline`, icons | `PwaController` | PWA shell |
| `GET /digest/opt-out` | `DigestController` | |
| `POST /billing/webhook` | `BillingWebhookController` (`@RestController`) | Stripe signature, CSRF-exempt |
| `GET /actuator/health` | actuator | |

## Authenticated
| Route | Controller / service | Notes |
|---|---|---|
| `GET /chats` (`page,q,mailbox,filter`) | `ConversationController` → `ConversationListService` | list + dashboard |
| `GET /chats/{key}` , `/chats/{key}/pane` | → `ChatService.markRead`, `chatFor` | pane = HTML fragment |
| `GET/POST /chats/new` | → `OutboundMessageService` | new email, multipart attachments |
| `POST /chats/{key}/send` | → `OutboundMessageService`/`ReplyService` | `asReply`, `subject`, attachments |
| `POST /chats/refresh` | → `MailboxPollingService` | returns `{imported}` JSON |
| `GET /chats/events` | `ChatEventsController` / `ChatEventStream` | SSE new-mail events |
| `/threads/**`, `/senders` | `ChatRedirectController` | legacy redirects |
| `/mailboxes` (+`/new`, `/{id}/sync`, `/preferences`, `/{id}/signature`, `/{id}/delete`) | `MailboxController` → `MailAccountService` | |
| `/mailboxes/gmail/connect|reconnect/{id}|callback` | `GmailConnectController` | Gmail OAuth |
| `POST /searches`, `/searches/{id}/delete` | `SavedSearchController` | |
| `/account`, `/account/{notifications,appearance,privacy,password,email}` | `AccountController` | |
| `/billing/{checkout,portal,success,cancel,cancel-subscription}` | `BillingController` | |
| `/team/invite`, `/team/invite/accept` | `TeamInviteController` | |
| `/admin/revenue`, `/admin/retention/win-back` | `AdminRevenueController` | |

Shared fragments live in `templates/fragments/` (`chat-pane`, `live-refresh`, …). Model advice:
`NavModelAdvice`, `SiteModelAdvice`. Errors: `GlobalExceptionHandler` → `error.html`.
