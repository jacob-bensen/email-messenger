-- EPIC-14 Milestone 4: trial-end conversion email when paid trials expire.
--
-- A trialing subscription whose trial_ends_at lands inside the next ~24h
-- without the user having upgraded (status still 'trialing') is the
-- final revenue-critical leak in the funnel — past this point Stripe
-- either rebills the saved card silently or the user lapses to
-- canceled. A one-shot conversion email at T-1 day gives the visitor a
-- prompt to pick a plan (or downgrade to Free) before the trial clock
-- runs out.
--
-- `last_trial_end_email_sent_at` is the one-shot stamp the trial-end
-- service writes after sending, gating re-sends so a multi-day window
-- (or a scheduler that catches up after downtime) can't fire twice.
-- Tracked on `subscriptions` rather than `users` because the trial
-- lifecycle belongs to the subscription row, and a future re-trial on
-- a separate subscription should re-open the cohort.
ALTER TABLE subscriptions
    ADD COLUMN last_trial_end_email_sent_at TIMESTAMP;
