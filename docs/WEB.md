# WEB.md — Thymeleaf website inventory

> Page-level map of the server-rendered site. Each row lists the route,
> the template, the controller method, and the services the controller
> delegates to (so any future duplication of business logic in a REST
> controller can be caught by grepping for the same service).
>
> **Rule:** every page must go through the service layer — never
> `@Repository` or ad-hoc business logic inside a controller. If a
> service method is missing, add it. Both this website and the future
> mobile API must call the same service.

## Layout & fragments

- Shared shell: `templates/fragments/*` — nav, footer, live-refresh,
  chat pane, billing banner, mailbox chips.
- Templates root: `src/main/resources/templates/`.
- Static assets: `src/main/resources/static/` (`css/`, `js/`,
  `images/`).

## Marketing / landing / legal

| Route | Template | Controller | Notes |
| --- | --- | --- | --- |
| `GET /` | `landing` | `PricingController#landing` | Redirects to `/threads` if authed, or `/demo` with `?demo`. |
| `GET /pricing` | `pricing` | `PricingController#pricing` | |
| `GET /demo` | `conversation` | `DemoController#demo` | Reuses the chat template in demo mode via `DemoConversationService`. |
| `GET /privacy` | `legal` | `LegalController#privacy` | Content loaded from `resources/legal/`. |
| `GET /terms` | `legal` | `LegalController#terms` | |
| `GET /refund` | `legal` | `LegalController#refund` | |

## Auth

| Route | Template | Controller | Services |
| --- | --- | --- | --- |
| `GET /login` | `login` | `AuthController#loginPage` | |
| `GET /register` | `register` | `AuthController#registerPage` | |
| `POST /register` | `register` or redirect | `AuthController#register` | `UserService`, `BillingService`, `EmailVerificationService` |
| `GET /auth/google/start` | redirect | `OAuthStartController#startGoogle` | `OAuthIntentStore` |
| `GET /verify-email` | `verify-email` | `EmailVerificationController#verify` | `EmailVerificationService` |
| `POST /verify-email/resend` | redirect | `EmailVerificationController#resend` | `EmailVerificationService`, `UserService` |
| `GET /password/forgot` | `password/forgot` | `PasswordResetController#forgotForm` | |
| `POST /password/forgot` | `password/forgot` | `PasswordResetController#requestReset` | `PasswordResetService` |
| `GET /password/reset` | `password/reset` | `PasswordResetController#resetForm` | `PasswordResetService` |
| `POST /password/reset` | `password/reset` or redirect | `PasswordResetController#consumeReset` | `PasswordResetService` |

## Account (self-serve)

| Route | Template | Controller | Services |
| --- | --- | --- | --- |
| `GET /account` | `account` | `AccountController#account` | `UserService`, `AuthEventService`, `SubscriptionRepository` |
| `GET /account/notifications` | `account-notifications` | `AccountController#notifications` | |
| `GET /account/appearance` | `account-appearance` | `AccountController#appearance` | |
| `POST /account/appearance` | redirect | `AccountController#saveAppearance` | Writes theme/accent cookies |
| `GET /account/privacy` | `account-privacy` | `AccountController#privacy` | |
| `POST /account/password` | redirect | `AccountController#changePassword` | `AccountService` |
| `POST /account/email` | redirect | `AccountController#changeEmail` | `AccountService` |

## Mailboxes

Under `/mailboxes`.

| Route | Template | Controller | Services |
| --- | --- | --- | --- |
| `GET /mailboxes` | `mailboxes/index` | `MailboxController#listMailboxes` | `MailAccountService`, `PlanLimitService`, `PollingPolicy` |
| `GET /mailboxes/new` | `mailboxes/new` | `MailboxController#newMailbox` | `GoogleOAuthProperties` |
| `POST /mailboxes` | `mailboxes/new` or redirect | `MailboxController#createMailbox` | `MailAccountService` |
| `POST /mailboxes/{id}/sync` | redirect | `MailboxController#syncMailbox` | `MailboxPollingService`, `MailAccountRepository` |
| `POST /mailboxes/preferences` | redirect | `MailboxController#updatePreferences` | `MailAccountService`, `PlanLimitService`, `PollingPolicy` |
| `POST /mailboxes/{id}/signature` | redirect | `MailboxController#saveSignature` | `MailAccountService` |
| `POST /mailboxes/{id}/delete` | redirect | `MailboxController#deleteMailbox` | `MailAccountService` |
| `GET /mailboxes/gmail/connect` | redirect | `GmailConnectController#connect` | `GmailOAuthClient` |
| `GET /mailboxes/gmail/reconnect/{id}` | redirect | `GmailConnectController#reconnect` | `GmailOAuthClient`, `MailAccountRepository` |
| `GET /mailboxes/gmail/callback` | redirect | `GmailConnectController#callback` | `GmailOAuthClient`, `MailAccountService`, `MailboxPollingService` |

## Chats (primary app UX)

| Route | Template | Controller | Services |
| --- | --- | --- | --- |
| `GET /chats` | `chat` | `ConversationController#list` | `ConversationListService`, `MailAccountService`, `MailboxPollingService`, `BillingBannerService` |
| `GET /chats/new` | `compose` | `ConversationController#compose` | `MailAccountService` |
| `POST /chats/new` | `compose` or redirect | `ConversationController#createMessage` | `ReplyService`, `OutboundMessageService`, `ConversationKeyService`, `OwnerAddressService`, `MailAccountService` |
| `GET /chats/{key}` | `chat` or redirect | `ConversationController#chat` | `ChatService`, `ConversationListService`, `MailboxPollingService` |
| `GET /chats/{key}/pane` | `fragments/chat-pane :: pane` (fragment) | `ConversationController#chatPane` | `ChatService`, `BillingBannerService`, `MailAccountService` |
| `POST /chats/{key}/send` | `chat` or redirect | `ConversationController#send` | `ReplyService`, `OutboundMessageService`, `EmailThreadRepository`, `ChatService` |
| `GET /threads`, `/threads/**`, `/senders` | redirect to `/chats` | `ChatRedirectController#toChats` | Legacy URL bounces. |

## Saved searches

Under `/searches`.

| Route | Controller | Services |
| --- | --- | --- |
| `POST /searches` | `SavedSearchController#save` | `SavedSearchService` |
| `POST /searches/{id}/delete` | `SavedSearchController#delete` | `SavedSearchService` |

Both redirect back to `/chats?q=…`.

## Billing

Under `/billing`.

| Route | Template | Controller | Services |
| --- | --- | --- | --- |
| `POST /billing/checkout` | redirect (Stripe Checkout) | `BillingController#startCheckout` | `BillingService`, `UserService` |
| `POST /billing/portal` | redirect (Stripe Portal) | `BillingController#openPortal` | `BillingService` |
| `GET /billing/success` | `billing/success` | `BillingController#success` | |
| `GET /billing/cancel` | redirect | `BillingController#canceled` | |
| `GET /billing/cancel-subscription` | `billing/cancel-subscription` or redirect | `BillingController#cancelSubscription` | `BillingService` |
| `POST /billing/cancel-subscription` | redirect (Stripe Portal) | `BillingController#submitCancelSubscription` | `BillingService` |
| `POST /billing/webhook` | JSON | `BillingWebhookController#handle` | See `docs/API.md`. |

## Digest

| Route | Template | Controller | Services |
| --- | --- | --- | --- |
| `GET /digest/opt-out` | `digest/opt-out` | `DigestController#optOut` | `DigestEmailPreferenceRepository` |

## Team

Under `/team`.

| Route | Template | Controller | Services |
| --- | --- | --- | --- |
| `GET /team/invite` | `team/invite` | `TeamInviteController#inviteForm` | `TeamService`, `TeamInviteRepository` |
| `POST /team/invite` | redirect | `TeamInviteController#sendInvite` | `TeamInviteService` |
| `GET /team/invite/accept` | `team/accept` | `TeamInviteController#acceptForm` | `TeamInviteService`, `UserService` |
| `POST /team/invite/accept` | `team/accept` or redirect | `TeamInviteController#acceptInvite` | `TeamInviteService` |

## Admin (gated via `AdminAuthorizer.requireAdmin`)

| Route | Template | Controller | Services |
| --- | --- | --- | --- |
| `GET /admin/revenue` | `admin/revenue` | `AdminRevenueController#revenue` | `AdminAuthorizer`, `RevenueMetricsService`, `FunnelMetricsService`, `TrialEndConversionMetricsService`, `OnboardingFunnelMetricsService`, `ProAdoptionMetricsService`, `ChurnMetricsService`, `AtRiskRetentionService`, `WinBackConversionMetricsService` |
| `POST /admin/revenue/reconcile-billing-period` | redirect | `AdminRevenueController#reconcileBillingPeriod` | `BillingPeriodBackfillService` |
| `POST /admin/retention/win-back` | redirect | `AdminRevenueController#sendWinBack` | `WinBackOutreachService` |

## PWA / SEO

`SeoController` serves `robots.txt`, `sitemap.xml`, and `og-card.png`.
`PwaController` serves the manifest, service worker, icons and the
`/offline` fallback page. Neither belongs on the mobile-API contract.
