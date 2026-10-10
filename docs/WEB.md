# WEB.md — Thymeleaf website

Templates in `src/main/resources/templates`; fragments in `fragments/` (app-nav, settings-nav, chat-pane,
live-refresh, install-banner, cookie-banner, seo, sw-register). Global model attributes via `NavModelAdvice`/`SiteModelAdvice`.

| Page | Routes | Controller |
|---|---|---|
| Landing/pricing/demo | `/`, `/pricing`, `/demo` | `PricingController`, `DemoController` |
| Auth | `/login`, `/register`, `/auth/google/start`, `/password/forgot|reset`, `/verify-email[/resend]` | `auth.*Controller` |
| Chats workspace | `/chats`, `/chats/new`, `/chats/{key}`, `/chats/{key}/pane`, `POST /chats/{key}/send`, `POST /chats/refresh`, `/chats/events` | `ConversationController`, `ChatEventsController` |
| Legacy redirects | `/threads/**`, `/senders` → `/chats` | `ChatRedirectController` |
| Mailboxes | `/mailboxes`, `/new`, `/{id}/sync|signature|delete`, `/preferences`, `/mailboxes/gmail/connect|reconnect/{id}|callback` | `MailboxController`, `GmailConnectController` |
| Saved searches | `POST /searches`, `/searches/{id}/delete` | `SavedSearchController` |
| Account | `/account`, `/account/notifications|appearance|privacy|password|email` | `AccountController` |
| Billing | `POST /billing/checkout|portal|cancel-subscription`, `/billing/success|cancel` | `BillingController` |
| Teams | `/team/invite`, `/team/invite/accept` | `TeamInviteController` |
| Admin | `/admin/revenue` (+ reconcile, win-back POSTs) | `AdminRevenueController` |
| Legal/SEO/PWA | `/privacy /terms /refund /robots.txt /sitemap.xml /manifest.webmanifest /sw.js /offline /icons/**` | `LegalController`, `SeoController`, `PwaController` |
| Digest | `/digest/opt-out` | `DigestController` |

Controllers delegate to services (`ConversationService`, `ReplyService`, `OutboundMessageService`, `ChatService`,
`MailAccountService`, `BillingService`, …); no logic duplicated in templates.
