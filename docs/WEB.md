# Website (Thymeleaf) pages

Server-rendered HTML at the same host as (future) `/api/v1`. Every page
handler is a Spring `@Controller` returning a view name; all views live in
`src/main/resources/templates/`.

**Rule:** page controllers must delegate business logic to a class in
`service/` so the REST API can call the same code. Never inline logic that
`/api/v1` would need to re-implement.

## Public

| Route | Controller | Template | Purpose |
|---|---|---|---|
| `GET /` | `PricingController#landing` | `landing.html` | Marketing landing page |
| `GET /pricing` | `PricingController#pricing` | `pricing.html` | Plan comparison + checkout entry |
| `GET /demo` | `DemoController#demo` | (chat) | Interactive demo w/ seeded conversation |
| `GET /login` | `AuthController#loginPage` | `login.html` | Form + Google login |
| `GET /register` | `AuthController#registerPage` | `register.html` | Sign-up |
| `POST /register` | `AuthController#register` | – | Create user |
| `GET /verify-email` | `EmailVerificationController#verify` | `verify-email.html` | Verify via token |
| `POST /verify-email/resend` | `EmailVerificationController#resend` | – | Resend link |
| `GET /password/forgot` | `PasswordResetController#forgotPage` | `password/forgot.html` | Password reset request |
| `POST /password/forgot` | `PasswordResetController#request` | – | Send reset email |
| `GET /password/reset` | `PasswordResetController#resetPage` | `password/reset.html` | Reset via token |
| `POST /password/reset` | `PasswordResetController#reset` | – | Apply new password |
| `GET /auth/google/start` | `OAuthStartController` | – | Kick off OIDC |
| `GET /privacy`, `/terms`, `/refund` | `LegalController` | `legal.html` | Legal pages |
| `GET /robots.txt`, `/sitemap.xml`, `/images/og-card.png` | `SeoController` | – | SEO assets |
| `GET /manifest.webmanifest`, `/sw.js`, `/offline`, `/icons/*.png`, `/apple-touch-icon.png` | `PwaController` | – | PWA shell |

## Authenticated app

| Route | Controller | Template | Purpose |
|---|---|---|---|
| `GET /chats` | `ConversationController#list` | `chat.html` | Chats rail + open pane |
| `GET /chats/new` | `ConversationController#compose` | `compose.html` | Compose form |
| `POST /chats/new` | `ConversationController#createNew` | – | Send a first message |
| `POST /chats/refresh` | `ConversationController#refresh` | – | Trigger polling now |
| `GET /chats/{key}` | `ConversationController#chat` | `chat.html` | Chat detail |
| `GET /chats/{key}/pane` | `ConversationController#pane` | fragment | Pane fragment for live morph |
| `POST /chats/{key}/send` | `ConversationController#send` | – | Reply in a chat |
| `GET /chats/events` | `ChatEventsController#stream` | – | SSE: `new-mail`, `chat-updated` |
| `GET /threads`, `/senders` | `ChatRedirectController` | – | Legacy redirects → `/chats` |
| `GET /mailboxes` | `MailboxController#list` | `mailboxes/list.html` | Manage mailboxes |
| `GET /mailboxes/new` | `MailboxController#newForm` | `mailboxes/new.html` | Add mailbox form |
| `POST /mailboxes` | `MailboxController#create` | – | Save IMAP mailbox |
| `POST /mailboxes/{id}/sync` | `MailboxController#sync` | – | Poll now |
| `POST /mailboxes/preferences` | `MailboxController#preferences` | – | Save mailbox prefs |
| `POST /mailboxes/{id}/signature` | `MailboxController#signature` | – | Save signature |
| `POST /mailboxes/{id}/delete` | `MailboxController#delete` | – | Remove mailbox |
| `GET /mailboxes/gmail/connect` | `GmailConnectController` | – | Start Gmail OAuth |
| `GET /mailboxes/gmail/reconnect/{id}` | `GmailConnectController` | – | Reconnect (Testing-mode token) |
| `GET /mailboxes/gmail/callback` | `GmailConnectController` | – | OAuth callback |
| `GET /account`, `/account/notifications`, `/account/appearance`, `/account/privacy` | `AccountController` | `account*.html` | Settings |
| `POST /account/password`, `/account/email`, `/account/appearance` | `AccountController` | – | Save settings |
| `POST /searches`, `POST /searches/{id}/delete` | `SavedSearchController` | – | Saved-search CRUD |
| `GET /digest/opt-out` | `DigestController` | `digest/*.html` | Digest opt-out |
| `POST /team/invite`, `GET /team/invite/accept`, `POST /team/invite/accept` | `TeamInviteController` | `team/*.html` | Team invites |
| `POST /billing/checkout`, `POST /billing/portal`, `GET /billing/success`, `GET /billing/cancel`, `POST /billing/cancel-subscription` | `BillingController` | – | Stripe flow |
| `POST /billing/webhook` | `BillingWebhookController` | – | Stripe webhook (only `@RestController`) |
| `GET /admin/revenue`, `POST /admin/revenue/reconcile-billing-period`, `POST /admin/retention/win-back` | `AdminRevenueController` | `admin/*.html` | Admin console |

## Shared fragments

Under `templates/fragments/`. `live-refresh.html` wires SSE morph into
the chat pane. `nav.html` / model advice classes (`NavModelAdvice`,
`SiteModelAdvice`) inject common view models.
