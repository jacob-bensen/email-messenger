# Thymeleaf website

Templates: `src/main/resources/templates`. Shared fragments in `fragments/`
(`app-nav`, `settings-nav`, `chat-pane`, `live-refresh`, `seo`, `cookie-banner`,
`install-banner`, `sw-register`). `@ControllerAdvice`: `NavModelAdvice`,
`SiteModelAdvice`, `GlobalExceptionHandler`.

| Route | Auth | Controller | Template |
|---|---|---|---|
| `GET /`, `/pricing` | public | `PricingController` | landing, pricing |
| `GET /demo` | public | `DemoController` | chat (demo data) |
| `GET /privacy /terms /refund` | public | `LegalController` | legal |
| `GET/POST /login`, `/register` | public | `AuthController` | login, register |
| `GET /auth/google/start` | public | `OAuthStartController` | – |
| `/password/forgot`, `/password/reset` | public | `PasswordResetController` | password/* |
| `GET /verify-email`, `POST /verify-email/resend` | mixed | `EmailVerificationController` | verify-email |
| `GET /chats` | user | `ConversationController` | chat |
| `GET/POST /chats/new` | user | `ConversationController` | compose |
| `GET /chats/{key}`, `/chats/{key}/pane` | user | `ConversationController` | chat, fragments/chat-pane |
| `POST /chats/{key}/send` | user | `ConversationController` | redirect |
| `POST /chats/refresh` | user | `ConversationController` | JSON `{"imported":n}` |
| `GET /chats/events` | user | `ChatEventsController` | SSE |
| `/mailboxes`, `/mailboxes/new`, `/{id}/sync`, `/{id}/signature`, `/{id}/delete`, `/preferences` | user | `MailboxController` | mailboxes/* |
| `/mailboxes/gmail/connect`, `/reconnect/{id}`, `/callback` | user | `GmailConnectController` | – |
| `/searches`, `/searches/{id}/delete` | user | `SavedSearchController` | – |
| `/account`, `/account/notifications|appearance|privacy|password|email` | user | `AccountController` | account-* |
| `/billing/checkout|portal|success|cancel|cancel-subscription` | mixed | `BillingController` | billing/* |
| `POST /billing/webhook` | public (Stripe sig) | `BillingWebhookController` | JSON |
| `/team/invite`, `/team/invite/accept` | user | `TeamInviteController` | team/* |
| `/digest/opt-out` | public | `DigestController` | digest/opt-out |
| `/admin/revenue` + POSTs | admin | `AdminRevenueController` | admin/revenue |
| `/manifest.webmanifest`, `/sw.js`, `/offline`, `/icons/*` | public | `PwaController` | PWA shell |
| `/robots.txt`, `/sitemap.xml`, `/images/og-card.png` | public | `SeoController` | – |
| `/threads`, `/threads/**`, `/senders` | user | `ChatRedirectController` | redirect → /chats |

Services used by `/chats*`: `ConversationListService`, `ChatService`,
`ReplyService`, `OutboundMessageService`, `MailboxPollingService`,
`MailAccountService`, `ConversationKeyService`, `BillingBannerService`.
