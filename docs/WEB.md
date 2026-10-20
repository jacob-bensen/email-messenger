# Thymeleaf website

All routes below are served by controllers in `com.emailmessenger.*`; templates in `src/main/resources/templates`.
Auth column: P = public, A = authenticated.

| Route | Auth | Template / result | Notes |
|---|---|---|---|
| `/` | P | landing | |
| `/pricing`, `/demo`, `/privacy`, `/terms`, `/refund` | P | pricing, chat(demo), legal | |
| `/login`, `/register`, `/auth/google/start` | P | login, register | form login param `email`/`password`; Google optional |
| `/password/forgot`, `/password/reset` | P | password/* | |
| `/verify-email` (+POST `/verify-email/resend`) | P / A | verify-email | |
| `/chats` | A | chat | list; params `page`, `q`, `mailbox`, `filter`=ALL/UNREAD/AWAITING/ATTACHMENTS; page size 30 |
| `/chats/{key}` | A | chat | opens conversation, marks read |
| `/chats/{key}/pane` | A | fragments/chat-pane | HTML fragment for in-place swap |
| `/chats/{key}/send` POST | A | redirect | params `body`, `subject`, `asReply`, `attachments[]`, `mailbox` |
| `/chats/new` GET/POST | A | compose | params `to`, `subject`, `body`, `attachments[]`, `mailbox` |
| `/chats/refresh` POST | A | JSON `{"imported":n}` | foreground heartbeat |
| `/chats/events` | A | SSE | `NewMailEvent` |
| `/threads`, `/senders` | A | redirect to `/chats` | legacy |
| `/mailboxes`, `/mailboxes/new`, `/mailboxes/{id}/sync|signature|delete`, `/mailboxes/preferences` | A | mailboxes/* | |
| `/mailboxes/gmail/connect|reconnect/{id}|callback` | A | OAuth flow | |
| `/searches`, `/searches/{id}/delete` POST | A | redirect | saved searches |
| `/account`, `/account/{notifications,appearance,privacy,password,email}` | A | account-* | |
| `/billing/checkout|portal|cancel-subscription` POST, `/billing/success|cancel` | A/P | redirect | Stripe |
| `/billing/webhook` POST | P (signature) | JSON | CSRF-exempt |
| `/team/invite`, `/team/invite/accept` | A | team/* | |
| `/digest/opt-out` | P | digest/* | |
| `/admin/**` | A + allowlist | admin/* | non-admin gets 404 |
| `/manifest.webmanifest`, `/sw.js`, `/offline`, `/icons/**`, `/robots.txt`, `/sitemap.xml` | P | PWA/SEO | |

Shared fragments: `app-nav`, `chat-pane`, `live-refresh`, `settings-nav`, `seo`, `cookie-banner`, `install-banner`, `sw-register`.
Model-wide attributes come from `NavModelAdvice` / `SiteModelAdvice`.
