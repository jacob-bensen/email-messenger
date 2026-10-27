# WEB (Thymeleaf site)

Templates in `src/main/resources/templates`; shared fragments in `fragments/`
(`app-nav`, `settings-nav`, `chat-pane`, `live-refresh`, `seo`, `cookie-banner`,
`install-banner`, `sw-register`). Model attributes common to all pages come from
`NavModelAdvice` / `SiteModelAdvice`.

## Public pages
`/` and `/pricing` (PricingController), `/demo`, `/login`, `/register`,
`/password/forgot|reset`, `/verify-email`, `/privacy|terms|refund`
(LegalController), `/robots.txt`, `/sitemap.xml`, `/images/og-card.png`
(SeoController), PWA: `/manifest.webmanifest`, `/sw.js`, `/offline`, icons (PwaController).

## Authenticated pages
| Route | Controller | Notes |
|---|---|---|
| `/chats`, `/chats/new`, `/chats/{key}`, `/chats/{key}/pane`, `POST /chats/{key}/send`, `POST /chats/refresh`, `GET /chats/events` | ConversationController, ChatEventsController | main workspace; `/threads`, `/senders` redirect here (ChatRedirectController) |
| `/mailboxes` (+`/new`, `/{id}/sync`, `/preferences`, `/{id}/signature`, `/{id}/delete`), `/mailboxes/gmail/connect|reconnect/{id}|callback` | MailboxController, GmailConnectController | |
| `POST /searches`, `/searches/{id}/delete` | SavedSearchController | |
| `/account`, `/account/notifications|appearance|privacy`, `POST /account/password|email` | AccountController | |
| `/billing/checkout|portal|success|cancel|cancel-subscription` | BillingController | |
| `/team/invite`, `/team/invite/accept` | TeamInviteController | |
| `/digest/opt-out` | DigestController | public |
| `/admin/revenue` (+ POST reconcile, win-back) | AdminRevenueController | operator allowlist |
| `/auth/google/start` | OAuthStartController | |
