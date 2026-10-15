# WEB — Thymeleaf website

Templates: `src/main/resources/templates`. Fragments in `fragments/`
(`app-nav`, `settings-nav`, `chat-pane`, `live-refresh`, `seo`, `cookie-banner`,
`install-banner`, `sw-register`).

| Route | Method | Template / result | Controller → services |
|---|---|---|---|
| `/` | GET | `landing` | PricingController |
| `/pricing` | GET | `pricing` | PricingController |
| `/demo` | GET | demo chat | DemoController → DemoConversationService |
| `/privacy` `/terms` `/refund` | GET | `legal` | LegalController |
| `/login` | GET (POST by Spring Security) | `login` | AuthController |
| `/register` | GET, POST | `register` | AuthController → UserService |
| `/auth/google/start` | GET | OIDC redirect | OAuthStartController |
| `/verify-email`, `/verify-email/resend` | GET, POST | `verify-email` | EmailVerificationController |
| `/password/forgot`, `/password/reset` | GET, POST | `password/*` | PasswordResetController |
| `/account`, `/account/notifications`, `/account/appearance`, `/account/privacy` | GET | `account*` | AccountController |
| `/account/appearance`, `/account/password`, `/account/email` | POST | redirect | AccountController |
| `/chats` (`page,q,mailbox,filter`) | GET | `chat` (rail + pane) | ConversationController → ChatService, ConversationListService |
| `/chats/new` | GET, POST | `compose` | ConversationController → OutboundMessageService |
| `/chats/refresh` | POST | redirect | ConversationController → MailboxPollingService |
| `/chats/{key}` | GET | `chat` | ConversationController |
| `/chats/{key}/pane` | GET | `fragments/chat-pane` (HTML fragment) | ConversationController |
| `/chats/{key}/send` | POST multipart | redirect | ConversationController → ReplyService |
| `/chats/events` | GET | SSE `NewMailEvent` | ChatEventsController |
| `/threads`, `/senders` | GET | redirect to /chats | ChatRedirectController |
| `/mailboxes`, `/mailboxes/new` | GET | `mailboxes/*` | MailboxController |
| `/mailboxes` , `/{id}/sync`, `/preferences`, `/{id}/signature`, `/{id}/delete` | POST | redirect | MailboxController → MailAccountService |
| `/mailboxes/gmail/connect`, `/reconnect/{id}`, `/callback` | GET | OAuth flow | GmailConnectController |
| `/searches`, `/searches/{id}/delete` | POST | redirect | SavedSearchController |
| `/billing/checkout`, `/portal`, `/cancel-subscription` | POST (+GET page) | Stripe redirect | BillingController → BillingService |
| `/billing/success`, `/billing/cancel` | GET | `billing/*` | BillingController |
| `/billing/webhook` | POST | Stripe webhook (no CSRF) | BillingWebhookController |
| `/team/invite`, `/team/invite/accept` | GET, POST | `team/*` | TeamInviteController |
| `/digest/opt-out` | GET | `digest/opt-out` | DigestController |
| `/admin/revenue` (+2 POSTs) | GET/POST | `admin/revenue` | AdminRevenueController |
| `/manifest.webmanifest`, `/sw.js`, `/offline`, icons | GET | PWA | PwaController |
| `/robots.txt`, `/sitemap.xml`, `/images/og-card.png` | GET | SEO | SeoController |
