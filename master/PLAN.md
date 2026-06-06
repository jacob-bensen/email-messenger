# PLAN

## What this is

**email-messenger** (product name: MailIM) is a Spring Boot web app that
imports email threads over IMAP and renders them as a modern IM-style chat
view — bubbles, avatars, day separators, dark mode — instead of the
nested-quote wall most mail clients show. It is positioned as a freemium
SaaS: Free (1 mailbox, 500 threads, 1 saved search), Personal $9/mo
(3 mailboxes, unlimited history, unlimited saved searches),
Team $29/mo (10 mailboxes, sharing), Enterprise $99/mo (SSO, audit).
Annual billing offers 2 months free. Money comes from recurring
subscriptions, with natural Free → Personal → Team upgrades as mailbox
count, history, and saved-search counts grow. EPICs 02–07 (Monetization,
Mailbox Onboarding, Deployability, Acquisition, Launch readiness, Inbox
Search) are code-complete in `claude_routine`; live deploy is gated on
Master ops (hosting, domain, Stripe live keys, encryption secrets, demo
video URL).

## Primary Objective

**Ship EPIC-08 Saved Searches & Reactivation.** Inbox search and chip
filters let a paid user drill to "everything from Ada in the last 30
days," but that filter combination is rebuilt by hand on every visit —
there is nothing in the product that rewards keeping the user pinned to
a recurring view. Saved searches turn the search infrastructure into a
retention loop: paid users persist complex queries, hit them from the
rail on every visit, and (in later milestones) get email digests of new
matches without re-opening the app. They are also a second concrete Free
vs Personal gap — Free caps at one saved search, Personal+ is unlimited
— giving an upgrade trigger that fires for engaged users specifically.
This Objective ends when a paid user can save and revisit named
"smart folders" of their inbox, gets a weekly digest of new matches, and
re-engagement emails recover dormant accounts before they cancel.

## Milestones

1. **Save the current search; rail surface.** "Save this search" inline
   form above the result list captures `q`/`from`/`since`/`unread`/`attachments`
   under a user-chosen name; saved searches render as a "Saved searches"
   block at the top of the left rail with an active-state highlight and a
   per-row delete. Free=1 saved search, Personal+=unlimited; the second
   Free save lands the standard upgrade modal via a new
   `PlanLimitKind.SAVED_SEARCH_COUNT`.
   _(Shipped 2026-06-06 — V10 migration adds `saved_searches` table with
   `(owner_id, name)` unique constraint; new `SavedSearch` entity,
   `SavedSearchRepository`, `SavedSearchService` (cap-check after
   duplicate-name check so a Free dup retry doesn't get the upgrade
   modal); new `SavedSearchController` (`POST /searches`,
   `POST /searches/{id}/delete`, CSRF-armed, redirects back preserving
   the active query string); `PlanLimits` gains a third dimension
   (`savedSearches`: Free=1, paid=UNLIMITED), `PlanLimitService.enforceCanCreateSavedSearch`
   throws `PlanLimitExceededException` with the new kind so the existing
   `GlobalExceptionHandler` redirect-to-`/threads`-with-`upgradeModal`
   flash path lights up unchanged. `threads.html` upgrade modal gains a
   SAVED_SEARCH_COUNT body branch ("Save more searches on Personal");
   rail visibility widens so users with saved searches but no senders
   still see the rail. 31 new tests; 358 total pass.)_
2. **Saved-search match counts (live).** Each rail row shows the current
   matching thread count + an unread-since-last-visit badge so the rail
   becomes a "what's new" surface. Counts come from the existing
   `EmailThreadRepository` search methods reused with the saved-search
   params, with a per-saved-search `last_viewed_at` column tracking the
   unread delta.
   _(Shipped 2026-06-06 — V11 adds nullable `last_viewed_at` to
   `saved_searches`; new `SavedSearchCountService` reuses
   `findByOwnerFiltered` / `findByOwnerAndSender` / `search` /
   `searchIncludingBody` with `PageRequest.of(0, 1).getTotalElements()`
   for matchCount and re-runs the same query with `since = max(originalSince,
   lastViewedAt ?? createdAt)` for newCount, honouring the same Free vs
   paid body-search gating the inbox uses. `SavedSearchView` carries
   matchCount/newCount + `hasNew()`; rail link gains `?s={id}`,
   `SavedSearchService.markViewed` stamps `last_viewed_at` before counts
   render so the just-clicked badge clears. 7 new tests; 365 total pass.)_
3. **Weekly digest email of new matches (Personal+).** `@Scheduled` job
   emails paid users a digest of new threads matching each saved search
   over the past 7 days. Per-user opt-out token in the footer; a
   `digest_email_preferences` row stores opt-out state. Free users are
   intentionally excluded — that's the upgrade hook.
   _(Shipped 2026-06-06 — V12 adds `digest_email_preferences` with a
   unique `opt_out_token` and nullable `last_sent_at`; new
   `DigestEmailPreference` entity + repository, `WeeklyDigestService` in
   `com.emailmessenger.digest` iterates `SavedSearchRepository.findDistinctOwners`,
   skips Free / opted-out / no-new-match users, lazily provisions the
   pref row with a UUID token, and for each remaining user reuses the
   same plan-aware `findByOwnerFiltered` / `findByOwnerAndSender` /
   `searchIncludingBody` / `search` repository methods that power the
   inbox — capped at 5 threads per saved-search section and floored at
   the 7-day window — to assemble a plain-text digest delivered via the
   existing `JavaMailSender`. `WeeklyDigestScheduler` runs cron
   `0 0 14 ? * MON` UTC gated by `digest.enabled=true` (default off in
   dev/tests so nothing accidentally mails). Unauthenticated
   `GET /digest/opt-out?token=…` flips the row and renders a confirmation
   (or a generic "invalid link" page for missing/unknown tokens to avoid
   leaking which tokens existed); `SecurityConfig` permits the path.
   12 new tests; 377 total pass.)_
4. **Re-engagement email after 7 days of inactivity.** Cross-references
   the user's last `/threads` GET against `last_login_at` (new `users`
   column) and fires a single "you have N unread threads waiting"
   email; idempotent per inactivity window so a user who reads then
   re-disappears gets a second nudge.

## Done means

A paid user with 200+ imported threads can name and pin three or four
recurring filter combinations, hit them from the rail with one click, and
receive a weekly email summarising new matches in each — and a Free user
trying to save their second pinned view sees the upgrade modal with copy
that names the Personal benefit. Dormant paid accounts get exactly one
reactivation email per inactivity window pointing at unread inbox content.
