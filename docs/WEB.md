# WEB.md — Thymeleaf website

Templates: `src/main/resources/templates`. Shared fragments in `templates/fragments` (`app-nav`, `settings-nav`,
`chat-pane`, `live-refresh`, `cookie-banner`, `install-banner`, `seo`, `sw-register`).

| Route | Auth | Template / result | Main collaborators |
|---|---|---|---|
| `GET /` | public | `landing` | `LandingProperties` |
| `GET /pricing`, `/demo` | public | `pricing`, demo chat | `PricingController`, `DemoConversationService` |
| `GET/POST /login`, `/register`, `/auth/google/start`, `/logout` | public | `login`, `register` | `SecurityConfig`, `UserService` |
| `/password/forgot`, `/password/reset` | public | `password/*` | `PasswordResetService` |
| `/verify-email`, `POST /verify-email/resend` | token / auth | `verify-email` | `EmailVerificationService` |
| `/privacy`, `/terms`, `/refund` | public | `legal` | `LegalProperties` |
| `/chats` (params `page,q,mailbox,filter`) | auth | `chat` | `ConversationListService`, `MailboxPollingService` |
| `/chats/new` GET/POST | auth | `compose` | `ReplyService`, `OutboundMessageService` |
| `/chats/{key}`, `/chats/{key}/pane` | auth | `chat`, `fragments/chat-pane` | `ChatService` |
| `POST /chats/{key}/send`, `POST /chats/refresh`, `GET /chats/events` | auth | redirect / JSON / SSE | `ReplyService`, `ChatEventStream` |
| `/threads`, `/threads/**`, `/senders` | auth | redirect to `/chats` | `ChatRedirectController` |
| `/mailboxes`, `/mailboxes/new`, `POST /mailboxes`, `/{id}/sync`, `/{id}/signature`, `/{id}/delete`, `/preferences` | auth | `mailboxes/*` | `MailAccountService` |
| `/mailboxes/gmail/connect|reconnect/{id}|callback` | auth | redirect (OAuth) | `GmailOAuthClient` |
| `/searches/{id}/delete` | auth | redirect | `SavedSearchService` |
| `/account`, `/account/{notifications,appearance,privacy}`, `POST /account/{password,email,appearance}` | auth | `account*` | `AccountService` |
| `POST /billing/checkout`, `/billing/portal`, `/billing/success`, `/billing/cancel`, `/billing/cancel-subscription` | mixed | redirects / `billing/*` | `BillingService` |
| `/team/invite`, `/team/invite/accept` | auth | `team/*` | `TeamInviteService` |
| `/digest/opt-out` | public (token) | `digest/opt-out` | `WeeklyDigestService` |
| `/admin/revenue`, `POST /admin/retention/win-back`, `/admin/revenue/reconcile-billing-period` | admin | `admin/revenue` | admin metrics services |
| `/manifest.webmanifest`, `/sw.js`, `/offline`, `/icons/*`, `/robots.txt`, `/sitemap.xml` | public | PWA/SEO | `PwaController`, `SeoController` |
