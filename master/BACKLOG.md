# BACKLOG

Up to 10 items, each advancing a PLAN.md milestone. Pick from the top.

## EPIC-18 M3 — Recovered badge + auto-suppress in the at-risk queue
When a canceled sub flips `canceled → active` after a win-back stamp,
render the row with a "Recovered" badge and count it in the M2 card.
Closes the loop on the same page.

## EPIC-18 M4 — Win-back queue + recovered tally in operator weekly digest
Extend `AdminWeeklyDigestService` so Monday's email surfaces
"un-emailed paid cancels this week" + "recovered after win-back (+$X MRR)".
Pushes the action into the operator's inbox.
