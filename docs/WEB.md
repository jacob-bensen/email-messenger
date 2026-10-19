# Thymeleaf website

Templates in `src/main/resources/templates`; shared fragments in `fragments/`
(`app-nav`, `settings-nav`, `chat-pane`, `live-refresh`, `seo`, `cookie-banner`,
`install-banner`, `sw-register`). Controllers delegate to services; keep it so.

## Public
`/` and `/pricing` (PricingController), `/demo` (DemoController), legal
`/privacy` `/terms` `/refund`, `/robots.txt` `/sitemap.xml` (SeoController),
PWA: `/manifest.webmanifest`, `/icons/*`, `/sw.js`, `/offline` (PwaController).

## Auth & account
`/login`, `/register`, `/auth/google/start`, `/password/forgot|reset`,
`/verify-email[/resend]`, `/account`, `/account/{notifications,appearance,privacy,password,email}`.

## Chats workspace (authenticated)
`GET /chats` (rail + open pane), `/chats/new` (compose), `/chats/{key}`,
`/chats/{key}/pane` (fragment for in-place morph), `POST /chats/{key}/send`,
`POST /chats/refresh`, `GET /chats/events` (SSE). `/threads/**` and `/senders`
redirect to `/chats`. Services: `ConversationListService`, `ChatService`,
`OutboundMessageService`, `MailboxPollingService`.

## Mailboxes, searches, teams, billing, admin
`/mailboxes` (+`/new`, `/{id}/sync|signature|delete`, `/preferences`),
`/mailboxes/gmail/{connect,reconnect/{id},callback}`, `POST /searches`,
`POST /searches/{id}/delete`, `/team/invite[/accept]`,
`/billing/{checkout,portal,success,cancel,cancel-subscription}`,
`/digest/opt-out`, `/admin/revenue` (+ POST reconcile-billing-period, win-back).
