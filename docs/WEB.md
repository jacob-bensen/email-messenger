# WEB (Thymeleaf website)

Templates: `src/main/resources/templates`. Shared fragments in
`templates/fragments` (app-nav, settings-nav, chat-pane, live-refresh, seo,
cookie-banner, install-banner, sw-register). Controllers are package-private
`@Controller` classes.

| Route(s) | Controller | Template | Notes |
|---|---|---|---|
| `/`, `/pricing`, `/demo` | PricingController, DemoController | landing, pricing, chat | public; demo uses `DemoConversationService` |
| `/login`, `/register`, `/auth/google/start` | AuthController, OAuthStartController | login, register | public |
| `/password/forgot`, `/password/reset` | PasswordResetController | password/* | public |
| `/verify-email`, `/verify-email/resend` | EmailVerificationController | verify-email | |
| `/chats`, `/chats/new`, `/chats/{key}`, `/chats/{key}/pane`, `/chats/{key}/send`, `/chats/refresh`, `/chats/events` | ConversationController, ChatEventsController | chat, compose, fragments/chat-pane | main workspace; services: ChatService, ConversationListService, ReplyService, OutboundMessageService, MailboxPollingService |
| `/threads`, `/threads/**`, `/senders` | ChatRedirectController | — | legacy redirects to `/chats` |
| `/mailboxes`, `/mailboxes/new`, `/{id}/sync`, `/{id}/signature`, `/{id}/delete`, `/preferences` | MailboxController | mailboxes/* | MailAccountService, PlanLimitService |
| `/mailboxes/gmail/connect`, `/reconnect/{id}`, `/callback` | GmailConnectController | — | Gmail OAuth |
| `/searches`, `/searches/{id}/delete` | SavedSearchController | — | |
| `/account`, `/account/notifications`, `/account/appearance`, `/account/privacy`, `/account/password`, `/account/email` | AccountController | account-* | |
| `/billing/checkout`, `/portal`, `/success`, `/cancel`, `/cancel-subscription` | BillingController | billing/* | Stripe gateways |
| `/team/invite`, `/team/invite/accept` | TeamInviteController | team/* | |
| `/digest/opt-out` | DigestController | digest/opt-out | public |
| `/admin/revenue`, `/admin/revenue/reconcile-billing-period`, `/admin/retention/win-back` | AdminRevenueController | admin/revenue | admin only |
| `/privacy`, `/terms`, `/refund` | LegalController | legal | |
| `/robots.txt`, `/sitemap.xml`, `/images/og-card.png` | SeoController | — | |
| `/manifest.webmanifest`, `/icons/*`, `/sw.js`, `/offline` | PwaController | — | PWA shell |
