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
count, history, and saved-search counts grow. EPICs 02–15 (Monetization
through in-app onboarding checklist) are code-complete in
`claude_routine`; live deploy is gated on Master ops (hosting, domain,
Stripe live keys, encryption secrets, Google OAuth credentials, demo
video URL).

## Primary Objective

**Ship EPIC-16 shared-inbox features that make the Team plan feel
earned.** Team $29/mo currently upgrades on mailbox-cap + scaffolded
invite flow, neither of which is *experienced* daily inside the
product — a freshly-invited teammate logs in and finds the same
single-user inbox the inviter already had. The Team plan needs visible
collaboration surface inside a thread: an internal note (separate from
the email reply) that other team members will see, attribution + time
on every note, and a clear paid-feature boundary so Free/Personal users
encounter a "this is Team-only — upgrade to add notes" CTA on the same
spot. That moment is the upgrade event. This Objective ends when any
Team-plan thread owner can pin private notes onto a thread, those
notes are attributed and visible to all members of their team viewing
that thread, Free/Personal users see the upgrade CTA in the same
spot, and `/admin/revenue` shows the Free→Team conversion rate
trending up versus the pre-EPIC-16 baseline.

## Milestones

1. **Internal team notes on a thread — owner-side, Team-gated.** New
   `thread_notes` table (V25) keyed by `thread_id` + `team_id` +
   `author_user_id`. `ThreadNote` entity, `ThreadNoteRepository`,
   `ThreadNoteService` with `canAccessNotes(user)` (TEAM/ENTERPRISE
   on entitling status), `notesFor(thread, viewer)` (owner-only for
   M1), and `post(thread, author, body)` returning POSTED / GATED /
   BLANK / TOO_LONG (4 KB cap). `ThreadController.viewConversation`
   loads notes + `canPostNote` + a `notesUpgradeNudge` for non-Team
   plans; new `POST /threads/{id}/note` handles posting. The
   `conversation.html` template renders a sticky-note panel between
   messages and the reply form: existing notes with author + time,
   a textarea + submit for Team users, an upgrade-to-Team CTA for
   Free/Personal.
2. **Thread visibility for invited teammates — notes work cross-user.**
   Lift thread access from `findByIdAndOwner` to a team-scoped check
   so a team member who follows a shared link to a teammate's thread
   sees the same conversation view + notes panel. Notes posted by
   either party show on both sides with proper attribution.
3. **@mention notifications inside a note.** Parse `@email` /
   `@name` tokens against the team membership; matched mentions get
   a transactional email pointing back at `/threads/{id}#note-{id}`.
   The "@" trigger in the textarea opens a name picker bound to the
   team members.
4. **Operator dashboard card: Team-plan conversion lift.** Add a
   "Team-plan adoption — last 30 days" card on `/admin/revenue`
   showing notes-posted, thread-share-clicks, mentions-sent for the
   cohort plus a Free→Team vs Personal→Team split so the lift over
   the pre-EPIC-16 baseline is legible at a glance.

## Done means

A Team-plan thread owner sees an "Internal notes" panel under the
messages, can pin a private note, and any teammate who follows a
shared thread link sees the same panel and the same notes attributed
to the right person. Free/Personal users see an "Upgrade to Team to
add internal notes" CTA in the same spot. The post-EPIC-16
Free→Team conversion rate on `/admin/revenue` is visibly higher than
the pre-EPIC-16 baseline.
