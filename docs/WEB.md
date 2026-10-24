# Thymeleaf website

Templates: `src/main/resources/templates`. Shared fragments: `fragments/` (`app-nav`,
`settings-nav`, `chat-pane`, `live-refresh`, `seo`, `cookie-banner`, `install-banner`,
`sw-register`). `NavModelAdvice` / `SiteModelAdvice` add global model attributes.

| Route | Controller | Template | Auth |
|---|---|---|---|
| `/`, `/pricing` | PricingController | landing, pricing | public |
| `/demo` | DemoController | chat (demo data via `DemoConversationService`) | public |
| `/login`, `/register`, `/auth/google/start` | AuthController, OAuthStartController | login, register | public |
| `/password/forgot`, `/password/reset` (GET/POST) | PasswordResetController | password/* | public |
| `/verify-email` (+POST `/resend`) | EmailVerificationController | verify-email | public / user |
| `/privacy`, `/terms`, `/refund` | LegalController | legal | public |
| `/chats` | ConversationController | chat | user |
| `/chats/{key}`, `/chats/{key}/pane` | ConversationController | chat, fragments/chat-pane | user |
| `POST /chats/{key}/send`, `/chats/new`, `/chats/refresh` | ConversationController | chat, compose | user |
| `/chats/events` | ChatEventsController | SSE stream | user |
| `/threads/**`, `/senders` | ChatRedirectController | redirect → `/chats` | user |
| `/mailboxes` (+`/new`, `/{id}/sync`, `/{id}/signature`, `/{id}/delete`, `/preferences`) | MailboxController | mailboxes/* | user |
| `/mailboxes/gmail/connect`, `/reconnect/{id}`, `/callback` | GmailConnectController | redirects | user |
| `/searches` (+`/{id}/delete`) | SavedSearchController | redirect | user |
| `/account`, `/account/{notifications,appearance,privacy}`, POST `/account/{password,email,appearance}` | AccountController | account-* | user |
| `/billing/{checkout,portal,success,cancel,cancel-subscription}` | BillingController | billing/* | user (`cancel` public) |
| `/billing/webhook` | BillingWebhookController (`@RestController`) | JSON | public, Stripe-signed |
| `/team/invite` (+`/accept`) | TeamInviteController | team/* | user |
| `/digest/opt-out` | DigestController | digest/opt-out | public |
| `/admin/revenue`, POST `/admin/...` | AdminRevenueController | admin/revenue | admin |
| `/manifest.webmanifest`, `/sw.js`, `/offline`, `/icons/**` | PwaController | PWA shell | public |
| `/robots.txt`, `/sitemap.xml`, `/images/og-card.png` | SeoController | — | public |
