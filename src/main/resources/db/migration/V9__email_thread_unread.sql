-- Per-thread read state powers the inbox "Unread" filter chip. Threads
-- start unread on import; viewing a thread marks it read; a new message
-- arriving in an existing thread re-flags it unread.
ALTER TABLE email_threads
    ADD COLUMN unread BOOLEAN NOT NULL DEFAULT TRUE;

CREATE INDEX idx_email_threads_owner_unread
    ON email_threads (owner_id, unread);
