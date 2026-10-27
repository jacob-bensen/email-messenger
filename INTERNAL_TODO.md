# Internal TODO (ordered by severity)

## Contract breakage
- [ ] No `/api/**` exists; the mobile client has nothing to call. Implement `/api/v1`
      per `docs/API.md` §2 (chats list, chat detail, send, new chat, mailboxes, me).
- [ ] No non-browser auth: only session cookie + CSRF + form/OIDC login. Need Bearer token
      auth (and a stateless `SecurityFilterChain` for `/api/**`).
- [ ] Unauthenticated requests redirect to `/login` (302) instead of 401 JSON — API chain must differ.

## Logic drift / layering
- [ ] `ConversationController.send/createMessage` holds orchestration (recipient parsing,
      signature append, sender selection, reply-vs-new, record outbound) — extract to a
      service so web and API share it. Same for `selectMailbox/scopeAddress` scoping rules.
- [ ] `ChatService`, `OutboundMessageService` are package-private in `web`; widen/move when API needs them.

## Missing / unverified
- [ ] SSE event name/payload undocumented in API.md (read `ChatEventStream`).
- [ ] Stripe webhook error status codes undocumented.
- [ ] IMAP IDLE unverified against a real server (see CLAUDE.md roadmap).
- [ ] Full `mvn test` not run in Phase 0 (docs-only); `mvn test-compile` passed.

## Housekeeping
- [ ] `CLAUDE.md` says Dockerfile/docker-compose were removed, but both exist in the repo root.
- [ ] Routine prompt says work on `master`; this session was pinned to feature branches
      (`claude/gifted-carson-04g2zd`, `claude/trusting-cori-04g2zd`) and CLAUDE.md forbids pushing main.
