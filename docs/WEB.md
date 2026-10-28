# Website (Thymeleaf)

Templates in `src/main/resources/templates`; shared fragments in `fragments/` (`app-nav`, `settings-nav`, `chat-pane`, `live-refresh`, `seo`, `cookie-banner`, `install-banner`, `sw-register`).

| Route(s) | Template | Controller → services |
|---|---|---|
| `/`, `/pricing`, `/demo` | landing, pricing | `PricingController`, `DemoController` → `DemoConversationService` |
| `/login`, `/register`, `/verify-email`, `/password/forgot|reset` | login, register, verify-email, password/* | `auth.*Controller` → `UserService`, `EmailVerificationService`, `PasswordResetService` |
| `/chats`, `/chats/new`, `/chats/{key}`, `/chats/{key}/pane`, `/chats/{key}/send`, `/chats/events`, `/chats/refresh` | chat, compose, fragments/chat-pane | `ConversationController` → `ConversationListService`, `ChatService`, `ReplyService`, `OutboundMessageService`, `MailboxPollingService` |
| `/mailboxes/**`, `/mailboxes/gmail/**` | mailboxes/* | `MailboxController`, `GmailConnectController` → `MailAccountService` |
| `/searches/{id}/delete` | (redirect) | `SavedSearchController` → `SavedSearchService` |
| `/account/**` | account-* | `AccountController` → `AccountService` |
| `/billing/**` | billing/* | `BillingController` → `BillingService` |
| `/team/invite*` | team/* | `TeamInviteController` |
| `/admin/revenue` | admin/revenue | `AdminRevenueController` |
| `/privacy`, `/terms`, `/refund` | legal | `LegalController` |
| `/digest/opt-out` | digest/opt-out | `DigestController` |
| `/threads/**`, `/senders` | redirect to `/chats` | `ChatRedirectController` |
