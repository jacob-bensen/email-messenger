# INTERNAL_TODO.md

Cross-repo tech debt, deferred work, and known gaps between this backend and
the ConexusMail mobile client. Ordered by severity: contract breakage first,
then logic drift, then missing tests, then style.

## Severity 1 — contract / auth (blocks mobile from doing anything real)

- **No authenticated REST endpoints for the mobile client yet.** The website
  uses session cookies + CSRF; the mobile client needs a stateless bearer
  flow. Define `POST /api/v1/auth/login`, `POST /api/v1/auth/refresh`, and
  the authorization filter for `/api/v1/**` (excluding the health endpoint).
  This is the next feature run's likely pick.
- **Google OAuth on mobile.** Once the bearer flow exists, define how a
  mobile Google sign-in exchanges its ID token for a session token here.

## Severity 2 — logic drift risk

- **IM transform mirroring.** The backend's `IMTransformService` /
  `EmailBodyCleaner` decide how quoted replies are stripped and bubbles are
  grouped. The mobile client MUST NOT re-implement these rules — the server
  is authoritative and the mobile client just renders what it receives.
  When the first conversation endpoint lands, decide whether the server
  returns pre-grouped `BubbleRun`s or raw `Message`s (the recommendation is
  pre-grouped, matching what the Thymeleaf view already gets from
  `ChatService`).
- **Plan limits.** `PlanLimitKind` gates several features on the web app.
  Any mobile endpoint that would exercise them needs to reject with a
  documented `PLAN_LIMIT` error code the client can render.

## Severity 3 — missing tests

- The web-side chat flow has good coverage; the new `api/` package needs a
  slice test per endpoint as it grows.

## Severity 4 — housekeeping

- `docs/API.md` currently documents only `/api/v1/health`. Grow the endpoint
  list as we add them and keep the change log at the bottom current.
- The `api` package is brand-new; expect the layout to evolve. Whatever it
  becomes, controllers stay thin and delegate to existing services.
