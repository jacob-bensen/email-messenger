# Thymeleaf website

Templates: `src/main/resources/templates` (+ `fragments/`, `account-*`, `admin/`,
`billing/`, `digest/`, `mailboxes/`, `password/`, `team/`).

| Route(s) | Controller | Template | Auth |
|---|---|---|---|
| `/`, `/pricing` | PricingController | landing, pricing | public |
| `/demo` | DemoController | chat (demo data via `DemoConversationService`) | public |
| `/login`, `/register`, `/auth/google/start` | AuthController, OAuthStartController | login, register | public |
| `/password/forgot`, `/password/reset` | PasswordResetController | password/* | public |
| `/verify-email`, `/verify-email/resend` | EmailVerificationController | verify-email | public / auth |
| `/privacy`, `/terms`, `/refund` | LegalController | legal | public |
| `/chats`, `/chats/new`, `/chats/{key}`, `/chats/{key}/pane`, `/chats/{key}/send`, `/chats/refresh`, `/chats/events` | ConversationController, ChatEventsController | chat, compose, fragments/chat-pane | auth |
| `/threads/**`, `/senders` | ChatRedirectController | redirect to /chats | auth |
| `/mailboxes/**`, `/mailboxes/gmail/**` | MailboxController, GmailConnectController | mailboxes/* | auth |
| `/searches`, `/searches/{id}/delete` | SavedSearchController | (redirect) | auth |
| `/account/**` | AccountController | account-* | auth |
| `/billing/**` | BillingController, BillingWebhookController | billing/* | mixed (`/billing/webhook`, `/billing/cancel` public) |
| `/team/invite[/accept]` | TeamInviteController | team/* | auth |
| `/digest/opt-out` | DigestController | digest/* | public |
| `/admin/revenue` + 2 POSTs | AdminRevenueController | admin/* | admin |
| `/manifest.webmanifest`, `/sw.js`, `/offline`, icons | PwaController | — | public |
| `/robots.txt`, `/sitemap.xml`, `/images/og-card.png` | SeoController | — | public |

Shared services: see BACKEND.md. Live updates: `fragments/live-refresh.html`
consumes the `/chats/events` SSE stream.
