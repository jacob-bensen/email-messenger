# WEB.md — Thymeleaf website

Templates: `src/main/resources/templates` (fragments in `fragments/`, e.g. `live-refresh.html`).

| Route(s) | Controller | Template | Auth |
|---|---|---|---|
| `/`, `/pricing` | PricingController | landing, pricing | public |
| `/demo` | DemoController | (demo conversation) | public |
| `/privacy` `/terms` `/refund` | LegalController | legal | public |
| `/robots.txt` `/sitemap.xml` `/images/og-card.png` | SeoController | — | public |
| `/manifest.webmanifest` `/sw.js` `/offline` `/icons/**` | PwaController | offline | public |
| `/login` `/register` `/auth/google/start` | AuthController, OAuthStartController | login, register | public |
| `/password/forgot` `/password/reset` | PasswordResetController | password/* | public |
| `/verify-email` (+`/resend`) | EmailVerificationController | verify-email | public / authed |
| `/chats`, `/chats/new`, `/chats/{key}`, `/chats/{key}/pane`, `/chats/{key}/send`, `/chats/refresh`, `/chats/events` | ConversationController, ChatEventsController | chat, compose | authed |
| `/threads/**`, `/senders` | ChatRedirectController | redirect to /chats | authed |
| `/mailboxes` (+`/new`, `/{id}/sync`, `/preferences`, `/{id}/signature`, `/{id}/delete`) | MailboxController | mailboxes/* | authed |
| `/mailboxes/gmail/connect`, `/reconnect/{id}`, `/callback` | GmailConnectController | — | authed |
| `/searches` (+`/{id}/delete`) | SavedSearchController | — | authed |
| `/account`, `/account/{notifications,appearance,privacy,password,email}` | AccountController | account-* | authed |
| `/billing/{checkout,portal,success,cancel,cancel-subscription}` | BillingController | billing/* | mixed |
| `/team/invite`, `/team/invite/accept` | TeamInviteController | team/* | authed |
| `/digest/opt-out` | DigestController | digest/* | public |
| admin revenue | AdminRevenueController | admin/* | admin only (`AdminAuthorizer`) |

Shared logic lives in services (`ChatService`, `MailAccountService`, `ReplyService`, `BillingService`, …); controllers only bind, validate and render.
