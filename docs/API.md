# API contract

Canonical contract between the backend and the mobile client
(`conexus-mail-mobile`). Update this file BEFORE changing either side.

## Status (2026-10-07)

**There is no mobile REST API yet.** The backend is a server-rendered
Thymeleaf app with cookie-session auth and CSRF. The only JSON/stream
endpoints are listed below; everything else in `docs/WEB.md` returns HTML.
The mobile client must not scrape HTML. The first feature run should add a
versioned `/api/v1/**` surface (see `INTERNAL_TODO.md`).

## Auth model today

- Form login: `POST /login` (`email`, `password`, optional `remember-me`),
  session cookie `JSESSIONID`; CSRF token required on every non-GET except
  `/billing/webhook` and `/h2-console/**`.
- Google OIDC via `/auth/google/start` (only when `GOOGLE_CLIENT_ID` is set).
- Unauthenticated access to non-public routes redirects to `/login` (302),
  not 401.

## Existing non-HTML endpoints

### `POST /chats/refresh`
Auth: session + CSRF. Synchronously polls the user's active mailboxes.
Response `200 application/json`: `{"imported": <int>}`.

### `GET /chats/events`
Auth: session. `text/event-stream` (SSE). Event `new-mail`, data
`{"imported": <int>}`, emitted after a poll imports ≥1 message for the user.

### `POST /billing/webhook`
Auth: Stripe signature header (no session, no CSRF). Server-to-server only.

### Public
`GET /actuator/health`, `/robots.txt`, `/sitemap.xml`,
`/manifest.webmanifest`, `/sw.js`, `/icons/*`.

## Planned `/api/v1/**` (TBD — not implemented)

Nothing here is a contract until it is specified in this file with request and
response schemas and implemented on both sides.
