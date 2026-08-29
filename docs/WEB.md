# Web (Thymeleaf) surface

Page inventory. Every page below is a Thymeleaf template under
`src/main/resources/templates/`. Controllers are in
`com.emailmessenger.web`, `.auth`, `.billing`, `.team`, `.digest`, and
`.admin` — see `docs/API.md` for the full route table.

Shared layout: `templates/fragments/` holds header, footer,
`live-refresh.html` (SSE + morph client), nav, and the signature rail.
`web/NavModelAdvice` and `web/SiteModelAdvice` populate the layout
model for every page.

## Marketing / legal (public)

| Route      | Template                    | Controller             |
| ---------- | --------------------------- | ---------------------- |
| `/`        | `landing.html`              | `PricingController`    |
| `/pricing` | `pricing.html`              | `PricingController`    |
| `/privacy` | `legal.html` (privacy view) | `LegalController`      |
| `/terms`   | `legal.html` (terms view)   | `LegalController`      |
| `/refund`  | `legal.html` (refund view)  | `LegalController`      |
| `/demo`    | (demo chat)                 | `DemoController`       |

## Auth

| Route                    | Template                | Controller                   |
| ------------------------ | ----------------------- | ---------------------------- |
| `/login`                 | `login.html`            | `AuthController`             |
| `/register`              | `register.html`         | `AuthController`             |
| `/verify-email`          | `verify-email.html`     | `EmailVerificationController`|
| `/password/forgot|reset` | `password/*.html`       | `PasswordResetController`    |

## Account settings

| Route                      | Template                     | Controller          |
| -------------------------- | ---------------------------- | ------------------- |
| `/account`                 | `account.html`               | `AccountController` |
| `/account/notifications`   | `account-notifications.html` | `AccountController` |
| `/account/appearance`      | `account-appearance.html`    | `AccountController` |
| `/account/privacy`         | `account-privacy.html`       | `AccountController` |

## Mailboxes

| Route                            | Template                    | Controller                |
| -------------------------------- | --------------------------- | ------------------------- |
| `/mailboxes`                     | `mailboxes/list.html`       | `MailboxController`       |
| `/mailboxes/new`                 | `mailboxes/new.html`        | `MailboxController`       |
| Gmail OAuth `/connect,callback`  | (redirects only)            | `GmailConnectController`  |

## Chats (the core product)

| Route                | Template                              | Controller               |
| -------------------- | ------------------------------------- | ------------------------ |
| `/chats`             | `chat.html`                           | `ConversationController` |
| `/chats/new`         | `compose.html`                        | `ConversationController` |
| `/chats/{key}`       | `chat.html` (with conversationKey)    | `ConversationController` |
| `/chats/{key}/pane`  | `fragments/conversation-pane.html`    | `ConversationController` |

`chat.html` is a two-pane layout: rail on the left, open pane on the
right. Both are morph-updated in place when a `new-mail` event arrives
on `/chats/events`; the pane fragment endpoint is what the SSE client
re-fetches.

## Team

| Route                        | Template               | Controller             |
| ---------------------------- | ---------------------- | ---------------------- |
| `/team/invite`               | `team/invite.html`     | `TeamInviteController` |
| `/team/invite/accept`        | `team/accept.html`     | `TeamInviteController` |

## Billing

| Route                            | Template               | Controller          |
| -------------------------------- | ---------------------- | ------------------- |
| `/billing/success|cancel`        | `billing/*.html`       | `BillingController` |
| `/billing/cancel-subscription`   | `billing/cancel.html`  | `BillingController` |

## Admin (allowlist)

| Route                    | Template               | Controller               |
| ------------------------ | ---------------------- | ------------------------ |
| `/admin/revenue`         | `admin/revenue.html`   | `AdminRevenueController` |

## Shared services

Web controllers do NOT own business logic. All of the following go
through services under `com.emailmessenger.service` or the domain
sub-packages: conversation listing (`ConversationListService`,
`ConversationService`), IM transform (`IMTransformService`), reply
sending (`ReplyService`), outbound message state
(`OutboundMessageService`), saved searches (`SavedSearchService`),
onboarding (`OnboardingService`), chat aggregation (`ChatService`).

If a REST API is ever added, its controllers MUST call the same
services — never fork logic between web and API paths.
