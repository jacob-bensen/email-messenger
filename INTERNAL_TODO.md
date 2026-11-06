# Internal TODO (backend)

## Sync audit (2026-11-06), by severity
### Contract breakage
1. No REST API / token auth exists; mobile client has nothing to call. Needs `/api/v1`
   with a stateless security filter chain (session+CSRF chain stays for the website).
### Logic drift
2. `scopeAddress` / `sendingAccount` / `selectMailbox` live as private helpers in
   `ConversationController`; extract to a service so a REST controller shares them.
3. Send flow (reply vs new email + record) is orchestrated inside the controller; move to a
   service method.
4. Stack deviation: generic routine assumes jOOQ; project uses JPA (documented).
### Missing tests / verification
5. IMAP IDLE unverified against a real server (see CLAUDE.md roadmap).
### Style
6. Roadmap items in CLAUDE.md remain (support email, OAuth publish, Web Push, delivery states...).
