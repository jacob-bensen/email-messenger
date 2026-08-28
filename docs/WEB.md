# WEB — ConexusMail Thymeleaf site

Server-rendered pages under `src/main/resources/templates/`. Every page
that mutates data delegates to a service class in `com.emailmessenger.*`;
the REST API in `docs/API.md` calls the same services, so behavior stays
identical across surfaces.

## Public marketing / auth pages

| Route | Controller | Template |
| --- | --- | --- |
| `GET /` | `web.LandingController` (via `SeoController`) | `landing.html` |
| `GET /pricing` | `web.PricingController` | `pricing.html` |
| `GET /demo` | `web.DemoController` | `demo.html` |
| `GET /login` | `auth.AuthController` | `auth/login.html` |
| `GET /register` | `auth.AuthController` | `auth/register.html` |
| `GET /password/forgot` | `auth.PasswordResetController` | `auth/forgot.html` |
| `GET /password/reset` | `auth.PasswordResetController` | `auth/reset.html` |
| `GET /verify-email` | `auth.EmailVerificationController` | `auth/verify.html` |
| `GET /privacy` \| `/terms` \| `/refund` | `web.LegalController` | `legal/*.html` |
| `GET /auth/google/start` | `auth.OAuthStartController` | 302 to Google OIDC |

## Authenticated app pages

| Route | Controller | Template |
| --- | --- | --- |
| `GET /chats` | `web.ConversationController` | `chat.html` (rail + open pane) |
| `GET /chats/{key}` | `web.ConversationController` | `chat.html` |
| `GET /chats/events` | `web.ChatEventsController` | text/event-stream — live `NewMailEvent` fan-out |
| `POST /chats/{key}/reply` | `web.ConversationController` | via `ReplyService` |
| `GET /mailboxes` | `web.MailboxController` | `mailboxes.html` |
| `POST /mailboxes` | `web.MailboxController` | connect IMAP mailbox |
| `GET /mailboxes/gmail/start` | `web.GmailConnectController` | OAuth start |
| `GET /mailboxes/gmail/callback` | `web.GmailConnectController` | OAuth callback |
| `GET /mailboxes/gmail/reconnect/{id}` | `web.GmailConnectController` | weekly reconnect while OAuth app is in Testing |
| `GET /account` | `auth.AccountController` | `account.html` |
| `POST /account/*` | `auth.AccountController` | profile / preferences updates |
| `GET /account/notifications` | `auth.AccountController` | notification prefs (Web Push TBD) |
| `GET /saved-searches` | `web.SavedSearchController` | `saved-searches.html` |
| `GET /teams`, `POST /teams/*` | `team.TeamInviteController` | teams + invites |

## Billing pages

| Route | Controller |
| --- | --- |
| `GET/POST /billing/checkout` | `billing.BillingController` |
| `GET /billing/portal` | `billing.BillingController` (Stripe portal redirect) |
| `GET /billing/cancel` | `billing.BillingController` (Stripe checkout cancel return) |
| `GET/POST /billing/cancel-subscription` | `billing.BillingController` |
| `POST /billing/webhook` | `billing.BillingWebhookController` (`@RestController`) |

## Admin pages (`admin@` allowlist)

| Route | Template |
| --- | --- |
| `GET /admin/revenue` | `admin/revenue.html` |

Gated by `AdminProperties.emails`; a non-operator gets a 404.

## PWA + static

| Route | Controller |
| --- | --- |
| `GET /manifest.webmanifest`, `/sw.js`, `/offline` | `web.PwaController` |
| `GET /robots.txt`, `/sitemap.xml` | `web.SeoController` |

## Fragments and layouts

- `templates/layout.html` — outer shell, nav, footer.
- `templates/fragments/*.html` — reusable pieces (`live-refresh.html`,
  `mailbox-card.html`, `chat-bubble.html`, `admin-*.html`).
- CSS in `src/main/resources/static/css/main.css`; page-specific CSS is
  namespaced by a page-scoped class.

## Guidance

- Add a new REST endpoint at the same time as any new mutation route in a
  Thymeleaf controller — both call the same service.
- Never inline business rules in a `.html` file: templates render data,
  services compute it.
