# INTERNAL_TODO (backend)

Ordered by severity: contract breakage > logic drift > missing tests > style.

## Contract breakage
- [ ] No `/api/v1` REST API exists; the mobile client has nothing to call. Define in `docs/API.md` §2, then implement.
- [ ] No token (bearer) authentication — only session cookie + CSRF form login. Needs token issue/refresh/revoke + Flyway migration (V36+), and a second `SecurityFilterChain` for `/api/**` with CSRF disabled.
- [ ] `/chats/events` SSE payload schema is undocumented (**TBD** — read `ChatEventStream`/`NewMailEvent`).

## Logic drift
- [ ] Chat view logic currently lives behind `Model`-populating controller methods in `ConversationController` (`populateList`, `populateChat`, `selectMailbox`, `sendingAccount`, `withSignature`). Extract into a service before an API controller can reuse it, to avoid duplicating the send/scope rules.
- [ ] `docs/` did not exist before 2026-10-13; keep `WEB.md` in sync when routes change.

## Missing tests
- [ ] Not audited this run beyond running the suite (see CHANGELOG for result).

## Deferred / tech debt
- Stack differs from the generic target (JPA, not jOOQ) — intentional; do not introduce jOOQ.
- Attachments not downloadable (`blob_ref` null) — would block mobile attachment viewing.
- Existing roadmap items in `CLAUDE.md` (support email, Google OAuth publish, Web Push, delivery states, …) remain open.
