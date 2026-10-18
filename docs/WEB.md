# Thymeleaf website

Templates in `src/main/resources/templates`; shared fragments in `fragments/` (`app-nav`, `settings-nav`, `chat-pane`, `live-refresh`, `seo`, `cookie-banner`, `install-banner`, `sw-register`).

| Area | Routes | Controller | Template |
|---|---|---|---|
| Marketing | `/`, `/pricing`, `/demo`, `/privacy`, `/terms`, `/refund` | `PricingController`, `DemoController`, `LegalController` | `landing`, `pricing`, `legal` |
| Auth | `/login`, `/register`, `/auth/google/start`, `/password/forgot`, `/password/reset`, `/verify-email`, `/verify-email/resend` | `auth.*Controller` | `login`, `register`, `password/*`, `verify-email` |
| Chats workspace | `/chats`, `/chats/new`, `/chats/{key}`, `/chats/{key}/pane`, `/chats/{key}/send`, `/chats/refresh`, `/chats/events`; legacy `/threads/**`,`/senders` redirect | `ConversationController`, `ChatEventsController`, `ChatRedirectController` | `chat`, `compose`, `fragments/chat-pane` |
| Mailboxes | `/mailboxes`, `/new`, `/{id}/sync`, `/{id}/signature`, `/{id}/delete`, `/preferences`; Gmail `/mailboxes/gmail/connect|reconnect/{id}|callback` | `MailboxController`, `GmailConnectController` | `mailboxes/*` |
| Saved searches | `POST /searches`, `/searches/{id}/delete` | `SavedSearchController` | (in `chat`) |
| Account | `/account`, `/account/notifications|appearance|privacy`, `POST /account/password|email` | `AccountController` | `account*` |
| Billing | `/billing/checkout|portal|success|cancel|cancel-subscription`, `/billing/webhook` | `BillingController`, `BillingWebhookController` | `billing/*` |
| Teams | `/team/invite`, `/team/invite/accept` | `TeamInviteController` | `team/*` |
| Digest | `/digest/opt-out` | `DigestController` | `digest/opt-out` |
| Admin | `/admin/revenue`, `POST /admin/revenue/reconcile-billing-period`, `POST /admin/retention/win-back` | `AdminRevenueController` | `admin/revenue` |
| PWA/SEO | `/manifest.webmanifest`, `/sw.js`, `/offline`, `/icons/*`, `/robots.txt`, `/sitemap.xml`, `/images/og-card.png` | `PwaController`, `SeoController` | — |

Services used by chat pages: `ConversationListService`, `ChatService`, `OutboundMessageService`/`ReplyService`, `MailboxPollingService`, `MailAccountService`, `BillingBannerService`.
