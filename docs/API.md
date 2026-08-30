# API — canonical wire contract

This file is the SINGLE SOURCE OF TRUTH for the wire contract between
`email-messenger` (backend) and `conexus-mail-mobile` (mobile client).
Both repos depend on it; every change lands here first, in the same PR
that touches either implementation.

## Status (2026-08-30)

There is **no mobile-facing REST API yet**. The backend today serves the
public website and the authenticated web app through Thymeleaf (see
[`docs/WEB.md`](./WEB.md)). The mobile scaffold is fresh and does not yet
issue HTTP calls.

The first mobile-facing endpoints will be added under the `/api/v1/**`
prefix. This file will grow into the endpoint catalogue as they land.

## Conventions (once endpoints exist)

- **Base path**: `/api/v1`. Never mount mobile-facing endpoints at bare
  routes that a Thymeleaf controller also owns; the two sit side by side
  under different prefixes.
- **Media type**: `application/json; charset=utf-8` for requests and
  responses unless otherwise noted.
- **Auth**: TBD — the current server uses session cookies from form login
  / Google OIDC. The mobile API will need either a bearer-token issuer
  (JWT or opaque) or explicit cookie sessions with a mobile-friendly
  login endpoint. This decision must be made in the PR that adds the
  first authenticated `/api/v1` endpoint and recorded in this section.
- **Errors**: JSON body `{ "error": "code_slug", "message": "human string",
  "details": { ... } }` with a matching HTTP status. `422` for validation,
  `401` unauth, `403` forbidden, `404` missing, `409` conflict, `429` rate
  limit, `500` for anything the client should treat as a retryable server
  fault.
- **Timestamps**: ISO-8601 with a `Z` suffix (UTC), rendered from
  `Instant`.
- **IDs**: strings, opaque to the client. Backend picks the format
  (numeric, base62, or UUID) per resource; the client MUST NOT parse
  them.
- **Pagination**: cursor-based on list endpoints
  (`?limit=<int>&cursor=<opaque>` → `{ items: [...], nextCursor: "..." }`).
  No offsets.
- **Server authority**: every state mutation the mobile client requests is
  re-validated on the server. The client's `lib/domain/` mirrors the
  server's rules only for optimistic UI; on conflict, server wins.

## Endpoint index

_(empty — populate as endpoints are added)_

## Non-endpoints: what the server exposes today

For reference, the server currently exposes the following HTTP surface
(all HTML / form / SSE — see `docs/WEB.md` for the full catalogue):

- Public marketing: `GET /`, `GET /pricing`, `GET /demo`, `GET /privacy`,
  `GET /terms`, `GET /refund`, `GET /robots.txt`, `GET /sitemap.xml`.
- Auth: `GET/POST /login`, `GET/POST /register`, `GET /verify-email`,
  `POST /verify-email/resend`, `GET/POST /password/forgot`,
  `GET/POST /password/reset`, `GET /auth/google/start`.
- Account: `GET /account`, `GET/POST /account/appearance`,
  `GET /account/notifications`, `GET /account/privacy`,
  `POST /account/password`, `POST /account/email`.
- Mailboxes: `GET /mailboxes`, `GET /mailboxes/new`, `POST /mailboxes`,
  `POST /mailboxes/{id}/sync`, `POST /mailboxes/preferences`,
  `POST /mailboxes/{id}/signature`, `POST /mailboxes/{id}/delete`,
  `GET /mailboxes/gmail/connect`, `GET /mailboxes/gmail/reconnect/{id}`,
  `GET /mailboxes/gmail/callback`.
- Chats (Thymeleaf UI, not JSON): `GET /chats`, `GET /chats/new`,
  `POST /chats/new`, `POST /chats/refresh`, `GET /chats/{key}`,
  `GET /chats/{key}/pane`, `POST /chats/{key}/send`,
  `GET /chats/events` (SSE stream).
- Saved searches: `POST /searches`, `POST /searches/{id}/delete`.
- Team: `GET/POST /team/invite`, `GET/POST /team/invite/accept`.
- Digest: `GET /digest/opt-out`.
- Billing: `POST /billing/checkout`, `POST /billing/portal`,
  `GET /billing/success`, `GET /billing/cancel`,
  `GET /billing/cancel-subscription`, `POST /billing/cancel-subscription`,
  `POST /billing/webhook` (Stripe → `@RestController`, JSON in).
- Admin: `GET /admin/revenue`, `POST /admin/revenue/reconcile-billing-period`,
  `POST /admin/retention/win-back`.
- PWA: `GET /manifest.webmanifest`, `GET /sw.js`, `GET /offline`,
  `GET /icons/*`, `GET /apple-touch-icon.png`.

These are for humans in a browser; the mobile client will not call them.
