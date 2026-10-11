# WEB.md — Thymeleaf site (as of 2026-10-11)

Templates: `src/main/resources/templates` (`landing, pricing, login, register,
chat, compose, conversation, account*, billing/, mailboxes/, team/, password/,
digest/, admin/, legal, error, verify-email`, `fragments/`).

| Route | Controller | Notes |
|---|---|---|
| `/`, `/pricing`, `/demo` | `PricingController`, `DemoController` | public |
| `/privacy /terms /refund` | `LegalController` | public |
| `/login /register /auth/google/start` | `AuthController`, `OAuthStartController` | public |
| `/password/forgot|reset`, `/verify-email[/resend]` | auth controllers | public |
| `/chats`, `/chats/new`, `/chats/{key}`, `/chats/{key}/pane`, `/chats/{key}/send`, `/chats/refresh`, `/chats/events` | `ConversationController`, `ChatEventsController` | main workspace; `/threads*` and `/senders` redirect here |
| `/mailboxes`, `/mailboxes/new`, `/{id}/sync|signature|delete`, `/preferences`, `/mailboxes/gmail/connect|reconnect/{id}|callback` | `MailboxController`, `GmailConnectController` | |
| `/searches`, `/searches/{id}/delete` | `SavedSearchController` | |
| `/account`, `/account/notifications|appearance|privacy|password|email` | `AccountController` | |
| `/billing/checkout|portal|success|cancel|cancel-subscription|webhook` | `BillingController`, `BillingWebhookController` | webhook is CSRF-exempt |
| `/team/invite[/accept]` | `TeamInviteController` | |
| `/digest/opt-out` | `DigestController` | public |
| `/admin/revenue`, `/admin/revenue/reconcile-billing-period`, `/admin/retention/win-back` | `AdminRevenueController` | admin only |
| `/manifest.webmanifest /sw.js /offline /icons/* /robots.txt /sitemap.xml` | `PwaController`, `SeoController` | public |
