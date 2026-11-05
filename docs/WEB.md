# WEB.md — Thymeleaf website

Templates: `src/main/resources/templates`. Fragments in `templates/fragments` (incl. `live-refresh.html`).

| Route | Controller | Template | Auth | Notes |
|---|---|---|---|---|
| `/`, `/pricing` | `PricingController` | landing / pricing | public | |
| `/demo` | `DemoController` | conversation | public | `DemoConversationService` |
| `/login`, `/register` | `AuthController` | login/register | public | |
| `/auth/google/start` | `OAuthStartController` | – | public | |
| `/password/forgot`, `/password/reset` | `PasswordResetController` | password/* | public | |
| `/verify-email` (+`/resend` POST) | `EmailVerificationController` | verify-email | public/auth | |
| `/privacy`, `/terms`, `/refund` | `LegalController` | legal | public | |
| `/chats` (GET) | `ConversationController` | chat | user | params: mailbox, q, filter, page |
| `/chats/new` GET/POST | same | compose | user | |
| `/chats/refresh` POST | same | – | user | triggers mailbox refresh |
| `/chats/{key}` GET | same | chat | user | marks read |
| `/chats/{key}/pane` GET | same | fragment | user | HTML fragment |
| `/chats/{key}/send` POST | same | chat | user | multipart; reply/new subject |
| `/chats/events` GET | `ChatEventsController` | SSE | user | live new-mail events |
| `/mailboxes` (+`/new`, `/{id}/sync`, `/{id}/signature`, `/{id}/delete`, `/preferences`) | `MailboxController` | mailboxes/* | user | |
| `/mailboxes/gmail/{connect,reconnect/{id},callback}` | `GmailConnectController` | – | user | Gmail OAuth |
| `/searches` POST, `/searches/{id}/delete` | `SavedSearchController` | – | user | |
| `/account`, `/account/{notifications,appearance,privacy,password,email}` | `AccountController` | account-* | user | |
| `/billing/{checkout,portal,success,cancel,cancel-subscription}` | `BillingController` | billing/* | user (`/billing/cancel` public) | |
| `/billing/webhook` | `BillingWebhookController` | – | public, Stripe-signed | |
| `/team/invite`, `/team/invite/accept` | `TeamInviteController` | team/* | user | |
| `/digest/opt-out` | `DigestController` | digest/* | public | |
| `/admin/revenue` (+POST actions) | `AdminRevenueController` | admin/* | admin | |
| `/manifest.webmanifest`, `/sw.js`, `/offline`, `/icons/*` | `PwaController` | – | public | PWA shell |
| `/robots.txt`, `/sitemap.xml`, `/images/og-card.png` | `SeoController` | – | public | |
| `/threads`, `/senders` | `ChatRedirectController` | – | user | legacy → `/chats` |
