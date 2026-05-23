# BACKLOG

Up to 10 items, each advancing a PLAN.md milestone. Pick from the top.

1. **First-mailbox onboarding wizard.**
   Replace the bare `/mailboxes/new` form with a guided connect flow
   (provider presets, app-password help links) so signup → first thread
   doesn't dead-end on a blank empty-state. Milestone 2.

2. **Manual "Sync now" trigger + sync status surfacing on /mailboxes.**
   Per-mailbox button that calls `MailboxPollingService.pollOne` on
   demand; render `lastSyncedAt` / `lastSyncError` with friendly copy.
   Milestone 3.

3. **Plan-tiered poll interval + jitter + consecutive-failure circuit
   breaker.** Free polls every 15 min, paid every 5 min; +/- 30s
   jitter; suspend an account after N consecutive IMAP failures with
   visible status. Milestone 4.
