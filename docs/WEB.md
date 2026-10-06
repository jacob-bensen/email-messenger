# WEB — Thymeleaf website

Templates: `src/main/resources/templates`. Fragments: `fragments/` (`app-nav`,
`settings-nav`, `chat-pane`, `live-refresh`, `seo`, `cookie-banner`,
`install-banner`, `sw-register`). Global model: `NavModelAdvice`,
`SiteModelAdvice`.

| Route | Auth | Controller → services | Template |
|---|---|---|---|
| `/`, `/pricing`, `/demo` | public | `PricingController` (both `/` and `/pricing`), `DemoController` → `DemoConversationService` | landing, pricing |
| `/privacy` `/terms` `/refund` | public | `LegalController` | legal |
| `/login` `/register` `/auth/google/start` | public | `AuthController`, `OAuthStartController` → `UserService`, `OAuth2ProvisioningService` | login, register |
| `/password/forgot` `/password/reset` | public | `PasswordResetController` | password/* |
| `/verify-email`, POST `/verify-email/resend` | public / user | `EmailVerificationController` | verify-email |
| `/chats` (workspace), `/chats/{key}`, `/chats/{key}/pane`, POST `/chats/refresh`, `/chats/new`, POST `/chats/{key}/send` | user | `ConversationController` → `ChatService`, `ConversationListService`, `OutboundMessageService`, `ReplyService` | chat, compose, fragments/chat-pane |
| `/chats/events` (SSE) | user | `ChatEventsController` → `ChatEventStream` | — |
| `/threads`, `/senders` | user | `ChatRedirectController` (redirect to /chats) | — |
| `/mailboxes`, `/mailboxes/new`, POST `{id}/sync`, `/preferences`, `{id}/signature`, `{id}/delete` | user | `MailboxController` → `MailAccountService` | mailboxes/* |
| `/mailboxes/gmail/connect|reconnect/{id}|callback` | user | `GmailConnectController` → `GmailOAuthClient` | — |
| POST `/searches/{id}/delete` | user | `SavedSearchController` → `SavedSearchService` | — |
| `/account`, `/account/notifications|appearance|privacy`, POST `/account/password|email|appearance` | user | `AccountController` → `AccountService` | account-* |
| POST `/billing/checkout|portal`, `/billing/success|cancel|cancel-subscription` | user (cancel public) | `BillingController` → `BillingService` | billing/* |
| POST `/billing/webhook` | public (Stripe sig) | `BillingWebhookController` | — |
| `/team/invite`, `/team/invite/accept` | user | `TeamInviteController` → `TeamInviteService` | team/* |
| `/digest/opt-out` | public | `DigestController` | digest/opt-out |
| `/admin/revenue`, POST `/admin/retention/win-back`, `/admin/revenue/reconcile-billing-period` | admin | `AdminRevenueController` | admin/revenue |
| `/manifest.webmanifest`, `/sw.js`, `/offline`, `/icons/*`, `/robots.txt`, `/sitemap.xml` | public | `PwaController`, `SeoController` | — |

