# Changelog

Dated run log, newest first. (Older 2026-06 entries from a previous
planning routine live in `master/CHANGELOG.md`.)

## 2026-11-16 — Full-stack sync bootstrap (Phase 0, backend side)

- Added the doc system the mobile repo already references: `docs/API.md`
  (canonical contract — §1 status: no mobile API yet; §2 planned
  conventions + error envelope; §3 planned endpoints; §4 shared rules with
  golden vectors; §5 mobile-auth proposal), `docs/BACKEND.md`
  (architecture), `docs/WEB.md` (Thymeleaf page inventory), and
  `INTERNAL_TODO.md` (sync-audit results).
- `ConversationKeyServiceTest` now asserts the three conversation-key golden
  vectors that `conexus-mail-mobile` asserts, so the Dart mirror and the
  server can't drift silently.
- `ConversationKeyService`'s self sentinel is written as `"\0self"` instead
  of a raw NUL byte (same value; the file is no longer detected as binary).
- `CLAUDE.md`: added "Related repositories".
