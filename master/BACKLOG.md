# BACKLOG

Up to 10 items, each advancing a PLAN.md milestone. Pick from the top.

## EPIC-09 Account self-serve

1. **Login throttling + auth audit log.**
   Brute-force lockout on `/login` after N failures per email/IP, and an
   `auth_events` table backing a "recent account activity" panel on
   `/account`.
   _Milestone 4._
