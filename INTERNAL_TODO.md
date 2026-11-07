# Internal TODO (backend)

Ordered by severity. Updated 2026-11-07 (Phase 0 sync audit).

## Contract breakage
- [ ] No mobile REST API exists: no `/api/v1`, no bearer/refresh-token auth,
      no stateless `SecurityFilterChain` for `/api/**`. The mobile client has
      nothing to call. Define schemas in `docs/API.md` first (see its
      "Proposed /api/v1"), then implement: first slice = login, chats list,
      chat detail, send, mailboxes.
- [ ] `/chats/events` SSE payload shape is undocumented and uses session auth;
      needs token auth + documented event schema for mobile.

## Logic drift / coupling
- [ ] `ConversationController` holds business logic (mailbox scoping, sending
      account choice, signature append, as-reply thread choice, attachment
      conversion). Extract into services before exposing via JSON.
- [ ] Controllers return Thymeleaf model attributes built from entities
      (`BubbleMessage` exposes `Attachment` entities); introduce API DTOs
      rather than serializing entities.

## Docs / repo hygiene
- [ ] `Dockerfile`, `docker-compose.yml`, `DEPLOY.md` contradict CLAUDE.md
      ("not deploying via Docker", CI removed; DEPLOY.md references the
      `claude_routine` branch and ghcr CI). Remove or reconcile.
- [ ] CLAUDE.md says "Do not push to main"; the sync routine says commit to
      `master`. Sessions are currently pinned to `claude/*` branches, so the
      routine used those. Decide the real policy.
- [ ] Stack differs from routine's assumption (JPA, not jOOQ) — no codegen.

## Missing tests
- [ ] No API tests (no API). Add MockMvc slice tests with each endpoint.

## Roadmap items carried from CLAUDE.md
Support email, Google OAuth publish, Web Push, delivery states, signature
contact cards, shared files/links (attachment blobs not downloadable —
relevant to mobile), reply-to-bubble.
