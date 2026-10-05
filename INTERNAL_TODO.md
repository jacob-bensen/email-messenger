# INTERNAL_TODO

Ordered by severity (contract breakage > logic drift > missing tests > style).

## Contract breakage
- No REST API exists; mobile client has nothing to call. Next feature: implement
  `/api/v1` auth + chats list/read/send per `docs/API.md` (token auth + migration).

## Logic drift / structural
- Stack differs from the generic routine's assumption: JPA/Hibernate, not jOOQ (no codegen step).
- Chat view models live in `service`/`web` and are HTML-oriented (`bodyHtml`); decide
  whether the API serves sanitized HTML or plain text + markup flags.
- Existing project docs live in `master/` (PLAN, BACKLOG, CHANGELOG 500KB, MASTER_ACTIONS);
  root `CHANGELOG.md` was added 2026-10-05 for this routine — consider reconciling.

## Missing tests / verification
- `./mvnw test` not run during bootstrap run (docs only).
- ImapIdle not verified against a real IDLE server (see CLAUDE.md roadmap).

## Roadmap items inherited from CLAUDE.md
Support email, Google OAuth publish, Web Push, delivery states, contact cards, shared files, reply-to-bubble.
