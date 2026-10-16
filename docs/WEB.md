# WEB.md — Thymeleaf website

Templates in `src/main/resources/templates`; fragments in `templates/fragments`
(`app-nav`, `settings-nav`, `chat-pane`, `live-refresh`, `seo`, `cookie-banner`, `install-banner`, `sw-register`).
Global model data comes from `NavModelAdvice` and `SiteModelAdvice`. Controllers are thin and delegate to services
(`ChatService`, `ConversationListService`, `OutboundMessageService`, `MailAccountService`, `BillingService`, …).

## Public pages
`/` and `/pricing` (PricingController → `landing.html`/`pricing.html`), `/demo`, `/login`, `/register`,
`/password/forgot|reset`, `/verify-email`, `/privacy|/terms|/refund` (`legal.html`), `/digest/opt-out`,
`/billing/cancel`, `/robots.txt`, `/sitemap.xml`, PWA: `/manifest.webmanifest`, `/sw.js`, `/offline`, icons.

## Authenticated pages
| Route | Template | Notes |
|---|---|---|
| `GET /chats`, `/chats/new` (+POST), `/chats/{key}`, `/chats/{key}/pane`, `POST /chats/{key}/send` | `chat.html`, `compose.html`, `fragments/chat-pane` | main workspace |
| `/mailboxes` (+`/new`, `/{id}/sync`, `/{id}/signature`, `/{id}/delete`, `/preferences`), `/mailboxes/gmail/*` | `mailboxes/*` | connect IMAP / Gmail OAuth |
| `/searches`, `/searches/{id}/delete` | (redirects) | saved searches |
| `/account`, `/account/notifications|appearance|privacy|password|email` | `account*.html` | settings |
| `/billing/checkout|portal|success|cancel-subscription` | `billing/*` | Stripe |
| `/team/invite`, `/team/invite/accept` | `team/*` | teams |
| `/admin/revenue` (+POST actions) | `admin/revenue.html` | admin-only (`AdminAuthorizer`) |
| `/threads`, `/senders` | — | redirect to `/chats` (`ChatRedirectController`) |

Live updates: `fragments/live-refresh.html` subscribes to `GET /chats/events` (SSE) and morphs rail/pane.
