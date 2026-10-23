# API.md — canonical API contract

**Status (2026-10-23): no mobile/REST API exists yet.** The backend is a
server-rendered Thymeleaf app. The mobile repo (`conexus-mail-mobile`) is an
empty Flutter placeholder. This file therefore documents (1) what HTTP surface
actually exists, and (2) the contract-to-be as TBD. Nothing below the "TBD"
heading is implemented — do not code against it until it is specified here
and implemented in the same run.

## 1. Existing JSON / non-HTML endpoints

### `POST /billing/webhook`
- Auth: none (permitAll, CSRF-exempt); authenticity via Stripe signature header.
- Body: raw Stripe event JSON. Invalid signature → `InvalidStripeSignatureException` (handled in `BillingWebhookController`).
- Not for client use.

### `GET /chats/events` (SSE, `text/event-stream`)
- Auth: session cookie. Emits new-mail events (`NewMailEvent`) so the web chat
  view can morph in place. Event payload schema: TBD (read `ChatEventStream`).

## 2. Form/HTML endpoints (web only, CSRF-protected, session auth)

See `docs/WEB.md` for the full route inventory. These return HTML/redirects and
are **not** a stable contract for the mobile client.

## 3. TBD — mobile API (`/api/v1/**`)

Open decisions (recorded in `INTERNAL_TODO.md`):
1. Auth scheme for mobile: bearer access/refresh tokens vs. session cookie. Google sign-in on mobile needs an ID-token exchange endpoint.
2. Resource set for a first slice: `GET /api/v1/conversations`, `GET /api/v1/conversations/{key}`, `POST /api/v1/conversations/{key}/messages`, `GET /api/v1/me`.
3. Error envelope (suggest `{ "error": { "code", "message" } }`), pagination, plan-limit error code mapping from `PlanLimitExceededException`.
4. Realtime: reuse SSE or Web Push (roadmap item "Live delivery, part 2").
