# WEB.md — Thymeleaf website (routes from controller annotations)

Public: `/` & `/pricing` (PricingController), `/demo`, `/privacy` `/terms` `/refund`, `/robots.txt`, `/sitemap.xml`, PWA (`/manifest.webmanifest`, `/sw.js`, `/offline`, icons).
Auth: `/login`, `/register`, `/auth/google/start`, `/password/forgot|reset`, `/verify-email`(+`/resend`), `/logout`.
Account: `/account`, `/account/notifications|appearance|privacy|password|email`.
Chats (core): `/chats` (list, `q`/`mailbox`/`filter`/`page`), `/chats/new`, `/chats/{key}`, `/chats/{key}/pane`, `/chats/{key}/send`, `/chats/refresh`, `/chats/events` (SSE). Legacy `/threads/**`,`/senders` redirect to chats.
Mailboxes: `/mailboxes` (+`/new`, `/{id}/sync`, `/{id}/signature`, `/{id}/delete`, `/preferences`), Gmail OAuth `/mailboxes/gmail/connect|reconnect/{id}|callback`.
Searches: `POST /searches`, `/searches/{id}/delete`. Teams: `/team/invite`(+`/accept`). Billing: `/billing/checkout|portal|success|cancel|cancel-subscription|webhook`. Digest: `/digest/opt-out`. Admin: `/admin/revenue` (+ reconcile, win-back).

Templates: `src/main/resources/templates` (chat.html workspace, fragments/ shared, admin/, billing/, mailboxes/, team/, digest/, password/). Controllers delegate to services (`ChatService`, `ConversationListService`, `ReplyService`, `MailAccountService`, ...); any future REST controller must reuse these.
