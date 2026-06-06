-- Saved-search rail rows now show match count + "new since last visit" badge.
-- last_viewed_at is set when the user clicks the rail link (which carries
-- ?s=<id>); NULL means the saved search has never been opened, in which case
-- the count service treats created_at as the lower bound so the badge
-- doesn't surface every thread in history as "new".
ALTER TABLE saved_searches
    ADD COLUMN last_viewed_at TIMESTAMP;
