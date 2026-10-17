# WEB — Thymeleaf website

Templates: `src/main/resources/templates`. Shared fragments in `templates/fragments`: `app-nav`, `settings-nav`,
`chat-pane` (open conversation, also returned alone by `/chats/{key}/pane`), `live-refresh` (SSE client),
`seo`, `cookie-banner`, `install-banner`, `sw-register`.

## Page inventory
| Route | Auth | Controller → template | Services |
|---|---|---|---|
| `/`, `/pricing`, `/demo` | public | PricingController, DemoController → landing, pricing | DemoConversationService |
| `/login`, `/register` | public | AuthController | UserService |
| `/auth/google/start` | public | OAuthStartController | OAuthIntentStore |
| `/password/forgot`, `/password/reset` | public | PasswordResetController | PasswordResetService |
| `/verify-email`, `POST /verify-email/resend` | public / user | EmailVerificationController | EmailVerificationService |
| `/privacy`, `/terms`, `/refund` | public | LegalController → legal | — |
| `/robots.txt`, `/sitemap.xml`, `/images/og-card.png` | public | SeoController | — |
| `/manifest.webmanifest`, `/sw.js`, `/offline`, `/icons/*` | public | PwaController | — |
| `/chats` | user | ConversationController → chat | ConversationListService, ChatService |
| `/chats/{key}`, `/chats/{key}/pane` | user | ConversationController → chat / chat-pane | ChatService |
| `POST /chats/{key}/send`, `/chats/new` | user | ConversationController | OutboundMessageService, ReplyService, PlanLimitService |
| `POST /chats/refresh` | user | ConversationController (JSON) | MailboxPollingService |
| `/chats/events` (SSE) | user | ChatEventsController | ChatEventStream |
| `/mailboxes`, `/mailboxes/new`, `/{id}/sync`, `/{id}/delete`, `/{id}/signature`, `/preferences` | user | MailboxController → mailboxes/* | MailAccountService, MailboxPollingService |
| `/mailboxes/gmail/connect`, `/reconnect/{id}`, `/callback` | user | GmailConnectController | GmailOAuthClient |
| `/searches/{id}/delete` | user | SavedSearchController | SavedSearchService |
| `/account`, `/account/{notifications,appearance,privacy,password,email}` | user | AccountController | AccountService |
| `/billing/checkout`, `/portal`, `/success`, `/cancel`, `/cancel-subscription` | user (cancel public) | BillingController | BillingService |
| `/team/invite`, `/team/invite/accept` | user | TeamInviteController | TeamInviteService |
| `/digest/opt-out` | public | DigestController | — |
| `/admin/revenue`, `POST /admin/revenue/reconcile-billing-period`, `POST /admin/retention/win-back` | admin | AdminRevenueController | — |
| `POST /billing/webhook` | public (Stripe signature) | BillingWebhookController | BillingService |

Legacy `/threads`, `/threads/**`, `/senders` redirect to `/chats` (ChatRedirectController).
