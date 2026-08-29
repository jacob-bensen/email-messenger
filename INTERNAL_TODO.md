# INTERNAL_TODO

Cross-repo full-stack routine backlog. Feature-only backend backlog stays
in `master/BACKLOG.md` and the CLAUDE.md roadmap.

## Contract mismatches / gaps

**S1 — critical (nothing to reconcile because nothing exists yet)**

- No REST/JSON API for a mobile client. Sibling repo
  (`conexus-mail-mobile`) is an empty scaffold. A "sync audit" is
  therefore trivially clean — client and server can't diverge because
  the client is absent. The relevant next steps sit below under
  "Blocking questions."

## Blocking questions (needs a human decision)

- **Do we want a mobile client at all, and if so what shape?** The sibling
  repo has only a stub README. Before we scaffold Flutter and start on
  `docs/API.md`, confirm: (a) mobile vs mobile-web-first, (b) if mobile,
  Flutter is fine, (c) which state-management + HTTP libraries are
  acceptable, (d) auth model for mobile (bearer tokens from an existing
  Google OIDC exchange? new API-key issuance? mTLS?).
- **What is the first mobile-supported flow?** Cheapest first slice is
  probably read-only "list my chats + open a conversation." Confirm before
  we lock in the first API endpoints.
- **REST or GraphQL?** Backend today has neither. The scheduled routine
  assumes REST; confirm before we lay JSON DTOs on top of the existing
  Thymeleaf-only controllers.

## Tech debt / non-blocking

- `application.yml` mixes production defaults with commented-out env
  toggles. Not urgent, but worth tidying next time we touch config.
- The website and any future REST layer must both go through the same
  services (see `docs/WEB.md`); no duplicated logic in controllers /
  templates has been introduced.
- Flutter toolchain is not installed in the routine's container. Any
  frontend implementation run will need to either install Flutter or
  document the gate.

## Done in the 2026-08-29 bootstrap run

- `docs/BACKEND.md`, `docs/API.md`, `docs/WEB.md` written from what is
  actually implemented; unknowns marked TBD.
- `CHANGELOG.md` and `INTERNAL_TODO.md` created.
- `CLAUDE.md` "Related repositories" section added.
