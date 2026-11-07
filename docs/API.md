# API contract

Canonical contract between the backend and the mobile client
(`conexus-mail-mobile`). Last audited against code: 2026-11-07.

## Status: no mobile REST API exists yet

The backend today exposes **only** server-rendered Thymeleaf pages (session
cookie + CSRF), one SSE stream, one Stripe webhook, and a few static/PWA
endpoints. There is **no JSON API, no token auth, and no CORS config**. The
mobile client cannot call anything usefully until the work in
"Proposed `/api/v1`" below is built. Everything in that section is
**PROPOSED / TBD** and not implemented.

## 1. Implemented HTTP surface that is JSON/stream/non-HTML

| Method | Path | Auth | Notes |
|---|---|---|---|
| GET | `/chats/events` | session | `text/event-stream` (SSE). Published by `ChatEventStream` when `MailboxPollingService` imports new mail. Per-user emitter. Event payload shape: TBD (see `NewMailEvent`). |
| POST | `/billing/webhook` | none (Stripe signature header `Stripe-Signature`) | Raw JSON body. CSRF-exempt. 400 on missing/invalid signature, 503 if webhook not configured. |
| GET | `/actuator/health`, `/actuator/health/**` | none | Spring Actuator. |
| GET | `/manifest.webmanifest`, `/sw.js`, `/icons/*`, `/offline` | none | PWA shell. |
| GET | `/robots.txt`, `/sitemap.xml`, `/images/og-card.png` | none | SEO. |

All other routes are HTML pages or form posts; see `WEB.md`.

## 2. Auth model today

- Form login: `POST /login` (`email`, `password`, optional `remember-me`), CSRF
  token required. Session cookie `JSESSIONID`; persistent remember-me (30 days,
  `persistent_logins` table).
- Google OIDC via `/auth/google/start` (only when `GOOGLE_CLIENT_ID` is set).
- Login throttling (`LoginThrottleFilter`) + auth event audit.
- Logout: `POST /logout`.

## 3. Proposed `/api/v1` (TBD — not implemented)

Goal: expose the existing service layer (`ConversationListService`,
`ChatService`, `ReplyService`/`OutboundMessageService`, `MailAccountService`,
`BillingService`) to the mobile client without forking logic from the web
controllers. Suggested first slice (the minimum for a usable chat client):

1. Auth: `POST /api/v1/auth/login` → short-lived access token + refresh token
   (new `api_tokens` table, new Flyway migration). Needs a second
   `SecurityFilterChain` (stateless, bearer, CSRF off) for `/api/**`.
2. `GET /api/v1/chats?mailbox=&filter=all|unread|awaiting|attachments&q=&page=`
   → page of `ConversationListItem` + `ChatCounts`.
3. `GET /api/v1/chats/{key}` → `ChatConversation` (members, runs of
   `BubbleMessage`); marks read.
4. `POST /api/v1/chats/{key}/send` `{body, subject?, asReply, mailbox?}`.
5. `GET /api/v1/mailboxes`.
6. Live updates: reuse `/chats/events` (token auth) or push (FCM/APNs, TBD).

Error envelope (proposed): `{ "error": "<code>", "message": "<text>" }` with
401 unauthenticated, 403 plan-limit (`PlanLimitExceededException`), 404
unknown key, 422 validation.

Before implementing any of this, the shapes above must be turned into real
schemas here, in the same commit as the code.
