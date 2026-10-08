# INTERNAL_TODO (backend)

## Contract breakage
1. No JSON REST API or bearer-token auth exists; mobile client has nothing to call. Design `/api/v1` in docs/API.md first, add a stateless `SecurityFilterChain` (`/api/**`), reuse `ChatService`/`ConversationListService`/`ReplyService`.
## Logic drift
2. None found (web only; single code path). Re-audit when REST is added.
## Doc/convention discrepancies
3. Routine assumes jOOQ + `master` branch; repo uses JPA and this session is on `claude/gifted-carson-1m2c78` (session branch rule wins over routine text).
4. Existing `master/` dir (PLAN/BACKLOG/CHANGELOG/MASTER_ACTIONS) is a separate planning system from an older routine; root CHANGELOG.md added now. CLAUDE.md says CI was removed but `.github/` contains `ci.yml` — verify with owner.
## Missing tests / other
5. Attachments not downloadable (`blob_ref` null) — mobile will need it.
6. Not verified this run: `./mvnw test` (Phase 0 was docs-only).
