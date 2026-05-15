-- Multi-tenant from day one: every imported thread belongs to one User.
-- Greenfield (no production data yet), so add the FK column NOT NULL directly.

ALTER TABLE email_threads
    ADD COLUMN owner_id BIGINT NOT NULL;

ALTER TABLE email_threads
    ADD CONSTRAINT fk_email_threads_owner
    FOREIGN KEY (owner_id) REFERENCES users (id);

CREATE INDEX idx_email_threads_owner_id ON email_threads (owner_id);

-- A given Message-ID may legitimately appear in two different users' inboxes
-- (e.g. both were on TO/CC of the same email), so root_message_id is unique
-- only within an owner's mailbox.
ALTER TABLE email_threads
    DROP CONSTRAINT uq_email_threads_root_message_id;

ALTER TABLE email_threads
    ADD CONSTRAINT uq_email_threads_owner_root_msg
    UNIQUE (owner_id, root_message_id);

-- Same reasoning for message_id_header on messages: drop global uniqueness;
-- idempotency is now scoped per-owner inside EmailImportService.
ALTER TABLE messages
    DROP CONSTRAINT uq_messages_header;

CREATE INDEX idx_messages_message_id_header ON messages (message_id_header);
