-- Replies the user sends are persisted as outbound messages so they show up in
-- the conversation as chat bubbles (rendered as "you"), instead of vanishing
-- after send. Inbound (imported) messages stay false.
ALTER TABLE messages
    ADD COLUMN outbound BOOLEAN NOT NULL DEFAULT FALSE;
