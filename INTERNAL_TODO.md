# Internal TODO (backend)

Rolling list of gaps, tech debt, and deferred work discovered during sync
runs. Consume from the top, append at the bottom. Roadmap-level items live
in `CLAUDE.md`; this file is for cross-repo API/architecture debt.

## Contract breakage / mobile-blockers (severity: HIGHEST)

- [ ] **Token-based auth for `/api/**`.** Everything except `/health` will
      need it. Design + endpoints:
      - `POST /api/v1/auth/login` — body `{email, password}` → `{token,
        refreshToken, expiresAt, user: {id, email, displayName}}`.
      - `POST /api/v1/auth/refresh` — body `{refreshToken}` → same shape.
      - `POST /api/v1/auth/logout` — bearer-authed; revoke the refresh
        token.
      - New `AuthTokenService` + Flyway migration for a
        `mobile_refresh_tokens` table (id, user_id, token_hash,
        issued_at, expires_at, revoked_at).
      - Add a `BearerTokenAuthenticationFilter` before the anonymous
        filter, populate the `Authentication` from the DB. Do not touch
        the existing form-login chain.
      - Google-OIDC users have `passwordSet = false`; the login endpoint
        must reject them with `AUTH_PASSWORD_NOT_SET` so the mobile app
        can trigger a Google sign-in flow instead. Google sign-in on
        mobile is a separate follow-up (native SDK → server token
        exchange).
- [ ] **Chats-list endpoint.** `GET /api/v1/chats?cursor=…&limit=…` →
      paginated list keyed by `conversationKey`, mirroring
      `ConversationListService`.
- [ ] **Conversation fetch.** `GET /api/v1/chats/{key}` → messages sorted,
      quoted-reply stripped (reuse `service/ConversationService`).
- [ ] **Send reply.** `POST /api/v1/chats/{key}/messages` — body
      `{body, replyToMessageId?}` → the created message. Delegate to
      `service/ReplyService`.
- [ ] **Mailboxes list.** `GET /api/v1/mailboxes`.
- [ ] **Live delivery for mobile.** Either an SSE stream at
      `/api/v1/events` (bearer-authed) or WebSocket. Web Push (top-level
      roadmap) is orthogonal — this is for foreground live updates.

## Logic drift risk

- [ ] Once the mobile chats endpoint exists, unit-test that the JSON DTO
      list and the Thymeleaf `ChatRail` model list are produced from the
      same `ConversationListService` call — no forked ordering or unread
      counts.
- [ ] Same for reply: server-authoritative `ReplyService` must be the
      only writer for outbound messages. Mobile client sends body text,
      never a pre-baked `Message-ID`.

## Missing tests

- [ ] Integration test that a bearer-authed request from the mobile
      client sees the same conversation list as a browser session for
      the same user.

## Style / cleanup

- [ ] `docs/API.md` currently documents one endpoint. Grow it (and the
      error-code catalog) as each new endpoint lands.
- [ ] Consider extracting a `com.emailmessenger.api.dto` sub-package once
      more DTOs exist — for now `HealthResponse` sits alongside its
      controller.
