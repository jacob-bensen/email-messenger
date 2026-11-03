# Thymeleaf website

Templates: `src/main/resources/templates`. Layout is per-page with shared fragments in `fragments/` (`app-nav`, `settings-nav`, `chat-pane`, `live-refresh`, `seo`, `cookie-banner`, `install-banner`, `sw-register`).

## Public routes
`/` landing, `/pricing`, `/demo`, `/login`, `/register`, `/password/forgot|reset`, `/verify-email`, `/privacy`, `/terms`, `/refund`, `/digest/opt-out`, `/billing/cancel`, `/billing/webhook` (Stripe, CSRF-exempt), PWA/SEO: `/manifest.webmanifest`, `/sw.js`, `/offline`, `/robots.txt`, `/sitemap.xml`, `/icons/**`.

## Authenticated routes
| Route | Controller → services |
|---|---|
| `/chats`, `/chats/{key}`, `/chats/{key}/pane`, `/chats/new` (GET/POST), `/chats/{key}/send`, `/chats/refresh`, `/chats/events` | `ConversationController`/`ChatEventsController` → `ConversationListService`, `ChatService`, `ReplyService`, `OutboundMessageService`, `MailboxPollingService` |
| `/mailboxes` (+`/new`, `/{id}/sync|signature|delete`, `/preferences`), `/mailboxes/gmail/connect|reconnect/{id}|callback` | `MailboxController`, `GmailConnectController` → `MailAccountService` |
| `/searches` (+`/{id}/delete`) | `SavedSearchController` → `SavedSearchService` |
| `/account`, `/account/appearance|notifications|privacy|email|password` | `AccountController` |
| `/billing/checkout|portal|success|cancel-subscription` | `BillingController` → `BillingService` |
| `/team/invite`, `/team/invite/accept` | `TeamInviteController` |
| `/admin/revenue`, `/admin/retention/win-back` | `AdminRevenueController` (allowlist) |
| `/threads`, `/threads/**`, `/senders` | `ChatRedirectController` (legacy redirects) |
