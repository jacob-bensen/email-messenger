# Internal TODO

Known gaps and deferred work, most severe first (contract breakage > logic
drift > missing tests > style/hygiene). Client-side items live in
`conexus-mail-mobile/INTERNAL_TODO.md`. Product roadmap: `CLAUDE.md`
§Roadmap (website) and `conexus-mail-mobile/docs/DESIGN.md` §ROADMAP (mobile).

## 1. Contract

- [ ] **No mobile API exists.** Every endpoint is a session+CSRF browser
      endpoint (`docs/API.md` §1). Next step is mobile M1 "API foundation +
      sign in": `/api/v1` stateless security chain, bearer tokens, `me`,
      JSON error envelope (`docs/API.md` §2, §5).

## 2. Logic drift / duplication risk

- [ ] **Send flow lives in `ConversationController`** (`/chats/new` and
      `/chats/{key}/send`): recipient parsing, sending-account selection,
      signature append, `(no subject)` default, reply-vs-new choice, and the
      `ReplyService` + `OutboundMessageService` pairing. Extract into one
      service before any API endpoint sends mail, so web and API cannot fork.
- [ ] `ConversationLabels.initials` uses `toUpperCase()` with the default
      locale (Turkish `i` → `İ`) and splits on Java `\s` (ASCII only). The
      Dart mirror uppercases locale-independently and splits on Unicode
      whitespace. Switch the server to `Locale.ROOT` and decide the
      whitespace class, then update `docs/API.md` §4.2 and both tests.
- [ ] `ConversationController` reads `EmailThreadRepository` directly for
      the reply target; move into the extracted send service.

## 3. Missing tests

- [ ] No shared golden vectors for `ConversationLabels` (only the
      conversation key has them, since 2026-11-16).
- [ ] IMAP IDLE never verified against a real server (see `CLAUDE.md`).

## 4. Hygiene

- [ ] A top-level `master/` directory holds stale planning files
      (`PLAN.md`, `BACKLOG.md`, `MASTER_ACTIONS.md`, a ~500 KB
      `CHANGELOG.md`) describing a "MailIM" product with pricing that
      contradicts `PlanLimits`. Probably an accidental commit; confirm with
      the owner before deleting.
- [ ] `Dockerfile` and `docker-compose.yml` are present although `CLAUDE.md`
      says Docker was removed 2026-06-11. Confirm and delete or fix the note.
- [ ] The cross-repo routine assumes jOOQ; this project uses JPA. Docs say
      so; no migration planned.
