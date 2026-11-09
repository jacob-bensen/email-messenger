# Website (Thymeleaf) — page inventory

Templates: `src/main/resources/templates`. Shared fragments in `fragments/`
(app-nav, settings-nav, chat-pane, live-refresh, seo, cookie/install banners, sw-register).

| Route | Controller | Template | Auth |
|---|---|---|---|
| `/`, `/pricing` | PricingController | landing, pricing | public |
| `/demo` | DemoController | chat (demo data) | public |
| `/login`, `/register`, `/auth/google/start` | AuthController, OAuthStartController | login, register | public |
| `/password/forgot`, `/password/reset` | PasswordResetController | password/* | public |
| `/verify-email`, POST `/verify-email/resend` | EmailVerificationController | verify-email | public / authed |
| `/privacy`,`/terms`,`/refund` | LegalController | legal | public |
| `/chats` (q, mailbox, filter, page) | ConversationController | chat | authed |
| `/chats/new` GET/POST | ConversationController | compose | authed |
| `/chats/{key}`, `/chats/{key}/pane` | ConversationController | chat, fragments/chat-pane | authed |
| POST `/chats/{key}/send` (multipart; subject, asReply, attachments, mailbox) | ConversationController | redirect | authed |
| POST `/chats/refresh`, GET `/chats/events` (SSE) | ConversationController, ChatEventsController | JSON / SSE | authed |
| `/mailboxes`, `/mailboxes/new`, POST `/{id}/sync`, `/preferences`, `/{id}/signature`, `/{id}/delete` | MailboxController | mailboxes/* | authed |
| `/mailboxes/gmail/connect`, `/reconnect/{id}`, `/callback` | GmailConnectController | redirect | authed |
| POST `/searches/{id}/delete` | SavedSearchController | redirect | authed |
| `/account`, `/account/{notifications,appearance,privacy}`, POST `/account/{password,email,appearance}` | AccountController | account-* | authed |
| `/billing/checkout`, `/portal`, `/success`, `/cancel`, `/cancel-subscription` | BillingController | billing/* | mixed |
| POST `/billing/webhook` | BillingWebhookController | JSON (Stripe sig) | public |
| `/team/invite`, `/team/invite/accept` | TeamInviteController | team/* | authed |
| `/digest/opt-out` | DigestController | digest/opt-out | public |
| `/admin/revenue` (+ POSTs) | AdminRevenueController | admin/revenue | admin |
| `/manifest.webmanifest`, `/sw.js`, `/offline`, icons, `/robots.txt`, `/sitemap.xml` | PwaController, SeoController | assets | public |
| `/threads/**`, `/senders` | ChatRedirectController | redirect to /chats | authed |

Conversations are keyed by `conversationKey` (participant-set hash), not thread id.
