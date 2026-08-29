# INTERNAL_TODO.md

Severity-ordered running list of gaps and deferred work found by the
full-stack sync routine. `[H]` = contract breakage or blocking gap,
`[M]` = logic drift or missing coverage, `[L]` = style / hygiene.

## Contract / API

- `[H]` **No mobile REST API exists.** `docs/API.md` describes the
  planned `/api/v1/*` surface; no controllers implement it. First
  concrete slice should be `/api/v1/auth/*` (decide session-cookie vs.
  bearer-token first) and `/api/v1/chats` (list + detail read).
- `[H]` **Auth mechanism for the mobile client is undecided.** Sessions
  or bearer tokens. Blocks every other API endpoint.
- `[M]` `docs/API.md` DTO shapes are drafts — mirror them from the
  service-layer models (`ChatConversation`, `ChatMember`, `MailAccount`,
  `Message`) once endpoints are implemented, don't invent new ones.

## Logic drift

- `[M]` The routine's target stack names **jOOQ**; the code uses **JPA**.
  Docs now say JPA (evidence wins). No action beyond keeping docs
  honest.
- `[M]` `V33__drop_thread_notes.sql` removed `thread_notes` — verify no
  dead references remain (search the codebase before next feature).

## Tests

- `[M]` No integration tests exist for a mobile API path (there is no
  such path yet). Add slice tests as soon as the first `/api/v1/*`
  controller lands.

## Style / hygiene

- `[L]` A stray file called `master` sits at the repo root
  (`/home/user/email-messenger/master`). Investigate origin before
  deleting — do NOT `rm` blind.
- `[L]` `Dockerfile` and `docker-compose.yml` remain even though the
  roadmap marks Docker "Removed 2026-06-11 — not deploying via Docker".
  Either delete them or add a note explaining why they stay.

## Frontend gaps (tracked here for cross-repo visibility)

- `[H]` Mobile repo skeleton has no platform folders — `flutter create .`
  must be run once a Flutter toolchain is available. Detail in the
  mobile repo's own `INTERNAL_TODO.md`.
