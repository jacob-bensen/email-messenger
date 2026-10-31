# WEB.md — Thymeleaf website

Templates in `src/main/resources/templates`, fragments in `templates/fragments`.

| Route(s) | Controller | Template |
|---|---|---|
| `/`, `/pricing`, `/demo` | PricingController, DemoController | landing, pricing, chat (demo data) |
| `/login`, `/register`, `/password/*`, `/verify-email`, `/auth/google/start` | auth.* | login, register, password/*, verify-email |
| `/chats`, `/chats/new`, `/chats/{key}`, `/chats/{key}/pane`, `/chats/{key}/send` | ConversationController | chat, compose, fragments/chat-pane |
| `/mailboxes/**`, `/mailboxes/gmail/**` | MailboxController, GmailConnectController | mailboxes/* |
| `/searches/**` | SavedSearchController | (redirects) |
| `/account/**` | AccountController | account*, settings-nav |
| `/billing/**` | BillingController | billing/* |
| `/team/invite*` | TeamInviteController | team/* |
| `/admin/revenue*` | AdminRevenueController | admin/* |
| `/digest/opt-out` | DigestController | digest/* |
| `/privacy`, `/terms`, `/refund` | LegalController | legal |
| `/manifest.webmanifest`, `/sw.js`, `/offline`, icons | PwaController | — |
| `/robots.txt`, `/sitemap.xml` | SeoController | — |

Shared fragments: app-nav, chat-pane, live-refresh (SSE morph), cookie/install banners, seo.
Service layer used by pages: `ChatService`, `ConversationListService`, `OutboundMessageService`/`ReplyService`, `MailAccountService`, `PlanLimitService`.
