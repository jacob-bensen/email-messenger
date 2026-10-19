# Internal TODO (severity ordered)

## Contract breakage
- No REST API exists for the mobile client; `docs/API.md` §Planned is TBD. Needs: token auth
  design (current auth is cookie+CSRF), `/api/v1` chat list/detail/send via existing services.
## Logic drift
- Controllers (`ConversationController`) assemble view models inline (`populateList`, mailbox scoping
  `selectMailbox`/`scopeAddress`); extract to a service before an API duplicates it.
## Missing tests / docs
- SSE event payload schema undocumented.
- Routine prompt assumes jOOQ; backend uses JPA. No codegen step.
## Process
- Scheduled routine says "commit to master"; this session's harness mandates branch
  `claude/gifted-carson-unuw5y` (CLAUDE.md also forbids pushing to main). Branch rule followed.
