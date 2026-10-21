# WEB.md — Thymeleaf website

Templates in `src/main/resources/templates`; shared fragments in `fragments/`
(`app-nav`, `settings-nav`, `chat-pane`, `live-refresh`, `seo`, `cookie-banner`, `install-banner`, `sw-register`).

## Public (permitAll)
`/` landing, `/pricing`, `/demo`, `/login`, `/register`, `/auth/google/start`,
`/password/forgot|reset`, `/verify-email`, `/privacy`, `/terms`, `/refund`, `/billing/cancel`,
`/billing/webhook` (Stripe, POST), `/digest/opt-out`, `/robots.txt`, `/sitemap.xml`,
PWA: `/manifest.webmanifest`, `/sw.js`, `/offline`, `/icons/**`.

## Authenticated
| Route | Controller | Template |
|---|---|---|
| `/chats`, `/chats/new`, `/chats/{key}`, `/chats/{key}/pane`, POST `/chats/{key}/send`, POST `/chats/refresh`, `/chats/events` | `ConversationController`, `ChatEventsController` | `chat`, `compose`, `fragments/chat-pane` |
| `/mailboxes/**`, `/mailboxes/gmail/**` (connect, reconnect, sync, delete, signature) | `MailboxController`, `GmailConnectController` | `mailboxes/*` |
| `/searches/**` | `SavedSearchController` | — |
| `/account`, `/account/appearance|notifications|privacy`, `/account/email|password` | `AccountController` | `account*` |
| `/billing/checkout|portal|success|cancel-subscription` | `BillingController` | `billing/*` |
| `/team/invite`, `/team/invite/accept` | `TeamInviteController` | `team/*` |
| `/admin/revenue` + POSTs | `AdminRevenueController` | `admin/revenue` |
| `/threads`, `/threads/**`, `/senders` | `ChatRedirectController` | redirects to `/chats` |

Services used: see `BACKEND.md`. Controllers should stay thin; shared logic lives in `service`/`web/*Service`.
