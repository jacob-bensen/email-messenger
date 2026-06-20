-- Groups threads into texting-style conversations by the set of people
-- involved. Nullable on add; backfilled in-app by ConversationKeyBackfill so
-- the hashing logic lives in one place. New threads get the key at import time.
ALTER TABLE email_threads ADD COLUMN conversation_key VARCHAR(64);

CREATE INDEX idx_email_threads_conversation_key
    ON email_threads (owner_id, conversation_key);
