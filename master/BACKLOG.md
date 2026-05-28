# BACKLOG

Up to 10 items, each advancing a PLAN.md milestone. Pick from the top.

## End-to-end Testcontainers + GreenMail integration test
Boot Postgres via Testcontainers + GreenMail as fake IMAP, walk connect →
poll → thread visible, gating CI on the revenue critical path.
_Milestone 3 — Integration tests with Testcontainers + GreenMail._

## DEPLOY.md + GHCR image publish
One-page deploy guide (env vars, HTTPS terminator, run command) plus CI
job that pushes the image to `ghcr.io/...` on `claude_routine` push.
_Milestone 4 — Production smoke deploy._
