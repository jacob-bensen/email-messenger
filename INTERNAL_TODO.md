# INTERNAL_TODO (backend)

## Sync audit — severity ordered
1. **Contract breakage:** No REST API / token auth exists; mobile repo has zero code. Blocks all client work. Next run: implement the slice proposed in `docs/API.md`.
2. **Logic drift risk:** Chat logic lives behind services (good), but `ConversationController` (~330 lines) holds orchestration (refresh, compose validation) that a REST layer would need — extract into services before adding `/api`.
3. **Missing tests:** none audited this run (tests not executed — see CHANGELOG).
4. **Style/docs:** Dockerfile/docker-compose/DEPLOY.md present although CLAUDE.md says Docker was removed; generic routine says jOOQ but repo uses JPA; routine says `master` branch but session is pinned to `claude/*` branches.

## Carried over from CLAUDE.md roadmap
See roadmap in `CLAUDE.md` (support email, Google OAuth publish, Web Push, delivery states, living contact card, shared files, reply-to-bubble).
