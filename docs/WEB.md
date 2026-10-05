# WEB.md — Thymeleaf website

Templates: `src/main/resources/templates`. Shared fragments in `templates/fragments`
(`app-nav`, `settings-nav`, `chat-pane`, `live-refresh`, `seo`, `cookie-banner`,
`install-banner`, `sw-register`). All state-changing routes are form POSTs with CSRF.

## Public
`/` landing · `/pricing` · `/demo` · `/login` · `/register` · `/password/forgot|reset` ·
`/verify-email` (+ POST `/verify-email/resend`, authed) · `/privacy` `/terms` `/refund` ·
`/digest/opt-out` · `/auth/google/start` · `/robots.txt` `/sitemap.xml` ·
PWA: `/manifest.webmanifest` `/sw.js` `/offline` `/icons/*` `/apple-touch-icon.png`.

## Chats (core) — `web/ConversationController`, services `ChatService`, `ConversationListService`
| Route | Notes |
|---|---|
| GET `/chats` | list; params `page`, `q`, `mailbox`, `filter` |
| GET `/chats/new`, POST `/chats/new` | compose (`to`,`subject`,`body`,`attachments[]`,`mailbox`) |
| GET `/chats/{key}`, `/chats/{key}/pane` | conversation by participant-set `conversationKey` (full page / HTML fragment) |
| POST `/chats/{key}/send` | reply (`body`,`subject`,`asReply`,`attachments[]`,`mailbox`) |
| POST `/chats/refresh` | manual sync |
| GET `/chats/events` | SSE `NewMailEvent` signal |
| `/threads`, `/threads/**`, `/senders` | legacy redirects to `/chats` (`ChatRedirectController`) |

## Mailboxes — `MailboxController`, `GmailConnectController`
GET `/mailboxes`, `/mailboxes/new`; POST `/mailboxes` (IMAP connect), `/{id}/sync`,
`/{id}/delete`, `/{id}/signature`, `/preferences`; Gmail OAuth: GET
`/mailboxes/gmail/connect`, `/reconnect/{id}`, `/callback`.

## Other
Saved searches `/searches` · Account `/account`, `/account/{appearance,notifications,privacy}`,
POST `/account/{email,password,appearance}` · Billing `/billing/{checkout,portal,success,cancel,cancel-subscription}`,
POST `/billing/webhook` · Team `/team/invite[/accept]` · Admin `/admin/revenue` (+ POST
`/admin/retention/win-back`, `/admin/revenue/reconcile-billing-period`) — gated by `AdminAuthorizer`.
