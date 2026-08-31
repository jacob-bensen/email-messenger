# Changelog — email-messenger

Dated entries per full-stack-sync run. Newest on top.

## 2026-08-31

- Bootstrapped the doc system required by the full-stack sync routine:
  `docs/BACKEND.md`, `docs/API.md`, `docs/WEB.md`, `CHANGELOG.md`,
  `INTERNAL_TODO.md`.
- Added the first JSON API endpoint intended for the mobile client:
  `GET /api/v1/meta` (`com.emailmessenger.api.MetaController`), which
  returns server discovery data (API version, server version, plans,
  feature flags including whether Google OAuth is configured). Wired
  into `SecurityConfig` as permit-all and exempted the entire
  `/api/v1/**` prefix from CSRF so mobile POSTs can land later without
  a config churn.
- Enabled the Spring Boot Maven plugin's `build-info` goal so
  `META-INF/build-info.properties` is emitted and `BuildProperties`
  auto-configures, feeding `/api/v1/meta`'s `serverVersion` field.
- Sibling repo (`conexus-mail-mobile`, branch
  `claude/hopeful-einstein-8v38fs`): scaffolded the Flutter project so
  the app has somewhere to grow — `pubspec.yaml`, `lib/main.dart`
  (splash screen that calls `/api/v1/meta`), `lib/domain/` (pure Dart
  domain mirror), `lib/net/` (HTTP client), tests for the domain layer,
  and its own doc system (`docs/DESIGN.md`, `docs/MOBILE.md`,
  `CLAUDE.md`, `CHANGELOG.md`, `INTERNAL_TODO.md`). Contract match with
  `docs/API.md` verified by hand — no Flutter toolchain available in
  this environment to run `flutter analyze` / `flutter test`; see the
  sibling repo's CHANGELOG for the honest gate status.
