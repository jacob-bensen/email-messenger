# INTERNAL_TODO — email-messenger

Known gaps, contract mismatches, deferred work. Ordered by severity
(contract breakage > logic drift > missing tests > style). Consume from
and append to this every routine run.

## Contract breakage (highest severity)

- [ ] **No `/api/v1/*` REST surface exists.** The mobile client cannot do
  anything the browser does. `docs/API.md` is entirely DESIGN except for
  three endpoints (`POST /chats/refresh`, `GET /chats/events`,
  `POST /billing/webhook`). Implementing this is now the primary path to
  a working mobile client.
- [ ] **Auth scheme for mobile not chosen.** Session cookies vs. bearer
  tokens is an owner decision that gates every other mobile endpoint.
  Recommendation in `docs/API.md`: bearer tokens.

## Logic drift / duplication risk

- [ ] Any new JSON controller MUST call the same `@Service` beans the
  Thymeleaf controllers call. Add a static analysis rule / architectural
  test if the API grows beyond a handful of endpoints.

## Missing tests

- [ ] Once `/api/v1/*` lands, add `@WebMvcTest` slices for each JSON
  controller and integration tests through `MockMvc` with JSON body
  assertions, plus contract-shape tests that reject unknown / renamed
  fields.

## Style / hygiene

- [ ] `master/` directory at the repo root contains PLAN.md / BACKLOG.md /
  MASTER_ACTIONS.md / CHANGELOG.md from a prior "master" routine. The
  new `docs/` and root-level `CHANGELOG.md`/`INTERNAL_TODO.md` overlap in
  purpose. Decide whether to consolidate; for now the two coexist.
- [ ] `Dockerfile` and `docker-compose.yml` are still in the tree even
  though CLAUDE.md marks Docker as removed (2026-06-11). Consider
  actually deleting the files to match the roadmap.

## Follow-ups from backend roadmap that would benefit the mobile client

(pulled from `CLAUDE.md` — cross-listed for visibility)

- [ ] Web Push (part of "Live delivery, part 2") — the same VAPID + push-
  subscription infra can serve APNs/FCM for the mobile app.
- [ ] Delivery states for outbound bubbles (Sending/Sent/Delivered/Not
  Delivered) — the mobile app will render these too.
- [ ] Attachment download endpoint (`blob_ref` currently always null) —
  required before mobile can render 📎 chips as anything useful.
