# Thymeleaf website

Templates: `src/main/resources/templates`. Fragments in `templates/fragments`
(`app-nav`, `settings-nav`, `chat-pane`, `live-refresh`, `seo`, `cookie-banner`,
`install-banner`, `sw-register`). Global model: `NavModelAdvice`,
`SiteModelAdvice`; errors: `GlobalExceptionHandler` → `error.html`.
"Public" = permitAll in `SecurityConfig`; everything else needs a session.

| Route | Method | Auth | Controller | Template / result |
|---|---|---|---|---|
| `/`, `/pricing` | GET | public | PricingController | landing, pricing |
| `/demo` | GET | public | DemoController | demo chat (DemoConversationService) |
| `/privacy` `/terms` `/refund` | GET | public | LegalController | legal |
| `/robots.txt` `/sitemap.xml` `/images/og-card.png` | GET | public | SeoController | |
| `/manifest.webmanifest` `/sw.js` `/offline` `/icons/*` `/apple-touch-icon.png` | GET | public | PwaController | PWA shell |
| `/login` | GET/POST | public | AuthController / Spring | login.html |
| `/register` | GET/POST | public | AuthController | register.html |
| `/auth/google/start` | GET | public | OAuthStartController | OIDC redirect |
| `/password/forgot`, `/password/reset` | GET/POST | public | PasswordResetController | password/* |
| `/verify-email`, `/verify-email/resend` | GET/POST | public / auth | EmailVerificationController | verify-email |
| `/digest/opt-out` | GET | public | DigestController | digest/opt-out |
| `/chats` | GET | auth | ConversationController | chat.html (list + empty pane); params `page,q,mailbox,filter` |
| `/chats/new` | GET/POST | auth | ConversationController | compose.html |
| `/chats/{key}` | GET | auth | ConversationController | chat.html (marks read) |
| `/chats/{key}/pane` | GET | auth | ConversationController | `fragments/chat-pane :: pane` |
| `/chats/{key}/send` | POST multipart | auth | ConversationController | redirect; params `body,subject,asReply,attachments,mailbox` |
| `/chats/refresh` | POST | auth | ConversationController | JSON `{"imported":n}` |
| `/chats/events` | GET | auth | ChatEventsController | SSE stream |
| `/threads`, `/threads/**`, `/senders` | GET | auth | ChatRedirectController | redirect to /chats |
| `/mailboxes` (+`/new`, `/{id}/sync`, `/preferences`, `/{id}/signature`, `/{id}/delete`) | GET/POST | auth | MailboxController | mailboxes/* |
| `/mailboxes/gmail/{connect,reconnect/{id},callback}` | GET | auth | GmailConnectController | Gmail OAuth |
| `/searches`, `/searches/{id}/delete` | POST | auth | SavedSearchController | |
| `/account`, `/account/{notifications,appearance,privacy}`, `/account/{password,email}` | GET/POST | auth | AccountController | account-* |
| `/team/invite`, `/team/invite/accept` | GET/POST | auth | TeamInviteController | team/* |
| `/billing/{checkout,portal,cancel-subscription}` (POST), `/billing/{success,cancel,cancel-subscription}` (GET) | | auth (`/billing/cancel` public) | BillingController | billing/* |
| `/billing/webhook` | POST | public, Stripe signature | BillingWebhookController | JSON |
| `/admin/revenue` + 2 POSTs | | auth + AdminAuthorizer | AdminRevenueController | admin/revenue |
| `/actuator/health` | GET | public | actuator | |

Services used by the chat pages: ConversationListService (list/counts),
ChatService (build/markRead), ReplyService + OutboundMessageService (send),
MailAccountService, MailboxPollingService, BillingBannerService.
