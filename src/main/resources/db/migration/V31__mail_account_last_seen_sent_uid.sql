-- Separate IMAP UID cursor for the mailbox's Sent folder, so the poller can
-- pull sent messages (imported as outbound) independently of the INBOX cursor.
ALTER TABLE mail_accounts
    ADD COLUMN last_seen_sent_uid BIGINT;
