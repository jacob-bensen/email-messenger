# Internal TODO

Known gaps, tech debt, and deferred work, ordered by severity within each
section. Consumed and appended to by each full-stack sync run. The mobile
repo keeps its own list for client-only items.

## Contract breakage

- [ ] Unknown paths under `/api/**` requested with a valid token return the
      website's HTML 404 (`GlobalExceptionHandler`), not the JSON error
      envelope `docs/API.md` promises. Handle `NoResourceFoundException` /
      `HttpRequestMethodNotSupportedException` for `/api/**` requests.

## Logic that must move out of web controllers before the API can share it

The API may not fork these; extract each into a service used by both.

- [ ] `ConversationController`: mailbox scoping (`selectMailbox`,
      `scopeAddress`) — needed for `?mailbox=` on the API list.
- [ ] `ConversationController.send` / `createMessage`: sending-account
      choice (`sendingAccount`), signature appending (`withSignature`),
      recipient parsing (`parseRecipients`), default subject, and the
      reply-vs-new decision. Needed for send/compose endpoints.
- [ ] `ConversationController`: `fallbackAccount` / `accountColorMap` — only
      if the mobile list wants per-account tags/colours.
- [ ] Filter parsing exists twice: lenient in `ConversationController`
      (unknown → ALL) and strict in `ConversationApiController` (unknown →
      400). Intentional difference in strictness, but the name→enum mapping
      should live on `ChatFilter`.

## API coverage gaps (web has it, API doesn't)

Ordered by the mobile roadmap (`conexus-mail-mobile/docs/DESIGN.md`).

- [ ] Conversation timeline + mark-read (`ChatService.buildFor`/`markRead`).
- [ ] Send message / reply, attachments.
- [ ] Live updates for mobile (SSE with bearer auth, or push).
- [ ] Google sign-in for mobile; registration; password reset; email
      verification resend.
- [ ] Mailbox list/scope; saved searches; billing status / plan in `/me`.

## Security / hardening

- [ ] No way for a user to see or revoke their API tokens (devices) from
      `/account`. `api_tokens.device_name` / `last_used_at` are recorded for
      this.
- [ ] Expired and revoked `api_tokens` rows are never purged.
- [ ] `ClientIp` trusts the leftmost `X-Forwarded-For` value unconditionally;
      the per-IP login lockout (web and API) can be sidestepped or abused by
      a client that sets the header itself unless the proxy overwrites it.
      Pre-existing; affects both doors.
- [ ] No general rate limit on authenticated API calls.

## Missing tests

- [ ] No test proves a web session cookie is *not* honoured on `/api/**`
      (the chain is `STATELESS`, so it should not be).
- [ ] One test is skipped in `./mvnw verify` in environments without Docker
      (Testcontainers); the V36 migration is therefore only verified against
      H2 here, not PostgreSQL.

## Docs / housekeeping

- [ ] `CLAUDE.md` roadmap and `master/PLAN.md` describe web-only work and
      predate the mobile client; `master/PLAN.md` still calls the product
      "MailIM" with old pricing.
- [ ] `CLAUDE.md` says Docker files and GitHub Actions CI were removed, but
      `Dockerfile`, `docker-compose.yml`, `.dockerignore` and `.github/`
      are present in the tree. Owner to decide which is right.
- [ ] The routine's target stack names jOOQ; this codebase is JPA. Left as
      is (matching existing architecture).
