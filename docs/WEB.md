# WEB (Thymeleaf site)

Templates: `src/main/resources/templates/`. Public routes are the permit-list in
`auth/SecurityConfig.java`; everything else needs a session.

## Public
`/` landing, `/pricing`, `/demo`, `/login`, `/register`, `/auth/google/start`,
`/password/forgot`, `/password/reset`, `/verify-email`, `/privacy` `/terms` `/refund`,
`/digest/opt-out`, `/billing/cancel`, `POST /billing/webhook` (Stripe, signature-verified),
`/robots.txt`, `/sitemap.xml`, `/manifest.webmanifest`, `/sw.js`, `/offline`, `/icons/**`.

## Authenticated
| Route | Controller | Notes |
|---|---|---|
| `GET /chats` (`page,q,mailbox,filter`) | `ConversationController` | conversation rail |
| `GET /chats/{key}` , `/chats/{key}/pane` | same | open conversation (full page / pane fragment) |
| `POST /chats/{key}/send` | same | reply; multipart (`body, subject, asReply, attachments, mailbox`) |
| `GET/POST /chats/new` | same | compose |
| `POST /chats/refresh` | same | manual sync |
| `GET /chats/events` | `ChatEventsController` | SSE new-mail signal |
| `/mailboxes/**`, `/mailboxes/gmail/**` | `MailboxController`, `GmailConnectController` | connect/sync/delete mailbox, signature, Gmail OAuth + reconnect |
| `/searches/**` | `SavedSearchController` | saved searches |
| `/account*`, `POST /preferences` | `AccountController` | email/password/appearance/notifications/privacy |
| `/billing/*` | `BillingController` | checkout, portal, cancel, success |
| `/team/invite*` | `TeamInviteController` | |
| `/admin/revenue`, `/admin/retention/*` | `AdminRevenueController` | operator only (`AdminAuthorizer`) |
| `/threads`, `/senders` | `ChatRedirectController` | legacy redirects to /chats |

## Service reuse
`ConversationController` delegates to `ChatService`, `ConversationListService`,
`OutboundMessageService`, `MailAccountService`. A JSON API should reuse exactly those.
