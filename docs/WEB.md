# WEB — Thymeleaf website (server-rendered)

Templates: `src/main/resources/templates`. Shared fragments in `templates/fragments/`
(`app-nav`, `settings-nav`, `chat-pane`, `live-refresh`, `seo`, `cookie-banner`,
`install-banner`, `sw-register`). Model advice: `NavModelAdvice`, `SiteModelAdvice`.

## Public pages
| Route | Controller | Template |
|---|---|---|
| `/` | PricingController | landing |
| `/pricing` | PricingController | pricing |
| `/demo` | DemoController (`DemoConversationService`) | conversation-style demo |
| `/privacy` `/terms` `/refund` | LegalController | legal |
| `/login` `/register` | AuthController | login / register |
| `/password/forgot` `/password/reset` | PasswordResetController | password/* |
| `/verify-email` (+ POST `/verify-email/resend`) | EmailVerificationController | verify-email |
| `/digest/opt-out` | DigestController | digest/opt-out |
| `/robots.txt` `/sitemap.xml` `/images/og-card.png` | SeoController | generated |
| `/manifest.webmanifest` `/sw.js` `/offline` `/icons/*` `/apple-touch-icon.png` | PwaController | PWA shell |

## Authenticated pages
| Route | Controller | Notes |
|---|---|---|
| `GET /chats` (`page,q,mailbox,filter=ALL|UNREAD|AWAITING|ATTACHMENTS`) | ConversationController | list + dashboard; page size 30 |
| `GET /chats/{key}` , `/chats/{key}/pane` | ConversationController | open chat (marks read); pane = HTML fragment |
| `GET/POST /chats/new` | ConversationController | compose new conversation |
| `POST /chats/{key}/send` | ConversationController | `body, subject, asReply, attachments[], mailbox` |
| `POST /chats/refresh` | ConversationController | JSON `{imported:n}` heartbeat |
| `GET /chats/events` | ChatEventsController | SSE new-mail signal |
| `/mailboxes`, `/mailboxes/new`, `POST /mailboxes/{id}/sync|signature|delete`, `POST /mailboxes/preferences` | MailboxController | |
| `/mailboxes/gmail/connect|reconnect/{id}|callback` | GmailConnectController | Gmail OAuth |
| `/account`, `/account/notifications|appearance|privacy`, POST `/account/password|email|appearance` | AccountController | |
| `POST /billing/checkout|portal|cancel-subscription`, `GET /billing/success|cancel` | BillingController | Stripe |
| `/team/invite`, `/team/invite/accept` | TeamInviteController | |
| `POST /searches/{id}/delete` | SavedSearchController | |
| `/admin/revenue` + POSTs | AdminRevenueController | admin only |
| `/threads`, `/senders` | ChatRedirectController | legacy redirect to /chats |

Services behind /chats: `ChatService`, `ConversationListService`, `ConversationService`,
`ReplyService`, `OutboundMessageService`, `MailAccountService`, `MailboxPollingService`.
