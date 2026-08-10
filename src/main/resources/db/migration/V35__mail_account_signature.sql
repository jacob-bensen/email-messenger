-- Optional plain-text signature appended to messages sent from this account.
ALTER TABLE mail_accounts ADD COLUMN signature VARCHAR(2000);
