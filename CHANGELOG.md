# CHANGELOG

Append one dated entry per full-stack-sync routine run. Newest on top.

## 2026-08-29

- Bootstrapped the shared doc system for the full-stack sync routine:
  added `docs/BACKEND.md`, `docs/API.md`, `docs/WEB.md`,
  `CHANGELOG.md`, `INTERNAL_TODO.md`, and a "Related repositories"
  section in `CLAUDE.md`.
- Ran the Phase-0 sync audit against the sibling `conexus-mail-mobile`
  repository. Findings are captured in `INTERNAL_TODO.md`, ordered by
  severity. Headline: the mobile client does not exist yet and there
  is no dedicated JSON REST surface for it; the backend is currently
  fully server-rendered Thymeleaf.
- No functional code changes.
