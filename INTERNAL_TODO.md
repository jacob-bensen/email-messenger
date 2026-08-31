# Internal TODO — email-messenger (backend + website)

Sync-oriented gaps and tech debt, ordered by severity. Consumed and appended
to on every routine run.

## Contract breakage (highest severity)

- **NO REST API EXISTS FOR THE MOBILE CLIENT.** Everything under
  `com.emailmessenger.web`, `auth`, `billing`, `mailboxes` is a Thymeleaf
  `@Controller`. `conexus-mail-mobile` cannot function until the endpoints
  in `docs/API.md` land. Recommended order:
  1. `POST /api/v1/auth/login` + `POST /api/v1/auth/register` returning
     opaque bearer tokens (new `api_tokens` table, one-shot Flyway
     migration).
  2. `GET /api/v1/me`.
  3. Chats read path: `GET /api/v1/chats`, `GET /api/v1/chats/{key}`.
  4. Chats write path: `POST /api/v1/chats/{key}/send`,
     `POST /api/v1/chats/{key}/read`.
  5. Mailboxes CRUD.
  6. SSE `/api/v1/chats/events` (reuse `NewMailEvent` publisher).
- REST controllers MUST reuse `ConversationService`, `IMTransformService`,
  `ReplyService`, `MailAccountService`, etc. — no forked business logic in
  `@RestController`s.

## Logic drift risk

- `ReplyService` currently always threads onto the thread's last message
  (see roadmap "Reply-to-a-specific-bubble"). When adding the REST send
  endpoint, expose the same `replyToMessageIdHeader` parameter and fix
  threading; mobile and web must not diverge here.
- `MessageRecipient` normalization: the mobile client will echo participant
  emails back on send. Make sure the API layer normalizes (lowercase, trim)
  the same way `ConversationKeyService` does — factor into a shared helper
  before the first send endpoint ships.
- `blob_ref` on `Attachment` is always null. Mobile clients cannot download
  attachments. This blocks any mobile "shared files" surface, and it maps to
  the "Per-person shared files & links" roadmap item.

## Missing tests

- No integration tests yet exercise a `/api/v1/**` path (none exist). When
  the first REST endpoint ships, add a `WebMvcTest` slice AND a
  Testcontainers-based end-to-end test that hits the same service layer as
  the web controller does, to prove the two paths stay in sync.

## Style / hygiene

- The two-tier controller layout will grow; keep REST controllers under a
  new `com.emailmessenger.api` package to make the boundary obvious.
- `docs/API.md` "Status" column must be updated to **Implemented** the same
  commit an endpoint lands. Reviewers should reject the commit otherwise.

## Deferred / blocked

- Google OAuth consent screen still in Testing (roadmap): weekly reconnect
  is a real UX cost for OAuth mailboxes on both web and mobile. Note this
  in the mobile "connect mailbox" flow when it ships.
- Support email address is still TBD (roadmap); don't ship an app-store
  listing for the mobile client until it is decided.
