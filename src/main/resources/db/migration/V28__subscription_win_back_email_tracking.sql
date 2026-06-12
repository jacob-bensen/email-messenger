-- EPIC-18 Milestone 1: operator-initiated win-back outreach.
--
-- The /admin/retention queue lists canceled paid subscribers with their
-- plan, cadence, source, and reason. A "Send win-back" button on each row
-- POSTs to /admin/retention/win-back and the WinBackOutreachService
-- composes a reason-aware templated email through the existing
-- JavaMailSender path (same DigestEmailPreference opt-out gate as the
-- trial-end conversion email so a single unsubscribe still kills every
-- automated outbound channel).
--
-- `last_win_back_email_sent_at` is the one-shot stamp the service writes
-- after a successful send: it suppresses the action button on re-render
-- (replaced with "Sent X ago") and prevents the operator from accidentally
-- double-emailing the same customer twice on a single dashboard refresh.
-- Tracked on `subscriptions` rather than `users` because the win-back is
-- tied to the specific canceled subscription row — a future re-trial /
-- re-subscribe creates a new row, which the operator can win-back again.
ALTER TABLE subscriptions
    ADD COLUMN last_win_back_email_sent_at TIMESTAMP;
