# INTERNAL_TODO.md

Ordered by severity (contract breakage > logic drift > missing tests > style).

## Contract breakage
- No `/api/v1` REST API and no token auth exist; mobile client cannot talk to the server. First step: design auth (bearer token) + read-only conversation list/detail in `API.md`, then implement atop existing services.
- Mobile repo has no Flutter project (`flutter create` needed; no Flutter/Dart SDK in the routine sandbox as of 2026-10-09).

## Logic drift
- Conversation/list logic lives in `ConversationController`/`ChatService` with some view-shaping in the controller (`selectMailbox`, `scopeAddress`); extract before exposing via REST so web and API share it.

## Spec vs. reality
- Routine prompt assumes jOOQ; backend uses JPA/Hibernate. No codegen step. Keep JPA.
- Routine prompt says commit to `master`; this session's harness mandates feature branches (`claude/gifted-carson-dq01r4`, `claude/trusting-cori-dq01r4`) and `CLAUDE.md` forbids pushing to main. Branch rules followed; no merge to master was done.

## Missing tests / unverified
- IMAP IDLE not verified against a real server (see CLAUDE.md roadmap).
- Full `./mvnw test` not run in this session's sandbox (Maven wrapper download blocked; see CHANGELOG).

## Deferred (backend roadmap lives in `CLAUDE.md`)
- Support email, Google OAuth consent publish, Web Push, delivery states, signature contact card, attachment storage, reply-to-bubble.
