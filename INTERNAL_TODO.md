# INTERNAL_TODO — email-messenger (backend)

Running log of known gaps, tech debt, and deferred work. Consume from
and append to this every full-stack sync run. Order by severity:
contract breakage > logic drift > missing tests > style.

## Contract / mobile-API gaps

- [ ] **No REST auth story yet.** `SecurityConfig` is cookie / session
      only; the Flutter client can't ride form-login. Add
      `POST /api/v1/auth/session` issuing an opaque bearer token backed
      by a new `api_session` table, plus a Spring Security filter that
      resolves the token to the existing `User`. Spec is stubbed in
      `docs/API.md#auth-planned-not-yet-implemented`.
- [ ] **No REST chat endpoints yet.** `docs/API.md#chat-endpoints-planned`
      lists `/api/v1/chats`, `/api/v1/chats/{key}`, replies, read,
      SSE — all still marked planned. Each MUST delegate to
      `ChatService` / `ConversationListService` / `OutboundMessageService`;
      no fork of logic in the REST controller.
- [ ] **Attachment download endpoint missing on the web too.** Blobs
      are still not stored (roadmap item). Once the blob store lands,
      expose `GET /api/v1/attachments/{id}` for the client with the
      same plan-limit gating as the web download.
- [ ] **SSE `/chats/events` payload not documented as a public
      contract.** Today the fields are shaped for the Thymeleaf morph
      code. Before wiring the mobile client to `/api/v1/chats/events`,
      snapshot the payload schema in `docs/API.md`.

## Logic drift / risks

- [ ] `ReplyService` always threads onto the thread's last message
      (mis-threads replies to older bubbles). Roadmap item captured in
      root `CLAUDE.md`.
- [ ] Mailer-daemon DSNs surface as pseudo-conversations in the chat
      list — need suppression + a Not-Delivered bubble state. Roadmap.
- [ ] `MailAccount` OAuth refresh tokens expire after 7 days while the
      Google OAuth app is in "Testing" mode. Weekly reconnect required
      until Google publishes it. Blocked on human action
      (`master/MASTER_ACTIONS.md`).

## Docs

- [x] `docs/BACKEND.md`, `docs/API.md`, `docs/WEB.md` scaffolded
      2026-08-28.
- [x] `INTERNAL_TODO.md`, `CHANGELOG.md` seeded 2026-08-28.
- [ ] Keep `CLAUDE.md` "Related repositories" pointer accurate — it now
      lists `conexus-mail-mobile` and the shared contract.

## Tests

- [ ] Add `@WebMvcTest` for `VersionController` once the endpoint lands
      (this run).
- [ ] When bearer-auth ships, add a slice test covering
      `Authorization: Bearer` happy path + expired / bad token.

## Style / cleanup

- [ ] Move Thymeleaf-specific view models out of `service/` (some
      currently mix presentation + domain — case by case).
