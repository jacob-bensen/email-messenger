# WEB.md — Thymeleaf website (public + authenticated pages)

The backend serves ConexusMail's public marketing site and the
authenticated web app from the same Spring Boot process, using
Thymeleaf templates under `src/main/resources/templates`.

Every controller in `com.emailmessenger.web` (plus `auth/`, `billing/`,
`admin/`, `team/`, `digest/`) is an `@Controller` that returns a view
name. State mutations MUST call the same `service/` methods as the
mobile REST API — see `docs/API.md` and `docs/BACKEND.md`.

## Page inventory

### Public (unauthenticated, whitelisted in `SecurityConfig`)

| Path                | Template              | Controller                | Purpose                                     |
|---------------------|-----------------------|---------------------------|---------------------------------------------|
| `/`                 | `landing.html`        | `SeoController`           | Marketing landing page.                     |
| `/pricing`          | `pricing.html`        | `PricingController`       | Plan comparison + upgrade CTAs.             |
| `/demo`             | (redirects/renders)   | `DemoController`          | Read-only demo conversation.                |
| `/login`            | `login.html`          | `AuthController`          | Form login + Google OIDC entry.             |
| `/register`         | `register.html`       | `AuthController`          | Sign-up form.                                |
| `/password/forgot`  | `password/forgot.html`| `PasswordResetController` | Request reset link.                          |
| `/password/reset`   | `password/reset.html` | `PasswordResetController` | Consume reset token, set new password.       |
| `/verify-email`     | `verify-email.html`   | `EmailVerificationController` | Consume email-verify token.              |
| `/privacy`, `/terms`, `/refund` | `legal.html` | `LegalController`     | Legal pages driven by properties.           |
| `/billing/cancel`   | `billing/cancel-subscription.html` | `BillingController` | Cancel flow. Public so token links work. |
| `/billing/webhook`  | (JSON)                | `BillingWebhookController`| Stripe webhook consumer. CSRF-exempt.       |
| `/digest/opt-out`   | `digest/opt-out.html` | `DigestController`        | One-click digest unsubscribe.                |

### Authenticated

| Path                      | Template                            | Controller                | Purpose                                    |
|---------------------------|-------------------------------------|---------------------------|--------------------------------------------|
| `/chats`                  | `chat.html`                         | `ConversationController`  | Chats list workspace.                       |
| `/chats/new`              | `compose.html`                      | `ConversationController`  | New-message composer.                        |
| `/chats/{key}`            | `chat.html`                         | `ConversationController`  | Open one conversation in the workspace.      |
| `/chats/{key}/pane`       | `fragments/chat-pane :: pane`       | `ConversationController`  | HTML fragment (swap pane in place).          |
| `/chats/refresh` (POST)   | JSON                                | `ConversationController`  | Foreground poll trigger.                     |
| `/chats/events` (SSE)     | text/event-stream                   | `ChatEventsController`    | Live-refresh stream (paid tiers, IDLE).      |
| `/mailboxes`              | `mailboxes/index.html`              | `MailboxController`       | List connected mailboxes.                   |
| `/mailboxes/new`          | `mailboxes/new.html`                | `MailboxController`       | Add mailbox form (IMAP or Gmail OAuth).      |
| `/mailboxes/gmail/*`      | (redirects)                         | `GmailConnectController`  | Gmail OAuth flow.                           |
| `/account`                | `account.html`                      | `AccountController`       | Profile, plan, sessions.                    |
| `/account/appearance`     | `account-appearance.html`           | `AccountController`       | Theme / accent color prefs.                  |
| `/account/notifications`  | `account-notifications.html`        | `AccountController`       | Digest + push prefs.                         |
| `/account/privacy`        | `account-privacy.html`              | `AccountController`       | Data export / delete.                        |
| `/team`                   | `team/invite.html`                  | `TeamInviteController`    | Team invites.                                |
| `/team/accept`            | `team/accept.html`                  | `TeamInviteController`    | Accept invite via token.                     |
| `/searches` (etc.)        | (rendered in `chat.html`)           | `SavedSearchController`   | Saved-search CRUD.                          |
| `/admin/revenue`          | `admin/revenue.html`                | `AdminRevenueController`  | Operator dashboard (gated by `ADMIN_EMAILS`).|

## Shared fragments and layouts

- `templates/fragments/*` — reusable fragments (`chat-pane`,
  `live-refresh`, nav bar, billing banner, etc.). Referenced from
  page templates via `th:replace="fragments/... :: ..."`.
- `NavModelAdvice` and `SiteModelAdvice` are
  `@ControllerAdvice`-scoped model contributors so every rendered
  page gets the nav badges and site-wide properties without each
  controller having to add them.
- `LandingProperties`, `LegalProperties`, `SiteProperties` — externalised
  content driven by `application.yml` so copy can change without
  touching Java.

## Service reuse

Every controller listed above delegates to the same package's service:

| Controller                       | Service used                                                 |
|----------------------------------|--------------------------------------------------------------|
| `ConversationController`         | `ConversationListService`, `ChatService`, `ReplyService`, `OutboundMessageService`, `MailboxPollingService` |
| `MailboxController`              | `MailAccountService`                                          |
| `AccountController`              | `AccountService`, `UserService`                              |
| `BillingController`              | `BillingService`, `StripeCheckoutGateway`, `StripePortalGateway` |
| `BillingWebhookController`       | `StripeWebhookGateway`, `BillingService`                     |
| `AuthController`                 | `UserService`, `EmailVerificationService`                    |
| `PasswordResetController`        | `PasswordResetService`                                        |
| `TeamInviteController`           | (team service inside `com.emailmessenger.team`)               |
| `SavedSearchController`          | `SavedSearchService`, `SavedSearchCountService`               |
| `AdminRevenueController`         | admin metrics services in `com.emailmessenger.admin`          |

The mobile REST controllers in `com.emailmessenger.api` (new package,
mostly empty as of this run) MUST use the same service methods for the
same operations. Never re-implement business logic in a controller or
template.
