# PLAN

## What this is

**email-messenger** (product name: MailIM) is a Spring Boot web app that
imports email threads over IMAP and renders them as a modern IM-style chat
view — bubbles, avatars, day separators, dark mode — instead of the
nested-quote wall most mail clients show. It is positioned as a freemium
SaaS: Free (1 mailbox, 30-day history), Personal $9/mo (3 mailboxes,
unlimited history), Team $29/mo (10 mailboxes, sharing), Enterprise $99/mo
(SSO, audit). Annual billing offers 2 months free. Money comes from
recurring subscriptions, with natural Free → Personal → Team upgrades as
mailbox count and history needs grow. EPIC-02 Monetization Plumbing and
EPIC-03 Mailbox Onboarding are both code-complete; live-deploy verification
remains blocked on Master ops (hosting, domain, Stripe live keys).

## Primary Objective

**Ship EPIC-04 Deployability.** Both monetization and mailbox onboarding
are code-complete in `claude_routine`, but no paying user has ever
reached the app because there is no built artifact, no compose stack, no
CI, and no documented path from `git push` to a running URL. Until that
exists, every shipped feature is theoretical revenue. This Objective ends
when Master can run a single command on a vanilla VPS (or push to a Render
/ Railway / Fly app) and have the production stack — Postgres, Flyway
migrations, the Spring Boot app — come up healthy and serve `/pricing`
over the open internet.

## Milestones

1. ~~**Container build + local compose stack.** Multi-stage Dockerfile
   producing a slim JRE image, `docker-compose.yml` wiring the app to
   `postgres:16-alpine` with a healthcheck, env-var passthrough for every
   `application.yml prod` placeholder, non-root runtime user, and a
   `.dockerignore` so the build context stays under a few MB. README's
   "Run locally" path uses the compose stack.~~ Shipped 2026-05-27.
2. ~~**GitHub Actions CI.** `.github/workflows/ci.yml` builds the project
   on push / PR with Maven dep caching, runs `./mvnw verify`, and
   builds (but does not push) the Docker image so a broken build is
   caught before the first deploy.~~ Shipped 2026-05-28.
3. **Integration tests with Testcontainers + GreenMail.** A real
   end-to-end happy-path test that boots Postgres via Testcontainers,
   stands up GreenMail as a fake IMAP server, walks the
   `connect mailbox → poll → see thread in /threads` flow against the
   actual `EmailImportService` and `MailboxPollingService`. Gates CI so
   future regressions on the revenue critical path fail fast.
4. **Production smoke deploy.** Build artifact pushed to a public
   registry (GHCR), one-page `DEPLOY.md` walks Master from `git pull` to
   `https://mailaim.app/pricing` returning 200; HTTPS in front of the
   container, env vars set from secrets, Flyway runs on first boot.

## Done means

Master runs `docker compose up --build` against this repo on a fresh host
and `curl https://<host>/pricing` returns 200 with the rendered pricing
page; the Spring Boot logs show Flyway applied V1..V7 against a real
Postgres; CI on `claude_routine` is green; and the integration test
boots Testcontainers + GreenMail end-to-end without external network.
