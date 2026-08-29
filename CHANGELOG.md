# Changelog

Dated entries appended by the sync routine.

## 2026-08-29

Bootstrap of the shared documentation system so future runs of the
full-stack sync routine have real docs to reason against.

- Added `docs/BACKEND.md` — reverse-engineered architecture snapshot
  (packages, domain, real-time delivery, and what is explicitly NOT
  built yet).
- Added `docs/API.md` — full inventory of the current HTTP surface,
  called out that almost everything is Thymeleaf / form-post and that
  there is no JSON REST API for a mobile client yet. Reserved a
  "When we design a mobile API" section for the eventual contract.
- Added `docs/WEB.md` — page inventory mapping routes → templates →
  controllers, with the invariant that web and any future API paths
  share the same service layer.
- Added `INTERNAL_TODO.md` — cross-cutting mismatches and the product
  decisions the routine cannot make on its own (chief among them:
  whether the sibling mobile repo is intended and what it should look
  like).
- Added this `CHANGELOG.md` and a "Related repositories" section to
  `CLAUDE.md` pointing at `conexus-mail-mobile` and the new docs.

Deliberately not done: no scaffolding of a REST API, no changes to
existing controllers or services, no touching the roadmap items in
`CLAUDE.md`. All of that needs a product decision first.
