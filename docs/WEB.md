# Thymeleaf website

Templates: `src/main/resources/templates` (`fragments/` shared layout pieces,
`fragments/chat-pane` and `fragments/live-refresh` for in-place chat updates).

| Route | Auth | Template / response | Service(s) |
|---|---|---|---|
| `GET /`, `/pricing`, `/demo` | public | landing, pricing, demo | `DemoConversationService` |
| `/privacy` `/terms` `/refund` | public | legal | — |
| `/login` `/register` `/password/*` `/verify-email` `/auth/google/start` | public | auth pages | `UserService`, `PasswordResetService`, `EmailVerificationService` |
| `GET /chats`, `/chats/new`, `/chats/{key}`, `/chats/{key}/pane` | user | `chat`, `compose`, fragment | `ChatService`, `ConversationListService` |
| `POST /chats/new`, `/chats/{key}/send`, `/chats/refresh` (JSON `{imported:n}`) | user | redirect / JSON | `OutboundMessageService`, `ReplyService`, `MailboxPollingService` |
| `GET /chats/events` | user | SSE (`text/event-stream`) | `ChatEventStream` |
| `/mailboxes/**`, `/mailboxes/gmail/**` | user | mailbox templates | `MailAccountService`, `GmailOAuthClient` |
| `/searches/**` | user | redirects | `SavedSearchService` |
| `/account/**` | user | account pages | `AccountService` |
| `/billing/**` | user (webhook public) | redirects to Stripe | `BillingService` |
| `/team/invite*` | user | team | `TeamInviteService` |
| `/admin/revenue`, `/admin/retention/win-back` | admin | admin | admin services |
| `/digest/opt-out` | public | digest | `WeeklyDigestService` |
| `/manifest.webmanifest`, `/sw.js`, `/offline`, icons, `/robots.txt`, `/sitemap.xml` | public | PWA/SEO | — |

`/threads/**` and `/senders` redirect to `/chats` (`ChatRedirectController`).
