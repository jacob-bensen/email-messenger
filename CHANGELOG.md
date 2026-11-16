# Changelog

Dated entries, newest first. (Older planning-era history lives in
`master/CHANGELOG.md`; see `INTERNAL_TODO.md` §4.)

## 2026-11-16 — Cross-repo docs bootstrap (Phase 0)

- Added `docs/API.md` (canonical contract with the mobile client: current
  status, proposed `/api/v1` conventions and token auth, the three shared
  rules the client mirrors), `docs/BACKEND.md` (architecture) and
  `docs/WEB.md` (page inventory). The mobile repo already referenced these
  files; they did not exist.
- Added `INTERNAL_TODO.md` from a sync audit of both repos.
- `ConversationKeyServiceTest#goldenVectorsMatchTheMobileMirror`: the three
  golden conversation-key vectors the mobile client asserts are now asserted
  here too, as the mobile docs already claimed.
- `CLAUDE.md`: added "Related repositories".
