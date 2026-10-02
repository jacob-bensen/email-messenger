# Internal TODO (backend)

Ordered by severity: contract breakage > logic drift > missing tests > style.

## Contract breakage
- No REST API / token auth for the mobile client; `docs/API.md` has nothing
  for the client to call. Blocks all mobile feature work. Next run: define
  `/api/v1` auth + `GET /api/v1/chats` in API.md first, then implement over
  `ConversationListService`.
- CSRF-protected session auth only; needs a separate stateless security filter
  chain for `/api/**` (bearer token, CSRF ignored for it).

## Logic drift
- `ConversationController` holds list-filter/mailbox-param/plan-limit shaping
  that a REST controller would need too; extract to a service before adding
  the API (otherwise web and API diverge).

## Missing tests
- None audited this run (tests not executed; see CHANGELOG).

## Style / docs
- Routine template assumes jOOQ; project uses JPA. Docs reflect JPA.
- CLAUDE.md roadmap item "support email" and "Google OAuth publish" remain
  blocked on external decisions.
- Routine says commit to `master`; this session is constrained to branch
  `claude/gifted-carson-6bubt1`, so work was pushed there instead.
