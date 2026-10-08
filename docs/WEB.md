# Thymeleaf website

Templates: `src/main/resources/templates`. Shared fragments in `fragments/`:
`app-nav`, `settings-nav`, `chat-pane`, `live-refresh`, `seo`, `cookie-banner`,
`install-banner`, `sw-register`.

## Pages / routes
| Route | Template / handler | Auth | Main services |
|---|---|---|---|
| `GET /`, `/pricing` | landing, pricing (`PricingController`) | public | plan pricing |
| `GET /demo` | demo chat (`DemoController`) | public | `DemoConversationService` |
| `GET /privacy /terms /refund` | legal | public | `LegalProperties` |
| `GET/POST /login`, `/register`, `/logout` | login, register | public | `UserService` |
| `GET /auth/google/start` | OAuth start | public | `OAuthIntentStore` |
| `/password/forgot`, `/password/reset` | password/* | public | `PasswordResetService` |
| `/verify-email`, `POST /verify-email/resend` | verify-email | public / authed | `EmailVerificationService` |
| `GET /chats` | chat (list + empty pane) | user | `ConversationListService`, `MailboxPollingService` |
| `GET /chats/new`, `POST /chats/new` | compose | user | `OutboundMessageService` |
| `POST /chats/refresh` | sync trigger | user | `MailboxPollingService` |
| `GET /chats/{key}` | chat (open conversation, marks read) | user | `ChatService` |
| `GET /chats/{key}/pane` | `fragments/chat-pane` | user | `ChatService` |
| `POST /chats/{key}/send` | reply (multipart; `asReply`, `subject`, `attachments`) | user | `ReplyService`/`OutboundMessageService` |
| `GET /chats/events` | SSE new-mail stream | user | `ChatEventStream` |
| `/threads`, `/threads/**`, `/senders` | redirect to `/chats` | user | — |
| `/mailboxes` (+`/new`, `POST /`, `/{id}/sync`, `/preferences`, `/{id}/signature`, `/{id}/delete`) | mailboxes/* | user | `MailAccountService` |
| `/mailboxes/gmail/connect`, `/reconnect/{id}`, `/callback` | Gmail OAuth | user | `GmailOAuthClient` |
| `POST /searches`, `POST /searches/{id}/delete` | saved searches | user | `SavedSearchService` |
| `/account`, `/account/notifications`, `/account/appearance`, `/account/privacy`, `POST /account/password`, `POST /account/email` | account-* | user | `AccountService` |
| `POST /billing/checkout`, `/billing/portal`, `GET /billing/success`, `/billing/cancel`, `/billing/cancel-subscription` (GET+POST) | billing/* | user (`cancel` public) | `BillingService` |
| `POST /billing/webhook` | Stripe webhook | public, signature-verified | `BillingService` |
| `/team/invite`, `/team/invite/accept` (GET+POST) | team/* | user | `TeamInviteService` |
| `GET /digest/opt-out` | digest/opt-out | public (token) | `DigestController` |
| `/admin/revenue`, `POST /admin/revenue/reconcile-billing-period`, `POST /admin/retention/win-back` | admin/revenue | admin | `*MetricsService` |
| `/manifest.webmanifest`, `/sw.js`, `/offline`, icons | PWA | public | `PwaController` |
| `/robots.txt`, `/sitemap.xml`, `/images/og-card.png` | SEO | public | `SeoController` |
