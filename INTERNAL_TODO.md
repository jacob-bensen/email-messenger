# Internal TODO — backend

Running list of known gaps, tech debt, and deferred work discovered
during full-stack sync runs. Severity ordered: contract breakage > logic
drift > missing tests > style. Consume from and append to this every
run.

## Contract / API

- [ ] Mobile client auth. The mobile app needs a way to sign in that is
      not a cookie flow. Decide: (a) reuse form login and swap the
      response for a JSON `{ token }` when the client sends `Accept:
      application/json`, or (b) add a dedicated `POST /api/v1/auth/token`
      endpoint issuing an opaque bearer token (or short-lived JWT +
      refresh). Blocked on: token storage decision (DB table vs. JWT
      signing key rotation).
- [ ] `GET /api/v1/me` — return the signed-in user's id, email, display
      name, plan, and account flags. Needs the auth decision above.
- [ ] `GET /api/v1/mailboxes` — list the user's connected mailboxes with
      auth type, status, and last poll timestamp.
- [ ] `GET /api/v1/chats` — the /chats rail as JSON: conversationKey,
      participants, last-message preview, unread count, updatedAt. Reuse
      `ConversationListService`.
- [ ] `GET /api/v1/chats/{conversationKey}/messages` — bubbles for one
      conversation. Reuse `ConversationService` /
      `IMTransformService`.
- [ ] `POST /api/v1/chats/{conversationKey}/messages` — reply. Reuse
      `ReplyService` and `OutboundMessageService`.

## Backend

- [ ] Verify IMAP IDLE against a real Gmail account (item deferred on the
      main roadmap: "Live delivery, part 1").
- [ ] Web Push (VAPID) — planned on the main roadmap.
- [ ] Attachment blob storage — `blob_ref` is always null today.

## Frontend (conexus-mail-mobile)

- [ ] Nothing but scaffold + version fetch exists today. Once auth is
      designed, wire login → `/api/v1/me` → chats list → conversation
      view. Mirror `IMTransformService`'s bubble grouping rules in
      `lib/domain/`.

## Testing

- [ ] Add an integration test that mounts the JSON API alongside the
      Thymeleaf stack and asserts CSRF/security config does not break
      it.
- [ ] Add a Postman/HTTPie collection under `docs/` once the contract
      grows past three endpoints.
