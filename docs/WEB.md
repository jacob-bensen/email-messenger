# Website (Thymeleaf) inventory

Templates in `src/main/resources/templates`; shared fragments in `fragments/` (`app-nav`, `settings-nav`, `chat-pane`, `live-refresh`, `seo`, `cookie-banner`, `install-banner`, `sw-register`).

| Route | Auth | Template / purpose | Service(s) |
|---|---|---|---|
| `/`, `/pricing`, `/demo` | public | landing, pricing, demo chat | `DemoConversationService` |
| `/privacy` `/terms` `/refund` | public | legal | – |
| `/login` `/register` `/auth/google/start` | public | auth | `UserService`, OAuth |
| `/password/forgot`, `/password/reset` | public | reset flow | `PasswordResetService` |
| `/verify-email`, `/verify-email/resend` | public/auth | email verification | `EmailVerificationService` |
| `/chats` (q, mailbox, filter, page) | user | `chat.html` workspace list | `ConversationListService` |
| `/chats/{key}` , `/chats/{key}/pane` | user | open chat / HTML fragment | `ChatService` |
| `POST /chats/{key}/send` | user | send (multipart attachments, `asReply`, `subject`) | `OutboundMessageService` |
| `/chats/new`, `POST /chats/refresh`, `/chats/events` (SSE) | user | compose, manual refresh, live updates | `MailboxPollingService` |
| `/mailboxes`, `/new`, `/{id}/sync`, `/{id}/signature`, `/{id}/delete`, `/preferences`, `/gmail/*` | user | mailbox management | `MailAccountService` |
| `/searches`, `/searches/{id}/delete` | user | saved searches | `SavedSearchService` |
| `/account`, `/account/{notifications,appearance,privacy,password,email}` | user | settings | `AccountService` |
| `/billing/{checkout,portal,success,cancel,cancel-subscription}` | user | Stripe | `BillingService` |
| `/team/invite`, `/team/invite/accept` | user | teams | `TeamInviteService` |
| `/admin/revenue*` | admin | metrics | `*MetricsService` |
| `/digest/opt-out` | public | digest unsubscribe | `WeeklyDigestService` |
| `/manifest.webmanifest`, `/sw.js`, `/offline`, `/icons/*`, `/robots.txt`, `/sitemap.xml` | public | PWA/SEO | – |
