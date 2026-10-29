# WEB.md — Thymeleaf website

Templates in `src/main/resources/templates`. Layout reuse is via fragments, not a layout dialect.

## Public pages
| Route | Controller | Template |
|---|---|---|
| `/` | `PricingController` | `landing.html` |
| `/pricing` | `PricingController` | `pricing.html` |
| `/demo` | `DemoController` (`DemoConversationService`) | `chat.html`/demo |
| `/privacy` `/terms` `/refund` | `LegalController` | `legal.html` |
| `/login` `/register` | `AuthController` | `login.html` `register.html` |
| `/password/forgot` `/password/reset` | `PasswordResetController` | `password/*` |
| `/verify-email` (+`POST /verify-email/resend`) | `EmailVerificationController` | `verify-email.html` |
| `/digest/opt-out` | `DigestController` | `digest/opt-out.html` |
| `/robots.txt` `/sitemap.xml` `/images/og-card.png` | `SeoController` | generated |
| `/manifest.webmanifest` `/sw.js` `/offline` `/icons/*` | `PwaController` | generated |

## Authenticated pages
| Route | Controller | Notes |
|---|---|---|
| `/chats`, `/chats/{key}`, `/chats/{key}/pane` | `ConversationController` | main workspace; `chat.html` + `fragments/chat-pane.html` |
| `/chats/new`, `POST /chats/{key}/send`, `POST /chats/refresh`, `/chats/events` (SSE) | same / `ChatEventsController` | compose, send, heartbeat, live updates |
| `/mailboxes`, `/mailboxes/new`, `/{id}/sync`, `/{id}/signature`, `/{id}/delete`, `/preferences`, `/mailboxes/gmail/*` | `MailboxController`, `GmailConnectController` | mailbox connect/manage |
| `/searches`, `/searches/{id}/delete` | `SavedSearchController` | |
| `/account`, `/account/{notifications,appearance,privacy,password,email}` | `AccountController` | |
| `/billing/{checkout,portal,success,cancel,cancel-subscription}` | `BillingController` | `/billing/webhook` is public JSON (Stripe) |
| `/team/invite`, `/team/invite/accept` | `TeamInviteController` | |
| `/admin/revenue` (+ POST actions) | `AdminRevenueController` | admin only (`AdminAuthorizer`) |
| `/threads/**`, `/senders` | `ChatRedirectController` | legacy → `/chats` |

Shared fragments: `app-nav`, `settings-nav`, `chat-pane`, `live-refresh`, `seo`, `cookie-banner`, `install-banner`, `sw-register`.
