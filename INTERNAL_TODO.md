# INTERNAL_TODO

## Sync audit (2026-10-23) — severity ordered
1. **Contract gap:** no `/api/v1` JSON API and no token auth; mobile client cannot exist against current server. Implement per `docs/API.md` "Proposed".
2. **Contract gap:** attachments not downloadable (`blob_ref` null) — blocks mobile attachment UX.
3. Logic drift risk: `ConversationController` still builds some view state inline; extract to services before adding REST controllers so both share it.
4. Routine template assumes jOOQ; backend uses JPA (documented in `docs/BACKEND.md`).
5. Branch policy conflict: routine says commit to `master`; session harness mandates `claude/*` branches. Harness branches were used.
6. Quality gates: Flutter/Dart not installed in this environment; frontend gates cannot run.
7. Existing CLAUDE.md roadmap items (Web Push, delivery states, etc.) remain open.
