# Internal TODO — backend

Consumed and appended to on every full-stack sync run. Ordered by severity
(contract breakage > logic drift > missing tests > style).

## Contract / architecture gaps

- [ ] **Mobile auth story.** `/api/v1/*` currently relies on the same
  session cookie the web uses. A native app needs one of:
    (a) bearer JWT + refresh token pair (mint on `POST /api/v1/auth/login`,
        revoke on `/api/v1/auth/logout`);
    (b) a Google OAuth code-exchange endpoint mirroring the web OIDC flow
        so mobile "Continue with Google" works;
    (c) a device-scoped API key issued from a signed-in web session.
  Pick (a) with (b) as a companion for Google users. Nothing implemented yet.

- [ ] **`/api/v1/*` in SecurityConfig.** Only `/api/v1/health` is currently
  permit-all; the rest of `/api/**` will fall through to `.anyRequest()
  .authenticated()`, which redirects unauthenticated browsers to `/login`
  (unhelpful for a JSON client). Add a request matcher that returns 401 JSON
  for `/api/**` instead of an HTML redirect.

- [ ] **Live delivery for mobile.** Server currently pushes new-mail events
  via SSE at `/chats/events`. Mobile equivalents: an SSE endpoint under
  `/api/v1/events`, or FCM/APNs push. Decision pending.

- [ ] **CSRF on `/api/**`.** Spring Security's CSRF token protection assumes
  browser forms. Once auth is bearer-token based, exempt `/api/**` from
  CSRF the same way `/billing/webhook` is exempted today.

## Logic drift risks

- [ ] The `service/` layer is not yet fully separated from Thymeleaf
  concerns — some services return view-model records
  (`ConversationListItem`, `BubbleRun`, …) that ship strings pre-formatted
  for HTML. When we add REST endpoints that return the same data, we need to
  either return the raw domain records + let the mobile client render them,
  or add DTO mappers that don't duplicate the transform. Do NOT fork the
  logic into an "API version".

- [ ] `ReplyService` always threads onto the thread's last message (roadmap
  item in `CLAUDE.md`). Mobile reply-to-bubble should not paper over that
  bug — fix it once in the service.

## Tests / tooling

- [ ] No test yet exercises the full `/api/v1/*` chain through
  `SecurityConfig`. Add a WebMvc slice test using
  `@WithMockUser` once real endpoints exist.

- [ ] `mvn` runs but tests occasionally take a long time to spin up
  H2 + Spring context. Consider a small `@DataJpaTest` split from the
  full `@SpringBootTest`s.

## Deferred

- [ ] Publish `docs/API.md` at a stable URL (e.g. `/docs/api.html`) once
  more than the health endpoint exists.
