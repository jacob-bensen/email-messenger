# Thymeleaf website

Templates in `src/main/resources/templates`; shared pieces in `fragments/`.

## Routes (auth = session required unless "public")
| Route | Controller | Notes |
|---|---|---|
| `GET /`, `/pricing`, `/demo`, `/privacy`, `/terms`, `/refund` | Pricing/Demo/LegalController | public |
| `GET /login`, `GET/POST /register`, `/password/forgot`, `/password/reset`, `/verify-email` | auth | public |
| `GET /auth/google/start`, `/mailboxes/gmail/{connect,reconnect/{id},callback}` | OAuthStart/GmailConnect | OAuth flows |
| `GET /chats` | ConversationController | list + workspace; params `page,q,mailbox,filter` |
| `GET /chats/{key}`, `/chats/{key}/pane` | ″ | opens (marks read); pane = HTML fragment |
| `GET/POST /chats/new`, `POST /chats/{key}/send`, `POST /chats/refresh` | ″ | compose / send (multipart) / poll |
| `GET /chats/events` | ChatEventsController | SSE, live new-mail |
| `/mailboxes` (+`/new`, `POST /`, `/{id}/sync`, `/{id}/signature`, `/{id}/delete`, `/preferences`) | MailboxController | |
| `POST /searches`, `/searches/{id}/delete` | SavedSearchController | |
| `/account`, `/account/{notifications,appearance,privacy}`, `POST /account/{password,email}` | AccountController | |
| `/billing/{checkout,portal,success,cancel,cancel-subscription}`, `POST /billing/webhook` | billing | |
| `/team/invite`, `/team/invite/accept` | TeamInviteController | |
| `/admin/revenue`, `POST /admin/revenue/...` | admin | admin only |
| `/manifest.webmanifest`, `/sw.js`, `/offline`, `/icons/**`, `/robots.txt`, `/sitemap.xml` | Pwa/Seo | public |

## Known layering issue
`ConversationController.send` / `/chats/new` contain business logic (sender-account choice,
signature appending, reply-vs-new decision, recipient derivation). It must move into a service
before a REST API can share it (INTERNAL_TODO #2).
