# INTERNAL_TODO.md

Ordered by severity (contract > logic drift > tests > style). Last sync audit: 2026-11-04.

## Contract breakage
1. **No REST API for the mobile client.** Backend is HTML-only with session+CSRF auth. Need `/api/v1` with token auth (stateless filter chain separate from the web chain), DTOs, error model. See `docs/API.md` "Planned". First slice suggestion: token login + `GET /api/v1/chats` + `GET /api/v1/chats/{key}`.
2. **Mobile repo has no Flutter project** (README only; no pubspec, lib/, docs). Needs `flutter create`, `lib/domain/` mirror of plan limits (`PlanLimits`), conversationKey, etc.
3. Google-only accounts (`password_set=false`) cannot use password token login — decide mobile Google sign-in flow.

## Logic drift
4. No duplicated logic found between web and (nonexistent) API. Keep controllers thin: `ConversationController` still builds list models inline (`populateList`, `selectMailbox`, `scopeAddress`) — extract to a service before reusing for the API.

## Missing tests / verification
5. IMAP IDLE unverified against a real server (see CLAUDE.md roadmap).
6. Quality gates 2026-11-04: backend `./mvnw -q -B test` exit 0; Flutter gates not run (no project, no SDK).

## Deferred / tech debt
7. Generic routine assumes jOOQ; backend uses JPA — no codegen; routine docs should be adjusted.
8. Routine says commit to `master`; this environment mandates feature branches (`claude/gifted-carson-47de62`, `claude/trusting-cori-47de62`) — followed the branches. CLAUDE.md here also says never push to main. Merge to master is a human step.
9. Existing CLAUDE.md roadmap items remain open (support email, Google OAuth publish, Web Push, delivery states, signature card, shared files, reply-to-bubble).
