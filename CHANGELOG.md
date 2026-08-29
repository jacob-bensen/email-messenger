# Changelog

Dated notes appended by the full-stack sync routine.

## 2026-08-29

- Phase 0 bootstrap of the two-repo doc system:
  - Added `docs/BACKEND.md` — module layout, storage, auth, mailbox
    integration, `/chats` workspace, plans/admin.
  - Added `docs/API.md` — canonical REST contract draft. Records that
    **no mobile REST API exists yet** and lays out the `/api/v1/*`
    surface, error envelope, and change protocol.
  - Added `docs/WEB.md` — full inventory of the Thymeleaf website
    routes grouped by controller, plus the "service layer is shared"
    rule for future API/web parity.
  - Added `INTERNAL_TODO.md` — severity-ordered gap list; the biggest
    item is the missing mobile API surface.
  - Added a "Related repositories" section to `CLAUDE.md` pointing at
    `jacob-bensen/conexus-mail-mobile` and the sibling docs.
- No production code changes this run; no migrations added.
