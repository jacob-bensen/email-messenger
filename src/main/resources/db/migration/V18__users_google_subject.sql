-- EPIC-13 Milestone 3: link Google accounts to existing email-password rows.
--
-- Stores the OIDC `sub` claim from Google so the link survives email changes
-- (Google's `sub` is the stable per-account id; `email` can change). On the
-- next "Continue with Google" we prefer the subject lookup over email match,
-- so a user who renames their Google address still lands on the same MailIM
-- account. Nullable because:
--   * pre-EPIC-13 rows exist before the column did, and email-password users
--     who never click "Continue with Google" never acquire a subject;
--   * an email-only row that does click is silently linked on first OAuth
--     login by writing the subject onto the matched row.
-- Unique constraint guarantees one Google account maps to one MailIM row;
-- the index doubles as the lookup path for the subject-first match.
ALTER TABLE users ADD COLUMN google_subject VARCHAR(255);

CREATE UNIQUE INDEX uq_users_google_subject ON users (google_subject);
