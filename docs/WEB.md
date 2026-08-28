# ConexusMail Website — Thymeleaf Inventory

The website is a first-party surface of the same Spring Boot app that
serves the JSON API (see `docs/API.md`). It is authenticated with session
cookies + CSRF and rendered with Thymeleaf templates under
`src/main/resources/templates/`.

Both the website and the API MUST share the same service layer — never
fork business logic between a Thymeleaf controller and a REST controller.
This inventory lists every page in the site so the split stays visible.

## Pages, by feature area

### Marketing & anonymous entry points
| Route | Controller | Template |
| ----- | ---------- | -------- |
| `GET /` | `PricingController` | `landing.html` (or redirects to `/threads` if signed in) |
| `GET /pricing` | `PricingController` | `pricing.html` |
| `GET /demo` | `DemoController` | `conversation.html` (demoMode=true) |
| `GET /privacy`, `/terms`, `/refund` | `LegalController` | `legal.html` |
| `GET /robots.txt`, `/sitemap.xml`, `/images/og-card.png` | `SeoController` | (binary/text) |

### Auth
| Route | Controller | Template |
| ----- | ---------- | -------- |
| `GET /login`, `POST /login` (Spring form-login) | `AuthController` + Spring | `login.html` |
| `GET /register`, `POST /register` | `AuthController` | `register.html` |
| `GET /verify-email`, `POST /verify-email/resend` | `EmailVerificationController` | `verify-email.html` |
| `GET /password/forgot`, `POST /password/forgot` | `PasswordResetController` | `password/forgot.html` |
| `GET /password/reset`, `POST /password/reset` | `PasswordResetController` | `password/reset.html` |
| `GET /auth/google/start` | `OAuthStartController` | (redirect to `/oauth2/authorization/google`) |

### Account settings
| Route | Controller | Template |
| ----- | ---------- | -------- |
| `GET /account` | `AccountController` | `account.html` |
| `GET /account/notifications` | `AccountController` | `account-notifications.html` |
| `GET /account/appearance`, `POST /account/appearance` | `AccountController` | `account-appearance.html` |
| `GET /account/privacy` | `AccountController` | `account-privacy.html` |
| `POST /account/password` | `AccountController` | — (redirect) |
| `POST /account/email` | `AccountController` | — (redirect) |

### Chats (the primary logged-in workspace)
| Route | Controller | Template |
| ----- | ---------- | -------- |
| `GET /chats` | `ConversationController` | `chat.html` |
| `GET /chats/{key}` | `ConversationController` | `chat.html` (or redirect if the key doesn't belong to the user) |
| `GET /chats/{key}/pane` | `ConversationController` | `fragments/chat-pane :: pane` (AJAX) |
| `GET /chats/new` | `ConversationController` | `compose.html` |
| `POST /chats/new` | `ConversationController` | — (redirect to `/chats/{key}`) |
| `POST /chats/refresh` | `ConversationController` | (JSON `{imported:N}`) |
| `POST /chats/{key}/send` | `ConversationController` | — (redirect) |
| `GET /chats/events` (SSE) | `ChatEventsController` | (text/event-stream) |
| `GET /threads`, `/threads/**`, `/senders` | `ChatRedirectController` | (redirect to `/chats`) |

### Mailboxes
| Route | Controller | Template |
| ----- | ---------- | -------- |
| `GET /mailboxes` | `MailboxController` | `mailboxes/index.html` |
| `GET /mailboxes/new` | `MailboxController` | `mailboxes/new.html` |
| `POST /mailboxes` | `MailboxController` | — (redirect) |
| `POST /mailboxes/{id}/sync` | `MailboxController` | — (redirect) |
| `POST /mailboxes/preferences` | `MailboxController` | — (redirect) |
| `POST /mailboxes/{id}/signature` | `MailboxController` | — (redirect) |
| `POST /mailboxes/{id}/delete` | `MailboxController` | — (redirect) |
| `GET /mailboxes/gmail/connect` | `GmailConnectController` | (redirect to Google) |
| `GET /mailboxes/gmail/reconnect/{id}` | `GmailConnectController` | (redirect to Google) |
| `GET /mailboxes/gmail/callback` | `GmailConnectController` | — (redirect) |

### Saved searches
| Route | Controller | Template |
| ----- | ---------- | -------- |
| `POST /searches` | `SavedSearchController` | — (redirect to `/chats?…`) |
| `POST /searches/{id}/delete` | `SavedSearchController` | — (redirect to `/chats?…`) |

### Billing
| Route | Controller | Template |
| ----- | ---------- | -------- |
| `POST /billing/checkout` | `BillingController` | (redirect to Stripe Checkout) |
| `POST /billing/portal` | `BillingController` | (redirect to Stripe Portal) |
| `GET /billing/success` | `BillingController` | `billing/success.html` |
| `GET /billing/cancel` | `BillingController` | (redirect to `/pricing?canceled=1`) |
| `GET /billing/cancel-subscription`, `POST /billing/cancel-subscription` | `BillingController` | `billing/cancel-subscription.html` |
| `POST /billing/webhook` | `BillingWebhookController` | (JSON) |

### Teams
| Route | Controller | Template |
| ----- | ---------- | -------- |
| `GET /team/invite`, `POST /team/invite` | `TeamInviteController` | `team/invite.html` |
| `GET /team/invite/accept`, `POST /team/invite/accept` | `TeamInviteController` | `team/accept.html` (or redirect) |

### Digest opt-out
| Route | Controller | Template |
| ----- | ---------- | -------- |
| `GET /digest/opt-out?token=…` | `DigestController` | `digest/opt-out.html` |

### Admin
| Route | Controller | Template |
| ----- | ---------- | -------- |
| `GET /admin/revenue` | `AdminRevenueController` | `admin/revenue.html` |
| `POST /admin/revenue/reconcile-billing-period` | `AdminRevenueController` | — (redirect) |
| `POST /admin/retention/win-back` | `AdminRevenueController` | — (redirect) |

All `/admin/**` handlers begin with `authorizer.requireAdmin(principal.getName())`,
which throws `NoSuchElementException` for non-admins — the global exception
handler turns that into a 404. Admin membership is `admin.emails` in
`application.yml`.

### PWA shell (all permitAll)
| Route | Controller |
| ----- | ---------- |
| `GET /manifest.webmanifest` | `PwaController` |
| `GET /icons/*.png`, `/apple-touch-icon.png` | `PwaController` |
| `GET /sw.js`, `/offline` | `PwaController` |

## Shared fragments

Live under `templates/fragments/` and are included with `th:insert` /
`th:replace`:

- `app-nav.html` — the primary top nav.
- `chat-pane.html` — the AJAX-loaded chat body, exposed as fragments
  `:: pane` and `:: missing`; returned directly by
  `GET /chats/{key}/pane` for live updates.
- `cookie-banner.html`, `install-banner.html` — footer bars.
- `live-refresh.html` — hooks the page onto the `/chats/events` SSE stream.
- `seo.html` — canonical + OpenGraph tags injected into every full page.
- `settings-nav.html` — sub-nav on `/account/**`.
- `sw-register.html` — inline registration of the service worker.

## Model advice (cross-cutting)

- `SiteModelAdvice` — adds `siteBaseUrl`, `oauthGoogleEnabled` to every
  model (drives canonical URLs and the "Continue with Google" button
  conditional).
- `NavModelAdvice` — adds nav / theming attributes (`navActive`,
  `navProfileName`, `navMailboxes`, `navHasBilling`, `htmlTheme`,
  `htmlAccent`) so every page shares the same shell.

## Error rendering

`GlobalExceptionHandler` maps exceptions to the `error.html` template (or,
in the case of `PlanLimitExceededException`, a flash-message + redirect
back to `/threads` with an upgrade modal). The JSON API has its own error
entry point (see `docs/API.md` §Errors) — never lift the HTML error path
into `/api/v1/**`.
