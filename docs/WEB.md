# Website (Thymeleaf)

Server-rendered pages of ConexusMail, reverse-engineered from the current
controllers and templates. Backend architecture is in
[BACKEND.md](BACKEND.md); the JSON API under `/api/v1` is in
[API.md](API.md) and is not covered here.

Conventions used below:

- **Auth** = `yes` means the website security chain (`auth/SecurityConfig`)
  requires a session; `no` means the path is in its `permitAll` list.
- All state-changing form posts need a CSRF token (hidden `_csrf` field, or
  the header named by the `_csrf_header` meta tag for `fetch`). Exceptions:
  `/billing/webhook` and `/h2-console/**`.
- Templates live in `src/main/resources/templates`; names are given without
  `.html`.
- Controllers resolve the current user with
  `UserService.requireByEmail(principal.getName())`; that call is omitted
  from the "Services" column.

## 1. Public paths

`SecurityConfig` permits without login: `/`, `/pricing`, `/demo`, `/login`,
`/register`, `/auth/google/start`, `/password/forgot`, `/password/reset`,
`/verify-email`, `/privacy`, `/terms`, `/refund`, `/billing/cancel`,
`/billing/webhook`, `/digest/opt-out`, `/robots.txt`, `/sitemap.xml`,
`/.well-known/**`, `/manifest.webmanifest`, `/icons/**`,
`/apple-touch-icon.png`, `/sw.js`, `/offline`, `/css/**`, `/js/**`,
`/images/**`, `/favicon.ico`, `/error`, `/h2-console/**`,
`/actuator/health`, `/actuator/health/**`.

Everything else requires authentication. Spring Security itself handles
`POST /login`, `POST /logout`, and (when Google is configured)
`/oauth2/authorization/google` and `/login/oauth2/code/google`.

## 2. Page inventory

### 2.1 Chats workspace (`web`)

| Route | Controller#method | Template | Auth | Services |
|---|---|---|---|---|
| GET `/chats` (`page`, `q`, `mailbox`, `filter`) | `ConversationController#list` | `chat` (empty pane) | yes | `MailboxPollingService.refreshDueForUserAsync`, `MailAccountService.list`, `ConversationListService.list` + `.counts`, `BillingBannerService` |
| GET `/chats/{key}` (`mailbox`) | `ConversationController#chat` | `chat` (pane filled); redirects to `/chats` if the key has no messages | yes | as above + `ChatService.markRead`, `ChatService.buildFor` |
| GET `/chats/new` (`mailbox`) | `ConversationController#compose` | `compose` | yes | `MailAccountService.list` |
| POST `/chats/new` (multipart: `to`, `subject`, `body`, `attachments`, `mailbox`) | `ConversationController#createMessage` | redirect `/chats/{key}`; re-renders `compose` on validation error | yes | `ConversationKeyService.keyFor`, `OwnerAddressService`, `ReplyService.sendNewEmail`, `OutboundMessageService.recordNewEmail` |
| POST `/chats/{key}/send` (multipart: `body`, `subject`, `asReply`, `attachments`, `mailbox`) | `ConversationController#send` | redirect `/chats/{key}`; re-renders `chat` on validation error | yes | `ChatService.buildFor`, `EmailThreadRepository.findThreadsByConversationKey`, `ReplyService.sendReply` or `.sendNewEmail`, `OutboundMessageService.recordReply` or `.recordNewEmail` |
| GET `/threads`, `/threads/**`, `/senders` | `ChatRedirectController#toChats` | redirect `/chats` | yes | none |

Model attributes set by the chats pages: `conversations`
(`Page<ConversationListItem>`), `chatCounts`, `activeFilter`, `isHub`,
`accountColors`, `fallbackAccount`, `searchQuery`, `billingBanner`,
`emailUnverified`, `navActiveMailboxId`, `selectedMailbox`, and, when a
conversation is open, `chat` (`ChatConversation`), `activeKey`, `replyForm`.
Flash attributes: `successMessage`, `sendError`.

The `mailbox` query parameter is a `mail_accounts.id`. It scopes the list and
conversation to that account only when the user has more than one connected
mailbox; an unknown id is ignored.

### 2.2 Mailboxes (`web`)

| Route | Controller#method | Template | Auth | Services |
|---|---|---|---|---|
| GET `/mailboxes` | `MailboxController#listMailboxes` | `mailboxes/index` | yes | `MailAccountService.list`, `PlanLimitService.currentPlan`, `PollingPolicy.intervalFor` |
| GET `/mailboxes/new` (`provider`) | `MailboxController#newMailbox` | `mailboxes/new` | yes | `MailboxProvider`, `GoogleOAuthProperties.isEnabled` |
| POST `/mailboxes` (`MailboxForm`) | `MailboxController#createMailbox` | redirect `/threads` (ok) or `/mailboxes` (initial sync error); re-renders `mailboxes/new` on error | yes | `MailAccountService.connect` |
| POST `/mailboxes/{id}/sync` | `MailboxController#syncMailbox` | redirect `/mailboxes` | yes | `MailAccountRepository.findByIdAndUser`, `MailboxPollingService.pollOne` |
| POST `/mailboxes/preferences` (`syncFrequency`, `defaultAccountId`) | `MailboxController#updatePreferences` | redirect `/mailboxes` | yes | `PollingPolicy`, `PlanLimitService`, `MailAccountService.updateMailPreferences` |
| POST `/mailboxes/{id}/signature` (`signature`) | `MailboxController#saveSignature` | redirect `/mailboxes` | yes | `MailAccountService.updateSignature` |
| POST `/mailboxes/{id}/delete` | `MailboxController#deleteMailbox` | redirect `/mailboxes` | yes | `MailAccountService.delete` |
| GET `/mailboxes/gmail/connect` | `GmailConnectController#connect` | redirect to Google | yes | `GmailOAuthClient.buildAuthorizationUrl` |
| GET `/mailboxes/gmail/reconnect/{id}` | `GmailConnectController#reconnect` | redirect to Google | yes | `MailAccountRepository.findByIdAndUser`, `GmailOAuthClient` |
| GET `/mailboxes/gmail/callback` (`code`, `state`, `error`) | `GmailConnectController#callback` | redirect `/threads`, `/mailboxes`, or `/mailboxes/new?provider=gmail` | yes | `GmailOAuthClient.exchangeCode` + `.fetchEmail`, `MailAccountService.connectGmailOAuth` or `.reconnectGmailOAuth`, `MailboxPollingService.pollOne` |

Flash attributes: `syncMessage`, `syncError` (rendered by
`mailboxes/index`), `connectError` (rendered by `mailboxes/new`). A foreign
or unknown mailbox id yields a 404 page.

### 2.3 Saved searches (`web`)

| Route | Controller#method | Template | Auth | Services |
|---|---|---|---|---|
| POST `/searches` (`name`, `q`, `from`, `since`, `unread`, `attachments`) | `SavedSearchController#save` | redirect `/chats?q=...` | yes | `SavedSearchService.create` |
| POST `/searches/{id}/delete` | `SavedSearchController#delete` | redirect `/chats?q=...` | yes | `SavedSearchService.delete` |

No current template posts to these routes or lists saved searches, and the
`savedSearchMessage` / `savedSearchError` flash attributes are not rendered.

### 2.4 Marketing, legal, demo (`web`)

| Route | Controller#method | Template | Auth | Services |
|---|---|---|---|---|
| GET `/` (`utm_source`, `demo`) | `PricingController#landing` | `landing`; redirect `/threads` if signed in; redirect `/demo` if `demo` is set | no | `LandingProperties` (`LandingVideo`) |
| GET `/pricing` (`utm_source`) | `PricingController#pricing` | `pricing` | no | none |
| GET `/demo` | `DemoController#demo` | `conversation` (`demoMode=true`) | no | `DemoConversationService.buildDemo` |
| GET `/privacy`, `/terms`, `/refund` | `LegalController#privacy` / `#terms` / `#refund` | `legal` | no | `LegalProperties`, `ResourceLoader` (bodies from `src/main/resources/legal/*.html` by default) |

### 2.5 Auth and account (`auth`)

| Route | Controller#method | Template | Auth | Services |
|---|---|---|---|---|
| GET `/login` (`plan`, `billing`, `utm_source`) | `AuthController#loginPage` | `login` | no | none |
| GET `/register` (`plan`, `billing`, `utm_source`) | `AuthController#registerPage` | `register` | no | none |
| POST `/register` (`RegistrationForm`, `plan`, `billing`) | `AuthController#register` | redirect `/threads` or Stripe Checkout; re-renders `register` on error | no | `UserService.register`, `EmailVerificationService.sendVerification`, `request.login`, `BillingService.startCheckout` |
| GET `/auth/google/start` (`plan`, `billing`, `utm_source`) | `OAuthStartController#startGoogle` | redirect `/oauth2/authorization/google` | no | `OAuthIntentStore.store` |
| GET `/password/forgot` | `PasswordResetController#forgotForm` | `password/forgot` | no | none |
| POST `/password/forgot` (`email`) | `PasswordResetController#requestReset` | `password/forgot` (`status` = `sent` or `google`) | no | `PasswordResetService.requestReset` |
| GET `/password/reset` (`token`) | `PasswordResetController#resetForm` | `password/reset` | no | `PasswordResetService.findUserForValidToken` |
| POST `/password/reset` (`token`, `password`) | `PasswordResetController#consumeReset` | redirect `/login?reset`; re-renders `password/reset` on error | no | `PasswordResetService.consumeToken` |
| GET `/verify-email` (`token`) | `EmailVerificationController#verify` | `verify-email` (`status` = `verified` or `invalid`) | no | `EmailVerificationService.verify` |
| POST `/verify-email/resend` | `EmailVerificationController#resend` | redirect `/threads` | yes | `EmailVerificationService.sendVerification` |
| GET `/account` | `AccountController#account` | `account` | yes | `SubscriptionRepository.findByUser`, `AccountBillingSummary`, `AuthEventService.recentFor` |
| GET `/account/notifications` | `AccountController#notifications` | `account-notifications` | yes | none (static page) |
| GET `/account/appearance` | `AccountController#appearance` | `account-appearance` | yes | `AppearanceSettings` (cookies) |
| POST `/account/appearance` (`theme`, `accent`) | `AccountController#saveAppearance` | redirect `/account/appearance` | yes | `AppearanceSettings`; writes `theme` and `accent` cookies (1 year) |
| GET `/account/privacy` | `AccountController#privacy` | `account-privacy` | yes | none (static page) |
| POST `/account/password` (`currentPassword`, `newPassword`) | `AccountController#changePassword` | redirect `/account` (flash `passwordOutcome`) | yes | `AccountService.changePassword` |
| POST `/account/email` (`currentPassword`, `newEmail`) | `AccountController#changeEmail` | redirect `/login?email-changed` (session invalidated) or `/account` (flash `emailOutcome`) | yes | `AccountService.changeEmail` |

`login` reads the query flags `error`, `error=google`, `error=locked`,
`error=oauth`, `logout`, `reset`, `email-changed` (exact rendering per flag:
TBD, not verified for each).

### 2.6 Billing (`billing`)

| Route | Controller#method | Template | Auth | Services |
|---|---|---|---|---|
| POST `/billing/checkout` (`plan`, `billing`) | `BillingController#startCheckout` | redirect to Stripe Checkout | yes | `BillingService.startCheckout` |
| POST `/billing/portal` | `BillingController#openPortal` | redirect to Stripe Billing Portal, or `/pricing` | yes | `BillingService.startPortal` |
| GET `/billing/success` (`session_id`) | `BillingController#success` | `billing/success` | yes | none |
| GET `/billing/cancel` | `BillingController#canceled` | redirect `/pricing?canceled=1` | no | none |
| GET `/billing/cancel-subscription` | `BillingController#cancelSubscription` | `billing/cancel-subscription`, or redirect `/pricing` when no Stripe customer | yes | `BillingService.hasManagedBilling` |
| POST `/billing/cancel-subscription` (`reason`) | `BillingController#submitCancelSubscription` | redirect to Stripe Billing Portal, or `/pricing` | yes | `BillingService.recordCancellationReason`, `.startPortal` |

The pricing page's Pro call-to-action is a link to `/register?plan=pro`; the
checkout redirect then happens in `AuthController#register` or
`PlanCheckoutSuccessHandler`. No template posts to `/billing/checkout`.

### 2.7 Admin (`admin`)

| Route | Controller#method | Template | Auth | Services |
|---|---|---|---|---|
| GET `/admin/revenue` | `AdminRevenueController#revenue` | `admin/revenue` | yes + allowlist | `AdminAuthorizer.requireAdmin`, `RevenueMetricsService`, `FunnelMetricsService`, `TrialEndConversionMetricsService`, `OnboardingFunnelMetricsService`, `ProAdoptionMetricsService`, `ChurnMetricsService`, `AtRiskRetentionService`, `WinBackConversionMetricsService` (`snapshot()` on each) |
| POST `/admin/revenue/reconcile-billing-period` | `AdminRevenueController#reconcileBillingPeriod` | redirect `/admin/revenue` (flash `reconcile`) | yes + allowlist | `BillingPeriodBackfillService.reconcile` |
| POST `/admin/retention/win-back` (`subscriptionId`) | `AdminRevenueController#sendWinBack` | redirect `/admin/revenue` (flash `winBack`) | yes + allowlist | `WinBackOutreachService.sendWinBackFor` |

A signed-in user who is not in `admin.emails` gets the 404 error page.

### 2.8 Digest and team (`digest`, `team`)

| Route | Controller#method | Template | Auth | Services |
|---|---|---|---|---|
| GET `/digest/opt-out` (`token`) | `DigestController#optOut` | `digest/opt-out` (`status` = `ok` or `invalid`) | no | `DigestEmailPreferenceRepository` (sets `opted_out`; a GET with a side effect) |
| GET `/team/invite` | `TeamInviteController#inviteForm` | `team/invite` | yes | `TeamService.findOrCreateOwnedTeam`, `TeamInviteRepository.findByTeamOrderByCreatedAtDesc` |
| POST `/team/invite` (`email`) | `TeamInviteController#sendInvite` | redirect `/team/invite` (flash `inviteFlash`) | yes | `TeamInviteService.invite` |
| GET `/team/invite/accept` (`token`) | `TeamInviteController#acceptForm` | `team/accept` (`status` = `ready`, `invalid`, `emailMismatch`) | yes | `TeamInviteService.findInviteForValidToken`, `UserService.findByEmail` |
| POST `/team/invite/accept` (`token`) | `TeamInviteController#acceptInvite` | redirect `/threads`; re-renders `team/accept` on mismatch/invalid | yes | `TeamInviteService.acceptInvite` |

No template links to `/team/invite`.

## 3. Non-page endpoints

### 3.1 Used by page JavaScript

| Endpoint | Handler | Response | Caller |
|---|---|---|---|
| POST `/chats/refresh` | `ConversationController#refresh` (`@ResponseBody`) | JSON `{"imported": <int>}`; synchronously polls the user's mailboxes not synced in the last minute (`MailboxPollingService.refreshActiveUserNow`) | `fragments/live-refresh`, every 60 s while the tab is visible; sends the CSRF header |
| GET `/chats/events` | `ChatEventsController#events` | `text/event-stream`; event `new-mail` with data `{"imported":<int>}`, comment `ping` keep-alives; 30 min emitter timeout | `fragments/live-refresh` via `EventSource` |
| GET `/chats/{key}/pane` (`mailbox`) | `ConversationController#chatPane` | HTML fragment `fragments/chat-pane :: pane`, or `:: missing` when the key has no messages; marks the conversation read | `chat` page script when a rail row is clicked or on `popstate` |
| GET `/chats`, GET `/chats/{key}` | page handlers | full HTML, parsed client-side | `chat` page `refreshInPlace()` (re-fetches the current URL and swaps `.chat-rail` and `#messages`) |
| POST `/account/appearance` | `AccountController#saveAppearance` | redirect (response ignored) | `account-appearance` auto-save on change |

Live-refresh flow in `chat`: SSE `new-mail` or a heartbeat with
`imported > 0` calls `window.ConexusLive.refresh()` (at most once per 4 s).
The rail is not replaced while the search box has focus; the reply draft is
never replaced; scroll position is kept unless the user was near the bottom.
Pages that include `live-refresh` without defining `ConexusLive` would
reload instead (only `chat` includes it today).

### 3.2 Machine endpoints

| Endpoint | Handler | Notes |
|---|---|---|
| POST `/billing/webhook` | `BillingWebhookController#handle` (`@RestController`) | Public, CSRF-exempt. Requires `Stripe-Signature`. Plain-text responses: `200 ok`; `400` missing/invalid signature; `503` webhook secret not configured; `500` apply failure. Delegates to `StripeWebhookGateway.parse` and `BillingService.applyStripeEvent`. |
| GET `/robots.txt` | `SeoController#robots` | Disallows `/threads`, `/mailboxes`, `/billing/`, `/actuator/`, `/h2-console/`; links the sitemap. Cached 1 h. |
| GET `/sitemap.xml` | `SeoController#sitemap` | `/`, `/pricing`, `/demo`, `/register`, `/login`, `/privacy`, `/terms`, `/refund`, built on `marketing.base-url`. |
| GET `/images/og-card.png` | `SeoController#ogCard` | 1200x630 PNG generated with Java2D. Cached 1 day. |
| GET `/actuator/health` | Spring Boot Actuator | Public. |
| `/api/v1/**` | `com.emailmessenger.api` | Separate stateless security chain; see API.md. |

## 4. Fragments and layout

There is no shared layout template. Each page is a full HTML document that
pulls in fragments with `th:replace`.

| Fragment (`templates/fragments/`) | Selector | Purpose | Included by |
|---|---|---|---|
| `app-nav` | `nav` | Left sidebar: brand, Dashboard link, connected mailboxes (`/chats?mailbox={id}`), Add mailbox, Pricing, Settings, Billing (POST `/billing/portal`, only when `navHasBilling`), profile name, Sign out | `chat`, `compose`, `account`, `account-notifications`, `account-appearance`, `account-privacy`, `mailboxes/index` |
| `settings-nav` | `nav(active)` | Secondary settings menu; `active` is one of `account`, `notifications`, `appearance`, `accounts`, `privacy` | `account`, `account-notifications`, `account-appearance`, `account-privacy`, `mailboxes/index` |
| `chat-pane` | `pane`, `missing` | Open conversation: header, trial banner, timeline, reply form (POST `/chats/{key}/send`), signature rail. `missing` is the placeholder for an unknown key | `chat` (when `chat != null`); returned directly by GET `/chats/{key}/pane` |
| `live-refresh` | `script` | SSE + heartbeat client (section 3.1). Requires `_csrf` and `_csrf_header` meta tags on the host page | `chat` |
| `seo` | `meta(pageTitle, pageDescription, pagePath)` | Title, description, canonical, Open Graph / Twitter tags, manifest and icon links; also pulls in `sw-register` | `landing`, `pricing`, `login`, `register`, `password/forgot`, `password/reset`, `verify-email`, `legal`, `conversation` |
| `sw-register` | `register` | Registers `/sw.js` (scope `/`) on load; on localhost it unregisters workers and deletes `conexusmail-*` caches instead | `chat`, `conversation`, and every page using `seo` |
| `cookie-banner` | `banner` | Cookie notice; dismissal stored in `localStorage` | `landing`, `pricing`, `login`, `register`, `password/forgot`, `password/reset`, `verify-email`, `legal`, `conversation` (demo) |
| `install-banner` | `banner` | PWA install prompt (`beforeinstallprompt` on Chromium, instructions on iOS Safari); dismissal stored in `localStorage` | `landing` |

Templates that include no fragment: `error`, `mailboxes/new`,
`billing/success`, `billing/cancel-subscription`, `digest/opt-out`,
`team/invite`, `team/accept`, `admin/revenue`.

`conversation` is rendered only by `/demo`. It still contains a reply form
that posts to `/threads/{id}/reply`, guarded by
`!demoMode and isThreadOwner`; no controller maps that route.

Theme: authenticated app pages set `data-theme` / `data-accent` on `<html>`
from `htmlTheme` / `htmlAccent` (see section 5).

## 5. Controller advice

All three are in `com.emailmessenger.web` and are not restricted to a base
package.

| Class | Type | Adds / handles |
|---|---|---|
| `NavModelAdvice` | `@ControllerAdvice`, one `@ModelAttribute` method | Always: `navActive` (`chats`, `inbox`, `settings`, `pricing`, or empty, derived from the request URI), `htmlTheme`, `htmlAccent` (from the `theme` / `accent` cookies via `AppearanceSettings`). When signed in: `navProfileName` (display name or email), `navMailboxes` (`List<NavMailbox(id, address)>`), `navHasBilling` (`BillingService.hasManagedBilling`). `navActiveMailboxId` is not set here; `ConversationController` sets it. |
| `SiteModelAdvice` | `@ControllerAdvice`, two `@ModelAttribute` methods | `siteBaseUrl` (`marketing.base-url`, used by `fragments/seo`), `oauthGoogleEnabled` (true when a `google` client registration exists; used by `login`, `register`, `password/forgot`) |
| `GlobalExceptionHandler` | `@ControllerAdvice` with `@ExceptionHandler`s | Renders `error` with model `status` and `message` (table below) |

`GlobalExceptionHandler` mappings:

| Exception | Status | Result |
|---|---|---|
| `NoResourceFoundException` | 404 | `error` |
| `NoSuchElementException` | 404 | `error` (also used for foreign ids and non-admins) |
| `MailException`, `EmailImportException` | 502 | `error` |
| `BillingException` | 502 | `error` |
| `PlanLimitExceededException` | 302 | redirect `/threads` with flash `upgradeModal` (`billing.UpgradeModal`) |
| `MaxUploadSizeExceededException` | 413 | `error` |
| `IllegalArgumentException` | 400 | `error` |
| `DataIntegrityViolationException` | 409 | `error` |
| any other `Exception` | 500 | `error` |

`/api/**` errors are handled by `api.ApiExceptionHandler` instead (scoped
to the `api` package, highest precedence).

## 6. PWA

All served by `web/PwaController`; nothing is a static file.

| Route | Content | Caching |
|---|---|---|
| GET `/manifest.webmanifest` | `application/manifest+json`: name "ConexusMail — Your inbox, as a chat", `start_url` `/chats`, `scope` `/`, `display` `standalone`, portrait, theme `#2f855a`, background `#0f1512`, three icons | public, 1 h |
| GET `/icons/icon-192.png`, `/icons/icon-512.png`, `/icons/icon-512-maskable.png`, `/apple-touch-icon.png` (180 px) | PNG brand icons drawn with Java2D at request time | public, 7 days |
| GET `/sw.js` | Service worker source, header `Service-Worker-Allowed: /` | `no-store` |
| GET `/offline` | Self-contained offline HTML page (inline string, not a template) | public, 5 min |

Service worker behaviour:

- Install: pre-caches the shell assets `/offline`, `/css/main.css`,
  `/icons/icon-192.png`, `/manifest.webmanifest`, then `skipWaiting`.
- Activate: deletes every cache except the current version, then claims
  clients.
- Fetch: GET navigations go network-first and fall back to the cached
  `/offline`; same-origin shell assets are cache-first; everything else is
  untouched. No push or background-sync handlers exist.
- Cache name is `conexusmail-shell-<hash>`, derived from the worker
  template, the asset list, and a fingerprint of `static/css/main.css`.

Registration is done by `fragments/sw-register` (section 4). The install
prompt is `fragments/install-banner`, included only on the landing page.

## 7. Static assets

```
src/main/resources/
├── static/
│   └── css/
│       └── main.css        # single stylesheet for every page (~112 KB)
├── legal/                  # not web-served; bodies injected into the `legal` template
│   ├── privacy.html
│   ├── refund.html
│   └── terms.html
└── templates/              # see section 2 and 4
```

- There is no `static/js` directory; all JavaScript is inline in templates
  and fragments. `/js/**` is permitted by the security chain but serves
  nothing.
- There is no `static/images` directory or `favicon.ico`; `/images/og-card.png`
  and all icons are generated by controllers.
- Open-pane behaviour, time localisation and in-place refresh are in the
  inline script at the end of `chat`.
