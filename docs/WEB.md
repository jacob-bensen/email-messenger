# WEB.md — Thymeleaf website

Templates: `src/main/resources/templates`. Fragments: `fragments/` (app-nav, chat-pane, live-refresh, seo,
settings-nav, cookie-banner, install-banner, sw-register).

| Route(s) | Controller | Template | Auth |
|---|---|---|---|
| `/`, `/pricing`, `/demo` | PricingController, DemoController | landing, pricing, chat (demo) | public |
| `/login`, `/register`, `/auth/google/start` | AuthController, OAuthStartController | login, register | public |
| `/password/forgot`, `/password/reset`, `/verify-email[/resend]` | PasswordReset/EmailVerificationController | password/*, verify-email | public (resend: user) |
| `/chats`, `/chats/new`, `/chats/{key}`, `/chats/{key}/pane`, `POST /chats/{key}/send`, `POST /chats/refresh`, `/chats/events` | ConversationController, ChatEventsController | chat, compose, fragments/chat-pane | user |
| `/mailboxes`, `/mailboxes/new`, `/{id}/sync`, `/preferences`, `/{id}/signature`, `/{id}/delete`, `/mailboxes/gmail/{connect,reconnect/{id},callback}` | MailboxController, GmailConnectController | mailboxes/* | user |
| `POST /searches`, `/searches/{id}/delete` | SavedSearchController | (chat rail) | user |
| `/account`, `/account/{notifications,appearance,privacy}`, `POST /account/{password,email,appearance}` | AccountController | account-* | user |
| `/billing/{checkout,portal,success,cancel,cancel-subscription}` | BillingController | billing/* | user (`cancel` public) |
| `/team/invite[/accept]` | TeamInviteController | team/* | user |
| `/admin/revenue` + 2 POSTs | AdminRevenueController | admin/* | admin (`AdminAuthorizer`) |
| `/privacy`, `/terms`, `/refund`, `/digest/opt-out` | LegalController, DigestController | legal, digest/* | public |
| `/manifest.webmanifest`, `/sw.js`, `/offline`, icons, `/robots.txt`, `/sitemap.xml` | PwaController, SeoController | — | public |
| `/threads/**`, `/senders` | ChatRedirectController | redirect to `/chats` | user |

Services used by the chats pages: `ConversationListService`, `ChatService`, `MailAccountService`,
`MailboxPollingService`, `OutboundMessageService`, `ReplyService`, `BillingBannerService`.
