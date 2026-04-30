-- Pre-launch viral loop: each waitlist entry has a unique referral token.
-- When a new signup arrives with ?ref={token}, the matching entry's
-- referrals_count is incremented (giving the referrer queue-skip credit).

ALTER TABLE waitlist_entries
    ADD COLUMN referral_token  VARCHAR(36);

ALTER TABLE waitlist_entries
    ADD COLUMN referrals_count INT NOT NULL DEFAULT 0;

ALTER TABLE waitlist_entries
    ADD CONSTRAINT uq_waitlist_entries_referral_token UNIQUE (referral_token);
