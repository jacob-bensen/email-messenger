-- Captures where a registration originated (utm_source from landing/pricing
-- CTAs). Nullable: direct signups have no source. Length 64 covers typical
-- UTM values (producthunt, twitter, hn, referral-jdoe) without bloating the
-- row. Read back per-cohort to answer "which channels are converting".
ALTER TABLE users ADD COLUMN acquisition_source VARCHAR(64);

CREATE INDEX idx_users_acquisition_source ON users (acquisition_source);
