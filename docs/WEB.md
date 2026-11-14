# ConexusMail website (Thymeleaf)

Server-rendered pages under `src/main/resources/templates`. No layout
dialect: pages compose shared pieces with `th:replace`. One stylesheet,
`static/css/main.css`; page scripts are inline in templates.

"Auth" below: **public**, **user** (signed in; otherwise 302 → `/login`), or
**admin** (email in `admin.emails`; everyone else gets 404).

## Page inventory

### Marketing, legal, SEO (public)
| Route | Template | Controller → services |
|---|---|---|
| `GET /` | `landing` | `PricingController` → `LandingProperties`. Signed-in users are redirected to `/threads` (→ `/chats`). `?demo=` redirects to `/demo`. |
| `GET /pricing` | `pricing` | `PricingController` |
| `GET /demo` | `conversation` | `DemoController` → `DemoConversationService.buildDemo` |
| `GET /privacy`, `/terms`, `/refund` | `legal` | `LegalController` → `LegalProperties` (content from `resources/legal/`, overridable) |
| `GET /robots.txt`, `/sitemap.xml`, `/images/og-card.png` | generated | `SeoController` |
| `GET /manifest.webmanifest`, `/sw.js`, `/offline`, `/icons/*.png`, `/apple-touch-icon.png` | generated | `PwaController` |

### Sign-in and account
| Route | Auth | Template | Controller → services |
|---|---|---|---|
| `GET /login`, `POST /login` | public | `login` | `AuthController`; POST is Spring Security form login (`DbUserDetailsService`, `LoginThrottleFilter`, `PlanCheckoutSuccessHandler`, `LoginFailureHandler`) |
| `POST /logout` | — | redirect `/login?logout` | Spring Security |
| `GET /register`, `POST /register` | public | `register` | `AuthController` → `UserService.register`, `EmailVerificationService.sendVerification`, optional `BillingService.startCheckout` |
| `GET /auth/google/start` | public | redirect | `OAuthStartController` → `OAuthIntentStore`; then Spring OIDC (`GoogleOidcUserService` → `OAuth2ProvisioningService`) |
| `GET/POST /password/forgot`, `GET/POST /password/reset` | public (token) | `password/forgot`, `password/reset` | `PasswordResetController` → `PasswordResetService` |
| `GET /verify-email` | public (token) | `verify-email` | `EmailVerificationController` → `EmailVerificationService.verify` |
| `POST /verify-email/resend` | user | redirect | `EmailVerificationService.sendVerification` |
| `GET /account` | user | `account` | `AccountController` → `SubscriptionRepository`, `AuthEventService.recentFor` |
| `POST /account/password`, `POST /account/email` | user | redirect | `AccountService.changePassword` / `changeEmail` |
| `GET/POST /account/appearance` | user | `account-appearance` | `AppearanceSettings` (theme + accent cookies) |
| `GET /account/notifications` | user | `account-notifications` | placeholder ("coming soon") |
| `GET /account/privacy` | user | `account-privacy` | — |

### Chats (the product)
| Route | Auth | Template | Controller → services |
|---|---|---|---|
| `GET /chats` | user | `chat` (empty pane) | `ConversationController#list` → `ConversationListService.list` / `counts`, `MailAccountService.list`, `MailboxPollingService.refreshDueForUserAsync`, `BillingBannerService`. Params: `page`, `q`, `mailbox`, `filter`. Page size 30. |
| `GET /chats/{key}` | user | `chat` | `#chat` → `ChatService.markRead` + `buildFor`. Unknown key → `/chats`. |
| `GET /chats/{key}/pane` | user | fragment `chat-pane :: pane` / `:: missing` | `#chatPane` (same services) |
| `POST /chats/{key}/send` | user | redirect | `#send` → `ReplyService.sendReply` / `sendNewEmail`, then `OutboundMessageService.recordReply` / `recordNewEmail`. Multipart: `body`, `subject`, `asReply`, `attachments`, `mailbox`. |
| `GET /chats/new`, `POST /chats/new` | user | `compose` | `#compose`, `#createMessage` → `ConversationKeyService.keyFor`, `ReplyService.sendNewEmail`, `OutboundMessageService.recordNewEmail` |
| `POST /chats/refresh`, `GET /chats/events` | user | JSON / SSE | see `docs/API.md` §3 |
| `GET /threads`, `/threads/**`, `/senders` | user | redirect `/chats` | `ChatRedirectController` (legacy URLs) |
| `POST /searches`, `POST /searches/{id}/delete` | user | redirect `/chats` | `SavedSearchController` → `SavedSearchService` (no page currently posts here) |

### Mailboxes
| Route | Auth | Template | Controller → services |
|---|---|---|---|
| `GET /mailboxes` | user | `mailboxes/index` | `MailboxController` → `MailAccountService.list`, `PollingPolicy`, `PlanLimitService.currentPlan` |
| `GET /mailboxes/new` | user | `mailboxes/new` | `MailboxProvider.fromSlug` |
| `POST /mailboxes` | user | redirect / re-render | `MailAccountService.connect` |
| `POST /mailboxes/{id}/sync` | owner | redirect | `MailboxPollingService.pollOne` (after an owner-scoped lookup) |
| `POST /mailboxes/preferences` | user | redirect | `MailAccountService.updateMailPreferences` |
| `POST /mailboxes/{id}/signature` | owner | redirect | `MailAccountService.updateSignature` |
| `POST /mailboxes/{id}/delete` | owner | redirect | `MailAccountService.delete` |
| `GET /mailboxes/gmail/connect`, `/reconnect/{id}`, `/callback` | user | redirects | `GmailConnectController` → `GmailOAuthClient`, `MailAccountService.connectGmailOAuth` / `reconnectGmailOAuth` |

### Billing, team, digest, admin
| Route | Auth | Template | Controller → services |
|---|---|---|---|
| `POST /billing/checkout`, `POST /billing/portal` | user | redirect to Stripe | `BillingService.startCheckout` / `startPortal` |
| `GET /billing/success` | user | `billing/success` | — |
| `GET /billing/cancel` | public | redirect `/pricing?canceled=1` | — |
| `GET/POST /billing/cancel-subscription` | user | `billing/cancel-subscription` | `BillingService.hasManagedBilling`, `recordCancellationReason`, `startPortal` |
| `POST /billing/webhook` | Stripe signature | text | see `docs/API.md` §3 |
| `GET/POST /team/invite` | user | `team/invite` | `TeamService.findOrCreateOwnedTeam`, `TeamInviteService.invite` |
| `GET/POST /team/invite/accept` | user + token | `team/accept` | `TeamInviteService.findInviteForValidToken` / `acceptInvite` |
| `GET /digest/opt-out` | public (token) | `digest/opt-out` | `DigestController` → `DigestEmailPreferenceRepository` directly |
| `GET /admin/revenue` | admin | `admin/revenue` | `AdminRevenueController` → the eight `*MetricsService.snapshot()` services |
| `POST /admin/revenue/reconcile-billing-period`, `POST /admin/retention/win-back` | admin | redirect | `BillingPeriodBackfillService.reconcile`, `WinBackOutreachService.sendWinBackFor` |

Errors render `error.html` via `GlobalExceptionHandler` (statuses listed in
`docs/API.md` §3). `PlanLimitExceededException` instead flashes an
`upgradeModal` and redirects to `/threads`.

## Shared fragments (`templates/fragments/`)
| Fragment | Used by |
|---|---|
| `app-nav :: nav` (sidebar; billing-portal and logout forms) | `chat`, `compose`, `account*`, `mailboxes/index` |
| `settings-nav :: nav(active)` | `account*`, `mailboxes/index` |
| `chat-pane :: pane` / `:: missing` (conversation + send form) | `chat`; returned directly by `GET /chats/{key}/pane` |
| `live-refresh :: script` (SSE + 60 s heartbeat) | `chat` |
| `seo :: meta(...)` (includes `sw-register`) | all public pages, `conversation` |
| `sw-register :: register` | `chat`, `conversation`, `seo` |
| `cookie-banner :: banner` | all public pages |
| `install-banner :: banner` | `landing` |

Global model attributes: `NavModelAdvice` (`navActive`, `htmlTheme`,
`htmlAccent`, `navProfileName`, `navMailboxes`, `navHasBilling`) and
`SiteModelAdvice` (`siteBaseUrl`, `oauthGoogleEnabled`).

## Relationship to the REST API
There is no REST API yet (`docs/API.md` §1). When `/api/v1` is built, its
controllers must call the same services these pages call. Today some rules
live in web controllers rather than services — most importantly the whole
send flow in `ConversationController` — and must be extracted before an API
controller can share them. The list is in `INTERNAL_TODO.md`.
