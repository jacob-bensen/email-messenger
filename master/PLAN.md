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
count, history, and saved-search counts grow. EPICs 02–14 (Monetization
through activation drip) are code-complete in `claude_routine`; live
deploy is gated on Master ops (hosting, domain, Stripe live keys,
encryption secrets, Google OAuth credentials, demo video URL).

## Primary Objective

**Ship EPIC-15 in-app onboarding checklist — drive Free→Personal→Team
upgrades on natural activation.** EPIC-14's drip catches cold signups in
their own inbox before they bounce; EPIC-15 catches the next leak: the
signup who *did* connect a mailbox but never makes it deep enough into
the product to feel why Personal (unlimited threads, unlimited saved
searches) or Team (sharing) is worth $9/$29 a month. The activation
arc — connect a mailbox → see real threads flowing → save a search you
care about → invite a teammate — is exactly the arc that turns Free
into paid. A visible progress card on `/threads` makes that arc legible,
ties each step to a one-click CTA, and naturally points the last two
steps at the saved-search and team-sharing features that gate paid
plans. This Objective ends when a brand-new signup who lands on
`/threads` sees a 4-step progress card naming each step, the card
disappears once all four are done, and the post-EPIC-15 7-day
"connected mailbox → saved a search → first invite" conversion rate
is visibly higher than the pre-EPIC-15 baseline on `/admin/revenue`.

## Milestones

1. **Always-visible 3-step progress card on `/threads`.** [shipped
   2026-06-10] Replaces the empty-state-only welcome card with a
   compact progress strip that sits above the thread list whenever the
   checklist is incomplete. Steps: (1) Connect a mailbox,
   (2) Import 10 threads (live count + "N to go" copy until 10 is
   hit), (3) Save a search. `OnboardingChecklist` carries
   `mailboxConnected`, `threadCount`, `savedSearchSaved` plus derived
   `completedSteps()`, `percentComplete()`, `nextStepCtaUrl/Label`,
   `isComplete()`. `OnboardingService` now also reads
   `SavedSearchRepository.countByOwner` and the raw thread count from
   `EmailThreadRepository.countByOwner`. `ThreadController` always
   exposes the checklist on `/threads` (including search/filter
   views) until `isComplete()` flips, at which point the attribute is
   omitted and the strip silently disappears. The legacy big
   welcome-state card still renders on a totally empty inbox, with
   the same three steps. 5 service tests + 3 controller tests cover
   per-step transitions, the 10-thread threshold, the
   suppress-when-complete contract, and the search/filter-active
   variants.
2. **Step 4 — invite a teammate.** [shipped 2026-06-10] Lightweight
   team-invite flow scaffolded end-to-end: V24 adds `teams`,
   `team_members`, `team_invites`; `TeamService.findOrCreateOwnedTeam`
   lazy-creates the inviter's team on first invite and stamps an
   `OWNER` member row; `TeamInviteService.invite` mints a SHA-256
   hashed 7-day-TTL token, rejects self-invites / blanks / duplicate
   pending addresses, and emails the invitee a `/team/invite/accept`
   link; `TeamInviteService.acceptInvite` validates the token, refuses
   to redeem when the signed-in email doesn't match the invitee, adds
   a `MEMBER` row, and consumes the token. `TeamInviteController`
   exposes `GET/POST /team/invite` (form + outstanding list) and
   `GET/POST /team/invite/accept` (ready / emailMismatch / invalid
   branches). `OnboardingChecklist` grows `teammateInvited` (true when
   the user has any non-revoked invite), `TOTAL_STEPS = 4`, with a 4th
   CTA `/team/invite` "Invite a teammate". `OnboardingService` reads
   `TeamInviteRepository.countNonRevokedByInviter`. `threads.html`
   renders step 4 in both the progress strip and the empty-inbox card.
   M3 (per-step upgrade nudges that monetize) will swap the post-step
   sub-copy for a Team plan CTA on Free/Personal.
3. **Per-step upgrade nudges that monetize the steps directly.** When
   the user completes step 2 (10 threads imported) on a Free plan
   trending toward the 500-thread cap, the progress card swaps the
   step-3 sub-copy for a Personal upgrade prompt. When the user
   completes step 3 (saved a search) on Free, the card reminds them
   Free caps at 1 saved search and offers Personal. When they hit
   step 4 (invited a teammate) the card surfaces the Team plan. Each
   nudge ties to existing `UpgradeModal` infrastructure.
4. **Operator dashboard card for onboarding-step conversion.** Add
   "Onboarding funnel — last 30 days" to `/admin/revenue` showing
   signups → mailbox-connected → 10-threads → saved-search →
   invite-sent → paid, anchored on user/subscription timestamps so the
   operator can tell which step is the next monetization leak.

## Done means

A brand-new signup who lands on `/threads` sees a 4-step progress card
("Connect a mailbox → Import 10 threads → Save a search → Invite a
teammate") with each step linked to a one-click action. The card
updates in real time as each step is completed and disappears once
all four are done. The post-EPIC-15 7-day onboarding-completion rate
and the Free→Personal/Team conversion rate among onboarded users are
visibly higher on `/admin/revenue` than the pre-EPIC-15 baseline.
