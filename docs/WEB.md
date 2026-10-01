# WEB.md — Thymeleaf website

Templates: `src/main/resources/templates`. Fragments: `app-nav`, `chat-pane`, `settings-nav`,
`live-refresh`, `seo`, `cookie-banner`, `install-banner`, `sw-register`.

| Route(s) | Template | Controller / services |
|---|---|---|
| `/`, `/pricing`, `/demo`, `/privacy`, `/terms`, `/refund` | landing, pricing, demo, legal | `web.*Controller`, `DemoConversationService` |
| `/login`, `/register`, `/auth/google/start`, `/verify-email`, `/password/*` | login, register, … | `auth.*` |
| `/chats`, `/chats/{key}`, `/chats/{key}/pane`, `/chats/new`, `POST /chats/{key}/send`, `POST /chats/refresh` | chat, compose | `ConversationController` → `ConversationListService`, `ChatService`, `ReplyService`, `OutboundMessageService`, `MailboxPollingService` |
| `/chats/events` (SSE) | — | `ChatEventsController`, `ChatEventStream` |
| `/mailboxes/**`, `/mailboxes/gmail/**` | mailboxes/* | `MailboxController`, `GmailConnectController`, `MailAccountService` |
| `/searches/**` | — | `SavedSearchController` |
| `/account/**`, `/preferences` | account-* | `AccountController`, `AccountService` |
| `/billing/**` | billing/* | `BillingController`, `BillingWebhookController` |
| `/team/**` | team/* | `TeamInviteController` |
| `/admin/revenue` | admin/revenue | `AdminRevenueController` |
| `/manifest.webmanifest`, `/sw.js`, `/offline`, icons | — | `PwaController` |
