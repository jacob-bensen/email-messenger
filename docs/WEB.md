# Website (Thymeleaf)

Templates: `src/main/resources/templates`; shared fragments in `templates/fragments` (`app-nav`, `settings-nav`, `chat-pane`, `live-refresh`, `seo`, `cookie-banner`, `install-banner`, `sw-register`).

| Route | Controller | Template | Auth |
|---|---|---|---|
| `/`, `/pricing` | PricingController | landing, pricing | public |
| `/demo` | DemoController | chat (demo data via DemoConversationService) | public |
| `/login`, `/register`, `/auth/google/start` | AuthController, OAuthStartController | login, register | public |
| `/password/forgot`, `/password/reset` | PasswordResetController | password/* | public |
| `/verify-email`, `/verify-email/resend` | EmailVerificationController | verify-email | public / user |
| `/privacy`, `/terms`, `/refund` | LegalController | legal | public |
| `/robots.txt`, `/sitemap.xml`, `/images/og-card.png` | SeoController | — | public |
| `/manifest.webmanifest`, `/sw.js`, `/offline`, icons | PwaController | — | public |
| `/chats` (list, `q`, `mailbox`, `filter`, `page`) | ConversationController | chat | user |
| `/chats/{key}`, `/chats/{key}/pane` | ConversationController | chat, fragments/chat-pane | user |
| `/chats/new`, `/chats/{key}/send` (multipart) | ConversationController | compose, chat | user |
| `/chats/refresh` (POST, JSON), `/chats/events` (SSE) | ConversationController, ChatEventsController | — | user |
| `/threads`, `/senders` | ChatRedirectController | redirect to /chats | user |
| `/mailboxes` (+`/new`, `/{id}/sync`, `/preferences`, `/{id}/signature`, `/{id}/delete`) | MailboxController | mailboxes/* | user |
| `/mailboxes/gmail/connect`, `/reconnect/{id}`, `/callback` | GmailConnectController | — | user |
| `/searches`, `/searches/{id}/delete` | SavedSearchController | — | user |
| `/account`, `/account/{notifications,appearance,privacy,password,email}` | AccountController | account-* | user |
| `/billing/checkout`, `/portal`, `/success`, `/cancel`, `/cancel-subscription` | BillingController | billing/* | user (`/cancel` public) |
| `/billing/webhook` | BillingWebhookController | — | public, Stripe signature |
| `/team/invite`, `/team/invite/accept` | TeamInviteController | team/* | user |
| `/digest/opt-out` | DigestController | digest/opt-out | public |
| `/admin/revenue` (+ reconcile, win-back POSTs) | AdminRevenueController | admin/revenue | admin (AdminAuthorizer) |

Services used by the chat pages: `ConversationListService`, `ChatService`, `OutboundMessageService`, `ReplyService`, `MailAccountService`, `MailboxPollingService`, `BillingBannerService`.
