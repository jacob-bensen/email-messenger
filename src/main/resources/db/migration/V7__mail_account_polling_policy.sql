-- Per-plan polling cadence + circuit breaker for the recurring poll loop.
-- next_poll_at NULL means "treat as immediately due" (e.g. fresh connect or
-- a row created before this migration); the scheduler stamps it forward
-- after each poll based on the owner's plan tier with +/-30s jitter.
-- consecutive_failure_count resets to 0 on every successful poll;
-- polling_suspended flips to true once the count crosses the threshold and
-- clears the next time a poll succeeds (manual Sync now or scheduled), so
-- a permanently broken mailbox stops getting hammered every interval.
ALTER TABLE mail_accounts ADD COLUMN consecutive_failure_count INT NOT NULL DEFAULT 0;
ALTER TABLE mail_accounts ADD COLUMN polling_suspended BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE mail_accounts ADD COLUMN next_poll_at TIMESTAMP;

CREATE INDEX idx_mail_accounts_next_poll_at ON mail_accounts (next_poll_at);
