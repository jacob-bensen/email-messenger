# Changelog

Ongoing log of what changed each autonomous run and why. Predates this
file lives in git history (`git log`) and, for older narrative-style
entries, in `master/CHANGELOG.md`.

## 2026-08-28

Bootstrapped the shared doc system that the two-repo full-stack sync
routine assumes: added `docs/BACKEND.md` (architecture + package
layout, notes JPA rather than jOOQ), `docs/API.md` (canonical mobile-
API contract — today only Stripe's webhook exists in JSON; the mobile
surface is spec-only), `docs/WEB.md` (Thymeleaf page inventory keyed to
their controllers and shared services), `CHANGELOG.md` (this file), and
`INTERNAL_TODO.md` (contract-drift + tech-debt tracker). No source-code
changes; no schema changes. Added a "Related repositories" section in
`CLAUDE.md` pointing at `conexus-mail-mobile`. The sibling mobile repo
is still an unscaffolded shell — see its `docs/DESIGN.md` for the
prerequisites blocking any real client work.
