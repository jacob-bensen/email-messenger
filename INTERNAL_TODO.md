# Internal TODO — email-messenger

Running list of known gaps, tech debt, and deferred work discovered
by the routine. Consumed and appended to on every run. Ordered by
severity: contract breakage > logic drift > missing tests > style.

## Contract breakage / missing implementation

- [ ] **No REST API exists for the mobile client.** Every endpoint
      described in `docs/API.md` is PLANNED. First target:
      `POST /api/v1/auth/login` + `POST /api/v1/auth/refresh` +
      `GET /api/v1/me` so a signed-in mobile client can identify
      itself. Reuse `DbUserDetailsService`,
      `LoginThrottleService`, and `AuthEventService` — do NOT fork
      auth logic.
- [ ] After auth: `GET /api/v1/chats` and `GET /api/v1/chats/{key}`
      backed by `ConversationListService` and `ConversationService`.
      Shape must match `docs/API.md` exactly.
- [ ] After chat read: `POST /api/v1/chats/{key}/send` reusing
      `ReplyService` + `OutboundMessageService`. Multipart for
      attachments, JSON otherwise, honouring the same 10 MB /
      25 MB caps configured in `application.yml`.
- [ ] Real-time: expose `GET /api/v1/events` as the mobile SSE
      counterpart to `/chats/events`, and wire Web Push (planned
      roadmap item) so the mobile app can receive delivery updates
      when backgrounded.

## Logic drift

- [ ] `Attachment.blob_ref` is unused across the codebase — the
      website's 📎 chips are non-downloadable. Any mobile endpoint
      that exposes attachments must land the blob store first, or
      it will ship a broken feature.
- [ ] `ReplyService` always threads onto the thread's last message —
      the "reply to a specific bubble" roadmap item is still open,
      and the mobile UI will hit the same limitation the website
      does.

## Missing tests

- [ ] No integration test exists yet for a JSON login flow. Add
      one when the endpoint lands, using `MockMvc` + JSON
      assertions.

## Style / conventions

- [ ] Once the first `/api/v1/*` endpoint lands, decide whether to
      keep JSON controllers under `web/` or split them into
      `web/api/`. Document the choice in `docs/BACKEND.md`.
