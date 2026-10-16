# WEB — Thymeleaf website

Templates: `src/main/resources/templates`. Routes below are from `@*Mapping` annotations
(2026-10-16). Auth: **P** = public, **U** = authenticated, **A** = admin allowlist (`admin.emails`).

| Route | Auth | Controller | Template / notes |
|---|---|---|---|
| `/`, `/pricing`, `/demo` | P | `ChatRedirect/Pricing/DemoController` | landing, pricing, demo |
| `/login`, `/register`, `/auth/google/start`, `/password/forgot\|reset`, `/verify-email[/resend]` | P | `auth.*Controller` | auth flows |
| `/privacy`, `/terms`, `/refund`, `/robots.txt`, `/sitemap.xml` | P | `LegalController`, `SeoController` | |
| `/manifest.webmanifest`, `/sw.js`, `/offline`, `/icons/**` | P | `PwaController` | PWA shell |
| `/chats` (q, mailbox, filter, page) | U | `ConversationController.list` | `chat.html` rail |
| `/chats/{key}` , `/chats/{key}/pane` | U | `ConversationController` | open conversation / in-place pane |
| `/chats/new` GET/POST | U | `ConversationController` | `compose.html` |
| `/chats/{key}/send` POST | U | `ConversationController.send` | reply/new message (multipart) |
| `/chats/refresh` POST | U | JSON `{"imported":n}` | foreground sync heartbeat |
| `/chats/events` GET | U | `ChatEventsController` | SSE, event data `{"imported":n}` |
| `/threads/**`, `/senders` | U | `ChatRedirectController` | legacy redirects |
| `/mailboxes` (+ `/{id}/sync`, `/{id}/delete`, `/{id}/signature`), `/mailboxes/gmail/{connect,callback,reconnect/{id}}` | U | `MailboxController`, `GmailConnectController` | |
| `/searches` | U | `SavedSearchController` | |
| `/account`, `/account/{appearance,notifications,privacy,email,password}`, `/preferences` | U | `AccountController` | |
| `/billing/{checkout,portal,cancel-subscription,success}` ; `/billing/cancel`, `/billing/webhook` | U ; P | `BillingController`, `BillingWebhookController` | Stripe |
| `/team/invite[/accept]` | U | `TeamInviteController` | |
| `/digest/opt-out` | P | `DigestController` | |
| `/admin/revenue` (+ win-back, reconcile POSTs) | A | `AdminRevenueController` | operator dashboard |

Shared fragments: `templates/fragments/` (incl. `live-refresh.html` for SSE-driven in-place updates).
Services used by chat pages: `ConversationListService`, `ChatService`, `MailAccountService`,
`ReplyService`, `OutboundMessageService`, `MailboxPollingService`.
