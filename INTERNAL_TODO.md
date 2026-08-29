# Internal TODO (backend)

Gaps, tech debt, and deferred work. Ordered by severity within each
section: contract breakage > logic drift > missing tests > style.

## Contract mismatches (mobile ↔ web)

- **No JSON REST API exists.** `docs/API.md` (v1) is entirely PLANNED.
  Every endpoint has to be built, feature by feature. Only two
  `@ResponseBody`/`@RestController` endpoints exist today
  (`POST /chats/refresh`, `POST /webhooks/stripe`) and neither is under
  `/api/v1`. First feature run should ship
  `POST /api/v1/auth/login` + `GET /api/v1/me`, since every other
  endpoint depends on bearer auth.
- **No mobile Google OAuth path.** Web uses the Spring OAuth2 client
  redirect flow (`OAuthStartController` → `/login/oauth2/code/google`).
  Mobile needs `POST /api/v1/auth/oauth/google` accepting a Google
  ID token from the client SDK, reusing `OAuth2ProvisioningService`.
- **Bearer-token store not designed.** Pick JWT (stateless, easy to
  rotate signing key) vs. DB-backed `api_tokens` table (revocable per
  device). Prefer DB-backed to match the pattern of
  `password_reset_tokens` / `email_verifications`. Will require a new
  Flyway migration `V36__api_tokens.sql`.
- **`POST /chats/refresh` returns `Map<String,Integer>` with
  bucket-name keys defined implicitly** — needs an explicit contract
  before duplicating under `/api/v1`.

## Logic drift risks

- The IM transform + quoted-reply stripping (`IMTransformService`,
  `EmailBodyCleaner`) is server-only today. When a mobile compose UI is
  added, the client MUST NOT re-implement stripping — it should render
  what the server returns. `docs/MOBILE.md` will state this.
- `PlanLimitService` decides per-feature caps. Mobile must never
  pre-decide: it renders whatever the server allows and surfaces `402
  plan_limit_exceeded` verbatim.

## Missing tests

- No slice tests exist for JSON endpoints because none exist yet. When
  `/api/v1/auth/login` ships, add a `@WebMvcTest` +
  a MockMvc slice test alongside the existing auth tests.

## Docs / style

- `docs/WEB.md` marks a TODO for the SavedSearch list+create routes —
  need to inspect `SavedSearchController` fully and add the missing
  entries.
- Roadmap items pending in `CLAUDE.md` (support email, Google OAuth
  consent screen publication, live-delivery part 2 web push, delivery
  states for outbound bubbles, signature contact card, per-person
  shared files, reply-to-specific-bubble) still stand; leave them there
  as the primary roadmap. This file is for cross-repo sync only.
