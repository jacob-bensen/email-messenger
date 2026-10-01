# INTERNAL_TODO

Ordered by severity: contract breakage > logic drift > missing tests > style. Last audit: 2026-10-01.

## Contract breakage
1. **No mobile REST API exists** (`docs/API.md`). The mobile repo is an empty README, so nothing is
   *broken* yet, but there is nothing for a client to call. Needs: token auth design (session+CSRF today),
   `/api/v1/**` controllers, JSON error envelope.
2. SSE event names/payload for `/chats/events` are undocumented (read `ChatEventStream`, add to API.md).

## Logic drift / architecture
1. Conversation list/scope helpers (`selectMailbox`, `scopeAddress`, `accountColorMap`, `fallbackAccount`,
   `parseRecipients`, `parseFilter`) live as private methods in `web/ConversationController`. Extract into a
   service so web and REST share one path before building the API.
2. Routine template assumes jOOQ; the project uses JPA. Treat JPA as canonical (no jOOQ codegen).
3. `master/PLAN.md` describes an outdated pricing model (Personal $9 / Team $29 / Enterprise $99) vs code
   (`Plan {FREE, PRO, BUSINESS}`, `PlanLimits`). Product name differs too (MailIM vs ConexusMail).
4. `CLAUDE.md` says Docker/CI were removed 2026-06-11, but `Dockerfile` and `docker-compose.yml` exist at the
   repo root. Confirm with owner; do not re-add or delete without a decision.
5. `attachments.blob_ref` is always null — attachments are not downloadable (roadmap item).
6. Two competing changelogs/plans: `master/*` (operator routine) and root docs. Decide whether to merge.

## Missing tests
- No contract tests (none needed until the API exists).
- Quality gate status at audit time: see `CHANGELOG.md` entry for 2026-10-01.

## Style
- Roadmap in `CLAUDE.md` has unchecked items (support email, Google OAuth consent publish, Web Push,
  delivery states, signature contact card, shared files, reply-to-bubble).
