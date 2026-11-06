# WEB — Thymeleaf website

Templates in `src/main/resources/templates`. Shared fragments: `fragments/app-nav`, `settings-nav`,
`chat-pane` (open conversation, also returned standalone by `/chats/{key}/pane`), `live-refresh`
(SSE-driven in-place updates), `seo`, `cookie-banner`, `install-banner`, `sw-register`.

## Public (permitAll)
`/` & `/pricing` (PricingController), `/demo`, `/login`, `/register`, `/auth/google/start`,
`/password/forgot|reset`, `/verify-email`, `/privacy|terms|refund`, `/billing/cancel`,
`/billing/webhook` (Stripe, JSON, CSRF-exempt), `/digest/opt-out`, `/robots.txt`, `/sitemap.xml`,
`/manifest.webmanifest`, `/sw.js`, `/offline`, `/icons/**`, `/actuator/health`.

## Authenticated
| Route | Controller | Template |
|---|---|---|
| `GET /chats` (q, mailbox, filter, page) | ConversationController | `chat` |
| `GET/POST /chats/new` | ConversationController | `compose` |
| `POST /chats/refresh` → JSON `{imported}` | ConversationController | — |
| `GET /chats/{key}` , `/chats/{key}/pane` | ConversationController | `chat`, `fragments/chat-pane` |
| `POST /chats/{key}/send` (multipart) | ConversationController | redirect |
| `GET /chats/events` (SSE) | ChatEventsController | — |
| `/mailboxes` (list/new/create, `{id}/sync`, `preferences`, `{id}/signature`, `{id}/delete`) | MailboxController | `mailboxes/*` |
| `/mailboxes/gmail/connect|reconnect/{id}|callback` | GmailConnectController | — |
| `POST /searches`, `/searches/{id}/delete` | SavedSearchController | — |
| `/account`, `/account/notifications|appearance|privacy`, `POST /account/password|email` | AccountController | `account*` |
| `POST /billing/checkout|portal`, `/billing/success`, `/billing/cancel-subscription` | BillingController | `billing/*` |
| `/team/invite`, `/team/invite/accept` | TeamInviteController | `team/*` |
| `/admin/revenue` (+ reconcile, win-back POSTs) | AdminRevenueController | `admin/revenue` |
| `/threads/**`, `/senders` | ChatRedirectController | redirect to `/chats` |
