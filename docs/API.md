# API.md — canonical API contract

**Status: no REST/JSON API exists yet.** The server today exposes only server-rendered HTML routes (see `docs/WEB.md`), session-cookie auth with CSRF, one SSE stream (`GET /chats/events`) and a Stripe webhook. The mobile app therefore has nothing to call. This file is the place where the contract will be defined *before* any implementation (per the sync routine).

## Conventions (proposed, TBD — not implemented)
- Base path `/api/v1`, JSON, auth = TBD (bearer token issued by a new `/api/v1/auth/login`; session+CSRF is unsuitable for mobile).
- Errors: `{ "error": "<code>", "message": "..." }` with standard HTTP status; plan-limit breaches map from `PlanLimitExceededException` (HTTP 402/403 TBD).

## Endpoints
None implemented. Planned first slice (see mobile `docs/DESIGN.md` §ROADMAP): auth, conversation list, conversation messages, send reply.
