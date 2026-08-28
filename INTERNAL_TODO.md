# Internal TODO

Running list of known gaps, tech debt, and deferred work. Ordered by
severity: contract breakage > logic drift > missing tests > style. Consume
from this list on every run; add newly discovered items back.

## Contract / cross-repo

- **Mobile auth.** `/api/v1/**` has no bearer-token layer yet. The web
  side uses session cookies + form login. Design + implement:
    - `POST /api/v1/auth/login` (email + password → token).
    - `POST /api/v1/auth/logout` (revoke token).
    - Token storage (probably a `mobile_session_token` table, hashed
      token, `user_id`, `created_at`, `last_seen_at`, optional
      `device_label`, expiry).
    - A Spring Security filter that authenticates `Authorization: Bearer …`
      requests against the token table, populating an `Authentication`
      that reuses `User` / `DbUserDetailsService`.
    - CSRF is already exempt for `/api/v1/**`.
    - Client side lives in `conexus-mail-mobile` under `lib/data/`.
  Must land in one run with a matching update to `docs/API.md` and the
  Flutter client's networking layer.

- **Chats API.** Once auth is in, mirror the `/chats/**` Thymeleaf
  endpoints as JSON: list, one conversation with messages, reply, and
  the SSE event stream. Payloads are large enough that the shape merits
  a dedicated design pass rather than a mechanical port.

## Backend

- The `spring.application.version` in `application.yml` is kept in sync
  with `pom.xml` by hand. Consider wiring the Maven `${project.version}`
  in via resource filtering so there's one source of truth.
- No integration test yet asserts that `/api/v1/**` is CSRF-exempt end
  to end (the current test covers a GET, which wouldn't need a token
  anyway). Add a POST test once a POST endpoint exists.

## Mobile (see conexus-mail-mobile/INTERNAL_TODO.md too)

- Flutter project scaffolded by hand (no `flutter` toolchain in this
  environment). First developer with Flutter installed should run
  `flutter create .` in the mobile repo to fill in `android/`, `ios/`,
  `linux/`, `macos/`, `windows/`, and `web/` platform folders without
  clobbering `lib/` or `test/`.
- No auth flow yet (waiting on the mobile-auth API above).
