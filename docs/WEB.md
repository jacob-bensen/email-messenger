# WEB — Thymeleaf website (as implemented)

Templates: `src/main/resources/templates`; shared fragments in `templates/fragments`
(`app-nav`, `settings-nav`, `chat-pane`, `live-refresh`, `seo`, `cookie-banner`,
`install-banner`, `sw-register`).

## Public pages
`/` (landing), `/pricing`, `/demo`, `/login`, `/register`, `/password/forgot`,
`/password/reset`, `/verify-email`, `/privacy`, `/terms`, `/refund`,
`/digest/opt-out`, `/robots.txt`, `/sitemap.xml`, PWA: `/manifest.webmanifest`,
`/sw.js`, `/offline`, icons.

## Authenticated pages
| Route | Controller | Notes |
|---|---|---|
| `GET /chats` (`page,q,mailbox,filter`) | ConversationController | list + empty pane; `chat.html` |
| `GET /chats/{key}` , `/chats/{key}/pane` | ConversationController | marks read; pane is an HTML fragment |
| `GET/POST /chats/new` | ConversationController | compose, multipart attachments |
| `POST /chats/{key}/send` | ConversationController | `asReply`, `subject`, attachments |
| `POST /chats/refresh` | ConversationController | JSON `{imported}` |
| `GET /chats/events` | ChatEventsController | SSE new-mail events |
| `/mailboxes`, `/mailboxes/new`, `/{id}/sync`, `/preferences`, `/{id}/signature`, `/{id}/delete` | MailboxController | |
| `/mailboxes/gmail/connect`, `/reconnect/{id}`, `/callback` | GmailConnectController | Gmail OAuth |
| `/searches`, `/searches/{id}/delete` | SavedSearchController | |
| `/account`, `/account/{notifications,appearance,privacy,password,email}` | AccountController | |
| `/billing/{checkout,portal,success,cancel,cancel-subscription}`, `POST /billing/webhook` | Billing controllers | webhook CSRF-exempt |
| `/team/invite`, `/team/invite/accept` | TeamInviteController | |
| `/admin/revenue`, `/admin/revenue/reconcile-billing-period`, `/admin/retention/win-back` | AdminRevenueController | admin only |
| `/threads`, `/threads/**`, `/senders` | ChatRedirectController | legacy → `/chats` |
