# ConexusMail website (Thymeleaf)

The server-rendered website served by this Spring Boot app. Architecture:
`docs/BACKEND.md`. Mobile API contract: `docs/API.md`. Reverse-engineered
from the code on 2026-11-16; update it whenever a route or template changes.

## Rules
- Pages call the same `@Service` classes the mobile API will use. A rule
  must never live only in a controller or template. Where it does today,
  it is listed under "Logic still in controllers" below and in
  `INTERNAL_TODO.md`.
- Every page is a full HTML document. There is no layout dialect; pages
  pull in shared fragments with `th:replace`.

## Auth legend
- **P**: public (SecurityConfig `permitAll`).
- **A**: signed-in session (form login, remember-me or Google OIDC).
- **Admin**: signed in, and the email is in `ADMIN_EMAILS`. This is checked by
  `AdminAuthorizer.requireAdmin`; anyone else gets a 404.

## Page and route inventory

### Marketing, legal, SEO, PWA
| Method | Path | Auth | View / result | Services |
|---|---|---|---|---|
| GET | `/` | P | `landing`. With `?demo` it redirects to `/demo`; a signed-in user goes to `/threads` | LandingProperties |
| GET | `/pricing` | P | `pricing` | – |
| GET | `/demo` | P | `conversation` (demo mode) | DemoConversationService |
| GET | `/privacy`, `/terms`, `/refund` | P | `legal` | LegalProperties |
| GET | `/robots.txt`, `/sitemap.xml`, `/images/og-card.png` | P | text, XML or PNG | SiteProperties |
| GET | `/manifest.webmanifest`, `/sw.js`, `/offline`, `/icons/*`, `/apple-touch-icon.png` | P | PWA assets | – |

### Auth
| Method | Path | Auth | View / result | Services |
|---|---|---|---|---|
| GET | `/login` | P | `login` | – |
| POST | `/login` | P | Spring Security form login (fields `email` and `password`) | DbUserDetailsService, LoginThrottleFilter |
| POST | `/logout` | A | redirect to `/login?logout` | – |
| GET/POST | `/register` | P | `register`, then Stripe checkout or `/threads` | UserService, EmailVerificationService, BillingService |
| GET | `/auth/google/start` | P | redirect to `/oauth2/authorization/google` | OAuthIntentStore |
| GET/POST | `/password/forgot`, `/password/reset` | P | `password/forgot`, `password/reset` | PasswordResetService |
| GET | `/verify-email` | P | `verify-email` | EmailVerificationService |
| POST | `/verify-email/resend` | A | redirect | EmailVerificationService |

### Chats (the core product)
| Method | Path | Auth | View / result | Services |
|---|---|---|---|---|
| GET | `/threads`, `/threads/**`, `/senders` | A | redirect to `/chats` (legacy URLs) | – |
| GET | `/chats` | A | `chat`: conversation rail, with no conversation open | ConversationListService, MailAccountService, MailboxPollingService, BillingBannerService |
| GET | `/chats/{key}` | A | `chat` with the conversation open; marks it read | ChatService |
| GET | `/chats/{key}/pane` | A | fragment `chat-pane :: pane` (or `:: missing`) | ChatService |
| POST | `/chats/{key}/send` | A | reply; redirects back | ReplyService, OutboundMessageService |
| GET/POST | `/chats/new` | A | `compose`; on send, redirects to `/chats/{key}` | ReplyService, OutboundMessageService, ConversationKeyService, OwnerAddressService |
| POST | `/chats/refresh` | A | JSON `{"imported": n}` (an internal XHR, not part of `API.md`) | MailboxPollingService |
| GET | `/chats/events` | A | SSE stream of new-mail events (internal) | ChatEventStream |
| POST | `/searches`, `/searches/{id}/delete` | A | redirect | SavedSearchService |

### Mailboxes
| Method | Path | Auth | View / result | Services |
|---|---|---|---|---|
| GET | `/mailboxes` | A | `mailboxes/index` | MailAccountService, PlanLimitService, PollingPolicy |
| GET | `/mailboxes/new` | A | `mailboxes/new` | – |
| POST | `/mailboxes` | A | connect an IMAP mailbox with a password | MailAccountService.connect |
| POST | `/mailboxes/{id}/sync` | A | sync now | MailboxPollingService.pollOne |
| POST | `/mailboxes/preferences`, `/mailboxes/{id}/signature`, `/mailboxes/{id}/delete` | A | redirect | MailAccountService |
| GET | `/mailboxes/gmail/connect`, `/mailboxes/gmail/reconnect/{id}`, `/mailboxes/gmail/callback` | A | Gmail OAuth flow | GmailOAuthClient, MailAccountService |

### Account, team, billing, digest, admin
| Method | Path | Auth | View / result | Services |
|---|---|---|---|---|
| GET | `/account` | A | `account` | UserService, AuthEventService |
| GET | `/account/notifications`, `/account/privacy` | A | static "coming soon" pages | – |
| GET/POST | `/account/appearance` | A | `account-appearance` (theme and accent cookies) | AppearanceSettings |
| POST | `/account/password`, `/account/email` | A | redirect | AccountService |
| GET/POST | `/team/invite`, `/team/invite/accept` | A | `team/invite`, `team/accept` | TeamService, TeamInviteService |
| POST | `/billing/checkout`, `/billing/portal` | A | redirect to Stripe | BillingService |
| GET | `/billing/success` | A | `billing/success` | – |
| GET | `/billing/cancel` | P | redirect to `/pricing?canceled=1` | – |
| GET/POST | `/billing/cancel-subscription` | A | `billing/cancel-subscription` | BillingService |
| POST | `/billing/webhook` | P, CSRF-exempt | Stripe webhook (text body) | StripeWebhookGateway, BillingService |
| GET | `/digest/opt-out` | P | `digest/opt-out` | DigestEmailPreferenceRepository (no service) |
| GET | `/admin/revenue` | Admin | `admin/revenue` | admin `*MetricsService`, AtRiskRetentionService |
| POST | `/admin/revenue/reconcile-billing-period`, `/admin/retention/win-back` | Admin | redirect | BillingPeriodBackfillService, WinBackOutreachService |

## Shared fragments (`templates/fragments/`)
| Fragment | Provides | Used by |
|---|---|---|
| `app-nav :: nav` | Signed-in sidebar: brand, mailboxes, Pricing, Settings | chat, compose, account*, mailboxes/index |
| `settings-nav :: nav(active)` | Settings sub-nav | account*, mailboxes/index |
| `chat-pane :: pane / missing` | The open conversation: timeline, reply box, signature rail | chat, `/chats/{key}/pane` |
| `live-refresh :: script` | SSE client for `/chats/events`, plus a `/chats/refresh` heartbeat | chat |
| `seo :: meta(...)` | Title, canonical and OG/Twitter tags; registers the service worker | marketing, auth and demo pages |
| `cookie-banner :: banner` | Consent banner for anonymous visitors | marketing and auth pages |
| `install-banner :: banner` | PWA install prompt | landing |
| `sw-register :: register` | Service-worker registration | via `seo` |

The team, billing, admin, digest and error pages use no fragments.

## Model advice
- **NavModelAdvice** adds `navActive`, `htmlTheme` and `htmlAccent`. For a
  signed-in user it also adds `navProfileName`, `navMailboxes` and
  `navHasBilling`.
- **SiteModelAdvice** adds `siteBaseUrl` and `oauthGoogleEnabled`.
- **GlobalExceptionHandler** renders `error` with an HTTP status:
  - 404 for not-found
  - 502 for mail and billing failures
  - 413 for uploads over the limit
  - 400 for `IllegalArgumentException`
  - 409 for integrity violations
  - 500 for anything else

  `PlanLimitExceededException` is handled differently: it puts an `upgradeModal`
  flash attribute on a redirect to `/threads`.

## Logic still in controllers
These rules must move into services before the mobile API needs them. They
are tracked in `INTERNAL_TODO.md`.
- `ConversationController`: recipient parsing and validation, the
  empty-body check, the "(no subject)" default, signature appending,
  choosing the sending account, and choosing reply vs. new email. The
  mobile reply/compose feature needs all of these.
- `MailboxController`: sync-frequency validation with the plan floor, the
  2000-character signature cap, and the sync ownership check.
- `PasswordResetController`: the minimum password length of 8.
- `DigestController`: writes the opt-out straight to the repository.
- `TeamInviteController`: a direct repository query and the
  invitee/email mismatch check.
