# WEB — Thymeleaf website

Templates: `src/main/resources/templates`. Shared fragments: `fragments/` (app-nav,
settings-nav, chat-pane, live-refresh, cookie-banner, install-banner, seo, sw-register).
Model advice: `NavModelAdvice`, `SiteModelAdvice`.

## Public routes
`/` landing, `/pricing`, `/demo`, `/login`, `/register`, `/auth/google/start`,
`/password/forgot|reset`, `/verify-email`, `/privacy`, `/terms`, `/refund`,
`/digest/opt-out`, `/billing/cancel`, `/robots.txt`, `/sitemap.xml`, `/images/og-card.png`,
PWA: `/manifest.webmanifest`, `/sw.js`, `/offline`, `/icons/*`.

## Authenticated routes (controller → services)
| Routes | Controller | Services |
|---|---|---|
| `/chats`, `/chats/new`, `/chats/{key}`, `/chats/{key}/pane`, `/chats/{key}/send`, `/chats/refresh`, `/chats/events` | `ConversationController`, `ChatEventsController` | `ChatService`, `ConversationListService`, `OutboundMessageService`, `MailboxPollingService`, `ChatEventStream` |
| `/threads`, `/senders` | `ChatRedirectController` | redirects to /chats |
| `/mailboxes/**`, `/mailboxes/gmail/**` | `MailboxController`, `GmailConnectController` | `MailAccountService`, `GmailOAuthClient` |
| `/searches/{id}/delete` | `SavedSearchController` | `SavedSearchService` |
| `/account/**` | `AccountController` | `AccountService` |
| `/billing/checkout\|portal\|success\|cancel-subscription` | `BillingController` | `BillingService` |
| `/team/invite[/accept]` | `TeamInviteController` | `TeamInviteService` |
| `/admin/revenue`, `/admin/retention/win-back`, `/admin/revenue/reconcile-billing-period` | `AdminRevenueController` | admin metrics services, `WinBackOutreachService` |
