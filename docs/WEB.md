# WEB.md — Thymeleaf website

Templates: `src/main/resources/templates`. Fragments: `fragments/{app-nav,chat-pane,cookie-banner,install-banner,live-refresh,seo,settings-nav,sw-register}.html`.

| Route(s) | Controller | Template | Auth |
|---|---|---|---|
| `/`, `/pricing` | PricingController | landing, pricing | public |
| `/demo` | DemoController | chat (demo data) | public |
| `/privacy /terms /refund` | LegalController | legal | public |
| `/login /register` | AuthController | login, register | public |
| `/password/forgot|reset` | PasswordResetController | password/* | public |
| `/verify-email` (+`/resend` POST) | EmailVerificationController | verify-email | GET public |
| `/auth/google/start` | OAuthStartController | — | public |
| `/chats`, `/chats/{key}`, `/chats/{key}/pane`, `/chats/new`, `/chats/{key}/send`, `/chats/refresh`, `/chats/events` | ConversationController, ChatEventsController | chat, compose, fragments/chat-pane | user |
| `/threads/**`, `/senders` | ChatRedirectController | redirect to /chats | user |
| `/mailboxes`, `/mailboxes/new`, `/{id}/sync|signature|delete`, `/preferences` | MailboxController | mailboxes/* | user |
| `/mailboxes/gmail/connect|reconnect/{id}|callback` | GmailConnectController | — | user |
| `/searches`, `/searches/{id}/delete` | SavedSearchController | — | user |
| `/account`, `/account/notifications|appearance|privacy|password|email` | AccountController | account-* | user |
| `/billing/checkout|portal|success|cancel-subscription`, `/billing/cancel`, `/billing/webhook` | BillingController, BillingWebhookController | billing/* | mixed; webhook is signature-verified |
| `/team/invite`, `/team/invite/accept` | TeamInviteController | team/* | user |
| `/digest/opt-out` | DigestController | digest/* | public |
| `/admin/revenue` (+ POST actions) | AdminRevenueController | admin/revenue | admin (`AdminAuthorizer`) |
| `/manifest.webmanifest /sw.js /offline /icons/** /apple-touch-icon.png` | PwaController | — | public |
| `/robots.txt /sitemap.xml /images/og-card.png` | SeoController | — | public |

Services used by chat pages: `ConversationListService`, `ChatService`, `ReplyService`, `OutboundMessageService`, `MailAccountService`, `MailboxPollingService`, `ConversationKeyService`, `BillingBannerService`.
