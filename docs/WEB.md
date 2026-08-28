# Website (Thymeleaf) — Page Inventory

The website is served by the same Spring Boot app that will host the
mobile REST API. Every page delegates to a service in `service/`,
`auth/`, `billing/`, etc. — never inlines business logic in the
controller or template. This document is a map from URL → template
→ controller → primary service.

Shared layout partials live under `templates/fragments/`.

## Public pages

| URL | Template | Controller | Notes |
| --- | --- | --- | --- |
| `/` | `landing.html` | `PricingController.landing` | Marketing home. |
| `/pricing` | `pricing.html` | `PricingController.pricing` | Plan tiers, Stripe checkout links. |
| `/privacy` | `legal.html` fragment | `LegalController.privacy` | Static legal. |
| `/terms` | `legal.html` fragment | `LegalController.terms` | Static legal. |
| `/refund` | `legal.html` fragment | `LegalController.refund` | Static legal. |
| `/demo` | `chat.html` (demo mode) | `DemoController` | Uses `DemoConversationService`; no auth. |
| `/robots.txt`, `/sitemap.xml`, OG card | — | `SeoController` | SEO plumbing. |
| `/manifest.webmanifest`, `/sw.js`, icons, `/offline` | — | `PwaController` | PWA shell. |

## Auth

| URL | Template | Controller |
| --- | --- | --- |
| `/login` | `login.html` | `AuthController` (form login via Spring Security) |
| `/register` (GET/POST) | `register.html` | `AuthController` |
| `/verify-email`, `/verify-email/resend` | `verify-email.html` | `EmailVerificationController` |
| `/password/forgot`, `/password/reset` | `password/…` | `PasswordResetController` |
| `/auth/google/start` | (redirect) | `OAuthStartController` → Google OIDC |

## Signed-in account

| URL | Template | Controller |
| --- | --- | --- |
| `/account` | `account.html` | `AccountController` |
| `/account/appearance` (GET/POST) | `account-appearance.html` | `AccountController` |
| `/account/notifications` | `account-notifications.html` | `AccountController` |
| `/account/privacy` | `account-privacy.html` | `AccountController` |
| `/account/password`, `/account/email` (POST) | — | `AccountController` |

## Chats workspace

| URL | Template | Controller | Service |
| --- | --- | --- | --- |
| `/chats` | `chat.html` | `ConversationController` | `ConversationListService`, `ChatService` |
| `/chats/new` (GET/POST) | `compose.html` | `ConversationController` | `OutboundMessageService` |
| `/chats/refresh` (POST, JSON) | — | `ConversationController` | `MailboxPollingService` |
| `/chats/{key}` | `chat.html` (pane state) | `ConversationController` | `ConversationService`, `IMTransformService` |
| `/chats/{key}/pane` | `fragments/pane` | `ConversationController` | in-place pane refresh |
| `/chats/{key}/send` (POST) | — (redirect back) | `ConversationController` | `ReplyService`, `OutboundMessageService` |
| `/chats/events` (SSE) | — | `ChatEventsController` | `ChatEventStream` |
| `/threads`, `/senders` | (redirects) | `ChatRedirectController` | Legacy URL compatibility. |

## Mailboxes

| URL | Template | Controller |
| --- | --- | --- |
| `/mailboxes` | `mailboxes/list.html` | `MailboxController` |
| `/mailboxes/new` | `mailboxes/new.html` | `MailboxController` |
| `/mailboxes` (POST) | — | `MailboxController` (IMAP form) |
| `/mailboxes/{id}/sync` (POST) | — | `MailboxController` (kicks a poll) |
| `/mailboxes/preferences` (POST) | — | `MailboxController` |
| `/mailboxes/{id}/signature` (POST) | — | `MailboxController` |
| `/mailboxes/{id}/delete` (POST) | — | `MailboxController` |
| `/mailboxes/gmail/connect` | (redirect) | `GmailConnectController` |
| `/mailboxes/gmail/reconnect/{id}` | (redirect) | `GmailConnectController` |
| `/mailboxes/gmail/callback` | (redirect) | `GmailConnectController` |

## Billing

| URL | Template | Controller |
| --- | --- | --- |
| `/billing/checkout`, `/billing/portal` (POST) | (redirect to Stripe) | `BillingController` |
| `/billing/success`, `/billing/cancel` | `billing/…` | `BillingController` |
| `/billing/cancel-subscription` (GET/POST) | `billing/cancel-subscription.html` | `BillingController` |
| `/billing/webhook` (POST) | — | `BillingWebhookController` |

## Teams & saved searches & digest

| URL | Template | Controller |
| --- | --- | --- |
| `/team/invite` (GET/POST) | `team/invite.html` | `TeamInviteController` |
| `/team/invite/accept` (GET/POST) | `team/accept.html` | `TeamInviteController` |
| `/saved-searches` (POST, `/{id}/delete`) | — | `SavedSearchController` |
| `/digest/opt-out` | `digest/opt-out.html` | `DigestController` |

## Admin

| URL | Template | Controller |
| --- | --- | --- |
| `/admin/revenue` | `admin/revenue.html` | `AdminRevenueController` |
| `/admin/revenue/reconcile-billing-period` (POST) | — | `AdminRevenueController` |
| `/admin/retention/win-back` (POST) | — | `AdminRevenueController` |

## Shared fragments and layout

`templates/fragments/`:
- `layout.html` — the base page (head, nav, footer).
- `nav.html` — main navigation (populated by `NavModelAdvice`).
- `live-refresh.html` — the SSE-driven client script.

## Rule: shared services

Any new mobile REST endpoint MUST reuse the same service the
Thymeleaf controller uses. Never fork logic between web and API paths.
See `docs/BACKEND.md` for the module map.
