# Internal TODO — email-messenger

Ordered by severity: contract breakage > logic drift > missing tests > style.
Consume from top; add newly discovered items at the bottom of the right
section. Keep in sync with `conexus-mail-mobile/INTERNAL_TODO.md`.

## Contract breakage

- **Mobile auth is entirely missing.** The backend only supports session
  cookies (form login + Google OIDC). The mobile app cannot authenticate.
  Decide the mechanism (leading candidates: OAuth2 password/refresh
  grants, or an app-issued personal access token) and ship
  `POST /api/v1/auth/token` + `POST /api/v1/auth/refresh` +
  `POST /api/v1/auth/revoke`, plus a bearer-token filter that populates
  the Spring Security context for `/api/**` requests.
- **No mobile chat endpoints yet.** The Flutter client will need
  `GET /api/v1/chats` (list, paged, filter/search parity with
  `ConversationListService`), `GET /api/v1/chats/{key}` (timeline +
  members + signatures), and `POST /api/v1/chats/{key}/send` (reply or
  new). Contracts must reuse `ChatService` / `OutboundMessageService` /
  `ReplyService` so behaviour matches the web exactly.
- **Live-mail channel for the mobile app** — the web uses SSE at
  `/chats/events` behind the session cookie. Mobile equivalent needs
  bearer-token auth (probably SSE over the same header). Decide, then
  document in `docs/API.md`.

## Logic drift

- `/chats/refresh` (in `web.ConversationController`) is JSON but lives
  under the website prefix. When the mobile equivalent lands, port the
  behaviour to `/api/v1/chats/refresh` and have the web page call the
  versioned path too, so there's exactly one implementation.
- Error responses: the API has no defined error shape yet. First real
  4xx/5xx endpoint should introduce a `problem+json`-style envelope
  and document it in `docs/API.md`.

## Missing tests

- The `HealthController` test uses `@SpringBootTest`; when more mobile
  controllers arrive, switch to a slice test (`@WebMvcTest`) with
  targeted mocks to keep the mobile-API test suite fast.

## Style / cleanup

- None yet.
