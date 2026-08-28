# INTERNAL_TODO — backend ↔ mobile sync gaps

Cross-repo gaps and follow-ups discovered while syncing with
`conexus-mail-mobile`. Product-facing planning still lives under
`master/` (`PLAN.md`, `BACKLOG.md`, `MASTER_ACTIONS.md`); this file is
scoped specifically to what stands between the backend and the mobile
client.

Severity: **P0 = contract-breaking / blocks any mobile progress**,
**P1 = logic drift or missing surface**, **P2 = testing / docs polish**.

## P0 — no mobile-facing API surface exists

- No JSON REST controllers. Everything is Thymeleaf `@Controller` +
  the `POST /billing/webhook` Stripe endpoint + the `GET /chats/events`
  SSE stream. See `docs/API.md` "Known gaps".
- No bearer/JWT auth. Session cookie + Google OIDC via the browser only.
  A mobile client needs an opaque refresh-token issuance + rotation
  flow (plus a device-registration model for push).
- Attachment bytes aren't stored (`Attachment.blob_ref` is always null),
  so `GET /api/v1/attachments/{id}` can't be implemented yet. Depends
  on the "Per-person shared files & links" roadmap item in `CLAUDE.md`.

## P1 — shape follow-ups the mobile client will need

- `Message` needs a `deliveryStatus` enum (`sending`, `sent`,
  `delivered`, `failed`) once "Delivery states for outbound bubbles"
  ships. Extend `docs/API.md` when it lands.
- The push-subscription abstraction (still to be written for Web Push /
  VAPID) must be shaped so APNs + FCM plug in behind the same
  interface, or the mobile client will fork it later.
- SSE stream today is browser-scoped (session cookie). When bearer auth
  lands, expose `/api/v1/events` accepting the same token so mobile can
  subscribe.

## P2 — docs & tests

- Once real endpoints exist, add Spring MVC slice tests covering the
  contract in `docs/API.md`; keep the DTO Java types the single source
  the client mirrors.
- Add a short "How web + API share services" note to `docs/BACKEND.md`
  the moment the first REST controller reuses an existing service.
- Re-check the "Related repositories" section of `CLAUDE.md` after each
  cross-repo run so it stays accurate.
