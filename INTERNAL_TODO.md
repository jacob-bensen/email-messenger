# INTERNAL_TODO

## Contract breakage (severity 1)
- No `/api/v1` REST surface and no token auth: the mobile client cannot log in or read conversations. Design in `docs/API.md` first (login/refresh tokens, conversations list/detail, send, mailboxes), then implement over existing services. CSRF + session-only auth must be bypassed only for the token-authenticated `/api/**` chain.
- `GET /chats/events` payload shape undocumented (read `ChatEventStream`).
- Errors are HTML only; define a JSON error envelope for `/api/**`.

## Logic drift
- Web controllers hold some logic (e.g. `ConversationController` is large); extract into services before the API reuses it.

## Missing tests / tooling
- Backend gate not run this run (docs-only).
- Flutter SDK absent in this environment; frontend gates cannot run.

## Other
- Roadmap items in CLAUDE.md remain (support email, Google OAuth publish, Web Push, delivery states, ...). Web Push needs a push-token API shared with mobile.
