# Thymeleaf website — page inventory

The backend also ships the public marketing site and the signed-in web
app as server-rendered Thymeleaf pages. Every controller here MUST call
into the same service classes as the (planned) mobile API — never
duplicate business logic in the controller or the template.

The full REST/JSON surface lives in `docs/API.md`. This document only
lists HTML pages.

## Shell / layout

Shared fragments live under `src/main/resources/templates/fragments/`:

- `app-nav.html` — persistent left/top nav shell for authed pages.
- `settings-nav.html` — sub-nav for `/account/*` pages.
- `chat-pane.html` — open-conversation pane, also returned as an
  HTML fragment by `GET /chats/{key}/pane`.
- `live-refresh.html` — SSE listener + in-place morph JS for the chats
  view.
- `install-banner.html` — PWA "add to home screen" prompt.
- `cookie-banner.html` — consent banner.
- `seo.html` — canonical / og / twitter meta injection.
- `sw-register.html` — service worker registration snippet.

Two `@ControllerAdvice` classes seed the model on every page:

- `SiteModelAdvice` — `siteBaseUrl`, `oauthGoogleEnabled`.
- `NavModelAdvice` — `navActive`, `navProfileName`, `navMailboxes`,
  `navHasBilling`, `htmlTheme`, `htmlAccent`.

## Public pages

| Route            | Template               | Purpose                                                 |
| ---------------- | ---------------------- | ------------------------------------------------------- |
| `/`              | `landing.html`         | Marketing landing page. Redirects to `/threads` if authed. |
| `/pricing`       | `pricing.html`         | Plan comparison + Stripe checkout entry.                |
| `/demo`          | `conversation.html`    | Demo conversation rendered from `DemoConversationService`. |
| `/login`         | `login.html`           | Form login + Google button.                             |
| `/register`      | `register.html`        | Sign-up form.                                           |
| `/verify-email`  | `verify-email.html`    | Result page for magic verification link.                |
| `/password/forgot` | `password/forgot.html` | Request a password reset.                              |
| `/password/reset`  | `password/reset.html`  | Set new password from token.                           |
| `/privacy`, `/terms`, `/refund` | `legal.html` | Policy pages (shared template, content switched by controller). |
| `/digest/opt-out?token=…` | `digest/opt-out.html` | One-click unsubscribe from weekly digest.       |
| `/billing/cancel`  | (redirect)             | Stripe cancel URL; redirects to `/pricing?canceled=1`.  |

Plus static / PWA endpoints served by `SeoController` and
`PwaController`: `/robots.txt`, `/sitemap.xml`, `/images/og-card.png`,
`/manifest.webmanifest`, `/icons/*.png`, `/apple-touch-icon.png`,
`/sw.js`, `/offline`.

## Authed pages

| Route                       | Template                     | Purpose                                              |
| --------------------------- | ---------------------------- | ---------------------------------------------------- |
| `/threads`, `/threads/**`, `/senders` | (redirect)         | Legacy paths → `/chats`.                             |
| `/chats`                    | `chat.html`                  | Rail list + empty pane.                              |
| `/chats/{key}`              | `chat.html`                  | Rail + open pane for one conversationKey.            |
| `/chats/{key}/pane`         | `fragments/chat-pane :: pane` | HTML fragment for in-place swap.                    |
| `/chats/new`                | `compose.html`               | New-message composer.                                |
| `/account`                  | `account.html`               | Profile + billing summary + recent auth events.      |
| `/account/notifications`    | `account-notifications.html` | Placeholder.                                         |
| `/account/appearance`       | `account-appearance.html`    | Theme + accent picker (writes cookies).              |
| `/account/privacy`          | `account-privacy.html`       | Placeholder.                                         |
| `/mailboxes`                | `mailboxes/index.html`       | Connected mailboxes + prefs.                         |
| `/mailboxes/new`            | `mailboxes/new.html`         | Provider picker + IMAP form.                         |
| `/team/invite`              | `team/invite.html`           | Send/list team invites.                              |
| `/team/invite/accept`       | `team/accept.html`           | Accept an invite (states: ready/invalid/mismatch).   |
| `/billing/success`          | `billing/success.html`       | Post-checkout success page.                          |
| `/billing/cancel-subscription` | `billing/cancel-subscription.html` | Reason picker before Stripe portal.             |
| `/admin/revenue`            | `admin/revenue.html`         | Admin-only revenue dashboard.                        |

### Form / DTO objects backing these pages

- `auth/RegistrationForm` — `email`, `password`, `displayName`, `source`.
- `web/MailboxForm` — `host`, `port` (1..65535, default 993), `ssl`
  (default true), `username`, `password`, `provider`.
- `web/ReplyForm` — `body` (`@Size max 100_000`).
- Ad-hoc `@RequestParam` maps for password change, email change,
  appearance, password reset, team invite, saved search, mailbox
  sync/preferences/signature, `/chats/new`, `/chats/{key}/send`,
  `/billing/checkout`, `/billing/cancel-subscription`, and
  `/admin/retention/win-back`.

## Redirect / SSE-only routes

| Route                                | Behavior                                            |
| ------------------------------------ | --------------------------------------------------- |
| `/auth/google/start`                 | Stashes intent in session, redirects to Spring's `/oauth2/authorization/google`. |
| `/mailboxes/gmail/connect`           | Redirect to Google authorize URL.                   |
| `/mailboxes/gmail/reconnect/{id}`    | Redirect to Google authorize URL (re-consent).      |
| `/mailboxes/gmail/callback`          | Handles code, then redirects to `/threads` or `/mailboxes`. |
| `/chats/events`                      | `text/event-stream` SSE — `new-mail` payloads.     |

## Error handling

`web/GlobalExceptionHandler`:

| Exception                          | Status | Rendered as                          |
| ---------------------------------- | ------ | ------------------------------------ |
| `NoResourceFoundException`         | 404    | `error.html`                         |
| `NoSuchElementException`           | 404    | `error.html`                         |
| `MailException`, `EmailImportException` | 502 | `error.html`                        |
| `BillingException`                 | 502    | `error.html`                         |
| `PlanLimitExceededException`       | 302    | Redirect to `/threads`, flash `upgradeModal`. |
| `MaxUploadSizeExceededException`   | 413    | `error.html`                         |
| `IllegalArgumentException`         | 400    | `error.html`                         |
| `DataIntegrityViolationException`  | 409    | `error.html`                         |
| catch-all `Exception`              | 500    | `error.html`                         |

## Security matrix

Configured in `auth/SecurityConfig`. `permitAll` paths: `/`, `/pricing`,
`/demo`, `/login`, `/register`, `/auth/google/start`,
`/password/forgot`, `/password/reset`, `/verify-email`, `/privacy`,
`/terms`, `/refund`, `/billing/cancel`, `/billing/webhook`,
`/digest/opt-out`, `/robots.txt`, `/sitemap.xml`, `/.well-known/**`,
`/manifest.webmanifest`, `/icons/**`, `/apple-touch-icon.png`, `/sw.js`,
`/offline`, `/css/**`, `/js/**`, `/images/**`, `/favicon.ico`, `/error`,
`/h2-console/**`, `/actuator/health/**`. Everything else requires
authentication. Form login processes at `POST /login`
(username=`email`, password=`password`, remember-me=`remember-me`).
Logout at `POST /logout`.

Admin-only actions call `AdminAuthorizer.requireAdmin(principalName)`
inside the controller — there is no URL-level admin role check today.
