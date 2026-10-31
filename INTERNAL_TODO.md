# INTERNAL_TODO (backend) — ordered by severity

## Contract breakage
1. No `/api/v1` REST API exists; `docs/API.md` is plan-only. Mobile client has nothing to call.
2. No stateless/token auth: `SecurityConfig` is session+CSRF only. Add a second `SecurityFilterChain` for `/api/**` (bearer tokens, CSRF off).
3. Mobile needs a login/refresh token endpoint + `/me`.

## Logic drift risk
4. Chat list/detail model assembly lives inside `ConversationController` (Thymeleaf Model code). Extract to a service returning records so the REST API shares it.
5. `ReplyService` always threads onto the thread's last message (see CLAUDE.md roadmap) — fix before exposing send via API.

## Process / doc gaps
6. Existing docs live in `master/` (PLAN, BACKLOG, CHANGELOG, MASTER_ACTIONS); root `CHANGELOG.md` is new for this routine. Consider reconciling.
7. Routine prompt assumes jOOQ; project uses JPA. No codegen step.
8. Routine prompt says commit to `master`; this environment mandates feature branches (`claude/gifted-carson-hjovnr`, `claude/trusting-cori-hjovnr`) and CLAUDE.md forbids pushing to main. Branch policy followed.

## Missing tests
9. No controller tests for any `/api/**` (nothing exists).
