# INTERNAL_TODO.md

Ordered by severity (contract breakage > logic drift > missing tests > style). Sync audit 2026-10-07.

## Contract breakage
1. **No JSON API exists for the mobile client.** Entire surface is HTML + session + CSRF (docs/API.md §2 is PLANNED only).
2. **No token auth** (Bearer/JWT or equivalent); mobile cannot log in without cookie+CSRF. Decide scheme, add a second `SecurityFilterChain` for `/api/**` with CSRF off.
3. Frontend repo has no Flutter project (no `pubspec.yaml`/`lib/`); nothing to diff against.
4. `/chats/events` SSE event names/payload undocumented — read `ChatEventStream` and document.
5. Google sign-in for mobile (OIDC redirect flow is browser-only) — needs ID-token exchange.

## Logic drift risk
6. `ConversationController` holds orchestration (recipient parsing, signature append, sending-account selection, `sendNewEmail` + `recordNewEmail` pair, mailbox scope resolution). Extract into a service before an API controller reuses it, or the API will fork the logic.
7. Spec for this routine assumes jOOQ; backend uses JPA/Hibernate. Keep JPA (consistent with code); revisit only if the owner wants jOOQ.
8. Attachments not downloadable (`blob_ref` null) — API can only expose metadata.

## Process notes
9. Routine says "work on master, no other branches"; this session's harness pins `claude/gifted-carson-w6zf7o` (backend) and `claude/trusting-cori-w6zf7o` (frontend). Branch pins were followed. Reconcile the routine config.
10. `CLAUDE.md` roadmap is the product roadmap for the web app; mobile roadmap lives in frontend `docs/DESIGN.md`.

## Missing tests
11. No tests for any `/api/**` (doesn't exist). Add MockMvc slice tests when built.
12. IMAP IDLE not yet verified against a real server (per CLAUDE.md).

## Done this run
- Created docs/BACKEND.md, API.md, WEB.md, INTERNAL_TODO.md, CHANGELOG.md; updated CLAUDE.md.
