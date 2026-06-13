-- Incremental-sync cursor: the highest IMAP UID we have imported for this
-- mailbox's INBOX. NULL on first poll means "establish a baseline from
-- the current INBOX without backfilling" (the initial connect-time sync
-- already pulled the most recent messages).
ALTER TABLE mail_accounts ADD COLUMN last_seen_uid BIGINT;
