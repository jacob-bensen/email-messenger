# WEB — Thymeleaf website

The public site + logged-in workspace + operator dashboard are all
Spring MVC + Thymeleaf, served by the same JVM as (eventually) the REST
API. Templates live under `src/main/resources/templates/` and share
`fragments/*.html` for the header, chat rail, live-refresh JS, etc.

Every page-serving controller must reuse the same services the REST API
uses — no duplicated business logic between web and API paths (see
[`BACKEND.md`](BACKEND.md) "Authoritative service layer").

## Page inventory

### Public / marketing
| Route | Template | Controller | Notes |
|---|---|---|---|
| `GET /` | `landing.html` | `web/PricingController` | Redirects to `/chats` when authenticated. |
| `GET /pricing` | `pricing.html` | `web/PricingController` | Plans, checkout entry. |
| `GET /demo` | `conversation.html` | `web/DemoController` | Anonymous sample conversation. |
| `GET /privacy`, `/terms`, `/refund` | `legal.html` (fragment) | `web/LegalController` | Legal copy under `src/main/resources/legal/`. |

### Auth
| Route | Template | Controller |
|---|---|---|
| `GET /login`, `POST /login` (form-login) | `login.html` | `auth/AuthController` + Spring Security |
| `GET /register`, `POST /register` | `register.html` | `auth/AuthController` |
| `GET /password/forgot`, `/reset` | `password/*.html` | `auth/PasswordResetController` |
| `GET /verify-email`, `POST /verify-email/resend` | `verify-email.html` | `auth/EmailVerificationController` |
| Google OIDC: `GET /auth/google/start` → `/oauth2/authorization/google` → callback | — | `auth/OAuthStartController` + Spring Security |

### Chats workspace
| Route | Template | Controller |
|---|---|---|
| `GET /chats` | `chat.html` (rail-only) | `web/ConversationController` |
| `GET /chats/{key}` | `chat.html` (rail + open pane) | `web/ConversationController` |
| `GET /chats/{key}/pane` | `fragments/chat-pane :: pane` | `web/ConversationController` |
| `GET /chats/new` | `compose.html` | `web/ConversationController` |
| `POST /chats/new` | — (redirect) | `web/ConversationController` |
| `POST /chats/{key}/send` | — (redirect) | `web/ConversationController` |
| `POST /chats/refresh` | — (`@ResponseBody Map`) | `web/ConversationController` |
| `GET /chats/events` | — (SSE) | `web/ChatEventsController` |
| `GET /threads[/**]`, `GET /senders` | — (redirect to `/chats`) | `web/ChatRedirectController` |

Shared fragments under `templates/fragments/` power the chat-rail row,
the open-pane bubbles, day separators, unread badges, live-refresh JS
(`fragments/live-refresh.html` subscribes to `/chats/events` and swaps
in `fragments/chat-pane :: pane`), the nav header, and the user
dropdown.

### Mailboxes
| Route | Template | Controller |
|---|---|---|
| `GET /mailboxes` | `mailboxes/index.html` | `web/MailboxController` |
| `GET /mailboxes/new?provider=` | `mailboxes/new.html` | `web/MailboxController` |
| `POST /mailboxes`, `POST /mailboxes/{id}/sync`, `/signature`, `/delete` | — (redirects) | `web/MailboxController` |
| `POST /mailboxes/preferences` | — (redirect) | `web/MailboxController` |
| `GET /mailboxes/gmail/connect`, `/reconnect/{id}`, `/callback` | — (Google OAuth) | `web/GmailConnectController` |

### Account
| Route | Template | Controller |
|---|---|---|
| `GET /account` | `account.html` | `auth/AccountController` |
| `GET /account/notifications` | `account-notifications.html` | `auth/AccountController` |
| `GET /account/appearance`, `POST` | `account-appearance.html` | `auth/AccountController` |
| `GET /account/privacy` | `account-privacy.html` | `auth/AccountController` |
| `POST /account/password`, `/email` | — (redirects) | `auth/AccountController` |

### Saved searches
| Route | Controller |
|---|---|
| `POST /searches`, `POST /searches/{id}/delete` | `web/SavedSearchController` (renders back into `chat.html`) |

### Teams
| Route | Template | Controller |
|---|---|---|
| `GET /team/invite`, `POST /team/invite` | `team/invite.html` | `team/TeamInviteController` |
| `GET /team/invite/accept?token=`, `POST` | `team/accept.html` | `team/TeamInviteController` |

### Billing
| Route | Template | Controller |
|---|---|---|
| `POST /billing/checkout`, `POST /billing/portal` | — (Stripe redirect) | `billing/BillingController` |
| `GET /billing/success` | `billing/success.html` | `billing/BillingController` |
| `GET /billing/cancel` | — (redirect) | `billing/BillingController` |
| `GET /billing/cancel-subscription`, `POST` | `billing/cancel-subscription.html` | `billing/BillingController` |
| `POST /billing/webhook` | — (JSON reply from `@RestController`) | `billing/BillingWebhookController` |

### Admin (operator dashboard, `AdminAuthorizer.requireAdmin`)
| Route | Template | Controller |
|---|---|---|
| `GET /admin/revenue` | `admin/revenue.html` | `admin/AdminRevenueController` |
| `POST /admin/revenue/reconcile-billing-period` | — (redirect) | `admin/AdminRevenueController` |
| `POST /admin/retention/win-back` | — (redirect) | `admin/AdminRevenueController` |

### Digest, PWA, SEO
| Route | Controller |
|---|---|
| `GET /digest/opt-out?token=` → `digest/opt-out.html` | `digest/DigestController` |
| `GET /manifest.webmanifest`, `/icons/*.png`, `/apple-touch-icon.png`, `/sw.js`, `/offline` | `web/PwaController` |
| `GET /robots.txt`, `/sitemap.xml`, `/images/og-card.png` | `web/SeoController` |

## Shared model advice

- `NavModelAdvice` populates the header nav on every authenticated
  page.
- `SiteModelAdvice` + `SiteProperties` and `LandingProperties` /
  `LegalProperties` populate the public pages.
- `GlobalExceptionHandler` renders `error.html`.

## Responsive + PWA

`static/css/` styles produce the IM look (bubbles, day separators, dark
mode). The app installs as a PWA via `sw.js` + `manifest.webmanifest`.
Long-term the mobile client (Flutter) replaces the PWA on iOS/Android
for push notifications and native niceties; the PWA remains for
browsers.

## Non-goals

- The web pages are the operator's and desktop user's home. Feature
  parity with the mobile app isn't required and shouldn't drive
  design.
- The mobile app must not talk to any of these routes directly (except
  the SSE stream and, for now, `POST /chats/refresh`). Prefer
  `/api/v1/**` — see [`API.md`](API.md).
