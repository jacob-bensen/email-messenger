# INTERNAL_TODO — backend

Running list of known gaps, tech debt, and deferred work. Consume the top
of the list each run; append newly discovered items at the bottom of the
relevant section.

## Sync-routine mismatches (severity-ordered)

1. **Contract breakage** — the mobile client has no endpoints to call.
   There is no `/api/v1/**` surface yet, and the web endpoints require
   a session cookie from form login. Priority for the first Phase 1 run.

2. **Auth-for-mobile is undecided.** Session cookies from `/login` are
   the only auth mechanism today. The mobile API needs either a bearer
   token (JWT or opaque) or a mobile-friendly login endpoint that mints
   a session cookie the client can carry. Decide in the PR that adds
   the first authenticated `/api/v1` endpoint; record the decision in
   `docs/API.md`.

3. **Attachments have no download endpoint.** `Attachment.blob_ref` is
   always null today (see the roadmap in `CLAUDE.md`); nothing to serve
   until a blob store lands. The mobile client will need this too.

## Deferred from Phase 0 discovery

- Reverse-engineered `docs/BACKEND.md`, `docs/WEB.md`, and `docs/API.md`
  from the code on 2026-08-30. If a controller or service is added later
  and forgotten, these docs will drift. On each subsequent run, spot-check
  one controller or one service class against `docs/BACKEND.md` /
  `docs/WEB.md` and reconcile.
- Backend uses **JPA / Spring Data**, not jOOQ. The generic sync-routine
  target stack mentions jOOQ; that guidance does not apply here. When
  adding schema, still write a new immutable Flyway migration.

## Roadmap items unchanged from CLAUDE.md

The pre-existing CLAUDE.md roadmap is still the source of truth for
website-side work (support-email wiring, publishing the Google OAuth
consent screen, Web Push, delivery states, signature-card, per-person
files, quote-reply-to-a-bubble). Do not duplicate them here; when the
sync routine picks a feature, pick from that roadmap if nothing above
is unblocked and higher-value.
