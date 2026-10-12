# Website (Thymeleaf)

Templates: `src/main/resources/templates`. Shared fragments: `fragments/` (`app-nav`,
`settings-nav`, `chat-pane`, `live-refresh`, `seo`, `cookie-banner`, `install-banner`,
`sw-register`). Model-wide data from `NavModelAdvice` / `SiteModelAdvice`.

| Area | Routes | Controller |
|---|---|---|
| Public marketing | `/`, `/pricing`, `/demo`, `/privacy`, `/terms`, `/refund` | `PricingController`, `DemoController`, `LegalController` |
| Auth | `/login`, `/register`, `/auth/google/start`, `/password/forgot`, `/password/reset`, `/verify-email`, `/verify-email/resend` | `AuthController`, `OAuthStartController`, `PasswordResetController`, `EmailVerificationController` |
| Chats workspace | `GET /chats`, `/chats/new` (GET/POST), `/chats/{key}`, `/chats/{key}/pane`, `POST /chats/{key}/send`, `POST /chats/refresh` (JSON), `GET /chats/events` (SSE); `/threads**`,`/senders` redirect | `ConversationController`, `ChatEventsController`, `ChatRedirectController` |
| Mailboxes | `/mailboxes` (+`/new`, `/{id}/sync`, `/preferences`, `/{id}/signature`, `/{id}/delete`), `/mailboxes/gmail/{connect,reconnect/{id},callback}` | `MailboxController`, `GmailConnectController` |
| Saved searches | `POST /searches`, `POST /searches/{id}/delete` | `SavedSearchController` |
| Account | `/account`, `/account/notifications`, `/account/appearance`, `/account/privacy`, `POST /account/{password,email}` | `AccountController` |
| Billing | `POST /billing/{checkout,portal,cancel-subscription}`, `/billing/{success,cancel,cancel-subscription}`, `POST /billing/webhook` | `BillingController`, `BillingWebhookController` |
| Teams | `/team/invite`, `/team/invite/accept` | `TeamInviteController` |
| Other | `/digest/opt-out`, `/admin/revenue` (+POST reconcile/win-back), PWA (`/manifest.webmanifest`, `/sw.js`, `/offline`, icons), SEO (`/robots.txt`, `/sitemap.xml`, `/images/og-card.png`) | `DigestController`, `AdminRevenueController`, `PwaController`, `SeoController` |
