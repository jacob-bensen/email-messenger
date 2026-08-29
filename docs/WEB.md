# email-messenger — Website

Inventory of the Thymeleaf-rendered website. The pages here are
distinct from the mobile REST surface at `/api/mobile/v1/**` (see
`docs/API.md`), but they MUST share the service layer under the hood —
no page ever reaches into a repository or Stripe/IMAP client that a
mobile controller wouldn't go through the same way.

## Layouts and fragments

Layouts and shared fragments live in
`src/main/resources/templates/fragments/`. Named references from
controllers use the `layout.html` decorator pattern for the app shell
(nav, toast bar, footer) and inline fragments for
`live-refresh.html` (the SSE wiring for the chats surface).

## Page inventory

Grouped by area. Each row: HTTP method + path + template + one-line
purpose + services touched. Auth column: 🌐 = public, 🔒 = signed-in
user, 🛡️ = operator-only (`admin.emails` allowlist), 💳 = Stripe
signature.

### Public / marketing

| Method | Path | Template | Auth | Purpose |
|---|---|---|---|---|
| GET | `/` | `landing.html` | 🌐 | Landing page; served by `PricingController` (same controller because pricing gates land on it). |
| GET | `/pricing` | `pricing.html` | 🌐 | Plan matrix + checkout CTAs (calls `BillingService` server-side to compute per-plan prompts). |
| GET | `/demo` | `demo-*.html` | 🌐 | Static product demo pages. |
| GET | `/privacy`, `/terms`, `/refund` | `legal.html` | 🌐 | Legal pages (`LegalController`). |

### SEO / PWA shell

| Method | Path | Template | Auth | Purpose |
|---|---|---|---|---|
| GET | `/robots.txt`, `/sitemap.xml` | (rendered inline) | 🌐 | Crawler assets (`SeoController`). |
| GET | `/images/og-card.png` | (static) | 🌐 | OpenGraph card. |
| GET | `/manifest.webmanifest`, `/sw.js`, `/offline`, `/icons/**`, `/apple-touch-icon.png` | (`pwa/*`) | 🌐 | Installable PWA manifest, service worker, icons (`PwaController`). |

### Auth

| Method | Path | Template | Auth | Purpose |
|---|---|---|---|---|
| GET | `/login` | `login.html` | 🌐 | Login form; accepts `plan`, `billing`, `utm_source` for post-login redirect. |
| GET | `/register` | `register.html` | 🌐 | Registration form. |
| POST | `/register` | — | 🌐 | Create account, auto-login, optionally start Stripe Checkout. Uses `AuthService` + `BillingService`. |
| GET | `/verify-email` | `verify-email.html` | 🌐 | Consume verification token. |
| POST | `/verify-email/resend` | — | 🌐 | Re-send verification mail. |
| GET | `/password/forgot` | `password/forgot.html` | 🌐 | Request-reset form. |
| POST | `/password/forgot` | — | 🌐 | Send reset email. |
| GET | `/password/reset` | `password/reset.html` | 🌐 | Reset form (token in query). |
| POST | `/password/reset` | — | 🌐 | Apply new password. |
| GET | `/auth/google/start` | (redirect) | 🌐 | Kick off Google OIDC via Spring Security OAuth2 client. |

### Chats & compose (the app itself)

| Method | Path | Template | Auth | Purpose |
|---|---|---|---|---|
| GET | `/chats` | `chat.html` | 🔒 | Chats workspace: rail + open pane. Rail via `ConversationListService`; open pane via `ChatService`. |
| GET | `/chats/new` | `compose.html` | 🔒 | New-chat compose form. |
| POST | `/chats/new` | (redirect) | 🔒 | Create outbound message + convo (`OutboundMessageService`). |
| GET | `/chats/{conversationKey}` | `chat.html` | 🔒 | Same workspace, with the pane opened to `{key}`. |
| GET | `/chats/{conversationKey}/pane` | `fragments/*` | 🔒 | HTML fragment for the pane — used by live-refresh to morph in place without a full reload. |
| POST | `/chats/{conversationKey}/send` | (redirect) | 🔒 | Reply/append (`ReplyService`). |
| GET | `/chats/events` | (SSE) | 🔒 | See `docs/API.md § 1.1`. |
| POST | `/chats/refresh` | (JSON) | 🔒 | See `docs/API.md § 1.2`. |
| GET | `/threads`, `/threads/**`, `/senders` | (redirect) | 🔒 | Legacy URLs — `ChatRedirectController` sends them to `/chats`. |

### Mailboxes

| Method | Path | Template | Auth | Purpose |
|---|---|---|---|---|
| GET | `/mailboxes` | `mailboxes/list.html` | 🔒 | Connected-account list. |
| GET | `/mailboxes/new` | `mailboxes/new.html` | 🔒 | IMAP connect form. |
| POST | `/mailboxes` | (redirect) | 🔒 | Create mailbox row + validate IMAP creds. |
| POST | `/mailboxes/{id}/sync` | (redirect) | 🔒 | Manual "Sync now" — enqueues a `MailboxPollingService` run. |
| POST | `/mailboxes/preferences` | (redirect) | 🔒 | Update default mailbox / global mail prefs. |
| POST | `/mailboxes/{id}/signature` | (redirect) | 🔒 | Update per-account signature (V35). |
| POST | `/mailboxes/{id}/delete` | (redirect) | 🔒 | Disconnect mailbox. |
| GET | `/mailboxes/gmail/connect` | (redirect) | 🔒 | Start Gmail OAuth (`GmailConnectController`). |
| GET | `/mailboxes/gmail/reconnect/{id}` | (redirect) | 🔒 | Re-consent an existing Gmail account (needed until OAuth consent screen is published — see backend roadmap). |
| GET | `/mailboxes/gmail/callback` | (redirect) | 🔒 | Gmail OAuth redirect target. |

### Account settings

| Method | Path | Template | Auth | Purpose |
|---|---|---|---|---|
| GET | `/account` | `account.html` | 🔒 | Profile / account root. |
| GET | `/account/notifications` | `account-notifications.html` | 🔒 | Notification prefs. |
| GET | `/account/appearance` | `account-appearance.html` | 🔒 | Theme / appearance. |
| POST | `/account/appearance` | (redirect) | 🔒 | Save appearance. |
| GET | `/account/privacy` | `account-privacy.html` | 🔒 | Privacy controls. |
| POST | `/account/password` | (redirect) | 🔒 | Change password. |
| POST | `/account/email` | (redirect) | 🔒 | Change email. |

### Billing

| Method | Path | Template | Auth | Purpose |
|---|---|---|---|---|
| POST | `/billing/checkout` | (redirect) | 🔒 | Create Stripe Checkout session and redirect. |
| POST | `/billing/portal` | (redirect) | 🔒 | Redirect to Stripe customer portal. |
| GET | `/billing/success` | `billing/success.html` | 🔒 | Post-checkout landing. |
| GET | `/billing/cancel` | `billing/cancel.html` | 🔒 | Checkout-cancelled landing. |
| GET | `/billing/cancel-subscription` | `billing/cancel-subscription.html` | 🔒 | Confirm cancellation. |
| POST | `/billing/cancel-subscription` | (redirect) | 🔒 | Cancel subscription. |
| POST | `/billing/webhook` | (plain-text) | 💳 | Stripe webhook — see `docs/API.md § 1.3`. |

### Team

| Method | Path | Template | Auth | Purpose |
|---|---|---|---|---|
| GET | `/team/invite` | `team/invite.html` | 🔒 | Invite form. |
| POST | `/team/invite` | (redirect) | 🔒 | Send invite. |
| GET | `/team/invite/accept` | `team/accept.html` | 🌐 (with token) | Accept-invite landing. |
| POST | `/team/invite/accept` | (redirect) | 🌐 (with token) | Finalize acceptance. |

### Digest

| Method | Path | Template | Auth | Purpose |
|---|---|---|---|---|
| GET | `/digest/opt-out` | `digest/opt-out.html` | 🌐 (with token) | One-click unsubscribe from digest emails. |

### Saved searches

| Method | Path | Template | Auth | Purpose |
|---|---|---|---|---|
| POST | `/searches` | (redirect) | 🔒 | Create saved search (form-submitted from the chats surface filter bar). |
| POST | `/searches/{id}/delete` | (redirect) | 🔒 | Delete saved search. |

### Admin (operator-only)

| Method | Path | Template | Auth | Purpose |
|---|---|---|---|---|
| GET | `/admin/revenue` | `admin/revenue.html` | 🛡️ | Revenue metrics dashboard. |
| POST | `/admin/revenue/reconcile-billing-period` | (redirect) | 🛡️ | Trigger billing-period reconciliation job. |
| POST | `/admin/retention/win-back` | (redirect) | 🛡️ | Trigger the win-back email run. |

Non-admin users hitting `/admin/**` get 404 (silent gating).

## Conventions

- Forms POST to their own endpoints and redirect to a GET on success
  (POST-Redirect-GET); flash attributes carry one-shot toasts.
- Fragment endpoints (`.../pane`) exist for the live-refresh SSE
  path only — never fetched by the mobile client.
- Any new page that mutates state MUST have its logic in a service in
  `service/` and MUST NOT hold behavior a future
  `/api/mobile/v1/**` controller would need to re-implement.
