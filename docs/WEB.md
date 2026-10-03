# WEB — Thymeleaf website

Templates: `src/main/resources/templates`. Shared fragments: `fragments/` (`app-nav`, `settings-nav`, `chat-pane`,
`live-refresh`, `seo`, `cookie-banner`, `install-banner`, `sw-register`).

## Public pages (no auth)
| Route | Controller | Template |
|---|---|---|
| `GET /`, `/pricing` | `PricingController` | `landing`, `pricing` |
| `GET /demo` | `DemoController` (`DemoConversationService`) | `chat` (demo data) |
| `GET/POST /login`, `/register` | `AuthController` | `login`, `register` |
| `GET /auth/google/start` | `OAuthStartController` | redirect |
| `GET/POST /password/forgot`, `/password/reset` | `PasswordResetController` | `password/*` |
| `GET /verify-email` | `EmailVerificationController` | `verify-email` |
| `GET /privacy /terms /refund` | `LegalController` | `legal` |
| `GET /digest/opt-out` | `DigestController` | `digest/opt-out` |
| `/robots.txt /sitemap.xml /images/og-card.png` | `SeoController` | generated |
| `/manifest.webmanifest /sw.js /offline /icons/*` | `PwaController` | PWA shell |

## Authenticated pages
| Route | Controller | Notes |
|---|---|---|
| `GET /chats` | `ConversationController.list` | list/dashboard; params `page,q,mailbox,filter(all|unread|awaiting|attachments)` |
| `GET /chats/new`, `POST /chats/new` | same | compose; multipart `to,subject,body,attachments,mailbox` |
| `GET /chats/{key}` , `/chats/{key}/pane` | same | open chat (full / HTML fragment); marks read |
| `POST /chats/{key}/send` | same | `body,subject,asReply,attachments,mailbox` → `ReplyService` + `OutboundMessageService` |
| `POST /chats/refresh` | same | JSON `{"imported":n}` |
| `GET /chats/events` | `ChatEventsController` | SSE, fed by `NewMailEvent` |
| `/threads/**`, `/senders` | `ChatRedirectController` | legacy redirects to `/chats` |
| `/mailboxes` (+`/new`, `/{id}/sync`, `/preferences`, `/{id}/signature`, `/{id}/delete`) | `MailboxController` | |
| `/mailboxes/gmail/connect`, `/reconnect/{id}`, `/callback` | `GmailConnectController` | Gmail OAuth |
| `POST /searches`, `/searches/{id}/delete` | `SavedSearchController` | |
| `/account`, `/account/{notifications,appearance,privacy,password,email}` | `AccountController` | |
| `/billing/{checkout,portal,success,cancel,cancel-subscription}` | `BillingController` | Stripe |
| `/team/invite`, `/team/invite/accept` | `TeamInviteController` | |
| `/admin/revenue` (+ POST reconcile / win-back) | `AdminRevenueController` | `AdminAuthorizer` gated |

Services used by the chats workspace: `ConversationListService`, `ChatService`, `MailAccountService`,
`MailboxPollingService`, `ReplyService`, `OutboundMessageService`, `ConversationKeyService`, `OwnerAddressService`,
`BillingBannerService`, `UserService`.
