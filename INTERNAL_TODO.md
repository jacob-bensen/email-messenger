# INTERNAL_TODO (backend) — ordered by severity

## Contract breakage
- [ ] No `/api/v1` REST API exists; the mobile repo (`conexus-mail-mobile`) is empty. Define schemas in `docs/API.md`, then implement. (Blocks all client work.)
- [ ] No bearer-token auth for a mobile client; sessions + CSRF only. Owner decision needed: opaque token vs JWT.

## Logic drift / architecture
- [ ] Routine assumes jOOQ; repo uses JPA. Decision: keep JPA (recommended; matches CLAUDE.md "Match existing architecture").
- [ ] `ConversationController` holds logic (mailbox scope selection, send assembly, signature appending) that must move into services before a REST layer can share it.
- [ ] `master/` directory holds a 500KB CHANGELOG from an earlier routine plus PLAN/BACKLOG (EPIC-18 win-back); product name there is "MailIM" vs "ConexusMail" elsewhere, and its pricing differs from CLAUDE.md. Reconcile or archive.
- [ ] `docs/API.md` SSE event payload shape is TBD.

## Missing tests
- [ ] Not audited this run (docs-only run).

## Deferred
- Roadmap items in CLAUDE.md (support email, Google OAuth publish, Web Push, delivery states, ...).
