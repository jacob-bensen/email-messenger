# Thymeleaf website

Templates: `src/main/resources/templates`. Fragments: `fragments/` (`app-nav`, `settings-nav`,
`chat-pane`, `live-refresh`, `seo`, `cookie-banner`, `install-banner`, `sw-register`).

## Public pages (no auth)
`/` landing · `/pricing` · `/demo` · `/login` · `/register` · `/auth/google/start` ·
`/password/forgot|reset` · `/verify-email` · `/privacy` `/terms` `/refund` ·
`/billing/cancel` · `/digest/opt-out` · `/robots.txt` `/sitemap.xml` · PWA: `/manifest.webmanifest`,
`/sw.js`, `/offline`, `/icons/**`.

## Authenticated pages
| Route | Controller | Notes |
|---|---|---|
| `GET /chats`, `/chats/{key}`, `/chats/{key}/pane`, `/chats/new` | `ConversationController` | Chat workspace; uses `ChatService`, `MailboxPollingService` |
| `POST /chats/new`, `/chats/{key}/send`, `/chats/refresh` | `ConversationController` | send → `OutboundMessageService`/`ReplyService` |
| `GET /chats/events` | `ChatEventsController` | SSE live updates |
| `/threads`, `/senders` | `ChatRedirectController` | legacy redirects |
| `/mailboxes`, `/mailboxes/gmail/**` | `MailboxController`, `GmailConnectController` | connect/sync/delete/signature |
| `/searches` | `SavedSearchController` | saved searches |
| `/account`, `/account/{appearance,notifications,privacy,email,password}` | `AccountController` | |
| `/billing/{checkout,portal,success,cancel-subscription}` | `BillingController` | Stripe |
| `/team/invite[/accept]` | `TeamInviteController` | |
| `/admin/revenue` | `AdminRevenueController` | operator-only (`AdminAuthorizer`) |
| `/preferences` | `DigestController` | |
