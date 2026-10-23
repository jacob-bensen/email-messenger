# CHANGELOG

Entries before 2026-10-23 live in `master/CHANGELOG.md`.

## 2026-10-23
- Docs-only: bootstrapped `docs/BACKEND.md`, `docs/API.md`, `docs/WEB.md`, `INTERNAL_TODO.md`, this file, and a "Related repositories" section in `CLAUDE.md`.
- Why: first run of the full-stack sync routine. Audit found the backend exposes no REST API (only Thymeleaf + Stripe webhook + SSE) and the mobile repo is empty, so no feature could be built against a contract. Gaps recorded in `INTERNAL_TODO.md`.
