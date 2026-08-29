# Internal TODO (backend)

Running list of gaps, tech debt, and deferred work discovered during
routine runs. Ordered by severity: contract breakage first, then logic
drift, then missing tests, then style. Add items here whenever a run
turns them up; remove them when they're done.

## Contract breakage / blocking mobile client

- [ ] **No JSON REST API for mobile clients.** The backend is entirely
      Thymeleaf + session cookies today. Only `POST /chats/refresh`
      (`@ResponseBody`) and the `/chats/events` SSE stream return
      anything a client could parse, and both require `JSESSIONID`.
      Every feature in `conexus-mail-mobile/docs/DESIGN.md` Phase M1 is
      blocked on this. First steps:
      1. Add a new `com.emailmessenger.api` package with a
         `SecurityFilterChain` bean for `/api/**` that uses bearer
         tokens (JWT or opaque + DB lookup) and is CSRF-exempt.
      2. Implement `POST /api/v1/auth/login`, `.../refresh`,
         `.../logout` as JSON adapters over `UserService` /
         `DbUserDetailsService`.
      3. Every subsequent endpoint MUST be a thin JSON adapter over an
         existing service — never duplicate logic that the Thymeleaf
         controllers already delegate to.
- [ ] **Mobile Google OAuth flow is undecided.** The current
      `OAuthStartController` assumes a browser round-trip with a session
      cookie at the end. Mobile needs either (a) an in-app browser
      redirect back to a deep link with an app-issued token, or
      (b) native Google Sign-In → ID token → backend exchange for an
      app token. Pick one before Phase M1 lands.

## Logic drift / follow-ups

- [ ] `conversationKey` normalization lives only in Java
      (`ConversationKeyService`). Because DESIGN.md forbids the client
      from deriving keys, this doesn't leak — but any future "start a
      chat by picking recipients" flow needs the server to return the
      key rather than the client posting to a URL keyed by one it made
      up.
- [ ] Attachment `blob_ref` is always null. Attachments are metadata-
      only today; downloads don't work on the web either. Backend
      roadmap already tracks this ("Per-person shared files & links");
      note here so the mobile client doesn't ship an attachment UI
      before the backend supports downloads.

## Missing tests

- [ ] `ConversationController.refresh` (the sole `@ResponseBody` endpoint
      today) has no integration test asserting its JSON shape. Add one
      when the token-auth path lands, so the shape is pinned before
      mobile depends on it.

## Style / hygiene

- (none yet)

## Deferred until further notice

- (none yet)
