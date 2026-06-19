-- How each mailbox authenticates to IMAP. Existing rows are app-password /
-- plain-LOGIN connections, so they default to PASSWORD. Gmail OAuth
-- connections store an encrypted refresh token in password_ciphertext and
-- use XOAUTH2 instead.
ALTER TABLE mail_accounts ADD COLUMN auth_type VARCHAR(20) NOT NULL DEFAULT 'PASSWORD';
