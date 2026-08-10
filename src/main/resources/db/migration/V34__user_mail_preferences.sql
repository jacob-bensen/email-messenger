-- Per-user mail preferences surfaced on /mailboxes.
-- sync_frequency_minutes: user-chosen poll cadence; NULL means the plan
-- default. The effective interval is floored by the plan tier at read time
-- (PollingPolicy), so a downgrade never leaves a too-fast cadence behind.
-- default_mail_account_id: the account new messages are sent from when the
-- current view isn't scoped to one mailbox; NULL keeps the automatic choice.
ALTER TABLE users ADD COLUMN sync_frequency_minutes INT;
ALTER TABLE users ADD COLUMN default_mail_account_id BIGINT REFERENCES mail_accounts(id) ON DELETE SET NULL;
