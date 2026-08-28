# INTERNAL_TODO

Running list of sync gaps, tech debt, and deferred work for the
**email-messenger** backend, ordered **most severe first** per the
routine's rubric:

> contract breakage > logic drift > missing tests > style.

The pre-launch product roadmap is in
[`CLAUDE.md`](CLAUDE.md#roadmap) and the operator-facing plan is in
[`master/PLAN.md`](master/PLAN.md). This file tracks only the
two-repo sync gaps and the debt discovered while doing them.

## Contract breakage (highest severity)

- **[CONTRACT] No JSON REST API exists in the backend.** Every
  controller returns Thymeleaf HTML, an SSE stream, or a redirect. The
  mobile client at `conexus-mail-mobile` cannot consume the current
  surface at all. `docs/API.md` designs the target `/api/v1/*` layer
  end-to-end; each of its endpoints is currently **Status: TBD** and
  needs to be built. First implementation slice should be small and
  end-to-end (auth + `/api/v1/me`), then chats read, then send, then
  mailbox connect. Every endpoint must delegate to the existing
  service layer — no forked business logic in an "api" package.

- **[CONTRACT] No token-based auth.** Web is session-cookie only. The
  mobile client needs bearer tokens; the design in
  `docs/API.md#authentication-tbd` (short-lived access + rotating
  refresh, stored in Keychain/Keystore) has to be implemented,
  including a `refresh_tokens` table with a per-token JTI so logout
  and password change can revoke.

- **[CONTRACT] No SSE authentication path for a non-cookie caller.**
  `/chats/events` today is session-cookie only. `/api/v1/events` needs
  to accept `Authorization: Bearer` and enforce the same per-user
  filtering.

## Logic drift & missing pieces

- **[LOGIC] Mobile push (APNs/FCM) is undesigned.** The web roadmap
  mentions Web Push via VAPID (backend roadmap item). The mobile
  equivalent needs a `POST /api/v1/push/subscribe` device-token
  registry and a delivery adapter that mirrors what the web push
  worker does.

- **[LOGIC] View-model records in `service/` (`ChatConversation`,
  `BubbleRun`, `ConversationListItem`, `ThreadRef`, etc.) have no JSON
  mappers today.** As API endpoints ship, add a
  `com.emailmessenger.api.mapper` layer that turns each existing view
  model into the DTO shape defined in `docs/API.md`. Do not restructure
  the view models to be JSON-shaped; keep the mapping explicit.

- **[LOGIC] Gmail-OAuth mobile flow is unwritten.** Web goes browser →
  Google → server callback. Mobile needs either a system-browser
  round-trip terminated by an app-scheme deep link, or (better) a
  server-issued one-time code the app exchanges — see
  `docs/API.md` "Mailbox connect".

- **[LOGIC] Attachment blob bytes are still not stored** (`blob_ref`
  always null). This is a pre-existing backend roadmap item; add-on
  work for the mobile client is a `GET /api/v1/attachments/{id}`
  download endpoint once blob storage exists.

- **[LOGIC] Idempotency-Key store not implemented.** The API contract
  requires it for `send message` and `connect mailbox`; needs a
  server-side idempotency-key table with a 24h TTL.

## Missing tests

- **[TESTS]** As each JSON endpoint ships, add a WebMvc slice test
  covering the success path plus the error `code` values listed in
  `docs/API.md#error-catalogue` for that endpoint. Web-page
  Thymeleaf tests are unaffected.

- **[TESTS]** SSE test for `/api/v1/events` needs to assert bearer
  auth is enforced and that other users' events do not leak.

## Style / consistency

- **[STYLE]** DTO records for the JSON API should live in
  `com.emailmessenger.api.dto` (per the record convention in
  `CLAUDE.md`) and mirror `lib/domain/dto/` on the mobile side field
  for field. Enforce `camelCase` JSON field names and
  `SCREAMING_SNAKE_CASE` enum values in tests.

- **[STYLE]** The scheduled routine's default "target stack"
  mentioned jOOQ; this repo uses Spring Data JPA. Docs describe the
  actual code. Do not migrate to jOOQ on the routine's behalf.

## Non-goals for the sync routine

- The pre-launch churn/win-back epics in `master/PLAN.md` (EPIC-17,
  EPIC-18) are driven by a **separate** routine that consumes
  `master/PLAN.md`. The two-repo sync routine must not pick items
  from `master/BACKLOG.md`; that queue belongs to the other loop.

- Do **not** re-add Docker or GitHub Actions CI — owner decision,
  recorded in `master/PLAN.md#standing-constraints`.
