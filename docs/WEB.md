# Website (Thymeleaf)

Templates: landing, pricing, login, register, verify-email, password/{forgot,reset}, chat (main workspace), compose, conversation (legacy /threads → redirect via `ChatRedirectController`), mailboxes/{index,new}, account{,-appearance,-notifications,-privacy}, billing/{success,cancel-subscription}, team/{invite,accept}, digest/opt-out, admin/revenue, legal, error.
Fragments: app-nav, chat-pane, cookie-banner, install-banner, live-refresh, seo, settings-nav, sw-register.

Key routes (see `docs/API.md` for the full inventory): `/chats` (list, `?q&mailbox&filter&page`), `/chats/{key}` and `/chats/{key}/pane`, `POST /chats/{key}/send`, `/chats/new`, `/mailboxes/**`, `/searches/**`, `/account/**`, `/billing/**`, `/admin/revenue`.
Controllers delegate to services (`ChatService`, `ConversationListService`, `ReplyService`, `OutboundMessageService`, `MailAccountService`, `BillingService`, …). Any future REST API must call the same services.
