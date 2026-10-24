# WEB — Thymeleaf website (as implemented 2026-10-24)

Templates: `src/main/resources/templates`. Shared fragments: `fragments/`
(app-nav, settings-nav, chat-pane, live-refresh, seo, cookie-banner,
install-banner, sw-register). `NavModelAdvice`/`SiteModelAdvice` inject
common model attributes.

## Public
`/` landing · `/pricing` · `/demo` · `/login` · `/register` · `/auth/google/start` ·
`/password/forgot|reset` · `/verify-email` (+POST `/verify-email/resend`) ·
`/privacy` `/terms` `/refund` · `/digest/opt-out` · `/billing/cancel` ·
`/robots.txt` `/sitemap.xml` `/manifest.webmanifest` `/sw.js` `/offline` `/icons/**`

## Authenticated
| Route | Controller | Notes |
|---|---|---|
| GET `/chats`, `/chats/{key}`, `/chats/{key}/pane`, `/chats/new` | ConversationController | list, workspace, HTML pane fragment |
| POST `/chats/new`, `/chats/{key}/send`, `/chats/refresh` | ConversationController | compose, send (multipart), poll (returns JSON `{imported:n}`) |
| GET `/chats/events` | ChatEventsController | SSE new-mail signal |
| `/mailboxes` (+`/new`, POST `/{id}/delete|sync|signature`) | MailboxController | IMAP mailbox mgmt |
| `/mailboxes/gmail/connect|callback|reconnect/{id}` | GmailConnectController | Gmail OAuth |
| `/searches` | SavedSearchController | saved searches |
| `/account`, `/account/appearance|notifications|privacy`, POST `/account/email|password|appearance`, `/preferences` | AccountController / DigestController | |
| `/billing/checkout|portal|success|cancel-subscription`, POST `/billing/webhook` | Billing*Controller | Stripe |
| `/team/invite`, `/team/invite/accept` | TeamInviteController | |
| `/admin/revenue`, POST `/admin/retention/win-back`, `/admin/revenue/reconcile-billing-period` | AdminRevenueController | admin only |
| `/threads`, `/threads/**`, `/senders` | ChatRedirectController | legacy → `/chats` |
