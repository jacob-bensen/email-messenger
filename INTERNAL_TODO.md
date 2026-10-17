# Internal TODO (ordered by severity)

## Contract breakage
- No `/api/v1` JSON API exists; the mobile client has nothing to call. Needs bearer-token auth (stateless filter chain separate from the session/CSRF chain in `SecurityConfig`) before any endpoint.
## Logic drift
- Controllers (`ConversationController`) build view models inline (list filters, scope selection, mailbox param parsing). Extract into a service before a REST layer reuses it, to avoid forking logic.
## Missing capability
- Attachment bytes not stored (`blob_ref` null) — mobile can't download files.
- Roadmap items in CLAUDE.md (Web Push, delivery states, reply-to-bubble, ...).
## Missing tests / verification
- IMAP IDLE unverified against a real server.
## Process notes
- Routine prompt assumes jOOQ, `master` branch and `mvn verify`; actual: JPA, session-designated branches (`claude/gifted-carson-kzxd0r`), CLAUDE.md forbids pushing to main. Followed the session branch.
