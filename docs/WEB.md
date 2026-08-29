# Thymeleaf Website

The backend serves both the marketing site and the signed-in web app.
Templates live under `src/main/resources/templates/` and are rendered by
controllers in `com.emailmessenger.web`. All business rules go through
`com.emailmessenger.service` / `com.emailmessenger.*` — templates and
controllers never own state.

## Layouts and fragments

- `fragments/layout.html` — base HTML shell, header/footer.
- `fragments/live-refresh.html` — SSE bridge for the chats UI.
- Site-wide model advice: `SiteModelAdvice`, `NavModelAdvice`,
  `LandingProperties`, `SiteProperties`.

## Public / marketing pages

| Route            | Template            | Controller                   |
|------------------|---------------------|------------------------------|
| `/`              | `landing.html`      | `web.LandingController`      |
| `/pricing`       | `pricing.html`      | `web.PricingController`      |
| `/legal/privacy` | `legal.html`        | `web.LegalController`        |
| `/legal/terms`   | `legal.html`        | `web.LegalController`        |
| `/legal/refund`  | `legal.html`        | `web.LegalController`        |
| `/demo`          | `chat.html` (demo)  | `web.DemoController`         |
| `/pwa/*`         | (manifest / SW)     | `web.PwaController`          |
| `/robots.txt`    | plain text          | `web.SeoController`          |

## Auth pages

| Route             | Template               | Controller                   |
|-------------------|------------------------|------------------------------|
| `/login`          | `login.html`           | `auth.LoginController`       |
| `/register`       | `register.html`        | `auth.RegisterController`    |
| `/verify-email`   | `verify-email.html`    | `auth.EmailVerificationController` |
| `/password/*`     | `password/*.html`      | `auth.PasswordResetController` |

Session-cookie auth. Google OIDC entrypoint at `/oauth2/authorization/google`
returns to `/login/oauth2/code/google` (Spring Security default).

## App pages (session-authenticated)

| Route                         | Template                   | Controller                       |
|-------------------------------|----------------------------|----------------------------------|
| `/chats`                      | `chat.html`                | `web.ConversationController`     |
| `/chats/{key}`                | `chat.html`                | `web.ConversationController`     |
| `/chats/events` (SSE)         | —                          | `web.ChatEventsController`       |
| `/compose`                    | `compose.html`             | `web.ConversationController`     |
| `/conversation/{id}`          | `conversation.html`        | `web.ConversationController`     |
| `/mailboxes`                  | `mailboxes/*.html`         | `web.MailboxController` + `GmailConnectController` |
| `/account`                    | `account.html`             | `web.AccountController`          |
| `/account/notifications`      | `account-notifications.html` | `web.AccountController`        |
| `/account/appearance`         | `account-appearance.html`  | `web.AccountController`          |
| `/account/privacy`            | `account-privacy.html`     | `web.AccountController`          |
| `/saved-searches/*`           | (fragments)                | `web.SavedSearchController`      |
| `/team/*`                     | `team/*.html`              | `team.TeamController`            |
| `/billing/*`                  | `billing/*.html`           | `billing.*Controller`            |
| `/admin/*` (operator only)    | `admin/*.html`             | `admin.*Controller`              |
| `/digest/*` (in-mail preview) | `digest/*.html`            | `digest.*Controller`             |

## Shared with mobile

Chats, mailboxes, and account features are the ones the mobile client
mirrors. Both the Thymeleaf paths and the mobile-facing endpoints in
`api/` MUST call the same `ChatService`, `ConversationListService`,
`ConversationService`, and `MailboxService` — never fork logic.
