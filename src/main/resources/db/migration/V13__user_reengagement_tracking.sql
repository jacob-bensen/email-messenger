-- EPIC-08 Milestone 4: re-engagement email after 7 days of inactivity.
--
-- `last_login_at` is stamped by an AuthenticationSuccessEvent listener on
-- every form/remember-me/programmatic login. `last_inbox_visit_at` is
-- stamped on every `GET /threads`. Together they define "effective last
-- activity" — once that gap exceeds the inactivity window, the user is a
-- re-engagement candidate.
--
-- `last_reengagement_sent_at` guarantees idempotency per inactivity window:
-- the scheduler only sends if the most recent activity timestamp is more
-- recent than the most recent send, so a user who reads (or signs in) and
-- then re-disappears earns a second nudge — but a user who stays silent
-- doesn't get re-mailed every cycle.
--
-- All three columns are nullable; existing rows backfill as NULL and the
-- service code coalesces against `created_at` so pre-existing users are
-- treated as "last active when they registered" until they next show up.
ALTER TABLE users ADD COLUMN last_login_at TIMESTAMP;
ALTER TABLE users ADD COLUMN last_inbox_visit_at TIMESTAMP;
ALTER TABLE users ADD COLUMN last_reengagement_sent_at TIMESTAMP;
