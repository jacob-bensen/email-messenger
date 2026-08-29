# Internal TODO (backend)

Gaps and follow-ups discovered during full-stack sync. Highest severity
first: **contract breakage > logic drift > missing tests > style**.

## Contract gaps (mobile API)

- No JSON auth endpoints. `POST /api/auth/login`, `GET /api/auth/me`,
  `POST /api/auth/logout` are documented in `docs/API.md` roadmap but
  not implemented. Blocks the mobile client past its splash screen.
- No mailbox listing endpoint. `GET /api/mailboxes` needed for the
  mobile app's "connected accounts" screen.
- No chats endpoint. `GET /api/chats`, `GET /api/chats/{key}`,
  `POST /api/chats/{key}/reply` — the mobile app is a chat client, so
  these unblock its main surface.

## Logic drift risks

- `POST /chats/refresh` on `ConversationController` returns JSON via
  `@ResponseBody` but lives on a Thymeleaf controller. When the mobile
  API needs an unread-count refresh, mirror it under `/api/chats/refresh`
  routing through the same service, and treat this Thymeleaf endpoint as
  the website's private hook.
- `AppearanceSettings` is a per-user preference set that the mobile app
  will want to honor for theme consistency. Not yet exposed under `/api`.

## Missing tests

- Once auth endpoints exist, add MockMvc slice tests covering: correct
  password → 200 + session cookie; wrong password → 401; throttle
  interaction (locked email → 429).
- Health endpoint currently tests only 200 shape — add a test asserting
  it works when no session cookie is present *and* when one is (to
  guard against future `permitAll` rules regressing).

## Style / doc

- `EmailMessengerApplication` documents `@EnableConfigurationProperties`
  with a growing list — add a comment on why each is enabled inline
  rather than as a wall of imports.
