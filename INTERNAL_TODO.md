# Internal TODO (ordered by severity)

## Contract breakage
1. No token auth for mobile (session+CSRF only). Decide: JWT access+refresh vs
   opaque tokens; add a stateless `/api/**` filter chain in `SecurityConfig`.
2. No `/api/v1` endpoints exist; mobile client has nothing to call. Implement
   the PLANNED set in `docs/API.md` (JSON records over existing services).

## Logic drift / architecture
3. `ConversationController` holds orchestration (recipient parsing, signature
   append, mailbox selection, send+record) — extract into a service shared by
   web and API.
4. Prompt assumes jOOQ; backend uses JPA/Hibernate. Docs reflect reality; no
   jOOQ codegen exists.
5. Prompt says commit to `master`; this session's harness mandates dedicated
   branches (`claude/gifted-carson-gitvf7`, `claude/trusting-cori-gitvf7`).

## Missing tests / verification
6. Attachments not downloadable (`blob_ref` null) — blocks mobile attachments.
7. IMAP IDLE unverified against a real server (see CLAUDE.md roadmap).

## Style
(none recorded)
