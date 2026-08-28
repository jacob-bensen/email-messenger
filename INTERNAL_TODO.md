# INTERNAL_TODO — backend (email-messenger)

Running list of gaps, tech debt, and deferred work for the full-stack
sync routine. Ordered by severity: **contract breakage > logic drift >
missing tests > style**. Consume from and append to this file each run.

## Contract breakage (blocks mobile client)

- **No REST API for the mobile client.** All controllers return HTML
  keyed to a `JSESSIONID` cookie. The mobile Flutter client cannot
  authenticate or read data until we ship a token-authenticated
  `/api/v1/*` surface. First endpoints needed (in this order):
  1. `POST /api/v1/auth/login` (email+password → bearer token)
  2. `POST /api/v1/auth/refresh`
  3. `POST /api/v1/auth/logout`
  4. `GET  /api/v1/me`
  5. `GET  /api/v1/chats?cursor=&limit=` (chat list)
  6. `GET  /api/v1/chats/{key}` (one conversation timeline)
  7. `POST /api/v1/chats/{key}/messages` (send reply)
  8. `GET  /api/v1/events` (server push, SSE or WebSocket)
  9. `GET  /api/v1/mailboxes` + POST/DELETE
- **CSRF filter** currently applies to every non-whitelisted path.
  When `/api/v1/**` ships it must be excluded from CSRF (stateless
  bearer-token auth).
- **Token storage table** — decide opaque random string in a Postgres
  table with `hashed_token`, `user_id`, `created_at`, `expires_at`,
  `revoked_at`. Add a Flyway migration when the auth endpoints land.

## Logic drift

- `Attachment.blob_ref` is always null — the download endpoint promised
  on the roadmap doesn't exist yet, so the 📎 chips have nothing to
  link to on the web either. Fixing this unblocks both the website
  and the mobile client's file view.
- Google OAuth consent screen is still in Testing mode; refresh tokens
  expire after 7 days. Reconnect flow exists at
  `/mailboxes/gmail/reconnect/{id}` but the mobile client will need
  the same reconnect surface once the REST API exposes mailbox
  management.

## Missing tests

- Backend: no tests exist yet for the (unbuilt) REST API. Every new
  `/api/v1/*` endpoint ships with a `@WebMvcTest` slice + a service
  unit test.

## Docs / style

- The stray `master/` directory at the repo root holds obsolete
  routine notes (`BACKLOG.md`, `PLAN.md`, etc.). Move or delete once
  we're sure nothing references it — deferred to avoid mixing a chore
  into a feature commit.
- `CLAUDE.md`'s "Working agreement" refers to a single-repo routine;
  update it to reference the sibling mobile repo now that this file
  exists. Deferred to the next feature commit that changes CLAUDE.md
  anyway.
