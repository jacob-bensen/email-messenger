# Changelog

Dated entries, newest first. The mobile client keeps its own log in
`conexus-mail-mobile/CHANGELOG.md`.

## 2026-11-15 — Mobile API foundation + sign-in (M1)

- Bootstrapped the cross-repo docs: `docs/API.md` (canonical contract),
  `docs/BACKEND.md`, `docs/WEB.md`, this changelog and `INTERNAL_TODO.md`.
  The mobile repo already referenced them from its 2026-11-14 bootstrap,
  but they had never been created here.
- New `/api/**` stateless security chain (`api.ApiSecurityConfig`) with
  opaque bearer tokens (`ApiTokenService`, V36 `api_tokens`, SHA-256 only,
  30-day expiry, one per device).
- Endpoints: `POST /api/v1/auth/login`, `POST /api/v1/auth/logout`,
  `GET /api/v1/me`; JSON error envelope (`API.md` §3). Login reuses the
  website's `AuthenticationManager`, audit listeners and lockout throttle
  so both sign-in paths share one set of rules.
- `ConversationKeyServiceTest` now asserts the golden vectors shared with
  the mobile mirror.
- `ClientIp` made public for the API controller.
