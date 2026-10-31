# WEB — Thymeleaf site (`src/main/resources/templates`)

Auth: **P** = public, **A** = authenticated session. CSRF token required on all POSTs
except `/billing/webhook`.

| Route | Auth | Template / result | Service(s) |
|---|---|---|---|
| `GET /`, `/pricing`, `/demo` | P | `landing`, `pricing`, demo chat | `PricingController`, `DemoConversationService` |
| `/privacy` `/terms` `/refund` | P | `legal` | `LegalProperties` |
| `/login` `/register` `/verify-email` `/password/*` | P | `login`, `register`, `verify-email`, `password/*` | `UserService`, `EmailVerificationService`, `PasswordResetService` |
| `GET /auth/google/start` | P | redirect to OIDC | `OAuthIntentStore` |
| `GET /chats` | A | `chat` (list, empty pane); params `page,q,mailbox,filter` | `ConversationListService`, `MailboxPollingService` |
| `GET /chats/{key}` | A | `chat` (list + open conversation; marks read) | `ChatService` |
| `GET /chats/{key}/pane` | A | HTML fragment `fragments/chat-pane :: pane` | `ChatService` |
| `GET/POST /chats/new` | A | `compose` | `ReplyService`, `OutboundMessageService` |
| `POST /chats/{key}/send` | A | multipart form → redirect | `ReplyService`, `OutboundMessageService` |
| `POST /chats/refresh` | A | JSON `{imported}` | `MailboxPollingService` |
| `GET /chats/events` | A | SSE | `ChatEventStream` |
| `/threads`, `/senders` | A | redirect to `/chats` | — |
| `/mailboxes` (+`/new`, `/{id}/sync|signature|delete`, `/preferences`) | A | `mailboxes/*` | `MailAccountService`, `PlanLimitService` |
| `/mailboxes/gmail/{connect,reconnect/{id},callback}` | A | OAuth redirects | `GmailOAuthClient` |
| `/searches` (POST, `/{id}/delete`) | A | redirect | `SavedSearchService` |
| `/account`, `/account/{notifications,appearance,privacy,password,email}` | A | `account*` | `AccountService` |
| `/billing/{checkout,portal,success,cancel,cancel-subscription}` | A (`cancel`,`webhook` P) | redirects / forms | `BillingService` |
| `/team/invite[/accept]` | A | `team/*` | team services |
| `/admin/revenue`, `/admin/retention/win-back` | A (admin) | `admin/*` | admin metrics |
| `/digest/opt-out` | P | token link | `DigestController` |
| PWA/SEO: `/manifest.webmanifest`, `/sw.js`, `/offline`, `/icons/*`, `/robots.txt`, `/sitemap.xml` | P | static-ish | `PwaController`, `SeoController` |

Shared fragments: `app-nav`, `settings-nav`, `chat-pane`, `live-refresh` (SSE-driven
in-place updates), `seo`, `cookie-banner`, `install-banner`, `sw-register`.
`NavModelAdvice` / `SiteModelAdvice` inject nav and site-wide model attributes.
