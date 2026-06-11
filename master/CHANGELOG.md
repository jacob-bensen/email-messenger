# Changelog

## 2026-06-11
Shipped: Prior-30-day comparison column on the Team-plan adoption card so the EPIC-16 conversion lift PLAN.md "Done means" calls out is legible at a glance instead of inferred from a single absolute number. `ThreadNoteRepository` gains `countByCreatedAtBetween(start, end)` + `findCreatedBetween(start, end)` (`JOIN FETCH n.team` mirrors `findCreatedSince` so a future expansion past raw counts doesn't N+1); `PlanChangeEventRepository` gains `countDistinctUsersByTransitionBetween(fromPlan, toPlan, start, end)` so the Free→Team / Personal→Team prior-window baselines distinct-count the same way the current-window query does (`DISTINCT user.id` — churn-and-back inside the window still counts once). `TeamAdoptionMetricsService.snapshot()` now computes the prior bracket as `[now-60d, now-30d)` half-open and propagates `priorNotesPosted` + `priorFreeToTeamConversions` + `priorPersonalToTeamConversions` onto the metrics record. `TeamAdoptionMetrics` grows the three prior-window fields and four delta-helper methods (`notesPostedDeltaPercent`, `freeToTeamDeltaPercent`, `personalToTeamDeltaPercent`, `totalTeamConversionsDeltaPercent`) plus matching `*DeltaLabel()` formatters that branch into four cases the operator can read directly: `"▲ 67% vs. prior 30 days"`, `"▼ 70% vs. prior 30 days"`, `"flat vs. prior 30 days"` (both windows non-zero, delta rounds to 0), `"new vs. prior 30 days"` (prior was zero, current is non-zero — so a 100% lift over nothing doesn't render as a misleading 100% number), and `"no prior-window data"` (both windows zero — pre-launch state). The shared `deltaPercent(current, prior)` helper guards `prior <= 0` to return 0 so the divide-by-zero case can't leak; the rounding is `Math.round` half-up so it matches the share-percent helpers already on the record. `revenue.html` renders the delta label as an `.admin-kpi-sub` under "Notes posted" and as a new `.admin-funnel-delta` line under each of the three conversion buckets (Free → Team, Personal → Team, Total Team conversions). One namespaced CSS rule appended to `main.css` (`.admin-funnel-delta { font-size: 12px; color: var(--text-muted); margin-top: 2px; }`) — dark mode inherits via the existing `--text-muted` token. Tests (5 new — 727 total → 727 pass with 1 Docker-only skip; was 711 + 11 from the M4 ship that already landed = 722 before this session). `TeamAdoptionMetricsServiceTest` x5 new: `priorWindowQueriesUseTheTwoToOneTimesThirtyDayBracket` (capture both `countByCreatedAtBetween` args + both `countDistinctUsersByTransitionBetween` time args, assert `[now.minusDays(60), now.minusDays(30))`); `priorWindowCountsArePropagatedAndDeltaPercentReflectsLift` (notes 10→20 = +100%; free-to-team 4→8 = +100%; personal-to-team 2→2 = flat; totals 6→10 = +67% rounded — proves the per-bucket delta helpers AND the `totalTeamConversions` derived delta both compute correctly off the underlying prior-window fields); `deltaLabelReadsNewWhenPriorWindowWasEmpty` (current=5, prior=0 → "new vs. prior 30 days" + percent 0); `deltaLabelReadsNoPriorDataWhenBothWindowsAreZero` (fresh-deploy / pre-launch state → "no prior-window data" on every column so the empty card is informative instead of looking like a flat-line); `deltaLabelReadsDownWhenCurrentWindowIsBelowPrior` (free-to-team 10→3 → "▼ 70% vs. prior 30 days" + percent -70). `AdminRevenueControllerTest.revenuePageRendersTeamAdoptionCardWithEngagementAndConversionLabels` extended to assert the rendered HTML carries `prior-window data` copy + the new `admin-funnel-delta` class — guards against a future template refactor silently dropping the comparison column the EPIC-16 Done means depends on. `./mvnw test` → 727 tests pass (1 Docker-only skipped as before).
Advances: EPIC-16 shared-inbox features — Milestone 4 (Operator dashboard card for Team-plan conversion lift) — sharpens the M4 signal so the "post-EPIC-16 conversion rate visibly higher than the pre-EPIC-16 baseline" Done condition is observable on the card itself instead of requiring an off-platform comparison spreadsheet.
Master action: none

## 2026-06-11
Shipped: EPIC-16 Milestone 4 — operator "Team-plan adoption — last 30 days" card on `/admin/revenue`, closing EPIC-16 across all four milestones. The card answers the two questions the operator needs after the shared-inbox surface goes live: (a) is the Team plan actually being *used* daily (notes posted, active note authors, teams with notes, @-mentions written) and (b) is the upgrade pressure landing — split into Free→Team (cold signups closed by the collaboration surface) vs Personal→Team (existing payers climbing tiers) with a share-of-total percent on each, so the operator can read the lift direction at a glance. Flyway V26 creates `plan_change_events (id, subscription_id FK→subscriptions, user_id FK→users, from_plan, to_plan, occurred_at)` because the local `subscriptions` row only stores the *current* plan — without a transition log the Free→Team vs Personal→Team split cannot be computed at all (the data was being silently overwritten on every plan switch). New `PlanChangeEvent` entity in `com.emailmessenger.domain` (class per CLAUDE.md convention) + `PlanChangeEventRepository.countDistinctUsersByTransitionSince(fromPlan, toPlan, since)` — DISTINCT on user so a churn-and-back inside the window still counts as one conversion, matching how the rest of the cohort metrics treat repeat events. `BillingService.startCheckout` is instrumented to write one event per *accepted plan transition* — Free→Personal, Personal→Team, etc. — gated on `previousPlan != newPlan` so a retry-on-same-plan checkout (Stripe rejected card, user re-submits the same SKU) does not stutter a duplicate event into the log, and so the event count maps cleanly to "subscribers who decided to climb a tier". `previousPlan` is read off the existing subscription row before `setPlan` mutates it; `null` (first-ever subscription) is treated as FREE so the inaugural paid checkout produces a single Free→{paid-plan} event. `NoteMentionService.MENTION_TOKEN` is widened from package-private to public so the admin metrics package can reuse the same negative-lookbehind regex the mention-emailer uses — the operator number now matches what teammates actually receive in their inbox (an `@` embedded inside an email address still does not register as a mention). `ThreadNoteRepository` gains `countByCreatedAtAfter(since)` + `findCreatedSince(since)` (the latter `JOIN FETCH n.team` so the metrics service counts distinct teams without an N+1). `SubscriptionRepository.countEntitledOn(plan)` lights up the install-base anchor next to the rolling conversion bucket — currently-entitled (active + trialing) on TEAM, with a subtitle of "+ N Enterprise" when any Enterprise subs exist so the next tier up isn't invisible. New `TeamAdoptionMetrics` record + `TeamAdoptionMetricsService` in `com.emailmessenger.admin`: empty-window short-circuit skips `findCreatedSince` when `countByCreatedAtAfter==0` (no point materializing zero notes); per-note loop derives `distinctAuthors` + `distinctTeams` + total mention-token count from the regex; share-percent helpers on the record return 0 instead of NaN when total conversions is 0 (`/admin/revenue` renders correctly on a fresh-deploy instance with no Team subs yet). `AdminRevenueController` constructor gains `TeamAdoptionMetricsService` positioned after `OnboardingFunnelMetricsService` to match the new visual order; exposes `teamAdoption` as a model attribute. `revenue.html` renders a new `<section class="admin-card">` between the onboarding funnel and trial-end conversion cards, reusing the existing `.admin-kpis` + `.admin-funnel-row` markup (no new CSS, dark-mode and mobile inherited). Empty-state copy fires only when both engagement and conversions are zero — the card is otherwise legible at a single non-zero signal. Tests (10 new — 722 total → 722 pass with 1 Docker-only skip; was 691 before this ship). `TeamAdoptionMetricsServiceTest` x7 (`@ExtendWith(MockitoExtension.class)` + fixed Clock): cutoff passed to both `notes.countByCreatedAtAfter` and `planChanges.countDistinctUsersByTransitionSince(FREE, TEAM, since)` is exactly `now.minusDays(30)`; empty window skips `findCreatedSince` entirely (proven via `verify(notes, never())` — saves a fetch when there are no notes); a 4-note fixture across 2 teams and 2 authors with 3 distinct `@token` mentions surfaces `notesPosted=4, activeNoteAuthors=2, teamsWithNotes=2, mentionsWritten=3`; Free→Team and Personal→Team are bucketed under separate `Plan` argument pairs and `totalTeamConversions()` + share-percent helpers round half-up against the total; zero-conversion case returns 0% on both share helpers (no divide-by-zero); `countEntitledOn(TEAM)` + `countEntitledOn(ENTERPRISE)` are looked up separately so Enterprise counts feed the subtitle without inflating the Team headline; `countMentionTokens("ping jane@example.com but also @jane")` is 1, proving the negative-lookbehind regex is being applied. `BillingServiceTest` x3 new: first-ever checkout on Personal logs one `PlanChangeEvent(FREE→PERSONAL)`; following up with a Team checkout on the same user logs a second `PlanChangeEvent(PERSONAL→TEAM)` (the upgrade case); calling `startCheckout(user, PERSONAL, ...)` twice in a row logs exactly one event, not two — restart-on-same-plan is a no-op for the conversion log. `AdminRevenueControllerTest.revenuePageRendersTeamAdoptionCardWithEngagementAndConversionLabels` (operator load → `teamAdoption` model attr present + rendered HTML contains "Team-plan adoption", "Notes posted", "Active note authors", "@mentions written", "Free → Team", "Personal → Team"). `./mvnw test` → 722 tests pass (1 Docker-only skipped as before). EPIC-16 "Done means" is now satisfied across all four code-complete milestones (M1 owner-side notes panel, M2 cross-user team visibility, M3 @-mention picker + email, M4 operator adoption + conversion-lift card); `[PLAN-REVIEW]` added to MASTER_ACTIONS proposing EPIC-17 (outbound saved-search digests) or per-team retention dashboarding as candidate next Primary Objectives. BACKLOG.md emptied pending [PLAN-REVIEW]. PR creation skipped per the routine's commit-and-push contract.
Advances: EPIC-16 shared-inbox features — Milestone 4 (Operator dashboard card for Team-plan conversion lift). EPIC-16 now code-complete on all four milestones; next Primary Objective proposed in MASTER_ACTIONS pending [PLAN-REVIEW].
Master action: none

## 2026-06-10
Shipped: EPIC-16 Milestone 1 — internal team notes on a thread, gated to TEAM/ENTERPRISE, opening the new Primary Objective (shared-inbox features that make the Team $29/mo plan feel earned). PLAN.md rotates to EPIC-16 because EPIC-15's "Done means" is satisfied across all four code-complete milestones (`[PLAN-REVIEW]` had sat in MASTER_ACTIONS since the funnel-card ship, waiting on live-traffic conversion measurement that no agent run can produce). EPIC-16's leak hypothesis: Team's current upgrade triggers — the 10-mailbox cap and the scaffolded invite flow — are scaffolding, not a daily-experienced collaboration surface. A freshly invited teammate logs in and finds the same single-user inbox the inviter already had. Internal notes are the first surface a teammate actually *uses* the Team plan for; M1 ships the owner-side preview + the Free/Personal upgrade CTA in the same spot, M2 lifts visibility to invited teammates, M3 adds @mention emails, M4 measures the conversion lift on `/admin/revenue`. Implementation: Flyway V25 creates `thread_notes (id, thread_id FK→email_threads, team_id FK→teams, author_user_id FK→users, body TEXT, created_at)` — `team_id` is denormalized so M2's cross-user visibility query doesn't need to walk through `email_threads.owner`. New `ThreadNote` entity in `com.emailmessenger.domain` (class per CLAUDE.md convention, not a record — entities are classes). New `ThreadNoteRepository` with `findByThreadOrderByCreatedAtAsc(thread)` (asc so the most recent note is at the bottom, matching the chat-bubble timeline) + `countByThread(thread)`. New `ThreadNoteService` in `com.emailmessenger.team` (next to `TeamInviteService`) with `canAccessNotes(viewer)` → `Set.of(TEAM, ENTERPRISE).contains(planLimits.currentPlan(viewer))` (entitling-status normalization already lives inside `PlanLimitService.currentPlan`, so trial/active/past_due all unlock; canceled drops to FREE); `notesFor(thread, viewer)` returns `List.of()` unless the viewer is on a Team plan AND owns the thread (M1 owner-only — `team_id` already keyed on the thread owner's team so M2 just widens the predicate, no schema change); `post(thread, author, body)` returns one of `POSTED/GATED/BLANK/TOO_LONG` (4 000-char cap matches Stripe's typical metadata limit and prevents a runaway paste from filling the TEXT column). The author + thread-owner check is by `User.id` equality so a detached entity comparison can't false-negative. New `POST /threads/{id}/note` on `ThreadController` reuses `threadRepository.findByIdAndOwner(id, owner)` so a posted note to someone else's thread surfaces the existing 404 (same access guard the reply endpoint already enforces), then dispatches `threadNoteService.post` and adds a flash attribute (`posted/gated/blank/tooLong`) the conversation view renders. `viewConversation` injects `teamNotes`, `canPostTeamNote`, `teamNoteForm`, and `teamNotesUpgradeNudge=true` whenever `!canAccessNotes`. `conversation.html` gains an `<section class="team-notes">` between messages and the reply form: existing notes render as sticky-note bubbles with `author + time + body` (whitespace preserved via `white-space: pre-wrap`); the textarea + submit button render iff `canPostTeamNote`; the `.note-upgrade` panel with the Team-plan headline + body + `POST /billing/checkout` form (`plan=TEAM`, `billing=monthly` hidden inputs — same Stripe entry point the EPIC-15 M3 onboarding nudge already uses) renders iff `teamNotesUpgradeNudge`. The CTA label is "Upgrade to Team — $29/mo" so the click decision happens at the button itself, not a separate pricing detour. New CSS `.team-notes*` + `.note-bubble` + `.note-form` + `.note-upgrade` in `main.css` lean on a warm yellow palette (`#fffaf0` background, `#fcd34d` border, `#92400e` heading) so notes are immediately distinguishable from chat bubbles at a glance; a dedicated `@media (prefers-color-scheme: dark)` block keeps the same hierarchy on a dark background (`#2a2410` panel / `#3b3414` bubble / `#fbbf24` heading), and a `@media (max-width: 640px)` rule narrows the side padding so the panel fits mobile without horizontal scroll. Tests (12 new — 691 total → 691 pass with 1 Docker-only skip; was 678 before this ship). `ThreadNoteServiceTest` x8 — Spring-loaded `@SpringBootTest + @Transactional + @MockBean JavaMailSender/StripeCheckoutGateway/StripePortalGateway/ReplyService` per the TeamInviteService pattern: FREE / PERSONAL can't access notes; TEAM can; posting on FREE returns GATED + persists nothing; posting on TEAM persists trimmed body attributed to author + `createdAt` non-null; blank/null/whitespace body → BLANK; `MAX_BODY_LENGTH + 1` → TOO_LONG; a TEAM stranger viewing someone else's thread sees zero notes while the owner sees their own; flipping the status from `active` to `canceled` on a Team subscription drops `canAccessNotes` back to false (covers the cancel-mid-month entitlement-loss case). `ThreadControllerTest` x4 — TEAM owner GETs `/threads/{id}` → `canPostTeamNote=true` + `teamNotes` present + no upgrade nudge; FREE owner → `canPostTeamNote=false` + `teamNotesUpgradeNudge=true`; POST `/threads/{id}/note` with body → 302 to `/threads/{id}` and `threadNoteService.post` invoked with trimmed body; POST to someone else's thread → 404 + service never called. `ThreadController` constructor gains `ThreadNoteService` positioned after `UserActivityService` to match the new field order; the existing 27 controller tests pick up a default `lenient().when(threadNoteService.canAccessNotes(owner)).thenReturn(false)` + `notesFor(any, eq(owner)).thenReturn(List.of())` BeforeEach stub so they don't accidentally surface the panel. `./mvnw test` → 691 tests pass (1 Docker-only skipped as before). BACKLOG.md seeded with M2 (lift thread access to team-scope so invited teammates see notes) and M3 (@mention picker + email notification on a note) — M4 (admin Team-plan adoption card) waits until M2/M3 land so the metrics aren't measuring an empty surface. [PLAN-REVIEW] entry cleared from MASTER_ACTIONS.
Advances: EPIC-16 shared-inbox features — Milestone 1 (Internal team notes on a thread — owner-side, Team-gated). Opens EPIC-16 and closes the [PLAN-REVIEW] on EPIC-15.
Master action: none

## 2026-06-10
Shipped: EPIC-15 Milestone 4 — operator "Onboarding funnel — last 30 days" card on `/admin/revenue`, closing EPIC-15 across all four milestones. New `OnboardingFunnelMetrics` record + `OnboardingFunnelMetricsService` in `com.emailmessenger.admin` anchored on `users.created_at >= now-30d` and the in-product `OnboardingChecklist.THREADS_TARGET=10` constant so the funnel rate matches the in-app checkmark. The service loads the signup cohort via the existing `UserRepository.findCreatedAtAfter(cutoff)`, extracts IDs, and runs 5 cohort-scoped count queries: `MailAccountRepository.countDistinctOwnersIn(ids)`, `EmailThreadRepository.findOwnerIdsWithAtLeastThreadsAmong(ids, 10).size()` (returns matching owner IDs so the caller takes `.size()` — JPQL doesn't portably support derived-table `COUNT`-over-`GROUP BY HAVING`), `SavedSearchRepository.countDistinctOwnersIn(ids)`, `TeamInviteRepository.countDistinctInvitersIn(ids)` (filters `revokedAt IS NULL` to mirror the checklist's step-4 predicate), `SubscriptionRepository.countActiveOwnersIn(ids)` (`LOWER(status)='active'` to match `RevenueMetricsService` normalization). Empty-cohort short-circuit returns `OnboardingFunnelMetrics.empty(WINDOW_DAYS)` so a fresh-deploy instance with zero signups doesn't pay 5 round-trips for guaranteed zeros. Each step rate is `{step}/signups` rather than `{step}/previous` — the operator reads each percent as "what fraction of the cohort reached this step", so the largest drop between two adjacent columns is the next monetization leak (the explicit goal of this milestone). `AdminRevenueController` constructor gains `OnboardingFunnelMetricsService` (positioned after `TrialEndConversionMetricsService` to match the visual order on the page) and exposes `onboardingFunnel` as a model attribute. `revenue.html` renders an "Onboarding funnel — last 30 days" `<section class="admin-card">` between the existing acquisition funnel and the trial-end conversion card, reusing the established `.admin-funnel-row` / `.admin-funnel-step` markup so the new row inherits the existing CSS (no new styles, no mobile-breakpoint divergence). Steps: Signups → Mailbox connected → 10 threads imported → Saved a search → Invite sent → Paid, each with the cohort-anchored % subtitle; an `<p class="admin-empty">` fallback renders the "No signups in the window yet" copy when the cohort is empty. Tests (7 new — 678 total → 678 pass with 1 Docker-only skip; was 671 before this ship): `OnboardingFunnelMetricsServiceTest` x6 (`@ExtendWith(MockitoExtension.class)` with mocked repos + fixed Clock) — `emptyCohortShortCircuitsToZerosWithoutHittingDownstreamRepos` proves the 5 downstream repos receive zero `verify` calls and every field is 0; `cutoffPassedToUserRepoIsThirtyDaysBeforeClockNow` captures the timestamp and asserts equality with `now.minusDays(30)`; `everyStepCountsAndRatesAreSignupAnchored` plants a 10-signup cohort + per-step stub counts (8/5/3/2/1) and asserts every step rate is `{step}/10` not `{step}/previous` — the key design contract; `cohortIdsPassedToEveryDownstreamRepoMatchTheUserCohort` captures the IN-list passed to each of the 5 repos and proves they all receive the same cohort IDs (cross-repo agreement on the slice); `zeroPaidConversionsStillRendersZeroPercentNotDivideByZero`; `ratesRoundHalfUpToNearestPercent` (1/3→33%, 2/3→67%, same rounding rule the other admin metrics use). `AdminRevenueControllerTest.revenuePageRendersOnboardingFunnelCardWithEverySignupBucket` registers a recent signup so the denominator is non-zero, asserts model has `onboardingFunnel` and rendered HTML contains "Onboarding funnel" + "Mailbox connected" + "10 threads imported" + "Saved a search" + "Invite sent". `./mvnw test` → 678 tests pass (1 Docker-only skipped as before). PLAN.md "Done means" for EPIC-15 is satisfied across all four milestones; `[PLAN-REVIEW]` added to MASTER_ACTIONS proposing **EPIC-16 Shared-inbox features that justify the Team plan** as the next Primary Objective (real-time collaborator presence on a thread, internal comments attached to messages, @mention notifications — Team $29/mo currently upgrades on mailbox-cap + scaffolded invite flow, which doesn't make the recurring charge feel earned once a team has more than one user inside it). BACKLOG.md emptied pending [PLAN-REVIEW].
Advances: EPIC-15 in-app onboarding checklist — Milestone 4 (Operator dashboard card for onboarding-step conversion). EPIC-15 now code-complete on all four milestones; next Primary Objective proposed in MASTER_ACTIONS pending [PLAN-REVIEW].
Master action: none

## 2026-06-10
Shipped: EPIC-15 Milestone 3 — per-step upgrade nudges that monetize the activation arc on Free, swapping a `/billing/checkout` CTA into the onboarding progress card the moment a Free user crosses each step's natural pressure point. New `OnboardingNudge` record (in `com.emailmessenger.web`) holds `upgradeTarget` Plan + `headline` + `body` + `ctaLabel` + `trigger` step-id; its static `from(Plan currentPlan, OnboardingChecklist checklist)` factory picks the highest-priority trigger in plan-precedence order (teammateInvited → Team $29/mo with shared-threads framing; savedSearchSaved → Personal $9/mo with "Free includes 1 saved search" framing; threadsImported → Personal $9/mo with "Free caps at 500 threads" framing) and returns `Optional.empty()` for paid plans OR for the empty/below-10-threads pre-step-2 state, so a fresh Free user still bumping along step 1/early step 2 gets the original CTA without an upsell layered on top. `ThreadController` injects `PlanLimitService` (new dependency), calls `planLimitService.currentPlan(owner)` after computing the checklist, and exposes `onboardingNudge` as a model attribute when present — independent of `onboarding` so a Free user with all four steps done still sees the Team nudge after `isComplete()` flips and `onboarding` is omitted. `threads.html` lifts the progress-card visibility condition from `${onboarding != null and …}` to `${(onboarding != null or onboardingNudge != null) and …}`, wraps the header/bar/steps/CTA in a `<th:block th:if="${onboarding != null}">` so they vanish once the checklist completes, and renders the new `.onboarding-nudge` panel below: brand-dashed border on a light-brand tint (`rgba(79, 128, 255, 0.06)` light / `0.12` dark via the existing `prefers-color-scheme` block) carrying the headline + body in stacked text, plus a `POST /billing/checkout` form with hidden `plan=${nudge.upgradeTargetParam()}` and `billing=monthly` inputs — same Stripe entry point the inline `UpgradeModal` already uses, so the conversion path is one code change away from picking up annual once the in-card billing toggle ships. Mobile breakpoint (`@media (max-width: 640px)`) stacks the text + CTA vertically with the button left-aligned, matching the existing progress-row mobile layout. The CTA label is the cash amount the user is about to commit to ("Upgrade to Personal — $9/mo", "Upgrade to Team — $29/mo") so the click decision happens at the button itself, not on a separate pricing page. `data-trigger="step2|step3|step4"` on the panel lets a future EPIC-15 M4 admin funnel slice nudge impressions/clicks by step. Tests (12 new — 671 total → 671 pass with 1 Docker-only skip; was 659 before this ship): new `OnboardingNudgeTest` x9 (fresh Free user with mailbox + 4 threads → empty; Free + 12 threads → step2 trigger + Personal + "500 threads" headline + "Upgrade to Personal — $9/mo" CTA; Free + 12 threads + savedSearch → step3 trigger + Personal + "1 saved search" headline + "unlimited saved searches" body; Free + 25 threads + savedSearch + invited → step4 trigger + Team + "Team plan" headline + "shared threads" body + "Upgrade to Team — $29/mo"; all-four-done Free still picks step4 over earlier triggers — precedence enforced; Personal/Team/Enterprise plans get no nudge across all checklist states; fresh empty checklist on Free returns empty; mailbox-only-with-7-threads (below the 10-thread threshold) returns empty so the upsell only fires after the user feels the chat-view payoff). `ThreadControllerTest` x4 new + 1 constructor signature update — `freeUserPastTenThreadsExposesPersonalUpgradeNudge` (FREE plan stub + 12-thread checklist → both `onboarding` and `onboardingNudge` model attrs present); `freeUserAfterSavedSearchSurfacesPersonalSavedSearchNudge` (extracts the nudge from the rendered model, asserts trigger=step3 + target=PERSONAL); `freeUserAfterTeammateInvitedSurfacesTeamPlanNudgeEvenWhenChecklistComplete` (all-four-done checklist → `onboarding` is null but `onboardingNudge` is present with trigger=step4 + target=TEAM — proves the post-complete card persistence contract); `paidUserDoesNotGetUpgradeNudgeEvenAtSameProgress` (PERSONAL plan + same 12-thread+savedSearch state → `onboardingNudge` is null). The 27 existing controller tests pick up a default `lenient().when(planLimitService.currentPlan(owner)).thenReturn(Plan.PERSONAL)` BeforeEach stub so they don't accidentally surface a nudge — the constructor signature gained a `PlanLimitService` param positioned after `OnboardingService` to match the new field order. `ThreadInboxRenderingIntegrationTest` x2 new (`onboardingNudgeRendersInsideProgressCardWithUpgradeCta` plants a Personal step3 nudge via `flashAttr` and asserts the rendered HTML contains `class="onboarding-nudge"`, `data-trigger="step3"`, headline+body+CTA copy, the `/billing/checkout` form action, and the `name="plan" value="personal"` hidden input; `teamPlanNudgeRendersStandaloneWhenChecklistIsHidden` plants a Team step4 nudge with no `onboarding` attribute and asserts the section still renders without the step list or progress bar — proves the wrapper `th:block` correctly hides the checklist UI when complete while the nudge keeps the card visible). `./mvnw test` → 671 tests pass (1 Docker-only skipped as before).
Advances: EPIC-15 in-app onboarding checklist — Milestone 3 (Per-step upgrade nudges that monetize the steps directly).
Master action: none

## 2026-06-10
Shipped: EPIC-15 Milestone 1 — always-visible 3-step onboarding progress card on `/threads`, opening the new Primary Objective (drive Free→Personal→Team upgrade on natural activation). PLAN.md is rotated to EPIC-15 because EPIC-14's "Done means" criteria are now satisfied across all four milestones (day-1, day-3, day-7, trial-end, all shipped between 2026-06-09 and 2026-06-10), and the [PLAN-REVIEW] flag has been sitting in MASTER_ACTIONS since the trial-end ship. EPIC-15's leak hypothesis is the inverse of EPIC-14's: cold signups are reached by email; warm signups (mailbox connected, threads flowing) still bounce before they feel why Personal ($9, unlimited threads + saved searches) or Team ($29, sharing) is worth the recurring charge — making the activation arc visible in-product, step-by-step, ties each next action to a CTA that lands them on a paid-plan feature. Milestone 1 replaces the empty-inbox-only welcome card with a compact horizontal progress strip that lives above the thread list whenever the checklist is incomplete, and silently disappears once it isn't. `OnboardingChecklist` is rewired from the 2-step `(mailboxConnected, firstThreadImported)` record to a 3-step `(mailboxConnected, threadCount, savedSearchSaved)` record carrying the raw thread count (so the card can show "Import 10 threads (N to go)" copy that ticks down as the IMAP poll lands more messages) plus derived `threadsImported()` (>= 10 threshold), `completedSteps()`, `totalSteps()`, `percentComplete()`, `threadsRemaining()`, `nextStepCtaUrl()`, `nextStepCtaLabel()`, `isComplete()`. The 10-thread threshold (not 1) is deliberate: a single thread is what the existing 2-step record measured, which is too low a bar for the "I see why this is useful" feeling — the chat-view payoff only lands once a few real conversations are rendered side by side. `OnboardingService` now also injects `SavedSearchRepository` and reads `countByOwner` (already present, used by `PlanLimitService`); thread count likewise comes from `EmailThreadRepository.countByOwner` (already present). `ThreadController.listThreads` now always computes the checklist (was: only on empty inbox) and exposes it as model attribute `onboarding` whenever `!isComplete()`, regardless of search/filter state — so a partly-onboarded user searching their threads still sees the progress strip nudging them toward the next step; the lockout branch still returns early before the checklist call so a `subscriptionEnded` user doesn't see onboarding while their inbox is paused. The empty-state welcome card stays for first-paint (no threads yet) and is updated to render all four visible steps (Create account → Connect mailbox → Import 10 threads → Save a search) for visual continuity with the strip. New CSS `.onboarding-progress*` styles (compact horizontal layout, progress-bar fill via `width: percentComplete%`, per-step done/current marks reusing the existing welcome-card green palette so dark mode just works, mobile breakpoint stacks the steps + CTA at 640px). Tests: `OnboardingServiceTest` x5 (fresh user → 0/3 + "Connect your inbox" CTA; mailbox-only → 1/3 + "Sync now"; 7 threads → still in sync step + `threadsRemaining()` returns 3; 10 threads no saved search → 2/3 (67%) + "Save your first search"; all three → 3/3 (100%) + `isComplete()`). `ThreadControllerTest` x3 changed/added (`nonEmptyInboxStillExposesOnboardingChecklistWhileIncomplete`, `completedChecklistIsNotExposedSoProgressBarDisappears`, `searchWithNoResultsStillShowsOnboardingProgressWhileIncomplete`, `senderFilterActiveStillShowsOnboardingProgressWhileIncomplete`, `filterChipsActiveStillShowsOnboardingProgressWhileIncomplete` — replacing the old `suppress…OnboardingCard` tests that enforced the empty-inbox-only contract the strip explicitly inverts). `./mvnw test` → 636 tests pass (1 Docker-only skipped as before). BACKLOG.md seeded with M2 (team-invite flow + step 4), M3 (per-step upgrade nudges), M4 (admin onboarding-funnel card); the [PLAN-REVIEW] entry is cleared from MASTER_ACTIONS.
Advances: EPIC-15 in-app onboarding checklist — Milestone 1 (Always-visible 3-step progress card on `/threads`). Opens EPIC-15 and closes the [PLAN-REVIEW] on EPIC-14.
Master action: none

## 2026-06-10
Shipped: trial-end conversion email at T-1 day for trialing PERSONAL/TEAM subscribers, closing EPIC-14 Milestone 4 — the final revenue-critical leak in the funnel where a 14-day trial is about to silently rebill or lapse to canceled and the operator has one shot at "pick a plan to keep going". Flyway V23 adds nullable `subscriptions.last_trial_end_email_sent_at` as a one-shot stamp — anchored on the `subscriptions` row rather than `users` because the trial lifecycle belongs to the subscription, and a future re-trial on a separate subscription row should naturally re-open the cohort with no schema gymnastics. New `SubscriptionRepository.findTrialEndCandidates(endingBy)` returns rows where `status='trialing'`, `plan IN (PERSONAL, TEAM)` (ENTERPRISE excluded — that's a sales-led path, mirroring `TrialConversionNudgeService` for consistency with the in-app modal), `trial_ends_at IS NOT NULL` AND `<= endingBy`, and `last_trial_end_email_sent_at IS NULL`, with `JOIN FETCH s.user` so the addressee + opt-out lookup are cheap. New `TrialEndConversionService.runTrialEndCycle()` + `@Transactional sendTrialEndFor(sub, now)` mirror the activation-service plumbing: idempotency one-shot via the stamp; opt-out via `DigestEmailPreference` (auto-creates if missing, skips if opted out, no stamp written on skip — same pattern as ActivationService). Body subject branches on `hoursUntilTrialEnd` ("ends today" / "ends in 24 hours" / "wrapping up soon") with the plan label injected ("Your MailIM Personal trial ends in 24 hours"). Body leads with `/pricing` ("pick a plan to keep using your chat-view inbox without interruption — your mailbox, threads, and saved searches stay exactly as they are"), then `/billing` (manage payment / cancel / downgrade), then a Free-fallback frame ("1 mailbox, 500 threads, no card on file. No trial clock, no auto-renewal. You won't lose your threads."), then `/demo` link, then the existing `digest_email_preferences` unsubscribe token so one click still kills every automated channel. New `TrialEndConversionScheduler` (`@Component` + `@ConditionalOnProperty("trial-end.enabled", havingValue = "true")`) runs cron `0 30 14 * * ?` UTC (override via `TRIAL_END_CRON`), 30min after the day-7 last-chance to avoid scheduler stampede; the service stays in the context with the flag off so direct invocation from tests/admin tooling works regardless. `application.yml` adds `trial-end.{enabled,cron,zone}` to both `dev` and `prod` profile blocks with `TRIAL_END_ENABLED:false` defaults — dev/CI never emails by accident. `/admin/revenue` gains a new "Trial-end conversion — last 30 days" card between the funnel and the acquisition-source breakdown: two-step row (Emails sent → Converted to active) with a "% of sent" sub-label so the operator can compare against the pre-EPIC-14 baseline at a glance. New `TrialEndConversionMetricsService.snapshot()` reads `SubscriptionRepository.findTrialEndEmailedSince(cutoff)` (anchored on the same one-shot stamp the service writes) and counts active-status subs as conversions; rounds to nearest percent and zero-guards the rate so a fresh-deploy instance with zero sends doesn't divide-by-zero. New `Subscription.lastTrialEndEmailSentAt` field + getter/setter, `SubscriptionRepository.touchTrialEndEmailSent(id, ts)` `@Modifying` query, `AdminRevenueController` injects `TrialEndConversionMetricsService` and exposes `trialEnd` as a model attribute. Tests (16 new — 633 total → 633 pass with 1 Docker-only skip): `TrialEndConversionServiceTest` x9 (`@SpringBootTest` + `@Transactional` + `@MockBean JavaMailSender/StripeCheckoutGateway/StripePortalGateway/ReplyService` per the activation-test pattern) — trialing PERSONAL with `trial_ends_at` in 12h sends + stamps + addresses correct recipient + subject mentions "Personal" + "trial", body links `/pricing`+`/billing`+`/demo`+opt-out token; trial >24h away → 0 sent; already-stamped → skipped; opted-out → skipped with no stamp; status='active' → not in cohort; ENTERPRISE plan → not in cohort; idempotency (first cycle 1, second cycle 0); 3-row cohort sweep returns exactly the in-window paid-plan trialing row; TEAM plan body shows "Team" in subject. `TrialEndConversionSchedulerFeatureFlagTest` x2 (`trial-end.enabled=false` → no scheduler bean, service bean still present). `TrialEndConversionMetricsServiceTest` x4 (`@ExtendWith(MockitoExtension.class)` with mocked SubscriptionRepository + fixed Clock) — empty repo → all zeros; 4-row sample with 2 active + 1 trialing + 1 canceled → sent=4, converted=2, rate=50%; 1-of-3 active → 33%; zero sent → zero rate no NaN. `AdminRevenueControllerTest.revenuePageRendersTrialEndConversionCardWithSentAndConvertedCounts` seeds 1 active + 1 canceled subscription both with the trial-end stamp set in-window, asserts model has `trialEnd` and rendered HTML contains "Trial-end conversion" + "Emails sent" + "Converted to active". `./mvnw test` → 633 tests pass (1 Docker-only skipped as before). PLAN.md "Done means" now satisfied across all four EPIC-14 milestones; `[PLAN-REVIEW]` added to MASTER_ACTIONS proposing **EPIC-15 In-app onboarding checklist** as the next Primary Objective (drives Free→Personal→Team upgrade on natural activation, complements the email drip that already handles cold-signup bounces).
Advances: EPIC-14 activation drip — Milestone 4 (Trial-end conversion email). EPIC-14 now code-complete on all four milestones; next Primary Objective proposed in MASTER_ACTIONS pending [PLAN-REVIEW].
Master action: `TRIAL_END_ENABLED=true` flip on the deploy after live mail send is verified — same gating pattern as `ACTIVATION_ENABLED` / `ADMIN_WEEKLY_DIGEST_ENABLED`.

## 2026-06-10
Shipped: day-7 last-chance activation email with plan-intent body branching for signups still cold a full week after registration. Flyway V22 adds nullable `users.last_activation_lastchance_sent_at` as a third one-shot stamp, tracked independently of the day-1 and day-3 stamps so each milestone in the drip fires at most once per signup. New `UserRepository.findActivationLastChanceCandidates(cutoff)` returns enabled rows where `created_at < cutoff` (168h ago), `lastActivationFollowupSentAt IS NOT NULL` (sequencing: day-3 already fired — transitively requires day-1 too, since day-3 itself gates on the day-1 stamp), `lastActivationLastChanceSentAt IS NULL`, and `NOT EXISTS` over `MailAccount`. New `ActivationService.runActivationLastChanceCycle()` + `@Transactional sendActivationLastChanceFor(user, now)` mirror the day-1 / day-3 plumbing but compose a body that branches on plan intent captured at signup via the `Subscription` row: a paid-intent signup (Subscription exists with PERSONAL/TEAM/ENTERPRISE plan) gets "It's been a week since you signed up for a MailIM trial, and the 14-day clock is still running…" + a downgrade-to-Free fallback pointing at `/billing` ("1 mailbox, 500 threads, no card on file, no trial clock"); everyone else (no Subscription, or FREE plan) gets "you picked Free — so there's no trial clock and no card on file…" with a Free-fits-most-personal-use frame. Both bodies end with the live `/demo` link and the existing `digest_email_preferences` opt-out token, and both subjects diverge ("Your MailIM trial is winding down — connect to use it" vs "Still curious about MailIM? Free is here whenever you're ready"). `ActivationScheduler` gains a third `@Scheduled` at `0 0 14 * * ?` UTC (15 min after the day-3 cron, override via `ACTIVATION_LASTCHANCE_CRON`) under the same `activation.enabled` flag. 6 new tests in `ActivationServiceTest`: free-intent body framing (subject "Free is here", body "you picked Free", no "14-day clock" leak, stamp persisted); paid-intent body framing (subject "trial is winding down", body "14-day clock" + `/billing` + `/demo`, no "you picked Free" leak); sequencing (no send before day-3 stamp); mailbox-connected exclusion (8d-old + day-3 stamped + MailAccount → 0 sent); 168h cool-off (5d-old + day-3 stamped → 0 sent); idempotency (second cycle is a no-op); opt-out skip (no send, no stamp). `./mvnw test` → 617 tests pass (1 Docker-only skipped as before).
Advances: EPIC-14 activation drip — Milestone 3 (Day-7 last-chance "here's what you're missing").
Master action: none

## 2026-06-09
Shipped: day-3 follow-up email leading with `/demo` for signups still cold 72h after registration. Flyway V21 adds nullable `users.last_activation_followup_sent_at` as a second one-shot stamp, tracked independently of the day-1 stamp so each milestone in the activation drip fires at most once per signup. New `UserRepository.findActivationFollowupCandidates(cutoff)` returns enabled rows where `created_at < cutoff` (72h ago), `lastActivationNudgeSentAt IS NOT NULL` (sequencing: day-1 already fired — a user can't receive day-3 before day-1 even if the day-1 scheduler skipped a tick or was wired up after they crossed the 72h threshold), `lastActivationFollowupSentAt IS NULL`, and `NOT EXISTS` over `MailAccount`. New `ActivationService.runActivationFollowupCycle()` + `@Transactional sendActivationFollowupFor(user, now)` mirror the day-1 plumbing but compose a different body: leads with "No pressure on connecting a mailbox — handing over IMAP credentials is a real ask. Want to see what you'd get first? Open the live demo (no signup, no credentials): /demo" and only mentions `/mailboxes/new` after — different framing for a cohort that didn't act on the day-1 IMAP-form CTA. Subject is "See MailIM in action — no mailbox needed". Same `digest_email_preferences` opt-out token, so a single unsubscribe still kills every automated channel. `ActivationScheduler` gains a second `@Scheduled` at `0 45 13 * * ?` UTC (15 min after the day-1 cron, override via `ACTIVATION_FOLLOWUP_CRON`) under the same `activation.enabled` flag — both schedulers go off only when the flag is on, and the service stays in the context either way for direct invocation. 7 new tests in `ActivationServiceTest`: demo-led body ordering (`/demo` index < `/mailboxes/new` index, both present, opt-out token present); sequencing (no send before day-1 stamp); mailbox-connected exclusion (4d-old + day-1 stamped + MailAccount → 0 sent); 72h cool-off (2d-old + day-1 stamped → 0 sent); idempotency (second cycle is a no-op); opt-out skip (no send, no stamp); cohort partitioning (4 users — eligible/connected/no-day-1/too-new → exactly 1 send). `./mvnw test` → 610 tests pass (1 Docker-only skipped as before).
Advances: EPIC-14 activation drip — Milestone 2 (Day-3 follow-up linking to the demo conversation).
Master action: none (existing `ACTIVATION_ENABLED=true` master action gates both the day-1 and day-3 scheduler ticks — wiring one wires the other).

## 2026-06-09
Shipped: day-1 "connect your mailbox" activation email — the funnel leak that EPIC-13 exposed. New `com.emailmessenger.activation` package with `ActivationService` (always in context, `@Transactional` `sendActivationFor`) + `ActivationScheduler` (gated by `activation.enabled=true`, default cron `0 30 13 * * ?` UTC). Flyway V20 adds nullable `users.last_activation_nudge_sent_at` as a one-shot stamp; `UserRepository.findActivationCandidates(cutoff)` returns enabled rows where `created_at < cutoff`, the stamp is null, and `NOT EXISTS` over `MailAccount` — so signups that bounced through the IMAP-credentials chasm are visible to the sweep that the existing reengagement service (gated on unread-thread count) silently skipped. The composed email points the recipient at `/mailboxes/new` for the 60-second connect form and `/demo` for a live preview of the chat view, and carries the same `digest_email_preferences.opt_out_token` link so a single unsubscribe kills every automated marketing email. Idempotency is one-shot: the stamp gates re-sends, and `runActivationCycle()` returns the count of sends. `application.yml` exposes `activation.enabled` / `activation.cron` / `activation.zone` env overrides for both profiles. New `ActivationTestSupport` (`@Component`) exposes a native `UPDATE users SET created_at = …` so tests can rewind past the 24h cool-off without sleeping (the entity's `@Column(updatable = false)` blocks the JPA route). Tests: `ActivationServiceTest` covers candidate-with-no-mailbox → sent + stamp persisted; candidate with `MailAccount` → not in cohort; fresh-signup within 24h → not in cohort; already-nudged → skipped; opted-out → skipped (no stamp written); `runActivationCycle` sweeps only the right cohort and returns the send count; email body links `/mailboxes/new`, `/demo`, and the unsubscribe token. `ActivationSchedulerFeatureFlagTest` asserts the scheduler bean is absent when the flag is off but the service bean is always present. Full `./mvnw test`: 603 run, 0 failures.
Advances: EPIC-14 Milestone 1 (Day-1 "connect your mailbox" activation email) — opens EPIC-14 activation drip.
Master action: set `ACTIVATION_ENABLED=true` on the deploy once a live transactional-email send has been verified end-to-end (see new MASTER_ACTIONS entry under "Operator dashboard & digest" tier).

## 2026-06-09
Shipped: `/password/forgot` no longer mints a reset link for a Google-provisioned user — they get a "Continue with Google" panel instead — and `/login` failures for Google-linked emails redirect to `/login?error=google`, which renders a "Did you mean to sign in with Google?" `alert-info` nudge above the form. Flyway V19 adds `users.password_set BOOLEAN NOT NULL DEFAULT TRUE` so every legacy email-password row keeps its reset eligibility; `OAuth2ProvisioningService.provisionFromGoogle` writes FALSE on a fresh Google-provisioned row (the bcrypt hash is a random 32-byte secret the user never picked, so resetting to anything would silently mint a credential that bypasses Google), and `PasswordResetService.consumeToken` flips it back to TRUE so a Google-only user who later sets a chosen password is no longer Google-only. `PasswordResetService.requestReset` swaps its `boolean` return for a tri-state `Outcome` enum (`SENT` / `IGNORED` / `GOOGLE_ONLY`); `PasswordResetController` maps `GOOGLE_ONLY` to a new `status=google` model attribute that the template renders as an `alert-info` + brand-styled Google button (gated on `oauthGoogleEnabled` so an unconfigured deploy still degrades to no-button). New `LoginFailureHandler` (`@Component` implementing `AuthenticationFailureHandler`) replaces Spring Security's default `failureUrl("/login?error")` — it parses the submitted `email` param, normalises via `UserService.normalizeEmail`, looks up the row, and 302s to `/login?error=google` when `google_subject != null`, else to `/login?error`; unknown emails get the generic path so the response doesn't leak account existence. `login.html` adds a third `th:if` branch for `param.error[0] == 'google'` and CSS (`main.css`) gains an `alert-info` selector with a dark-mode counterpart matching the existing `alert-error` / `alert-success` palette. Tests: `PasswordResetServiceTest` covers `Outcome.GOOGLE_ONLY` for a Google-only row, `Outcome.SENT` for a Google-linked row that still has a chosen password, and `consumeToken` stamping `password_set=true`; `PasswordResetControllerTest` asserts the controller renders `status=google` with no mail send for the Google-only path; `OAuth2ProvisioningServiceTest` asserts fresh Google rows are flagged Google-only and that linking a Google subject onto an existing email-password row leaves `password_set=true`; `AuthFlowIntegrationTest` exercises the failure-handler path for both Google-linked and unknown emails. Full `./mvnw test`: 594 run, 0 failures.
Advances: EPIC-13 Milestone 4 (Hide /password/forgot for Google-only users + helpful redirects) — completes EPIC-13.
Master action: none (PLAN-REVIEW line already on MASTER_ACTIONS.md flagging the next Primary Objective slot).

## 2026-06-09
Shipped: plan + billing + utm_source now carry through the Google OAuth round-trip so a "Continue with Google" click from `/pricing?plan=personal&billing=annual` lands at Stripe Checkout for the right plan instead of dumping the visitor onto `/threads`, and the inbound channel is credited on `users.acquisition_source` instead of the literal string `"google"`. New `OAuthIntent` record + `OAuthIntentStore` (`@Component`) wrap session-attribute storage under namespaced keys `mailim.oauth.plan` / `mailim.oauth.billing` / `mailim.oauth.utm_source` with `store(request, plan, billing, utmSource)` validating via `Plan.parse` (try/catch → null on unknown so a tampered `?plan=platinum` is harmless), `BillingPeriod.parse` (its own lenient null/blank/unknown-falls-back-to-MONTHLY semantics), and a 64-char clamp on utm; `peek(request)` / `peekCurrent()` (`RequestContextHolder` lookup for deep call sites like `GoogleOidcUserService` that don't see the request directly) return the current intent without mutating; `consume(request)` returns and clears all three keys atomically so a stale intent can't follow the visitor into a later login on the same session. New `OAuthStartController` (`GET /auth/google/start`) reads `plan` / `billing` / `utm_source` off the click URL, calls `intents.store(request, ...)`, then `return "redirect:/oauth2/authorization/google"` to hand off to Spring Security's existing OAuth2 authorization filter — bypassing this controller (e.g. crawler hitting the old `/oauth2/authorization/google` URL directly) just gives the existing no-intent OAuth flow back. `SecurityConfig` adds `/auth/google/start` to the permit-all matcher list. `login.html` and `register.html` flip their Google buttons from `@{/oauth2/authorization/google}` to `@{/auth/google/start(plan=${plan},billing=${billing},utm_source=${utmSource})}` — Thymeleaf URL syntax drops null-valued params automatically so an anonymous visitor with no plan/utm in scope still gets a clean `/auth/google/start` link, and a paid-intent visitor on `/register?plan=personal&billing=annual&utm_source=producthunt` gets all three baked into the href before the click. `AuthController.loginPage` grows the `utm_source` query param read so `${utmSource}` is in the model for the same template fragment to reuse on `/login`. `PlanCheckoutSuccessHandler` (which both form-login and OAuth2 success routes wire to via the existing `successHandler(planCheckoutSuccessHandler)` calls in `SecurityConfig`) now first checks `request.getParameter("plan")` (form-login path — unchanged) and only when that's absent calls `intents.consume(request)` to recover the stored plan/billing — so the OAuth callback URL (which carries no plan param of its own) still reaches Stripe Checkout; clearing on consume guarantees a session that completes one OAuth signup-to-checkout cycle doesn't keep redirecting subsequent logins to the same checkout. `GoogleOidcUserService` now `@Component`-injects `OAuthIntentStore`, calls `intents.peekCurrent().utmSource()` after delegating to `super.loadUser`, and threads the value into a new `OAuth2ProvisioningService.provisionFromGoogle(email, name, verified, acquisitionSource)` overload that normalises the source (null/blank falls back to `OAuth2ProvisioningService.DEFAULT_SOURCE="google"`; trimmed; clamped to 64 chars matching the `users.acquisition_source` column width set in Flyway V8) — only used on first provision so an existing user's source is never overwritten, preserving the EPIC-05 attribution-of-record contract. Old `provisionFromGoogle(email, name, verified)` overload retained as a `return provisionFromGoogle(email, name, verified, null)` thin shim so existing call sites and unit tests don't have to be touched at once. Tests (16 new + 2 existing assertions updated, 595 total → 581 pass with 1 Docker-only skip): new `OAuthStartControllerTest` (`@SpringBootTest` + `@AutoConfigureMockMvc`) x6 — `storesPlanBillingUtmIntoSessionAndRedirectsToSpringSecurityAuthorization` (full params → session has all three normalised values + redirect points at `/oauth2/authorization/google`); `missingParamsLeaveSessionAttributesUnsetAndStillRedirect` (no params → no session attrs, still 302s); `unknownPlanIsDroppedSoTamperedQueryStringIsHarmless` (`?plan=MYSTERYTIER` → plan attr stays null, no exception); `unknownBillingDegradesToMonthly` (`?billing=quarterly` with a valid plan → billing attr is the string `"monthly"`, matching BillingPeriod.parse's lenient semantics); `overlongUtmSourceIsClampedTo64Chars` (200-char utm → session attr is exactly 64 chars); `freshPlanClearsAnyStaleIntentFromEarlierStart` (pre-populated session attrs are wiped by a no-param start — covers the "user clicked twice from different tabs" race, last write wins). New `AuthFlowIntegrationTest#loginConsumesSessionStoredOauthIntentToReachCheckout` plants `mailim.oauth.plan=personal` + `mailim.oauth.billing=annual` on a MockHttpSession, form-logs in WITHOUT any plan param, and asserts the redirect is to the mocked `https://checkout.stripe.com/...` URL + the session attrs are gone afterwards (proves PlanCheckoutSuccessHandler's session-fallback path AND the consume-clears-on-success contract in one go). `OAuth2ProvisioningServiceTest` x4: `utmSourceOverridesGoogleAsAcquisitionSourceOnFirstProvision` (utm "producthunt" → source = "producthunt"); `blankAcquisitionSourceFallsBackToGoogle` (blank and null both → "google"); `overlongAcquisitionSourceClampedToColumnWidth` (200-char utm → source is exactly 64 chars); `utmSourceDoesNotOverwriteExistingUsersSource` (pre-existing user with `source="organic"` + new OAuth login with utm "producthunt" → source stays "organic"). Existing `GoogleOAuthSignInIntegrationTest` button assertions flipped from `/oauth2/authorization/google` to `/auth/google/start`; new `registerPageGoogleButtonCarriesPlanBillingAndUtm` proves the rendered href on `/register?plan=personal&billing=annual&utm_source=producthunt` contains `plan=personal`, `billing=annual`, and `utm_source=producthunt`. Existing `GoogleOAuthDisabledIntegrationTest` extended to assert `/auth/google/start` ALSO does not render on an unconfigured deploy (the `${oauthGoogleEnabled}` Thymeleaf gate covers it, but explicit is better than implicit so a future refactor that decouples the button URL from the OAuth-enabled flag doesn't silently start showing a broken link to crawlers). `./mvnw test` → 581 tests pass (1 Docker-only skip; was 569 before this ship).
Advances: EPIC-13 Google OAuth signup — Milestone 2 (Carry plan + billing + utm_source through OAuth state).
Master action: none

## 2026-06-09
Shipped: "Continue with Google" sign-in + auto-provisioning, opening **EPIC-13 Google OAuth signup** as the new Primary Objective (EPIC-12 First-paying-customer attribution funnel was code-complete on all four milestones per its 2026-06-08 closeout — `/admin/revenue`, Stripe pre-V17 backfill, 30-day funnel pane, and Monday 09:00 UTC operator digest — and PLAN.md "Done means" verified against the latest run). Registration today forces every visitor to invent another password and round-trip through an email-verification link before they can connect a mailbox — the single most-leveraged drop-off in the funnel. This ship collapses both into one tap. New `spring-boot-starter-oauth2-client` dependency in `pom.xml`. New `GoogleOAuthProperties` (`@ConfigurationProperties("auth.google")`, registered in `EmailMessengerApplication.@EnableConfigurationProperties`) carries `client-id`/`client-secret`/`redirect-uri` from the deploy. `application.yml` adds `auth.google.{client-id,client-secret,redirect-uri}` outside the dev/prod profile blocks (so the binding applies everywhere) with both `AUTH_GOOGLE_CLIENT_ID` and legacy `GOOGLE_CLIENT_ID` env var names accepted via nested defaults — operators who set up the existing Google credentials master action with the old name keep working without re-configuring. New `GoogleOAuthClientRegistrationConfig` is the bean factory — `@ConditionalOnExpression` that requires both `client-id` AND `client-secret` to be non-blank, so an unconfigured deploy (the default in dev/test) silently boots without the Google registration; with both set, it builds `CommonOAuth2Provider.GOOGLE.getBuilder("google")` + the configured client-id/secret/redirect-uri into a single-entry `InMemoryClientRegistrationRepository` (had to use the conditional+empty-list workaround because `InMemoryClientRegistrationRepository`'s constructor `Assert.notEmpty`s and Spring Boot's `OAuth2ClientPropertiesMapper` validates `clientId` has-text, so leaving the spring.security.oauth2 yaml hardwired with empty defaults would crash startup — the env-var-gated bean factory is the only path that lets an unconfigured fresh deploy still boot). New `OAuth2ProvisioningService` (`@Service`) is the find-or-create entry point: `provisionFromGoogle(email, displayName, emailVerified)` normalises the email lowercase+trim, looks up the row, returns it untouched on hit (stamping `email_verified_at` if Google says verified and we hadn't stamped it before — never overwriting an existing timestamp), or creates a fresh row on miss with `acquisition_source="google"`, `email_verified_at = clock.now()` when Google's `email_verified` claim is true, and a 32-random-byte URL-safe-base64 password hash (so the password-based code paths — DB user-details lookup, password-reset flow — keep working even though no human knows the password). New `GoogleOidcUserService extends OidcUserService` (`@Component`) bridges Spring Security's OIDC flow with our table: delegates to `super.loadUser(req)` to fetch the id_token + userinfo from Google, extracts `email`, `name`, `email_verified` from the attribute map, throws `OAuth2AuthenticationException(missing_email)` if email is missing (Google sometimes omits it on consent declines), calls `provisioner.provisionFromGoogle(...)`, then rebuilds a `DefaultOidcUser(authorities, idToken, userInfo, "email")` so `authentication.getName()` returns the email going forward — every controller in the codebase keys off `Principal.getName()` for `userService.requireByEmail(...)` lookups, and a raw Google `sub` claim there would break threads/account/billing/mailbox/saved-search pages on the very first OAuth login. `SecurityConfig` gains two `ObjectProvider`s — `ClientRegistrationRepository` (only present when the conditional bean factory's properties are configured) and `GoogleOidcUserService` — and only calls `http.oauth2Login(...)` when the registration repo is available, so the OAuth filter chain is fully absent on an unconfigured deploy (no `/oauth2/authorization/google` filter, no `/login/oauth2/code/google` endpoint registered, no broken-link surface for crawlers). When wired, `oauth2Login` reuses the existing `PlanCheckoutSuccessHandler` so a future EPIC-13 milestone 2 ship that lifts `?plan=…` through OAuth state will reach Stripe Checkout via the same code path as form login. `SiteModelAdvice` exposes `oauthGoogleEnabled` to every model by checking whether the `ClientRegistrationRepository` bean contains a non-blank `google` registration (handles both `InMemoryClientRegistrationRepository`'s iterable interface and the generic `findByRegistrationId` fallback). `login.html` and `register.html` each render — inside `<th:block th:if="${oauthGoogleEnabled}">` — a `.btn-oauth.btn-oauth-google` anchor to `@{/oauth2/authorization/google}` carrying the Google "G" multi-color SVG mark inline (4-path 48×48 viewBox, 20px rendered size, `aria-hidden="true" focusable="false"` so screen readers don't announce it), followed by an `.auth-divider` "or" separator rule that breaks the visual flow between the OAuth button and the password form so a returning user knows which fork they're on. ~45 lines of namespaced CSS appended to `main.css` (`.btn-oauth` + `.btn-oauth-icon` + `.auth-divider` + `:hover` + `:focus`) reusing the existing `--surface`/`--border`/`--brand`/`--text-muted` design tokens so dark mode picks up automatically with no extra overrides — `min-height: 44px` matches the mobile-tuned tap-target floor from EPIC-10; `:hover` flips the border to brand color + tints the background with `rgba(79, 128, 255, 0.06)` so the affordance reads clearly; the divider draws hairline rules left+right of the centered "OR" label so it reads as a structural break rather than decoration. New tests (16 total): `OAuth2ProvisioningServiceTest` (`@SpringBootTest` + `@Transactional` for cleanup) x8 — brand-new Google email creates a row with `acquisition_source="google"` + `email_verified_at` stamped from the injected `Clock` (avoiding the EmailMessengerApplication-vs-test-config Clock-bean duplicate by constructing the service manually in `@BeforeEach` rather than the @DataJpaTest + @TestConfig pattern UserServiceTest uses); `email_verified=false` from Google does NOT stamp `email_verified_at`; an existing user is returned with `displayName` + `passwordHash` untouched (so a future re-link won't overwrite their chosen name or rotate a known password to a random one); an existing unverified user gets `email_verified_at` stamped on first Google login (the "stop nagging me to verify my email" path); an existing verified user keeps the ORIGINAL `email_verified_at` timestamp (so the verification audit trail isn't blown away); `null` and blank email rejected with `IllegalArgumentException`; blank display name stored as null (matching `UserService.register`'s normalisation); mixed-case + whitespace-padded email gets normalised to lowercase+trimmed before insert. `GoogleOAuthSignInIntegrationTest` (`@SpringBootTest` + `@AutoConfigureMockMvc` + `@TestPropertySource(auth.google.client-id=test-id, client-secret=test-secret)`) x4 — `ClientRegistrationRepository.findByRegistrationId("google")` returns a registration whose `clientId` matches the configured test ID and scopes include `openid`+`email`+`profile`; `GET /login` renders "Continue with Google" + `/oauth2/authorization/google` href + `.btn-oauth.btn-oauth-google` class + `.auth-divider` class; `GET /register` renders the same; `GET /oauth2/authorization/google` returns a 302 to `https://accounts.google.com/o/oauth2/v2/auth` carrying the test `client_id` + `scope=openid` in the query string. `GoogleOAuthDisabledIntegrationTest` (no `@TestPropertySource` — relies on the dev profile's empty defaults) x4 — `@Autowired(required = false) ClientRegistrationRepository` is `null` (proving the bean factory's `@ConditionalOnExpression` correctly omits the bean when client-id is blank); `GET /login` and `GET /register` do NOT contain "Continue with Google" or the OAuth href (so a fresh-deploy crawler doesn't see a broken link); `GET /oauth2/authorization/google` falls through to the catch-all authenticated rule and 302s to `/login`. `./mvnw test` → 569 tests pass (1 Docker-only test skipped as before; +16 from 553). Done means EPIC-13 milestone 1; remaining milestones (plan/billing/utm_source state propagation, account linking via `google_subject` column, /password/forgot helpful redirect for Google-only users) seeded in `BACKLOG.md`. MASTER_ACTIONS [PLAN-REVIEW] resolved by adopting EPIC-13; the Google-credentials line was reworked to call out the new `AUTH_GOOGLE_CLIENT_ID`/`AUTH_GOOGLE_CLIENT_SECRET` env names with `GOOGLE_CLIENT_ID`/`GOOGLE_CLIENT_SECRET` retained as fallbacks.
Advances: EPIC-13 Google OAuth signup — Milestone 1 ("Continue with Google" sign-in + auto-provisioning).
Master action: Create the Google Cloud OAuth 2.0 client, register the production redirect URI `https://<prod-domain>/login/oauth2/code/google`, and set `AUTH_GOOGLE_CLIENT_ID` + `AUTH_GOOGLE_CLIENT_SECRET` on the deploy — without these the button stays hidden and the OAuth filter chain isn't wired (see MASTER_ACTIONS.md → OAuth & third-party APIs).

## 2026-06-08
Shipped: Monday 09:00 UTC weekly operator email digest (gated by `ADMIN_WEEKLY_DIGEST_ENABLED`) closing out EPIC-12 First-paying-customer attribution funnel — every address in `ADMIN_EMAILS` receives a plain-text "MailIM weekly" summary with MRR, ARR, active subscriber count, monthly/annual mix percentage, in-trial pipeline + trials-ending-soon, and the two week-over-week deltas the dashboard doesn't show on its own (new paying customers in the last 7 days, churn in the last 7 days), plus a direct link to `/admin/revenue` for the full breakdown. The point is that the operator has to remember to open `/admin/revenue` for the dashboard to do anything — and a solo-operator SaaS founder forgets, especially after the first dopamine hit fades and the MRR plateaus. The digest delivers the same headline numbers to the inbox the operator already reads first thing Monday morning so the "is this thing making money?" question gets answered without any action on the operator's part. New `com.emailmessenger.admin.AdminWeeklyDigestService` (`@Service`) takes `AdminProperties` for the recipient allowlist + `RevenueMetricsService` for the steady-state snapshot (avoids duplicating the MRR/ARR/active-count math that already lives in one place) + `SubscriptionRepository` for the two cheap derived-query weekly counts + `JavaMailSender` (same path as `ReplyService` / `WeeklyDigestService` / `ReengagementService`) + `SiteProperties` for the absolute dashboard URL + `Clock` for deterministic time anchoring; the `sendDigest()` entry point no-ops with `return 0` when `adminProperties.getEmails()` is empty so a deploy without the env var configured doesn't burn an SMTP round-trip, then for each allowlisted recipient builds one fresh `MimeMessage` via the helper, sets From/To/Subject/Text, and catches per-recipient `MailException` (logged WARN with the offending address + reason, then continues) so one bad SMTP relay can't stall the sweep. `SubscriptionRepository` grows a single Spring Data derived query — `long countByStatusAndUpdatedAtAfter(String status, LocalDateTime cutoff)` — wired by the service as `countByStatusAndUpdatedAtAfter("active", now-7d)` for new paying customers and `countByStatusAndUpdatedAtAfter("canceled", now-7d)` for churn; both anchor on `updatedAt` rather than `createdAt` so a trial-to-paid conversion (createdAt > 7 days ago but status flipped to 'active' this week) and a true brand-new paid signup BOTH register as "new paying customers" — the operator cares about revenue events that happened this week, not enrollment cohort age. Email subject is `"MailIM weekly: $X MRR, N new, M churn"` so the headline number is visible before the operator even opens the message — and the in-inbox preview pane on iOS/Android Mail / Gmail Web already shows it. Body is left-aligned monospace-friendly key:value rows ("MRR:                 $X" / "ARR:                 $X" / "Active subscribers:  N" / "Annual mix:          P% annual / Q% monthly" / "In trial:            T (V ending in 7 days)") plus a "Last 7 days:" block with "New paying customers: K" and "Churn (canceled):     M", closed with a `Full dashboard: $base/admin/revenue` line that link-opens straight to the existing operator page if the operator wants the per-plan/per-source breakdown. Body is composed once per send-cycle (not per-recipient) so a 5-operator allowlist runs one `RevenueMetricsService.snapshot()` + two count queries total, not 5× each — the email contents are identical so per-recipient rendering would be wasted work. New `AdminWeeklyDigestScheduler` (`@Component` + `@ConditionalOnProperty(name = "admin.weekly-digest.enabled", havingValue = "true")`) runs cron `0 0 9 ? * MON` UTC via `@Scheduled` and logs the sent count INFO; both cron + zone externalised as `ADMIN_WEEKLY_DIGEST_CRON` / `ADMIN_WEEKLY_DIGEST_ZONE` so an operator on Pacific time can shift the send window without a code change. `application.yml` adds `admin.weekly-digest.{enabled,cron,zone}` in both `dev` and `prod` profile blocks with `ADMIN_WEEKLY_DIGEST_ENABLED:false` as the default — the scheduler is dormant on every fresh deploy until the operator explicitly flips the flag, so dev/CI/staging environments never email anyone by accident (same gating pattern as `digest.enabled` for the user-facing weekly digest and `reengagement.enabled` for the 7-day dormant nudge). Tests: 5 `AdminWeeklyDigestServiceTest` cases full-Spring `@SpringBootTest` + H2 with `@MockBean JavaMailSender` returning a `thenAnswer(_ -> new MimeMessage((Session) null))` (fresh instance per call so the captor doesn't get aliased to the same final-state ref across multiple sends — a subtle ArgumentCaptor trap that the same-MimeMessage stub used elsewhere doesn't hit because most services only send one mail per invocation): `emptyAllowlistSendsNothing` short-circuits before any DB call + `mailSender.send` never invoked; `everyAllowlistedAddressReceivesOneEmail` two-recipient setup verifies count + `getAllValues()` preserves invocation order with op1@example.com captured first then op2@example.com; `bodyContainsMrrArrAndDashboardLink` plants a Personal-monthly active sub + asserts subject contains "MailIM weekly:" + "$9 MRR" + body contains "MRR:", "$9", "ARR:", "$108", "Active subscribers:  1", and "/admin/revenue"; `weeklyDeltasCountActiveAndCanceledTouchedInsideTheLookback` plants one fresh active + one fresh canceled subscription and asserts body contains "New paying customers: 1" + "Churn (canceled):     1"; `perRecipientMailExceptionIsLoggedAndDoesNotAbortTheSweep` stubs `send` to throw `MailSendException("smtp down")` on the first invocation + `doNothing()` on the second, then verifies sent=1 (only the second made it through) and the first failure didn't propagate. 2 `AdminWeeklyDigestSchedulerFeatureFlagTest` cases (scheduler bean absent when `admin.weekly-digest.enabled=false`; service bean always present so direct invocation from admin tooling/tests works regardless). `./mvnw test` → 553 tests pass (1 Docker-only test skipped as before). PLAN.md "Done means" now fully satisfied — the operator sees live revenue at `/admin/revenue`, the Stripe backfill button fills pre-V17 `billing_period` rows, the 30-day funnel pane breaks signup→trial→paid by source, and Monday morning email delivers the same headline metrics to every operator. EPIC-12 is code-complete; `[PLAN-REVIEW]` added to MASTER_ACTIONS proposing **EPIC-13 Google OAuth signup** as the next Primary Objective (lifts `acquisition_source` from OAuth state into the funnel dashboard automatically, removes the "make another password" friction at /register that costs conversion on the most leveraged step of the funnel, and uses the Google Cloud credentials the existing MASTER_ACTIONS line is already waiting on). BACKLOG drained pending that review.
Advances: EPIC-12 First-paying-customer attribution funnel — Milestone 4 (Weekly operator email digest). EPIC-12 now code-complete on all four milestones; next Primary Objective proposed in MASTER_ACTIONS pending [PLAN-REVIEW].
Master action: `ADMIN_WEEKLY_DIGEST_ENABLED=true` flip on the deploy after live mail send is verified — same gating pattern as the user-facing `DIGEST_ENABLED` flag.

## 2026-06-08
Shipped: 30-day signup → trial → paid funnel pane on `/admin/revenue` with per-`acquisition_source` breakdown — the operator can now see how many signups, trial starts, and paid conversions happened in the last 30 days, the trial-rate (% of signups) and paid-rate (% of trial starts) for the cohort, AND the same three counts + two percentages sliced by `users.acquisition_source` so a "twitter brings 200 signups → 1 paid" channel is visible next to a "producthunt brings 40 signups → 10 paid" channel without leaving the dashboard. This is the data needed to make per-channel acquisition-spend decisions — which channels actually convert to paid, vs. which only convert to free signups that never trial. New `FunnelMetrics` view-model record in `com.emailmessenger.admin` (windowDays, three counts, two percentages, plus a `List<SourceFunnel>` of per-source rows) and `FunnelMetricsService.snapshot()` that pulls signup cohort via `UserRepository.findCreatedAtAfter(cutoff)` and trial cohort via the new `SubscriptionRepository.findTrialCohortSince(cutoff)` JPQL `SELECT s FROM Subscription s JOIN FETCH s.user WHERE s.createdAt >= :cutoff AND s.status IN ('trialing','active','canceled')` — both anchored on `createdAt` (not `updatedAt`) so a year-old active subscription whose row gets touched by a routine webhook doesn't accidentally inflate this month's paid-conversion count. `incomplete` status is excluded from the trial cohort because those are Stripe-rejected payment attempts, not started trials. The service collapses the two cohorts into a `LinkedHashMap<String, int[3]>` in a single pass (signups+1 / trialStarts+1 / paidConversions+1 buckets per source), bucketing null/blank `acquisition_source` as "Direct / unknown" so a free signup with no UTM still appears in the table. Per-source rows are then sorted by signup count DESC (ties broken alphabetically by label) so the operator's eye lands on the highest-volume channel first. `percentOf(numerator, denominator)` returns 0 when the denominator is 0 — so a "no signups yet" instance shows "0% of signups" rather than NaN, and the per-source paid-rate for a source with zero trials reads 0% rather than crashing. `AdminRevenueController` gains a single `FunnelMetricsService` constructor dependency and exposes `funnel` as a second model attribute alongside the existing `metrics`. `templates/admin/revenue.html` inserts a new `.admin-card` between the existing KPI grid and the Acquisition-source card: an `.admin-funnel-row` flex strip with three `.admin-funnel-step` tiles (Signups → Trial starts → Paid conversions) joined by `→` arrow glyphs (hidden under 560px viewport via a media query so the strip wraps cleanly on phones), each tile carrying its count plus the conversion-rate sub-label ("X% of signups", "X% of trials") — and below that, a six-column `.admin-table` (Source / Signups / Trials / Trial % / Paid / Paid %) listing every source seen in the window. Empty-state copy ("No signups in the last 30 days yet — per-source breakdown will appear once the first new account lands.") is rendered in place of the table when no signups have happened, so a freshly-deployed instance doesn't look broken. ~50 lines of namespaced CSS reusing the existing `--surface`/`--border`/`--text`/`--text-muted`/`--bg` design tokens so dark mode picks up automatically with no extra overrides. New tests: `FunnelMetricsServiceTest` x8 (empty repo → all zeros + window=30; the cutoff passed to both repositories is exactly `clock.now() - 30 days` so the rolling window is anchored on the injected `Clock` for deterministic tests; 3 trial starts over 10 signups → trialRate=30%; 4 trial starts with 2 active + 1 trialing + 1 canceled → paidRate=50% — canceled and trialing both excluded from the paid bucket; zero signups + 1 active sub doesn't divide by zero, returns 0% trial rate and 100% paid rate; per-source breakdown sorted by signup count DESC, producthunk → twitter → Direct / unknown; a trial start whose user signed up BEFORE the window still appears in the per-source row with signups=0 + trialStarts=1 + paidConversions=1 so it isn't silently lost; null/blank/whitespace `acquisition_source` all fold into the "Direct / unknown" bucket — three rows collapse into a single 3-signup row). `AdminRevenueControllerTest.revenuePageExposesFunnelModelAndRendersHeading` boots full Spring + Thymeleaf + Security with `@WithMockUser`, seeds a producthunt-attributed signup + an active producthunt-attributed Personal subscription, and asserts `model().attributeExists("funnel")` plus the rendered HTML contains "Funnel — last 30 days", "Trial starts", "Paid conversions", and "producthunt" — so a future template regression that drops the funnel section fails CI. `./mvnw test` → 546 tests pass (1 Docker-only test skipped as before).
Advances: EPIC-12 First-paying-customer attribution funnel — Milestone 3 (Funnel conversion rates signup → trial → paid with per-source breakdown).
Master action: none

## 2026-06-08
Shipped: Operator-only `/admin/revenue` dashboard surfacing live MRR, ARR, active subscriber count, in-trial pipeline, monthly/annual mix percentage, trials ending in the next 7 days, per-plan revenue breakdown, and acquisition-source attribution from data already in the application database — opening EPIC-12 First-paying-customer attribution funnel as the new Primary Objective (EPIC-11 Annual billing surfacing was code-complete on all four milestones and verified against PLAN.md "Done means": V17 migration ships, `Subscription.billingPeriod` populates from webhook `applyStripeEvent`, `/account` renders `AccountBillingSummary.label()` like "Personal · Annual", and the in-app upgrade modal + trial nudge + pricing toggle all post `billing=annual` end-to-end). Without this page an operator who's just deployed MailIM cannot answer "is this thing making money?", "where are my paying customers coming from?", or "what's my monthly vs. annual ARPU mix?" without opening Stripe and reading raw SQL — the single largest blocker between a launched product and one that grows by killing losing acquisition channels and doubling down on winners. New `com.emailmessenger.admin` package: `AdminProperties` (`@ConfigurationProperties("admin")` with a comma-separated `emails` allowlist, sourced from `ADMIN_EMAILS:` env var in both dev and prod profiles — normalised on set: trim + lowercase + drop blanks + handle null, so a typo'd config doesn't silently let everyone in or no one in), `AdminAuthorizer` (`isAdmin(email)` case-insensitive against the allowlist + `requireAdmin(email)` throws `NoSuchElementException` on miss so the existing `GlobalExceptionHandler` `@ExceptionHandler(NoSuchElementException)` maps it to a clean 404 — non-operators never learn `/admin/revenue` exists; default empty allowlist = no one is admin, the URL is 404 for everyone until `ADMIN_EMAILS` is wired). `PlanPricing` is the single source of truth for per-cadence monthly-equivalent cents (Personal $9/$7, Team $29/$24, Enterprise $99/$83 — same numbers `/pricing` displays, kept in cents so the formatter is pure integer arithmetic). `RevenueMetrics` is a record-of-records view model (cents-denominated counters stay numeric for tests; matching `…Formatted` USD-with-comma-separator strings render directly into Thymeleaf). `RevenueMetricsService.snapshot()` does a single `findAllWithUserNewestFirst()` round-trip (new `SubscriptionRepository` JPQL `SELECT s FROM Subscription s JOIN FETCH s.user ORDER BY s.updatedAt DESC` so plan/source attribution doesn't N+1) and computes everything in one pass: MRR from `active` rows only (trialing don't count toward MRR — Stripe convention — but their projected MRR is reported separately as `trialPipelineCents`), ARR = MRR × 12, annual-mix % from active subs only, per-plan breakdown that ALWAYS lists Personal/Team/Enterprise in that order even when empty (so the table layout doesn't shuffle as the first subscriber lands), acquisition-source breakdown sorted by MRR contribution DESC (so the operator's eye lands on the highest-revenue channel first), null/blank `users.acquisition_source` bucketed as "Direct / unknown" rather than dropped, trials-ending-soon counter against a 7-day window from `Clock.now()`, and the 10 most recent subscription events (any status) for the activity panel. `AdminRevenueController` is the only `/admin/*` endpoint right now — calls `requireAdmin` first (so the 404 fires before any data is queried), then `metricsService.snapshot()`, then renders `templates/admin/revenue.html` — six KPI tiles in a responsive auto-fit grid (180px min, wraps to two rows on narrow viewports), three card sections (plan mix table, acquisition source table, recent events table) with `.admin-empty` placeholder copy when no paying subs exist yet so a freshly-deployed instance doesn't look broken. ~90 lines of namespaced CSS reusing existing `--surface`/`--border`/`--text`/`--text-muted` tokens so dark mode picks up automatically; `.admin-table .admin-num` uses `font-variant-numeric: tabular-nums` so dollar columns align rightward. SecurityConfig needs no change — `/admin/**` falls through to the existing `anyRequest().authenticated()` matcher (anonymous gets 302 to `/login`, authed non-admin gets 404 from the controller). 19 new tests: `AdminAuthorizerTest` x8 (empty allowlist matches no one; allowlisted email matches; case + whitespace insensitive on both ends; unlisted email rejected; null/empty/whitespace input returns false; null list is treated as empty; `requireAdmin` throws `NoSuchElementException` on miss; `requireAdmin` returns silently for admin); `RevenueMetricsServiceTest` x7 (empty repo → all zeros + 3-row plan breakdown stub + "$0" formatting; mixed active/trial/canceled → MRR sums active-only by cadence, trial pipeline tracks separately, $4,500/yr ARR; annual share % rounds 1-of-3 to 33; plan breakdown always lists Personal→Team→Enterprise even when one is empty; source breakdown groups by acquisition_source, sorts by MRR DESC, null bucketed as "Direct / unknown"; trials-ending-soon counts trialing rows whose `trial_ends_at` is inside the 7-day window; recent events capped at 10 in repo-order; thousands-separator formatting for $4,350/$52,200); `AdminRevenueControllerTest` x4 full-Spring `@AutoConfigureMockMvc` (anonymous → 302 to `/login`; authed non-admin → 404 because `requireAdmin` throws `NoSuchElementException` → existing `@ExceptionHandler(NoSuchElementException)` renders the error view; admin sees the page + view name + model `metrics` + renders the seeded $9 Personal sub + the payer's email; case-insensitive allowlist match works for `OPERATOR@EXAMPLE.COM` config matching `operator@example.com` principal). `./mvnw test` → 525 tests pass (1 Docker-only test skipped as before). PLAN.md adopts EPIC-12 with four milestones: dashboard (this ship), pre-V17 `billing_period` Stripe backfill, 30-day signup→trial→paid funnel with per-source breakdown, weekly operator email digest. BACKLOG.md seeded with the three remaining milestones; MASTER_ACTIONS.md drops the resolved [PLAN-REVIEW] and adds the `ADMIN_EMAILS` deploy step.
Advances: EPIC-12 First-paying-customer attribution funnel — Milestone 1 (`/admin/revenue` operator dashboard). New Primary Objective: convert data the system already collects (subscription cadence, acquisition source, trial state) into a single operator-visible page so revenue decisions are actionable from the deployed instance instead of requiring a Stripe-dashboard tab and raw SQL.
Master action: `ADMIN_EMAILS=op1@example.com,op2@example.com` env-var must be set on the deploy or the page is invisible (404) to everyone — empty default is intentional so no one can hit it before the operator chooses who counts as admin.

## 2026-06-08
Shipped: Annual switch surfaced in the `/threads` upgrade modal and the trial-conversion nudge so an in-app user who's about to commit to a paid plan can pick annual (2 months free, ~17% ARPU lift) in one click instead of having the choice cosmetically toggled only on `/pricing`. The upgrade modal grows a Monthly|Annual sub-toggle pill above the "Upgrade to Personal" button: the Annual chip carries a green "2 months free" badge, the `Personal — $9/mo` headline swaps to `Personal — $7/mo · billed annually as $84` when Annual is active (both per-month mental model and the cash amount the user will be charged today, mirroring the framing already on `/pricing`), and a hidden `billing` input on the existing checkout form (`#upgrade-modal-billing`) is rewritten to `annual` so the POST to `/billing/checkout` actually ships the customer into the annual SKU. JS is a tight IIFE that toggles `billing-period-toggle-btn-active` + `aria-selected` on the two buttons and swaps the `data-monthly-price` / `data-annual-price` / `data-monthly-cta` / `data-annual-cta` hidden attributes — no global state, no framework dependency, and the form still hits the monthly SKU correctly for visitors with JS disabled. The trial-conversion nudge modal gains a dedicated annual sub-block (rendered only when `daysLeft <= 3` via the new `inAnnualUpsellWindow()` predicate on `TrialConversionNudge`) sitting under the existing "Continue on Personal" form: brand-dashed top border, "Save 2 months by switching to annual today — pay $84 for the year instead of $9 every month" pitch copy, and a brand-outline `Switch to annual — $84/year` button that posts to `/billing/checkout` with `plan=personal&billing=annual` so a trial-end user who's already decided to convert lands on the higher-ARPU SKU at the very moment they're most price-sensitive (the original monthly continue CTA stays — annual is additive, not a replacement, so a final-window user still has both options). `TrialConversionNudge` record extends with `annualMonthlyEquivalent` ($7/$24) and `annualCashAmount` ($84/$288) fields populated by `TrialConversionNudgeService` from the same Plan-keyed switches the existing `monthlyPrice` field uses; `UpgradeModal` record gains the same three fields ($9/$7/$84 for the recommended-Personal default) plus an existing `monthlyPrice` headline so the modal can render both pricing models without a second view-model lookup. `UpgradeModal.fromException` populates the annual fields from the upgrade target so the existing `GlobalExceptionHandler` flash path works unchanged. ~70 lines of new CSS — `.billing-period-toggle` is a pill-shaped flex container with a 4px inset; `.billing-period-toggle-btn-active` lifts to the surface colour with a 1px box-shadow; `.billing-period-toggle-badge` is the same green token (`#047857` on `rgba(16, 185, 129, 0.15)`) as `/pricing`'s `.save-badge` so the value frame is visually consistent across the three surfaces (pricing toggle, register auth-badge, in-app modal); `.trial-nudge-annual` brand-dashed divider + `.modal-cta-annual` brand-outline secondary button visually subordinate the annual upsell to the primary continue CTA. Dark-mode block extended with matching tints for the toggle background + the active state + the badge so the modal reads correctly on either theme. Also fixed two pre-existing Thymeleaf nested-expression bugs that the new full-Spring rendering tests exposed: the upgrade-modal `<h2>` and trial-nudge `<h2>` titles both nested `${...}` inside another `${...}` ternary (template parse error the moment either modal actually rendered) — neither had been exercised end-to-end before because the existing `ThreadControllerTest` uses standalone MockMvc + `InternalResourceViewResolver` and never runs the real Thymeleaf engine; rewrote both titles using either a single outer expression (upgrade modal) or sibling `th:if` spans (trial nudge), which are also easier to read. 5 new + updated tests in `ThreadInboxRenderingIntegrationTest` and `TrialConversionNudgeServiceTest` (`upgradeModalExposesMonthlyAnnualToggleAndAnnualBilledAsLine` — real-Thymeleaf render asserts `.billing-period-toggle`, both `data-billing-period` buttons, `#upgrade-modal-billing` hidden input, the "$7/mo · billed annually as $84" cash sub-line, and the "2 months free" badge are all in the rendered markup; `trialNudgeWithinThreeDaysShowsAnnualSwitchCta` — daysLeft=2 nudge renders `.trial-nudge-annual`, "Save 2 months by switching to annual today" copy, "Switch to annual — $84/year" CTA, AND the `billing=annual` hidden input form-post target; `trialNudgeBeyondThreeDaysOmitsAnnualSwitchCta` — daysLeft=5 nudge does NOT render the annual block so we don't pre-empt the customer's own decision earlier in the trial; `TrialConversionNudgeServiceTest` Personal+Team cases extended to assert the new `$7`/`$84` and `$24`/`$288` fields populate + `inAnnualUpsellWindow()` returns true). `./mvnw test` → 500 tests pass (1 Docker-only test skipped as before).
Advances: EPIC-11 Annual billing surfacing — Milestone 3 (Annual switch from in-app upgrade + trial nudge + billing portal). Billing-portal annual swap is the default Stripe Billing Portal behaviour — verifying the price-swap toggle is enabled is a Stripe-Dashboard task captured under the existing portal MASTER_ACTIONS entry.
Master action: none

## 2026-06-08
Shipped: Annual savings copy + value framing on `/pricing` and `/register` — the toggle badge now reads "2 months free" instead of "Save 16%" (months-free framing is how SaaS pricing comparison sites describe annual discounts and is a stronger anchor than a percentage), each paid plan card grows a brand-coloured `Billed annually as $X` cash-amount sub-line that the pricing JS reveals only when the Annual tab is active (`$84` Personal, `$288` Team, `$996` Enterprise — derived directly from the displayed annual-equivalent $7/$24/$83 × 12 so the arithmetic is transparent), and `/register?plan=…&billing=annual` now renders a brand-blue `.auth-billing-badge` with "Annual billing · 2 months free" inside the auth card so a user who picked Annual on `/pricing` sees the choice acknowledged on the signup screen and doesn't lose context across the page transition. Pricing meta description swaps "Save 16% with annual billing" for "Choose annual billing and get 2 months free" to match the new value frame in search snippets / Slack unfurls. JS update: `pricing.html`'s existing `applyPeriod(period)` now iterates `.plan-annual-cash[data-annual-cash]` alongside the price-swap loop, filling each element's textContent from its `data-annual-cash` attribute and toggling an `is-visible` class on Annual, clearing both on Monthly. CSS: `.plan-annual-cash` ships `display: none` by default so the sub-line stays hidden until the toggle activates (and stays hidden entirely for visitors with JS disabled — they see the monthly price and the CTA, which still hits the correct SKU per Milestone 1); `.is-visible` flips it to `display: block`. `.auth-billing-badge` is a flex card with a low-alpha brand-blue background + 30%-alpha brand-blue border, "Annual billing" in brand-blue on the left and a green "2 months free" pill on the right — same green token (`#047857` on `rgba(16, 185, 129, 0.15)`) the pricing-toggle `.save-badge` uses so the value frame is visually consistent across the two surfaces. Badge only renders when `billing == 'annual'` AND `plan` is `personal` or `team` — free signups and explicit monthly signups must NOT show it because it would mis-frame the price the user is about to commit to. 4 new tests in `PublicPageSeoIntegrationTest`: `pricingPageFramesAnnualBillingAsTwoMonthsFreeWithCashAmount` (asserts the "2 months free" toggle copy, all three `data-annual-cash` cash amounts in the rendered markup, and the `.plan-annual-cash` class hook that the toggle JS keys on; also asserts the old "Save 16%" copy is gone so a future copy-tweak that brings it back fails CI), `registerPageAcknowledgesAnnualChoiceWhenBillingParamIsAnnual` (asserts `.auth-billing-badge`, the "Annual billing" + "2 months free" labels, and that hidden `name=plan` + `name=billing` round-trip the `personal` + `annual` values so the POST hits Stripe with the annual price ID), `registerPageOmitsAnnualBadgeWhenBillingParamAbsent` (asserts the badge is absent on default `/register` AND on `/register?plan=personal&billing=monthly` so a monthly signup doesn't see annual framing). `./mvnw test` → 497 tests pass (1 Docker-only test skipped as before).
Advances: EPIC-11 Annual billing surfacing — Milestone 2 (Annual savings copy + value framing on `/pricing` and `/register`).
Master action: none

## 2026-06-07
Shipped: Annual SKU plumbed end-to-end through `/pricing` → `/register` → Stripe Checkout, opening EPIC-11 Annual billing surfacing. The pricing page already shipped a Monthly|Annual toggle that visually swapped the displayed price but routed CTAs through the monthly Stripe price ID regardless — annual was cosmetic. This ship makes the toggle real: a customer who clicks Annual on `/pricing` lands in Stripe Checkout against the **annual** Personal/Team SKU, and the local `Subscription` row records the annual `stripe_price_id`. Adopts EPIC-11 as the new Primary Objective (resolves the [PLAN-REVIEW] from last session — annual billing is the direct ARPU lift, doesn't depend on Google OAuth credentials, and the same Stripe master action already lists annual prices as required). New `com.emailmessenger.billing.BillingPeriod` enum (MONTHLY / ANNUAL) with a lenient `parse(String)` that defaults to MONTHLY for null / blank / unknown / tampered input — a 500 mid-checkout strands a customer worse than a silent monthly fallback, so the parse never throws (verified explicitly: `parse("quarterly")` and `parse("'; DROP TABLE users--")` both return MONTHLY). `BillingProperties` grows three env-overridable fields — `personal-annual-price-id` / `team-annual-price-id` / `enterprise-annual-price-id` wired to `STRIPE_PERSONAL_ANNUAL_PRICE_ID` / `STRIPE_TEAM_ANNUAL_PRICE_ID` / `STRIPE_ENTERPRISE_ANNUAL_PRICE_ID` in both dev and prod profiles — and `priceIds(BillingPeriod period)` returns the period-correct EnumMap; the old no-arg `priceIds()` is gone (no backwards-compat shim per CLAUDE.md). `BillingService.startCheckout(User, Plan, BillingPeriod)` resolves the right price ID, and crucially degrades ANNUAL→MONTHLY when the annual SKU isn't configured for that plan — so a customer who clicked Annual still gets a working monthly Stripe Checkout instead of a 500, and the operator can wire annual prices one plan at a time. `/billing/checkout` reads a `billing` request param, `BillingPeriod.parse`s it, and passes it to the service. `PlanCheckoutSuccessHandler` reads the `billing` form param at login so a `/login?plan=personal&billing=annual` carry-through (e.g. a user who picked Annual on /pricing then bounced through /login) still ships them to the annual checkout. `AuthController` reads `billing` on both `GET /register` (so the registration page can hidden-field it back through the form), `POST /register` (so checkout-after-signup hits the right SKU), and `GET /login` (so the login form preserves it). `pricing.html`'s existing toggle JS now also rewrites the four `.plan-cta[href]` hrefs to add or strip `?billing=annual` whenever the user clicks Annual/Monthly — handles the case where the href already has a `?utm_source=…` query string (appends `&billing=annual`) and the case where it doesn't (appends `?billing=annual`). The strip side uses a regex that tolerates `billing=annual` anywhere in the query string so re-clicking Monthly cleans up the URL without leaving `?billing=monthly` litter behind. `register.html` and `login.html` round-trip `billing` as a hidden input alongside the existing `plan` hidden input, and the "Sign in" / "Create one" cross-link includes `billing` so a user bouncing between /register and /login doesn't lose the annual choice. `application.yml` adds the three annual env-var bindings in both `dev` and `prod` profile blocks. `MASTER_ACTIONS.md` updates the Stripe action to list all six required price IDs (3 monthly + 3 annual) with their exact env-var names, and notes that annual IDs are optional because of the silent monthly degradation path. PLAN.md adopts **EPIC-11 Annual billing surfacing** as the new Primary Objective with four milestones — annual SKU plumbed + pricing toggle wired (this ship), annual savings copy + value framing, annual switch from in-app upgrade modal + trial nudge + billing portal, subscription `billing_period` field + active-cadence display on `/account`. BACKLOG seeded with the three remaining milestones. 14 net new tests + the pre-existing `startCheckout` test sites adapted to the new 3-arg signature: `BillingPeriodTest` x8 (parse "annual"/"monthly"/"ANNUAL"/whitespace/null/blank/unknown — including SQL-injection-shaped strings — all behave per spec; `paramValue()` returns lowercase); `BillingServiceTest` x3 new (annual checkout uses annual price ID and persists it on `Subscription.stripePriceId`; annual falls back to monthly when annual not configured for the plan; null period defaults to monthly); `BillingControllerTest` x1 new (`?billing=annual` passes `BillingPeriod.ANNUAL` to `startCheckout`); `AuthFlowIntegrationTest` x2 new (register with `plan=personal&billing=annual` calls `startCheckout(_, PERSONAL, ANNUAL)`; login with the same params does the same so a cross-flow user still hits the annual SKU). `./mvnw test` → 494 tests pass (1 Docker-only test skipped as before).
Advances: EPIC-11 Annual billing surfacing — Milestone 1 (Annual SKU plumbed end-to-end + pricing toggle wired to CTA). New Primary Objective: turn the cosmetic Monthly|Annual toggle on `/pricing` into a real ARPU lift across the whole checkout funnel.
Master action: existing Stripe action updated to list six price IDs (3 monthly + 3 annual) with explicit env-var names; annual SKU creation is optional for first-launch but unlocks the full ARPU lift once wired.

## 2026-06-07
Shipped: Web app manifest + PWA brand-mark icons + theme color — MailIM is now installable on iOS Safari and Android Chrome from any public page or `/threads`, opening EPIC-10 Mobile / PWA. New `PwaController` in `com.emailmessenger.web` serves `GET /manifest.webmanifest` (`application/manifest+json`, `name`/`short_name`/`description`/`start_url=/threads`/`scope=/`/`display=standalone`/`orientation=portrait`/`theme_color=#4f80ff`/`background_color=#0f172a`/`lang=en`/`dir=ltr`/`icons[]`) plus four dynamically-rendered icon endpoints: `GET /icons/icon-192.png` (Chrome install prompt requirement), `GET /icons/icon-512.png` (Android home screen splash), `GET /icons/icon-512-maskable.png` (Android adaptive-icon safe-zone variant — full-bleed brand-blue square with the glyph shrunk into the inner 64% so circular/squircle/rounded masks don't crop the bubble), and `GET /apple-touch-icon.png` (180×180, the size iOS Safari pins to the home screen). Icons share a `renderBrandIcon(int size, boolean maskable)` Java 2D primitive that draws a brand-blue rounded-square (or full square in maskable mode), a white speech-bubble glyph with a tail in the lower-left to read as the IM-bubble metaphor at favicon scale, and a "MI" wordmark in brand-blue centered in the bubble. `fragments/seo.html` (the SEO meta fragment every public page already includes — `/`, `/pricing`, `/login`, `/register`, `/demo`, `/privacy`, `/terms`, `/refund`) gains `<link rel="manifest">` + `<meta name="theme-color">` + `<link rel="apple-touch-icon">` + `<link rel="icon" type="image/png" sizes="192x192">` + `apple-mobile-web-app-capable` / `apple-mobile-web-app-title` / `apple-mobile-web-app-status-bar-style=black-translucent` so iOS Safari recognises MailIM as a fullscreen-eligible web app. `threads.html` and `conversation.html` (which both don't use the SEO fragment because they're noindex) get the same six tags inline so a signed-in user who lands on `/threads` from a magic email link still sees the Chrome install prompt. `SecurityConfig` permitAll list extends with `/manifest.webmanifest`, `/icons/**`, `/apple-touch-icon.png` so browsers can fetch them before the user signs in. PLAN.md transitions Primary Objective from EPIC-09 Account self-serve (code-complete on all four milestones last session and verified end-to-end here) to **EPIC-10 Mobile / PWA** with four milestones — manifest + icons + theme color (this ship), service worker for offline shell, in-app install prompt with iOS Add-to-Home-Screen instructions, mobile-tuned 375px-viewport audit of `/threads` + `/conversation`. BACKLOG seeded with the three remaining milestones. 10 new tests: `PwaControllerTest` x7 (manifest content-type + start_url + standalone display + theme_color; manifest advertises all three required icon sizes (192/512/512-maskable); each of the four PNG endpoints returns a valid PNG with the correct dimensions (192×192, 512×512, 512×512 maskable, 180×180 apple-touch); maskable variant fills (0,0) opaquely vs. standard variant which is transparent at the corner so Android adaptive masks don't expose holes); `PublicPageSeoIntegrationTest` x3 (landing page advertises `<link rel=manifest>` + theme-color + apple-touch-icon + apple-mobile-web-app-capable; `/manifest.webmanifest` is reachable through the full Spring filter chain without auth and contains `start_url:/threads` + the icon paths; all four PNG endpoints reachable without auth so the install banner can render before signup). `./mvnw test` → 471 tests pass (1 Docker-only test skipped as before).
Advances: EPIC-10 Mobile / PWA — Milestone 1 (Web app manifest + PWA icons + theme color).
Master action: none

## 2026-06-07
Shipped: Login throttling + auth audit log close out EPIC-09 — `/login` now refuses further attempts for an email or IP that has failed 5 times in the last 15 minutes (configurable via `auth.throttle.max-failures` / `auth.throttle.window-minutes`), and `/account` grows a "Recent account activity" panel listing the last 10 sign-ins, lockouts, password changes, email changes, and reset completions with timestamp + IP. V16 adds `auth_events` (id, user_id FK nullable, email VARCHAR(254), event_type VARCHAR(32), ip_address VARCHAR(45) nullable, created_at) with three indexes — `(user_id, created_at)` drives the per-user panel; `(email, created_at)` and `(ip_address, created_at)` drive the throttle counts. New `com.emailmessenger.domain.AuthEventType` enum (LOGIN_SUCCESS / LOGIN_FAILURE / ACCOUNT_LOCKED / PASSWORD_CHANGED / EMAIL_CHANGED / PASSWORD_RESET_COMPLETED) and `AuthEvent` JPA entity. New `AuthEventService.record(user, email, type, ip)` normalizes the email (trim + lowercase + 254-char clamp), resolves the user from the email when the caller doesn't have one (so a LOGIN_FAILURE on a known address still links to the row that powers `/account`), and writes one row; `recordForEmail(email, type, ip)` is the unknown-user shortcut. `LoginThrottleService.isLocked(email, ip)` counts LOGIN_FAILURE rows in the sliding window for each key independently, so a credential-stuffing pass against one address from rotating IPs trips the email cap and a single attacker spraying many addresses from one IP trips the IP cap. New `LoginThrottleFilter extends OncePerRequestFilter` short-circuits `POST /login` to `redirect:/login?error=locked` before `UsernamePasswordAuthenticationFilter` ever sees the credentials; `SecurityConfig` registers it via `addFilterBefore` plus a `FilterRegistrationBean<>(filter); setEnabled(false)` so Spring Boot's servlet-chain auto-registration doesn't run it twice per request. New `ClientIp` helper prefers the leftmost `X-Forwarded-For` value (so a reverse-proxy deploy records the original caller) and falls back to `remoteAddr`; clamps to 45 chars for the IPv6 column. New `AuthFailureListener` consumes `AbstractAuthenticationFailureEvent`, writes the LOGIN_FAILURE row, then re-checks the throttle and writes a paired ACCOUNT_LOCKED row when the threshold has just been crossed so the lockout shows up in the user's activity panel instead of appearing out of nowhere. `LoginAuditListener` extended to also write LOGIN_SUCCESS alongside the existing `users.last_login_at` stamp. `AccountService` now records PASSWORD_CHANGED on `changePassword` OK and writes two EMAIL_CHANGED rows on `changeEmail` OK (one against the previous address, one against the new — so the activity panel shows the change against both keys); `PasswordResetService.consumeToken` records PASSWORD_RESET_COMPLETED. `AccountController` exposes `recentActivity` (`authEventService.recentFor(user, 10)`) and `account.html` renders an `.account-activity` `<ul>` with human-readable labels per `AuthEventType`, ISO timestamp, and monospace IP — empty state copy when the list is empty. `login.html` adds a `param.error == 'locked'` alert ("Too many failed sign-in attempts. Please wait a few minutes…"). 18 new tests: `AuthEventServiceTest` x5 (`record` persists with normalized email + user link + IP + timestamp; `recordForEmail` resolves known users and leaves unknown ones null; blank email skip; `recentFor` returns newest-first capped at the limit; `recentFor` returns empty for null/unsaved user); `LoginThrottleServiceTest` x7 (fresh email not locked; max failures inside window for email locks; max-1 failures doesn't lock; failures outside window don't lock; IP threshold locks regardless of email; email normalization matches across case; blank inputs return false); `LoginThrottleIntegrationTest` x4 boots full Spring + MockMvc (failed login records LOGIN_FAILURE; successful login records LOGIN_SUCCESS; 5 failures lock subsequent attempts even with the correct password + ACCOUNT_LOCKED row recorded; 5 failures from one IP across different emails lock a fresh email from the same IP); `LoginAuditListenerTest` extended (now also verifies `authEvents.recordForEmail(email, LOGIN_SUCCESS, ip)` runs on success and never on anonymous). `./mvnw test` → 461 tests pass (1 Docker-only test skipped as before). PLAN.md milestone 4 marked Shipped in BACKLOG; EPIC-09 Account self-serve is now code-complete on all four milestones — proposing **EPIC-10 Mobile / PWA** (responsive `/threads` + `/conversation` layouts already exist on desktop; ship a web-app manifest + service worker + install-prompt so a paying user can pin MailIM to the iOS/Android home screen and read threads offline, since the current desktop-only UX caps the natural Personal-tier "check it on my phone" upgrade path) as the next Primary Objective; alternatives in MASTER_ACTIONS.md are annual-billing surfacing or Google OAuth signup.
Advances: EPIC-09 Account self-serve — Milestone 4 (Login throttling + auth audit log). EPIC-09 now fully shipped; next Primary Objective proposed above pending [PLAN-REVIEW].
Master action: [PLAN-REVIEW] EPIC-09 Account self-serve is code-complete — adopt EPIC-10 Mobile / PWA (manifest + service worker + install prompt) as the next Primary Objective and update PLAN.md, or pick one of the listed alternatives.

## 2026-06-07
Shipped: In-app `/account` page lets a signed-in MailIM user change their password and email address without contacting support — closes the third milestone of EPIC-09 Account self-serve and removes the last "ticket support" path on the paid-tier UX (a user who wants to swap to a work email or rotate a leaked password now does it inline, instead of churning when the only available answer was "we can't help with that"). New `AccountService` in `com.emailmessenger.auth` owns both flows: `changePassword(user, currentPassword, newPassword)` does a length-then-bcrypt-match validation pair (length first so a too-short submission short-circuits the 100ms bcrypt check; current-password match second so the gate is identical to the one Spring Security uses on login), bcrypts the new password, persists the user, then calls `PasswordResetTokenRepository.markAllUsedFor(user, now)` AND `EmailVerificationTokenRepository.markAllUsedFor(user, now)` so any in-flight reset URL or stale verification link can't be replayed against the new credentials; `changeEmail(user, currentPassword, newEmail)` runs current-password first (so a wrong-password attempt never reveals whether the target address exists), then `UserService.normalizeEmail` (trim + lowercase) + a tight `looksLikeEmail` check (single `@`, non-empty local/domain, ≤254 chars — RFC-5321 cap), then a same-as-current short-circuit returning `NO_CHANGE` (so resubmitting your own address isn't punished as a "taken" error), then `users.existsByEmail` to catch the would-clobber-another-account case, then swaps the email + sets `email_verified_at = null` + revokes both token tables + calls `emailVerificationService.sendVerification` to dispatch a fresh confirmation mail to the new address — same code path as the signup-side flow so the new address gets the same 24h TTL / SHA-256-only-at-rest token shape as a brand-new signup. Outcomes are exposed as two enums (`PasswordChangeOutcome` = OK / CURRENT_INCORRECT / NEW_TOO_SHORT; `EmailChangeOutcome` = OK / CURRENT_INCORRECT / EMAIL_INVALID / EMAIL_TAKEN / NO_CHANGE) so the controller can flash a precise message without leaking internal exceptions. New `AccountController` in the same package exposes `GET /account` (renders the page with current email + display name + verified/unverified badge), `POST /account/password` (flashes `passwordOutcome` and redirects back), `POST /account/email` — on `OK` clears the `SecurityContextHolder` and invalidates the HTTP session, then redirects to `/login?email-changed` so the user must re-authenticate as the new address (the session principal still points at the old username and any further request would 500 on `requireByEmail`); on any other outcome flashes `emailOutcome` and redirects back to `/account`. New `User.setEmail(String)` setter — the entity already had `setPasswordHash` / `setEmailVerifiedAt` / etc., so this matches the rest of the mutable field surface. No new Flyway migration: the `users` table has had `email` mutable since V2 and `email_verified_at` since V15. No `SecurityConfig` change either — `/account` and `/account/*` fall through to the existing `anyRequest().authenticated()` matcher, the existing CSRF filter covers both POSTs (the password POST + email POST both hidden-field a `_csrf` token in the form). `threads.html` header nav gains an `Account` link between Mailboxes and the Billing/Sign-out forms. `login.html` adds a `param['email-changed']` success banner ("Email updated. Sign in with your new address — we've sent a confirmation link there.") so a freshly-logged-out post-email-change user lands on a screen that explains what just happened instead of a generic `/login`. New `templates/account.html` standalone page with three `account-card` blocks (Profile summary; Change password; Change email) sharing the existing `auth-form` / `auth-label` / `auth-submit` styles, each form CSRF-armed and each surfacing its outcome flash through a precise `th:if="${passwordOutcome == 'OK'}"` / `th:if="${emailOutcome == 'EMAIL_TAKEN'}"` etc. matcher so the user only ever sees one alert at a time. ~85 lines of namespaced CSS (`.account-page` 560px max-width, `.account-card` stacks with 20px gap, `.account-summary` two-column dl that collapses to one column under 520px, `.account-verified-badge` green pill + `.account-unverified-badge` amber pill that visually echo the colour language already in use on the `/threads` verify-email banner, all reusing `--surface`/`--border`/`--text`/`--brand` so dark mode picks up automatically). Tests: 12 new `AccountServiceTest` cases (password: correct-current swaps bcrypt + new matches; wrong-current returns CURRENT_INCORRECT + hash unchanged; new-too-short returns NEW_TOO_SHORT + hash unchanged; OK revokes outstanding reset + verification tokens via `usedAt` stamp; email: correct-current + valid-new swaps address + nulls email_verified_at + sends verification mail; normalises whitespace + case to `mixed.case@example.com`; wrong-current returns CURRENT_INCORRECT + address unchanged + no mail; same-as-current returns NO_CHANGE + no mail; existing-address returns EMAIL_TAKEN + address unchanged + no mail; not-an-email returns EMAIL_INVALID + no mail; OK revokes outstanding reset + verification tokens; OK dispatches MimeMessage to the new address with the correct "Confirm your MailIM email address" subject); 10 new `AccountControllerTest` cases full-Spring `@AutoConfigureMockMvc` (anonymous `GET /account` → 302 to `/login`; signed-in `GET /account` → 200 + view=`account` + model carries `currentEmail` / `displayName` + body contains the email; happy-path password POST flashes `passwordOutcome=OK` + redirects to `/account` + bcrypt matches new password on disk; wrong-current password POST flashes `CURRENT_INCORRECT` + hash unchanged; short-new password POST flashes `NEW_TOO_SHORT`; missing-CSRF password POST → 403; happy-path email POST clears session + redirects to `/login?email-changed` + user's email + email_verified_at + verification mail all correct; wrong-current email POST flashes `CURRENT_INCORRECT` + email unchanged; taken email POST flashes `EMAIL_TAKEN`; invalid-format email POST flashes `EMAIL_INVALID`). `./mvnw test` → 445 tests pass (1 Docker-only test skipped as before). PLAN.md milestone 3 marked Shipped in BACKLOG; one open item remains (Milestone 4 login throttling + auth audit log).
Advances: EPIC-09 Account self-serve — Milestone 3 (In-app `/account` change password + change email).
Master action: none.

## 2026-06-06
Shipped: Email verification on signup — a newly-registered MailIM user now receives a tokenised confirmation email within seconds of finishing `/register`, an unverified-account banner on `/threads` ("Confirm your email to keep your MailIM account. Check your inbox for the link we sent on signup, or resend it below." + Resend link button) keeps the prompt visible until they click through, and `GET /verify-email?token=…` flips `users.email_verified_at` end-to-end. Closes the email-verification half of EPIC-09 — without it any signup could use a typo'd / stolen / disposable address, the existing password-reset flow doubled as a free account-takeover vector (request reset on someone else's address that nobody actually owns), and the digest + re-engagement scheduled mail were happily delivering to unconfirmed addresses. V15 Flyway migration adds `users.email_verified_at TIMESTAMP` (nullable) and backfills every pre-existing row to `CURRENT_TIMESTAMP` so the developers and any pre-1.0 account aren't suddenly slapped with a banner they have no way to clear; new rows from `UserService.register` leave the column NULL until the user verifies. Same migration creates `email_verifications` (id PK, user_id FK, token_hash VARCHAR(64) UNIQUE, expires_at TIMESTAMP NOT NULL, used_at TIMESTAMP nullable, created_at) with a `(user_id)` index — same shape as `password_reset_tokens` so the surface and ops guarantees are identical (single-use, expiring, hash-only-at-rest). New `EmailVerificationToken` JPA entity + `EmailVerificationTokenRepository` (`findByTokenHash`, `markAllUsedFor(user, ts)` `@Modifying` JPQL that revokes every outstanding row for a user in one round-trip — so a stale verification email can't be replayed against an already-confirmed account). New `EmailVerificationService` in `com.emailmessenger.auth` mirrors `PasswordResetService`: `sendVerification(User)` no-ops on null / already-verified users (so duplicate resends don't mint a second live token), mints a 32-byte `SecureRandom` URL-safe base64 plaintext, stores `sha256Hex(plaintext)` so a DB dump can't be replayed, emails through the existing `JavaMailSender` with a 24h TTL (longer than password-reset's 1h because verification mail commonly sits unread for a day and the only thing that gates on verification is non-destructive — soft banner + future paid-feature unlocks); `verify(plain)` consumes a token (rejects missing / used / expired), stamps `email_verified_at` if not already set, and calls `markAllUsedFor` so all sibling tokens for the same user die at once; `MailException` is logged and swallowed so a flaky SMTP relay doesn't surface as a 500 to a user who just signed up. New `User.emailVerifiedAt` field + `isEmailVerified()` helper + setter. New `EmailVerificationController` exposes `GET /verify-email?token=…` (public — a user might verify before signing back in; renders `verify-email.html` with `status=verified` or `status=invalid` for missing / used / expired tokens) and `POST /verify-email/resend` (authenticated, RedirectAttributes flash key `verifyFlash` populated with `resent` / `resendFailed` / `alreadyVerified` so the next `/threads` render shows the right banner copy). `AuthController.register` now captures the returned `User` from `userService.register` and calls `emailVerificationService.sendVerification(user)` after a successful register but before `request.login(...)` — keeps the auto-login redirect to `/threads` (or the plan checkout) intact and means the very first time the user lands inside the app the banner is already there. `SecurityConfig` permits `/verify-email` alongside `/password/forgot` / `/password/reset` / the existing public marketing paths; `/verify-email/resend` stays behind the authn wall so it pulls the principal from the security context. `ThreadController` exposes `emailUnverified = !owner.isEmailVerified()` to the model on every `GET /threads`; `threads.html` renders a `.verify-email-banner` (warm amber, distinct from the blue trial banner) above the trial / lockout banners, reads `${verifyFlash}` to swap the lead-in copy ("We just sent a fresh verification link." / "We couldn't send the verification email — try again in a moment." / default), and posts to `/verify-email/resend` with the CSRF token. New `verify-email.html` standalone page reuses the same `auth-card` styling, SEO fragment, and cookie banner as the rest of the auth surface; renders a green success alert + "Back to your inbox" link on `status=verified` and a red error alert + "Sign in" link on `status=invalid`. New `.verify-email-banner` / `.verify-email-text` / `.verify-email-cta` styles in `main.css`. Tests: 6 `EmailVerificationServiceTest` cases (`@SpringBootTest` + H2 with `@MockBean JavaMailSender` returning a stubbed `MimeMessage((Session)null)` — fresh user → mail sent + MimeMessage To/Subject ("Confirm your MailIM email address") correct + body contains `/verify-email?token=…` + DB row stores `sha256Hex(plain)` not the plaintext + `expires_at > now + 23h` to honour the 24h TTL with CI slack; already-verified user → no mail + no row; `verify` flips `email_verified_at` and stamps `used_at`; consuming the first of two outstanding tokens revokes the second via `markAllUsedFor`; manually-aged-out token rejected by `verify` without flipping the user or burning the row; unknown plaintext rejected); 6 `EmailVerificationControllerTest` cases full-Spring `@AutoConfigureMockMvc` (registration triggers a verification email — saved user has `email_verified_at == null`, one token row exists, `mailSender.send` was called; `GET /verify-email?token=…` valid → view=verify-email + model.status=verified + user reloaded shows `email_verified_at != null`; `GET /verify-email?token=garbage` → status=invalid; `GET /verify-email` is public — no auth redirect; signed-in unverified user POSTs `/verify-email/resend` → 302 to `/threads` + one fresh token row written; anonymous POST to `/verify-email/resend` → 302 to the Spring Security `/login` URL). `AuthFlowIntegrationTest` gained a `@MockBean JavaMailSender` + `stubMimeFactory()` `@BeforeEach` so its existing happy-path register tests don't blow up on the new email send-side path. PLAN.md Milestone 2 marked Shipped in BACKLOG; the unfinished BACKLOG list now has 2 items (Milestones 3 and 4 of EPIC-09). `./mvnw test` → 423 tests pass (1 Docker-only test skipped as before).
Advances: EPIC-09 Account self-serve — Milestone 2 (Email verification on signup).
Master action: none (uses the existing `JavaMailSender`; the transactional-email provider API key and `MAIL_HOST` / `MAIL_USER` / `MAIL_PASS` env vars are already tracked under MASTER_ACTIONS.md → OAuth & third-party APIs).

## 2026-06-06
Shipped: Password reset via emailed token — a user who forgot their MailIM password can now recover from `/login` end-to-end: "Forgot your password?" link → `/password/forgot` email-entry form → tokenised reset URL in a plain-text email → `/password/reset?token=…` form sets a new password → redirect to `/login?reset` with success banner ("Password updated. Sign in with your new password."). Closes the largest silent paid-retention hole (a churned user with a forgotten password had no recovery path and just abandoned) and unblocks Stripe live-mode launches (no chargeback path for "locked out of my own paid account"). V14 Flyway migration adds `password_reset_tokens` (id PK, user_id FK, token_hash VARCHAR(64) UNIQUE — SHA-256 hex of the plaintext, expires_at TIMESTAMP NOT NULL, used_at TIMESTAMP nullable, created_at) with a `(user_id)` index for the per-user revoke query. New `PasswordResetToken` JPA entity + `PasswordResetTokenRepository` with `findByTokenHash` and a single-statement `@Modifying` JPQL `markAllUsedFor(user, ts)` that revokes every outstanding row for a user in one round-trip. New `PasswordResetService` in `com.emailmessenger.auth` is the only place that mints / validates / consumes tokens — `requestReset(email)` normalises via `UserService.normalizeEmail`, silently no-ops on unknown / disabled accounts (so the endpoint can't be used to enumerate which emails are registered), mints a 32-byte `SecureRandom` URL-safe base64 plaintext, stores `sha256Hex(plaintext)` so a full DB dump cannot be replayed, and emails through the existing `JavaMailSender`; `findUserForValidToken(plain)` returns the owner when the row exists / isn't used / isn't expired (1-hour TTL via `TOKEN_TTL`), used by the GET form to fail fast before the user types a new password; `consumeToken(plain, newPassword)` bcrypts the new password via the existing `PasswordEncoder`, saves the user, and calls `markAllUsedFor(user, now)` so racing reset emails can't yield two simultaneous login windows. New `PasswordResetController` exposes four unauthenticated endpoints: `GET /password/forgot` (form), `POST /password/forgot` (always renders the same "if we know that email, you'll get a link" status=sent screen regardless of whether the email is known — no enumeration leak), `GET /password/reset?token=…` (validates and renders either the new-password form with the token hidden-fielded through, or status=invalid for missing / used / expired), `POST /password/reset` (rejects passwords <8 chars with error=tooShort while keeping the token alive so the user can retry without re-requesting; on success consumes the token and redirects to `/login?reset`). `SecurityConfig` permits `/password/forgot` and `/password/reset` alongside the existing public marketing/legal/digest opt-out paths. `login.html` gains a "Forgot your password?" link below the sign-in button and a `?reset` success banner; new `password/forgot.html` + `password/reset.html` reuse the existing `auth-card` styling, the shared SEO fragment, and the cookie banner so they look like the rest of the auth surface. Tests: 9 `PasswordResetServiceTest` cases (`@SpringBootTest` + H2 with `@MockBean JavaMailSender` returning a stubbed `MimeMessage((Session)null)` — known user case-insensitively → mail sent + MimeMessage To/Subject correct + body contains `/password/reset?token=…` + DB row stores the hash not the plaintext + expires ~1h out; unknown email → no mail + no row; disabled user → no mail + no row; consume sets new bcrypt hash + flips used_at; consume revokes every other outstanding token for the same user when a second link is consumed first; manually-aged-out token rejected by both findUserForValidToken and consumeToken without burning the row; short password rejected without burning the token; unknown token rejected); 8 `PasswordResetControllerTest` cases full-Spring `@AutoConfigureMockMvc` (forgot GET public; POST unknown email → status=sent + no mail; POST known email → status=sent + mail + one token row; reset GET valid token → form + token hidden + no status; reset GET unknown token → status=invalid; reset POST valid → redirect `/login?reset` + password bcrypt-matches; reset POST short password → error=tooShort + form + token preserved + password unchanged on disk; reset POST invalid token → status=invalid). PLAN.md transitions Primary Objective from EPIC-08 Saved Searches & Reactivation (provably complete across all four milestones in the 2026-06-06 entries below) to **EPIC-09 Account self-serve** (4 milestones: password reset via emailed token / email verification on signup / in-app `/account` change-password + change-email / login throttling + auth audit log); Milestone 1 marked Shipped. MASTER_ACTIONS.md drops the resolved [PLAN-REVIEW] for EPIC-08. `./mvnw test` → 411 tests pass (1 Docker-only test skipped as before).
Advances: EPIC-09 Account self-serve — Milestone 1 (Password reset via emailed token).
Master action: none (uses the existing `JavaMailSender`; the transactional-email provider API key and `MAIL_HOST` / `MAIL_USER` / `MAIL_PASS` env vars are already tracked under MASTER_ACTIONS.md → OAuth & third-party APIs).

## 2026-06-06
Shipped: Weekly saved-search digest email for paid users — a Personal+ user with at least one saved search now receives a single plain-text "what's new in your saved searches" email on Monday 14:00 UTC summarising new threads matching each pinned filter over the last 7 days, with an unauthenticated one-click unsubscribe link in the footer. New V12 Flyway migration adds `digest_email_preferences` (id, user_id UNIQUE FK, opt_out_token VARCHAR(64) UNIQUE, opted_out BOOLEAN, last_sent_at TIMESTAMP) so per-user opt-out state is durable and the operator can audit who got mail when. New `DigestEmailPreference` JPA entity + `DigestEmailPreferenceRepository` (`findByUser`, `findByOptOutToken`). New `WeeklyDigestService` in `com.emailmessenger.digest` (`runDigestCycle()` for the scheduler sweep, `sendDigestFor(User)` for direct invocation) iterates `SavedSearchRepository.findDistinctOwners` so only users with at least one saved search are even considered — skips when `PlanLimitService.currentPlan` resolves to FREE (that's the upgrade hook), lazily provisions a pref row with a UUID-without-dashes token on first send, skips when `optedOut == true`, then for each saved search reuses the same plan-aware repository methods that power the rail (`findByOwnerFiltered` / `findByOwnerAndSender` / `searchIncludingBody` / `search`) capped at `PageRequest.of(0, 5)` per section and floored at `now - 7d` (with the saved search's own preset since-floor honoured if stricter), and aborts without sending when no section has matches so we don't dispatch empty mail. Composition lives in the same service: subject is "MailIM weekly digest — N new thread(s)"; plain-text body greets by `displayName` or "there", lists `* Saved-search name (K new threads)` blocks each with up to 5 `- Subject  <baseUrl>/threads/{id}` rows, then closes with an "Open your inbox" link and the unsubscribe URL built off `SiteProperties.getBaseUrl()`. Delivery goes through the existing `JavaMailSender` (same path as `ReplyService`); per-user `MailException` is logged and swallowed so one bad address doesn't stall the cycle. `last_sent_at` is stamped only on successful sends. New `WeeklyDigestScheduler` (`@ConditionalOnProperty(name="digest.enabled", havingValue="true")` so dev / tests don't fire by default) runs cron `0 0 14 ? * MON` UTC via `@Scheduled` — both knobs externalised as `DIGEST_CRON` / `DIGEST_ZONE`. New `DigestController` (`GET /digest/opt-out?token=…`, unauthenticated) trims the token, looks up via `findByOptOutToken`, flips `opted_out=true` idempotently and renders `digest/opt-out.html` with `status=ok` + the recipient's email; a missing/unknown token renders the same view with `status=invalid` (no signal about whether the token existed, to keep the URL non-enumerable). `SecurityConfig` adds `/digest/opt-out` to the permitAll matcher list alongside the other public marketing/legal paths so the link works without re-auth. `application.yml` gains a `digest:` block (`enabled` / `cron` / `zone`) wired to env-var defaults in both dev and prod profiles. `SavedSearchRepository` gains `findDistinctOwners()` (`SELECT DISTINCT s.owner FROM SavedSearch s`) — no ORDER BY because H2 rejects ORDER BY non-selected columns with SELECT DISTINCT on entity properties. Tests: 6 `WeeklyDigestServiceTest` cases full-Spring with `@MockBean JavaMailSender` returning a stubbed `MimeMessage((Session)null)` (paid user with new matches sends + stamps last_sent_at + provisions pref row with token; Free user skipped + no pref row; pre-opted-out user skipped + token row preserved untouched; paid user with saved searches but no matches in the 7-day window skipped + pref row still provisioned for the next cycle; `runDigestCycle` sweeps owners of saved searches only and counts only the paid send; MimeMessage has the right To/Subject + body contains saved-search name + thread subject + `/digest/opt-out?token=…` footer with the actual token); 4 `DigestControllerTest` cases (`@SpringBootTest @AutoConfigureMockMvc`: valid token flips opted_out + renders status=ok + email; unknown token renders status=invalid + doesn't touch the row; missing token param renders status=invalid; repeated click is idempotent + still status=ok); 2 `WeeklyDigestSchedulerFeatureFlagTest` cases (scheduler bean absent when `digest.enabled=false`; service bean always present). `./mvnw test` → 377 tests pass (1 Docker-only test skipped as before).
Advances: EPIC-08 Saved Searches & Reactivation — Milestone 3 (Weekly digest email of new matches, Personal+).
Master action: none (live mail delivery is already covered by existing MASTER_ACTIONS entries — transactional email provider API key and `MAIL_HOST` / `MAIL_USER` / `MAIL_PASS` env vars; flipping `DIGEST_ENABLED=true` after first live email send is verified is a one-line config flip, not a separate ops item).

## 2026-06-06
Shipped: Saved-search rail rows now carry live match counts + a "new since last visit" badge — the saved-search rail goes from a static bookmark list to a "what's new in each pinned view" surface that pulls the user back the next time something matches one of their saved filters. New V11 Flyway migration adds a nullable `last_viewed_at TIMESTAMP` column to `saved_searches`; new `SavedSearchCountService` in `com.emailmessenger.web` is the single place that turns a `List<SavedSearch>` into `List<SavedSearchView>` with counts populated — it reads the user's plan once via `PlanLimitService.currentPlan`, then for each saved search fans out to the same `EmailThreadRepository` methods the inbox already uses (`findByOwnerFiltered` when there's no query and no sender, `findByOwnerAndSender` when only a sender is set, `search` for Free-plan subject/participant search, `searchIncludingBody` for paid plans so the rail count matches what the user actually sees after clicking through) called as `PageRequest.of(0, 1)` and reading `.getTotalElements()` so the count query runs without materialising rows. For the "new" delta the same dispatch runs with the `since` floor raised to `max(originalSince, lastViewedAt)`, falling back to `s.getCreatedAt()` when `lastViewedAt` is null so a brand-new saved search doesn't surface every historical thread as new — newly created saved searches start with newCount=0 and only light up when fresh matches arrive after the first visit. `SavedSearchView` record gains `matchCount` + `newCount` (and a `hasNew()` helper) replacing the prior `from(SavedSearch)` factory with `withCounts(SavedSearch, long, long)`. `SavedSearchService.viewsFor(owner)` now delegates to the count service; new `SavedSearchService.markViewed(owner, id, when)` looks up the saved search via `findByIdAndOwner` and stamps `lastViewedAt` (no-ops on cross-user or stale ids so a missing record can't blow up an inbox render). `ThreadController` accepts a new optional `s=<id>` query param on `GET /threads`, calls `markViewed(owner, savedSearchId, LocalDateTime.now(clock))` BEFORE building the saved-search views so the just-clicked badge clears in the same response (no flicker round-trip). `threads.html` rail row link now emits `?s=${saved.id()}&q=…&from=…&since=…&unread=…&attachments=…`; the link span renders a brand-tinted `.saved-search-new` pill (only when `hasNew()`) followed by a neutral `.saved-search-count` total with `aria-label` so screen readers say "3 new since last visit" and "12 matching threads" instead of bare numbers. ~30 lines of CSS appended to the existing saved-search block — `.saved-search-count` mirrors the sender-rail count chip with `margin-left:auto` plus active-state brand-tint, `.saved-search-new` is a smaller bolder brand-bg pill with the `.new + .count` sibling selector keeping the two adjacent when both render; dark-mode block extended with matching tints. Tests: 5 new `SavedSearchCountServiceTest` cases full-Spring (`@SpringBootTest` + H2: matchCount equals number of matching threads + newCount falls back to created_at for never-opened rows so pre-existing matches are NOT counted as new; opening once + a fresh thread → newCount=1; Free plan saved-search counts skip body-only matches; Personal plan saved-search counts include body-only matches; empty input short-circuits without loading plan); 2 new `ThreadControllerTest` cases (`?s=42` triggers `markViewed(owner, 42L, now-from-clock)` before counts render; absent `s` param never calls `markViewed`); existing `ThreadControllerTest.savedSearchesAreExposedOnInbox` updated to construct `SavedSearchView` with `matchCount=7L, newCount=2L`. `./mvnw test` → 365 tests pass (1 Docker-only test skipped as before).
Advances: EPIC-08 Saved Searches & Reactivation — Milestone 2 (Saved-search match counts live).
Master action: none.

## 2026-06-06
Shipped: Saved searches end-to-end — Free=1/Paid=unlimited "Saved searches" rail surface on `/threads` and a CSRF-armed `POST /searches` endpoint that captures the active query+sender+chip combination under a user-chosen name, so a paid user can pin "Invoices from Ada, last 30 days, unread" once and revisit it with one click instead of rebuilding the filter every visit. New V10 Flyway migration adds `saved_searches` table (`id`, `owner_id` FK, `name VARCHAR(80)`, `query_text VARCHAR(200)`, `sender_email VARCHAR(254)`, `since_preset VARCHAR(10)`, `require_unread BOOLEAN`, `require_attachments BOOLEAN`, `created_at`, `updated_at`) with a `(owner_id, name)` unique constraint so the same name in two accounts is fine but a single user can't quietly clobber their own saved view + a `(owner_id, created_at DESC)` index for the rail's chronological list query. New `SavedSearch` JPA entity + `SavedSearchRepository` (`findByOwnerOrderByCreatedAtAsc`, `findByIdAndOwner`, `findByOwnerAndName`, `countByOwner`); new `SavedSearchService` in `com.emailmessenger.web` does the create/list/delete flow — trims the name, normalises blanks to nulls on every filter field, lower-cases + whitelists the since-preset against `{7d,30d,90d}` (anything else dropped), refuses a blank name and refuses a save with no filter fields at all (would otherwise persist a "saved everything" row that just links to the bare `/threads`), checks the duplicate-name path BEFORE the plan-cap check so a Free user retrying the same name on the same view gets the form-level "you already have a saved search named X" error instead of the upgrade modal, and only then calls `planLimits.enforceCanCreateSavedSearch(user)`. New `SavedSearchView` rail-row record (`id`, `name`, `query`, `senderEmail`, `sincePreset`, `requireUnread`, `requireAttachments`) carries a `matches(activeQ, activeSender, activeSincePreset, activeUnread, activeAttachments)` method (case-insensitive sender compare, blank-equivalent-to-null elsewhere) so the rail can highlight the saved row whose params match the current URL without duplicating URL-building logic in the template. `PlanLimits` gains a third dimension (`savedSearches`: FREE=1, PERSONAL/TEAM=UNLIMITED, ENTERPRISE=UNLIMITED); new `PlanLimitKind.SAVED_SEARCH_COUNT`; new `PlanLimitService.enforceCanCreateSavedSearch(User)` mirrors the existing `enforceCanCreateThread` / `enforceCanCreateMailbox` shape and throws `PlanLimitExceededException` with the new kind so the existing `GlobalExceptionHandler` redirect-to-`/threads`-with-`upgradeModal` flash path lights up unchanged. New `SavedSearchController` (`POST /searches` / `POST /searches/{id}/delete`) takes the form params + the current active query string, calls the service, flashes `savedSearchMessage` or `savedSearchError` (or lets `PlanLimitExceededException` propagate to the upgrade modal), and builds the redirect URL via `UriComponentsBuilder` so the user lands back on the same filter view they just saved instead of being kicked to a blank inbox. `ThreadController` now takes `SavedSearchService` via constructor injection and exposes `savedSearches: List<SavedSearchView>` + `hasActiveSearchToSave: boolean` on every `/threads` GET so the template can render the "Save this search" form only when there's actually something to save (any of q/from/since/unread/attachments active). `threads.html` upgrade modal gets a third body branch on `kind == 'SAVED_SEARCH_COUNT'` ("Save more searches on Personal" headline + "Free includes 1 saved search…" sub); the `.sender-rail` visibility condition widens to OR in the saved-searches list so a user with one saved search + zero senders still sees the rail; a new `.saved-search-rail` `<details>` block renders above the senders rail with the star-glyph row, the per-row delete `<form>` (CSRF-armed, `confirm()` JS, hidden inputs round-trip the active query string back), and active-row brand-tint based on `SavedSearchView.matches(...)`; a "Save this search" form (`.save-search-form` brand-dashed band) appears above the inbox-search bar when `hasActiveSearchToSave` is true, with a name input and hidden inputs carrying the current query/from/since/unread/attachments. Flash messages (`.saved-search-flash-success` / `-error`) render above the save form. ~120 namespaced CSS lines (`.saved-search-row(-active)`, `.saved-search-link`, `.saved-search-icon`, `.saved-search-name`, `.saved-search-delete*`, `.save-search-form`, `.save-search-name-input` with brand focus ring, `.saved-search-flash*`) reusing the existing `--surface`/`--border`/`--brand`/`--text` tokens with `<560px` column-stack + dark-mode tints throughout. Tests: 6 new `SavedSearchRepositoryTest` cases (persist + chronological list; owner scoping + count; `findByIdAndOwner` refuses cross-user; `findByOwnerAndName` is exact-case; `(owner, name)` unique constraint throws `DataIntegrityViolationException`; same name across owners is fine); 10 new `SavedSearchServiceTest` cases (create trims + lowercases preset; blank name throws; no-filter-at-all throws; unknown preset like `evil_payload` silently dropped; Free second-save throws `PlanLimitExceededException` with `SAVED_SEARCH_COUNT`; Personal can save many; duplicate-name throws specialised exception; duplicate-check fires BEFORE cap-check so a Free dup retry doesn't double-error; delete refuses cross-user id via `NoSuchElementException`; `viewsFor(...)` round-trips through `SavedSearchView.matches(...)`); 8 new `SavedSearchControllerTest` cases full-Spring (`@SpringBootTest` + `MockMvc`: anonymous→login; missing CSRF→403; happy-path persists + redirects back to `/threads?q=invoice&from=ada@example.com&since=30d&unread=true` + flashes savedSearchMessage; Free second-save flashes `upgradeModal` via the global handler; Personal user persists past the Free cap; duplicate name flashes savedSearchError without inserting; no-filter-fields flashes error; delete removes + flashes + redirects; cross-user delete 404s); 4 new `PlanLimitServiceTest` cases (under-cap pass; at-cap throw with full exception fields; paid no-op even at 3 saved; `PlanLimits.savedSearches() == 1` on Free); 2 new `ThreadControllerTest` cases (`savedSearches` model attr surfaced; `hasActiveSearchToSave` flips true when any of q/from/since/unread/attachments is non-blank). Existing `ThreadControllerTest` constructor + setup updated for the new dependency. PLAN.md transitions Primary Objective from EPIC-07 Inbox Search & Discovery (provably code-complete across all 4 milestones per 2026-06-05 entries below) to **EPIC-08 Saved Searches & Reactivation** (4 milestones: save current search + rail / live match counts / weekly digest email Personal+ / re-engagement email after inactivity); Milestone 1 marked Shipped. `./mvnw test` → 358 tests pass (1 Docker-only test skipped as before).
Advances: EPIC-08 Saved Searches & Reactivation — Milestone 1 (Save the current search; rail surface). New Primary Objective: turn the search infrastructure into a retention loop so paid users get pinned to recurring views and Free users have a third concrete upgrade trigger.
Master action: none.

## 2026-06-06
Shipped: Filter chips (date range, unread, has-attachment) on `/threads` — a `.filter-chips` row above the inbox list ANDs onto the active search/sender filter so a paid user can drill to "everything from Ada in the last 30 days with an attachment" via three clicks, and the URL is bookmarkable. New V9 Flyway migration adds `email_threads.unread BOOLEAN NOT NULL DEFAULT TRUE` (indexed by `(owner_id, unread)`); `EmailThread.addMessage` flips `unread = true` on import / new incoming reply, `EmailThread.markRead()` flips it false. `ThreadViewService.getConversation` becomes `@Transactional` (was readOnly) and calls `thread.markRead()` so JPA dirty-checks the flip on commit — viewing a thread clears the unread dot. New `ThreadFilters` record (`since`, `sincePreset`, `requireUnread`, `requireAttachments`) parses `?since=7d|30d|90d` through an injected `Clock` bean (unknown values silently dropped — a stray crawler-fuzzed `?since=evil_payload` falls through to the no-filter path), plus `?unread=true` / `?attachments=true`. `EmailThreadRepository` gains a new `findByOwnerFiltered` for the no-search-with-chips path and extends every existing search method (`search`, `searchIncludingBody`, `hasBodyOnlyMatch`, `findByOwnerAndSender`) with three filter params plumbed as `AND (:since IS NULL OR t.updatedAt >= :since) AND (:requireUnread = false OR t.unread = true) AND (:requireAttachments = false OR EXISTS (SELECT 1 FROM Message m JOIN m.attachments a WHERE m.thread = t))` — all H2 + Postgres-safe JPQL with no native SQL. `ThreadSearchService.search` accepts the filters, routes blank-query / no-sender requests with active filters through `findByOwnerFiltered` instead of throwing, and the existing tier-gated Free / Personal+ branching threads filters into both `search` and the `hasBodyOnlyMatch` upgrade-nag probe so the Free-tier nag scopes to the same result set as the visible matches. `ThreadController` reads `since`/`unread`/`attachments`, builds the `ThreadFilters` via `ThreadFilters.parse(..., clock)`, exposes it as `activeFilters` on the model, and now takes a `Clock` dependency (the existing `EmailMessengerApplication.clock()` bean). The onboarding card is suppressed when chips are active so a brand-new user filtering an empty inbox doesn't see "Welcome to MailIM". `threads.html` renders the `.filter-chips` row with five chips ("Last 7 days" / "Last 30 days" / "Last 90 days" / "Unread" / "Has attachment") + a "Clear filters" link; each chip URL preserves `?q` and `?from`, and clicking an active chip toggles it off by emitting the URL without that param (`th:href="@{/threads(..., since=${activeFilters.sincePreset() == '7d'} ? null : '7d', ...)}"`). Sender-rail rows, pagination links, search form (hidden inputs), and the sender-pill "Show all senders" link all forward `since`/`unread`/`attachments` so a bookmarked `/threads?q=invoice&from=ada@x&since=30d&unread=true&attachments=true` renders the same result set on reload. Unread threads in the list get a bolder subject + brand-coloured `::before` dot via `.thread-item-unread`; the search-empty state branches a fourth way for "No threads match these filters" when only chips are active. ~65 namespaced CSS lines for `.filter-chips`, `.filter-chip(-active)`, `.filter-chip-clear`, and `.thread-item-unread`, all reusing existing `--surface`/`--border`/`--text`/`--brand` tokens so dark mode picks up automatically. Tests: 7 new `ThreadFiltersTest` cases (7d/30d/90d preset math against a fixed `Clock`, unknown-preset dropped, blank/null handled, unread+attachments propagation, `NONE.isActive()`, case-insensitive presets); 6 new `EmailThreadRepositoryTest` cases (`findByOwnerFilteredWithUnreadOnlyReturnsThreadsWhereUnreadTrue` — uses `markRead` to flip half the inbox; `findByOwnerFilteredWithAttachmentsOnlyReturnsThreadsWithMessagesThatHaveAttachments`; `findByOwnerFilteredWithSinceCutsOffOlderThreads`; `findByOwnerFilteredWithSinceInFutureReturnsNothing`; `searchWithUnreadFilterANDsOntoSubjectMatch`; `searchIncludingBodyWithAttachmentFilterANDsOntoBodyMatch`); 5 new `ThreadControllerTest` cases (ArgumentCaptor-asserts `?unread=true` becomes `ThreadFilters.requireUnread=true`; `?attachments=true` → requireAttachments; `?since=7d` resolves to the fixed-Clock-derived `LocalDateTime`; unknown `?since=evil_payload` is dropped and falls through to `findByOwnerOrderByUpdatedAtDesc`; chip-active suppresses the onboarding checklist); 3 new `ThreadSearchServiceTest` cases (`blankQueryNoSenderButFilterChipActiveRoutesToFindByOwnerFiltered`; `senderFilterAndChipFiltersIntersectThroughFindByOwnerAndSender`; `freeUserQueryAndChipFiltersAllPropagateToRepoCalls`); plus 17 existing repo + 8 existing service + 6 existing controller tests updated to thread the new filter args (mechanical `null, false, false` padding, `ThreadFilters.NONE` for the service mocks). `./mvnw test` → 327 tests pass (1 Docker-only test skipped as before).
Advances: EPIC-07 Inbox Search & Discovery — Milestone 4 (Filter chips: date range, has-attachment, unread). EPIC-07 now fully shipped across all four milestones.
Master action: none

## 2026-06-05
Shipped: Sender-grouped sidebar drill-down on `/threads` — new `?from=<email>` query param filters the inbox to threads with at least one message from that participant, and a sticky left rail lists the user's top senders (avatar + display name + distinct-thread count) so finding "everything from Ada" is one click instead of a scroll. New `EmailThreadRepository.topSenders(User, Pageable) → List<SenderGroupRow>` aggregates `SELECT m.sender.email, MAX(m.sender.displayName), COUNT(DISTINCT m.thread.id) FROM Message m WHERE m.thread.owner = :owner GROUP BY m.sender.email ORDER BY COUNT(...) DESC, m.sender.email ASC` with a Spring Data interface projection; `findByOwnerAndSender(User, String, Pageable)` provides the sender-only filter path; the three existing search methods (`search`, `searchIncludingBody`, `hasBodyOnlyMatch`) gain a nullable `senderEmail` param plumbed through `(:senderEmail IS NULL OR EXISTS (SELECT 1 FROM Message ss WHERE ss.thread = t AND LOWER(ss.sender.email) = LOWER(:senderEmail)))` so the body-only nag probe and combined query+sender searches both intersect with the active drill-down. New `SenderGroupService` wraps the repo with a default limit of 8 and maps rows to `SenderGroup(email, displayName, threadCount, label, initials)` records — `label` falls back to email when displayName is blank, `initials` reuses the same 1–2 char algorithm as `Participant.initials()` (preferring 2 letters from a space-split display name, else 1 from the local part of the email). `ThreadSearchService.search(owner, query, senderEmail, pageable)` is the single composed entry: blank query + sender → `findByOwnerAndSender`; non-blank query → existing tier-gated routing with the sender plumbed through (`search` + `hasBodyOnlyMatch` on Free, `searchIncludingBody` on Personal+); blank query AND null sender throws `IllegalArgumentException` (caller handles the no-filter case via `findByOwnerOrderByUpdatedAtDesc`). `ThreadController` now takes `SenderGroupService` and `@RequestParam("from")` via constructor injection, exposes `senderGroups`, `activeSender`, `hasAnyThreads` as model attributes, suppresses the onboarding checklist when a sender filter is active (a brand-new user with 0 threads filtering wouldn't fit the welcome card), and routes through `threadSearchService` whenever either `q` or `from` is non-blank. `threads.html` wraps the inbox in a `.thread-layout` CSS-grid with a 240px sticky left aside (`top: 72px`, `max-height: calc(100vh - 96px)`, scrolls when long) — `.sender-rail-row` renders avatar + label + count badge, applies `.sender-rail-active` (brand-tinted background + left border) to the matching row, leads with an "Everyone" reset link that clears `from` while preserving `q`, and stacks below the main column at `<760px`; pagination + search-form links round-trip `from` via Thymeleaf URL builders; an active-sender pill above the result list shows the email and a "Show all senders" clear link; the result-count headline branches three ways ("N results for &quot;X&quot;" / "N threads from email" / "Conversations") plus a corresponding three-way empty-state. `.thread-page` `max-width` bumped from 760px → 980px so the sidebar + content both fit without crowding. New CSS: ~140 namespaced lines for `.thread-layout(-with-rail)`, `.thread-main`, `.sender-rail*` (sticky positioning, list-style-none summary, rotated chevron when `details` is closed), `.sender-rail-avatar(-everyone)` gradient circles, `.sender-rail-count` pill badge, `.sender-rail-active` brand-tint, `.sender-pill*`, mobile `<760px` column-stack, and dark-mode tints throughout — all reusing the existing `--surface`/`--border`/`--text`/`--brand` tokens. Tests: 9 new `EmailThreadRepositoryTest` cases (`findByOwnerAndSenderReturnsOnlyThreadsThatIncludeThatSender`, `findByOwnerAndSenderIsCaseInsensitive`, `findByOwnerAndSenderScopedByOwner`, `searchWithSenderFilterIntersectsBothConstraints`, `searchIncludingBodyWithSenderFilterIntersectsBothConstraints`, `hasBodyOnlyMatchWithSenderFilterScopesProbeToThatSender` — proves Grace's body-only match disappears when scoped to Ada, `topSendersReturnsParticipantsOrderedByThreadCount`, `topSendersScopedByOwner`, `topSendersHonorsLimit`); 4 new `ThreadSearchServiceTest` cases (sender-only routes through `findByOwnerAndSender` without hitting plan/search/body queries; Free combined query+sender plumbs sender into both `search` and `hasBodyOnlyMatch`; Personal combined query+sender uses `searchIncludingBody` with sender; blank-query-and-null-sender throws); 5 new `ThreadControllerTest` cases (`senderRailIsExposedOnInbox`, `fromParamRoutesThroughSenderOnlyFilter`, `combinedQueryAndFromGoesThroughSearchServiceWithBoth`, `blankFromParamFallsBackToFullList`, `senderFilterActiveSuppressesOnboardingCard`); 4 new `SenderGroupServiceTest` cases (label fallback to email when displayName missing/blank, default-limit page size, explicit-limit page size, empty-result mapping); 3 new full-Spring `ThreadInboxRenderingIntegrationTest` cases (real Thymeleaf render asserts the sidebar lists both Ada and Grace with Everyone-active highlight; `?from=` renders the pill + "1 thread from …" + scopes the visible threads; combined `?q=&from=` carries the sender in the hidden search-form input). `./mvnw test` → 306 tests pass (1 Docker-only test skipped as before).
Advances: EPIC-07 Inbox Search & Discovery — Milestone 3 (Sender-grouped sidebar drill-down).
Master action: none

## 2026-06-05
Shipped: Full-text body search on `/threads` for Personal+, with a Free-tier upgrade nag when a query matches inside email bodies the user can't see. New `EmailThreadRepository.searchIncludingBody(User, String, Pageable)` extends the existing `search` JPQL with `OR LOWER(COALESCE(m.bodyPlain, '')) LIKE LOWER(CONCAT('%', :q, '%'))` inside the `EXISTS` over `Message m`, so a paid user searching "stripe webhook" surfaces threads where the phrase appears in any message body — not just in subjects or sender names — and the `DISTINCT t` keeps a thread with multiple matching messages from showing twice. New `EmailThreadRepository.hasBodyOnlyMatch(User, String) → boolean` runs a single `SELECT (COUNT(t) > 0) FROM EmailThread t WHERE t.owner = :owner AND LOWER(t.subject) NOT LIKE ... AND NOT EXISTS (...participant match...) AND EXISTS (...body match...)` so the upgrade-nag detector skips threads the free-tier search already returned (don't double-count) and stays at one round-trip even when the inbox has thousands of messages. Both queries are plain JPQL `LIKE` so they execute identically on H2 (tests) and Postgres (prod) — the PLAN.md `tsvector` index is a follow-up perf knob, not a behavioural one. New `ThreadSearchService` in `com.emailmessenger.web` is the gate: it asks `PlanLimitService.currentPlan(owner)`, routes paid users (PERSONAL / TEAM / ENTERPRISE) through `searchIncludingBody` with `showBodySearchUpgradeNag=false`, and routes Free users through the existing subject+participant `search` plus a `hasBodyOnlyMatch` probe whose boolean becomes `showBodySearchUpgradeNag`. Returns a `Result(Page<EmailThread>, boolean)` record so the controller wires both into the model in one call. `ThreadController.listThreads` now takes `ThreadSearchService` via constructor injection and, when `q` is non-blank, replaces the old direct `threadRepository.search(...)` call with `threadSearchService.search(owner, q, pageRequest)`; when the service returns a nag, the controller sets `bodySearchUpgradeNag=true` on the model (otherwise leaves it absent so the Thymeleaf `th:if` evaluates false). `threads.html` renders a new `<div class="body-search-nag" role="status">` immediately below the search form (so users see the upsell at the top of the result list, not buried at the bottom): a brand-tinted banner reading "**Search across message bodies is a Personal feature.** We found more matches inside email bodies for &quot;${searchQuery}&quot;. Upgrade to read them." with a `POST /billing/checkout?plan=personal` form (CSRF-included) and an `Upgrade to Personal` CTA — same checkout entry point as the Free-plan-limit modal already on the page. ~25 namespaced CSS lines (`.body-search-nag`, `.body-search-nag-text`, `.body-search-nag-cta-form`, `.body-search-nag-cta`) using a `linear-gradient(135deg, rgba(--brand, 0.10), …)` tinted background with a `rgba(--brand, 0.35)` border, the same `--text`/`--text-muted` tokens, mobile `<560px` column-stack + full-width CTA, and a dark-mode tint bump. Tests: 8 new `EmailThreadRepositoryTest` cases (`searchIncludingBodyMatchesMessageBodyContent` — proves body-only matches surface; `searchIncludingBodyStillFindsSubjectMatches` — superset behaviour; `searchIncludingBodyScopedByOwner` — Alice's body content is invisible to a different user; `hasBodyOnlyMatchTrueWhenOnlyBodyMatches`; `hasBodyOnlyMatchFalseWhenSubjectAlsoMatches` — proves the `NOT LIKE` guard; `hasBodyOnlyMatchFalseWhenParticipantMatches`; `hasBodyOnlyMatchFalseWhenNothingMatches`; `hasBodyOnlyMatchScopedByOwner`); 4 new `ThreadSearchServiceTest` cases (Free → subject path + nag boolean; Free with no body-only match → no nag; Personal → body-inclusive path + skip nag probe; Team + Enterprise → also body-inclusive); 1 new `ThreadControllerTest` (`bodyOnlyMatchOnFreePlanExposesUpgradeNag` asserts `bodySearchUpgradeNag=true` rides the model when the service flags it) + the existing search test renamed to `searchQueryParamRoutesThroughThreadSearchService` to assert the new wiring + nag attr absent on the happy path. `./mvnw test` → 281 tests pass (1 Docker-only test skipped as before).
Advances: EPIC-07 Inbox Search & Discovery — Milestone 2 (Full-text body search Personal+).
Master action: none

## 2026-06-05
Shipped: Inbox search bar on `/threads` — new `?q=<term>` query param filters the user's threads by case-insensitive substring match against subject OR participant email OR participant display name, via a `EmailThreadRepository.search(User, String, Pageable)` JPQL method (`SELECT DISTINCT t FROM EmailThread t WHERE t.owner = :owner AND (LOWER(t.subject) LIKE LOWER(CONCAT('%', :q, '%')) OR EXISTS (SELECT 1 FROM Message m WHERE m.thread = t AND (LOWER(m.sender.email) LIKE ... OR LOWER(COALESCE(m.sender.displayName,'')) LIKE ...)))`) so a sender with hundreds of threads in their inbox can find "everything from Ada" without scrolling, and a casual user can find a months-old thread by typing a fragment of its subject. `ThreadController.listThreads` now accepts `@RequestParam("q")` (trimmed; blank routes back to the original `findByOwnerOrderByUpdatedAtDesc` so the empty-query case stays on the indexed scan, not the EXISTS-join plan), exposes `searchQuery` as a model attribute, and suppresses the onboarding checklist when a query is active (a brand-new user with 0 threads searching for "anything" sees the empty-search state, not a misleading "welcome" card). `threads.html` renders a `<form method=get action=/threads>` search bar above the conversation list (and above the empty-search state, so a user with no matches can still re-search without going back), the section title flips to "N result/s for &quot;query&quot;" when a query is active, pagination links round-trip `q`, and a "Clear" link returns to `/threads`. New `.search-empty` panel with a friendly "No matches for &quot;query&quot;" headline + "Back to inbox" CTA. ~80 namespaced CSS lines (`.inbox-search`, `.inbox-search-input` with brand-coloured focus ring, `.inbox-search-submit`, `.inbox-search-clear`, `.search-empty*`, `.visually-hidden` for the screen-reader label, `<560px` stacking + dark-mode focus tint) reusing the existing `--surface`/`--border`/`--text`/`--brand` tokens. Tests: 6 new `EmailThreadRepositoryTest` cases (`searchMatchesSubjectCaseInsensitively`, `searchMatchesParticipantDisplayName`, `searchMatchesParticipantEmail`, `searchScopedByOwner` — cross-user isolation, `searchReturnsEmptyPageWhenNoMatch`, `searchDeduplicatesThreadsWithMultipleMatchingMessages` — proves the `DISTINCT` keeps a thread from showing twice when two of its messages both match); 3 new `ThreadControllerTest` cases (`searchQueryParamRoutesToRepositorySearch` — verifies the search path is used and `findByOwnerOrderByUpdatedAtDesc` is never called when `q` is present; `blankSearchQueryFallsBackToFullList` — `q=   ` of whitespace falls through to the full list and `search` is never called; `searchWithNoResultsSuppressesOnboardingChecklist` — empty search results don't show the onboarding card to a user with 0 threads). PLAN.md transitions Primary Objective from EPIC-06 Launch readiness (all four milestones provably shipped per the 2026-06-05 entries below) to **EPIC-07 Inbox Search & Discovery** (4 milestones: subject+participant search, full-text body search Personal+, sender-grouped sidebar, filter chips); Milestone 1 marked Shipped in the new PLAN.md. `./mvnw test` → 268 tests pass (1 Docker-only test skipped as before).
Advances: EPIC-07 Inbox Search & Discovery — Milestone 1 (Subject + participant search on `/threads`). New Primary Objective: turn the flat-list inbox into a real find-anything surface so a paid user with 500+ threads stays paid and a Free user has a second compelling reason to upgrade beyond the thread cap.
Master action: none.

## 2026-06-05
Shipped: Trial-conversion nudge — a dismissable conversion modal on `/threads` for trialing users whose trial expires in ≤3 days, sitting alongside the existing `trial-banner-urgent` strip and giving last-mile signups a one-click checkout for the plan they're already trialing. New `TrialConversionNudge` record (`planLabel`, `planParam`, `daysLeft`, `monthlyPrice`, `dismissKey`) and `TrialConversionNudgeService` in `com.emailmessenger.billing` reading `SubscriptionRepository` directly: returns empty when no sub / status != "trialing" / trialEndsAt null / plan is FREE or ENTERPRISE (Enterprise is sales-assisted per `BillingService.startCheckout`) / daysLeft > 3 (`(hours + 23) / 24` ceiling-divide so 36h reads as 2 days, matching `BillingBannerService`'s rounding); otherwise produces "Personal"/"Team" + "personal"/"team" + "$9"/"$29" + a `mailim-trial-nudge-{trialEndDate}-d{daysLeft}` dismiss key so a Day-3 dismissal doesn't suppress the more-urgent Day-1 nudge the same user sees 48h later. `ThreadController` now takes the service via constructor injection and exposes `trialConversionNudge` as a model attribute on `GET /threads` only when the user has page-of-threads loaded (so the lockout-banner branch — already early-returns from the controller — never co-renders with a nudge). `threads.html` renders the modal at the top of `<main class="thread-page">` using the existing `.modal-overlay` / `.modal-card` / `.plan-compare-featured` / `.modal-cta` / `.modal-dismiss-link` classes that already power the Free-plan-limit upgrade modal (no new CSS), with a featured "Your plan" column listing Keep imported threads / Unlimited history & mailbox sync / Cancel anytime from billing portal, a `POST /billing/checkout` form carrying `plan=personal|team` + CSRF, and a `Remind me later` secondary dismiss. The headline branches on daysLeft (`ends today` / `ends tomorrow` / `ends in N days`). The trailing `<script>` block reads `data-dismiss-key` off the overlay and consults `localStorage` — if the key is already `1`, the overlay stays `hidden` and is never shown; on dismiss (close-X or "Remind me later"), the key is persisted and the node is removed. Tests: 9 new `TrialConversionNudgeServiceTest` cases (no sub / not trialing / window>3d / Personal exactly 3d → label+param+price+key / Team 20h → 1d + key ends "-d1" / dismissKey changes as trial approaches / Enterprise → empty / null plan → empty / null trialEndsAt → empty); 2 new `ThreadControllerTest` cases (`trialConversionNudgeIsExposedOnInboxWhenServiceReturnsOne` asserts the exact nudge instance is in the model; `trialConversionNudgeIsNotSurfacedDuringLockout` asserts the attribute is null and the service is never called when `billingBanner` is `subscriptionEnded`). PLAN.md milestone 4 marked Shipped with note; this closes all four EPIC-06 Launch readiness milestones in code (legal pages + cookie consent, onboarding checklist, demo video embed slot, trial-conversion nudge). `./mvnw test` → 259 tests pass (1 Docker-only test skipped as before).
Advances: EPIC-06 Launch readiness — Milestone 4 (Trial-conversion nudge). EPIC-06 is now fully code-complete; live deploy remains gated on Master-side ops (hosting, domain, Stripe live keys, encryption secrets, demo video URL) already tracked in MASTER_ACTIONS.md.
Master action: none

## 2026-06-05
Shipped: Demo-video embed slot in the landing hero — when `MARKETING_LANDING_VIDEO_PROVIDER` (`youtube` | `loom`) and `MARKETING_LANDING_VIDEO_ID` are both set in the deploy env, `landing.html` swaps the static chat-bubble mock for a click-to-play facade; otherwise the mock stays. New `LandingProperties` (`@ConfigurationProperties("marketing.landing")`) with a nested `Video` (provider, id, poster-url, title; setters trim + lowercase the provider, fall the title back to `MailIM demo` on blank) wired through `EmailMessengerApplication.@EnableConfigurationProperties`. New `LandingVideo` view-model record built by `LandingVideo.from(...)` — returns `null` (mock fallback) on unset provider/id or unknown provider, and bounces operator-supplied ids that don't match `[A-Za-z0-9_-]{4,64}` so a misconfigured env can't break out of the embed URL into an `onload=...` attribute injection. Embed URL uses `youtube-nocookie.com` (privacy-enhanced host, keeps the cookie banner honest until the visitor presses play) for YouTube and `loom.com/embed/{id}` for Loom, both with `autoplay=1` so the click-to-play user gesture immediately starts playback. `MarketingController` now injects `LandingProperties`, builds the `LandingVideo` per-request, and exposes it as `demoVideo` only when non-null. `landing.html` renders a 16:9 `<section class="landing-video">` with a `<button>` poster facade (poster image set via inline `background-image` from the configured URL when present), accessible `aria-label='Play <title>'`, a 72px play-button overlay, and a small `<script>` block whose `click` handler replaces the wrapper's innerHTML with a real `<iframe src=data-embed-url allow="… autoplay; fullscreen">` — so the first paint is just a styled poster and no third-party iframe loads until the visitor opts in. The static chat-bubble mock is now gated on `th:if="${demoVideo == null}"` so the two never render together. New `application.yml` block (both `dev` and `prod`) declaring `marketing.landing.video.{provider,id,poster-url,title}` wired to `MARKETING_LANDING_VIDEO_*` env vars; matching env passthrough added to `docker-compose.yml` so `docker compose up` honors the same overrides. ~70 namespaced CSS lines (`.landing-video`, `.landing-video-wrap` with `aspect-ratio: 16 / 9`, `.landing-video-poster`, `.landing-video-play`, `.landing-video-label`, `.landing-video-iframe`) reusing the existing `--surface`/`--border`/`--brand` tokens (dark mode auto-picks up) plus a `<640px` mobile shrink on the play button. Tests: 7 new `LandingVideoTest` cases (unset provider, unset id, unknown provider falls through, YouTube builds privacy-enhanced URL with all flags, Loom builds embed + propagates poster + defaults blank title, URL-attribute-injection ids are rejected, title setter trims/defaults blank); 2 new `MarketingControllerTest` cases (`demoVideo` model attr absent by default, present when provider+id configured); 1 new full-Spring `LandingHeroVideoIntegrationTest` (`@TestPropertySource` overrides → rendered HTML contains the `landing-video` wrapper, the privacy-enhanced YouTube `data-embed-url`, the human title in the aria/visible labels, the inline `background-image:url(...)` poster, AND no `landing-screenshot`/`screenshot-mock` so the mock is gone when the video is on); 1 new `PublicPageSeoIntegrationTest` case asserting the inverse — with no video config the mock still renders and the embed wrapper / `youtube-nocookie.com` is absent. `./mvnw test` → 248 tests pass (1 Docker-only test skipped as before).
Advances: EPIC-06 Launch readiness — Milestone 3 (Demo video in the landing hero).
Master action: Recording + supplying the actual 60–90s demo URL is already tracked under Launch / marketing in MASTER_ACTIONS.md; once Master sets `MARKETING_LANDING_VIDEO_PROVIDER` + `MARKETING_LANDING_VIDEO_ID` (and optionally `_POSTER_URL` + `_TITLE`) in the deploy env, the hero swaps with no further code change.

## 2026-06-05
Shipped: Legal pages + cookie-consent banner — Stripe live-mode's last code-side gate. New `LegalController` serves `GET /privacy`, `/terms`, `/refund`, each loading its HTML body from a `Resource` configured via `LegalProperties` (`marketing.legal.{privacy,terms,refund}`, defaulting to `classpath:legal/<page>.html`; Master overrides per-page at deploy via env like `MARKETING_LEGAL_PRIVACY=file:/etc/mailim/privacy.html` to drop in Termly/Iubenda output with no rebuild). Three shipped boilerplate bodies (`src/main/resources/legal/{privacy,terms,refund}.html`) cover MailIM's actual data flows — bcrypt account hashes, AES-GCM-encrypted IMAP credentials, header-only email metadata, Stripe-mediated billing, JSESSIONID + remember-me cookies only, 30-day GDPR/CCPA SAR turnaround, 14-day trial + 30-day annual refund window, monthly pro-rate forgiveness, chargeback policy — each ending with a "this is starting boilerplate, replace before launch" callout so an operator can't ship them by accident. New `templates/legal.html` wraps each body in the standard app header + `.legal-card` (with strong heading scale, list spacing, callout-styled `.legal-note`), pulls in the shared SEO fragment with per-page `pageTitle`/`pageDescription`/`pagePath`, and surfaces the legal footer + cookie banner. New `fragments/cookie-banner.html` — fixed-bottom banner ("We use only essential cookies for sign-in and your session. No tracking, no ads.") with a `Got it` dismiss button; visibility gated client-side by `localStorage['mailim-cookie-consent-v1']` so it shows only on first visit and persists dismissal across pages without a server roundtrip; injected on `/`, `/pricing`, `/login`, `/register`, the three legal pages, and `/demo`. `/register` gains an inline `auth-legal` "By creating an account you agree to our Terms and Privacy Policy" line under the form. Every marketing footer (`landing.html`, `pricing.html`, `login.html`, `register.html`, `legal.html`) now links to `/privacy`, `/terms`, `/refund`. `SecurityConfig` permitAll extended to the three legal routes; `SeoController.PUBLIC_PATHS` extended so `/privacy`, `/terms`, `/refund` appear in `sitemap.xml` with the same `lastmod`/`changefreq` treatment as the rest. `EmailMessengerApplication` `@EnableConfigurationProperties` extended with `LegalProperties.class`. CSS: ~85 new namespaced lines (`.legal-page`, `.legal-card`, `.legal-body h2/h3/code/a`, `.legal-updated`, `.legal-note`, `.auth-legal`, `.cookie-banner*`) reusing the existing `--surface`/`--border`/`--text`/`--brand` custom properties so dark mode picks up automatically, plus a `<560px` stacked layout for the banner. Tests: 5 new `LegalControllerTest` cases (default-resource render for each of the three routes asserts view name + model attributes; override-via-`LegalProperties` proves a per-deploy env swap works without code; multi-route title-uniqueness sweep) + 5 new full-Spring `PublicPageSeoIntegrationTest` cases (`/privacy`, `/terms`, `/refund` each render through the legal-page template with the right `<title>`/canonical/og + the shipped boilerplate content; landing footer links to all three legal pages and embeds the cookie banner div + dismiss button + `mailim-cookie-consent-v1` storage key; `/register` inlines the click-through-terms line); the sitemap assertion in `PublicPageSeoIntegrationTest` + `SeoControllerTest` extended to assert `/privacy`, `/terms`, `/refund` all appear. Latent fix during integration: the cookie banner's dismiss JS was outside the `th:fragment="banner"` block in v1 of `fragments/cookie-banner.html`, so Thymeleaf's `th:replace` pulled in the div but not the script and dismissal would have been silently dead in production — the boot of `/` through `PublicPageSeoIntegrationTest` was the first end-to-end render and surfaced it; both elements are now wrapped in a single `<th:block th:fragment="banner">`. `./mvnw test` → 237 tests pass (1 Docker-only test skipped as before).
Advances: EPIC-06 Launch readiness — Milestone 1 (Legal pages + cookie consent).
Master action: none (the three boilerplate pages are credible Stripe-application material as-shipped; if Master wants Termly/Iubenda output instead, the existing `MASTER_ACTIONS.md → Legal` items + the new `MARKETING_LEGAL_*` env overrides cover the swap).

## 2026-06-02
Shipped: Public landing page at `/` — new `GET /` route on `MarketingController` returns the `landing` view for anonymous visitors and `redirect:/threads` for an authenticated principal (checks `SecurityContextHolder` so the marketing site stops intercepting the app once you're signed in); removed the old `ThreadController#home()` that mapped `/` to `redirect:/threads` (which sent cold visitors straight into `/login` and made the funnel un-marketable). New `templates/landing.html` renders an above-the-fold hero ("Your inbox, as a chat.") with primary `Start free` CTA + secondary `See pricing`, a CSS-only chat-bubble preview (three back-and-forth bubbles using mock avatar gradient), a six-tile feature grid (IM bubbles, quoted-reply stripping, header threading, inline reply, background sync, dark mode), a three-step "how it works" list (account → connect mailbox → read as chats), a three-card pricing recap with featured-Personal callout and link to `/pricing`, a final-CTA panel, and a footer; includes `<title>`, `meta description`, OpenGraph + Twitter-card tags for unfurls. Header gains a `Start free` button next to `Sign in`. Appended ~250 lines of namespaced landing CSS to `main.css` (`.landing-*`, `.feature-*`, `.how-*`, `.pricing-recap-*`, `.mock-bubble*`, `.btn-lg`, `.btn-compact`) with dark-mode overrides keyed off `prefers-color-scheme` and a 640px breakpoint that drops the hero to mobile sizing. PLAN.md transitions Primary Objective from EPIC-04 Deployability (all 4 milestones shipped) to **EPIC-05 Acquisition** (4 milestones: landing, conversion-tracked signup funnel, SEO/OG, demo content); MASTER_ACTIONS [PLAN-REVIEW] block removed now that the next Objective is picked. 4 `MarketingControllerTest` cases (`/pricing` still maps to pricing; `/` renders `landing` for anonymous; `/` renders `landing` for an `AnonymousAuthenticationToken`; `/` 302→`/threads` for a `UsernamePasswordAuthenticationToken`) and dropped the now-redundant `ThreadControllerTest.rootRedirectsToThreads` since `/` no longer lives on `ThreadController`. 194 tests pass, 1 skipped (Docker-gated GreenMail integration test).
Advances: EPIC-05 Acquisition — Milestone 1 (Public landing page at `/`). New Primary Objective: turn `mailaim.app/` into a real conversion funnel so referral / Product Hunt / SEO traffic doesn't bounce off a 302-to-login.
Master action: none.

## 2026-06-01
Shipped: CI compose-stack smoke job (`.github/workflows/ci.yml` → new `compose-smoke` job, `needs: build`, 15-min timeout) — boots the actual deploy artifact end-to-end on every push and PR by running `docker compose up -d --build`, polling `http://localhost:8080/actuator/health` until the body matches `"status":"UP"` (3s interval, 180s deadline so first-boot JVM + Flyway have headroom), then asserting `curl /pricing` returns 200 and the body contains the `MailIM` brand marker and `/login` returns HTTP 200; dumps `docker compose logs --no-color` on failure and `docker compose down -v` always. Adds a `sudo systemctl stop postgresql` guard so the runner's preinstalled Postgres can't squat port 5432. Catches Dockerfile / compose / prod-profile regressions (image build failures, missing env passthrough, Flyway failures against real Postgres, healthcheck misconfiguration) before Master attempts a real deploy — Milestone 4 in PLAN.md now has a CI-enforced "the stack actually serves /pricing" signal, not just the `DEPLOY.md` write-up. With this, every PLAN.md "Done means" clause is verifiable from CI alone except the open-internet domain bit (still gated on Master DNS/hosting in MASTER_ACTIONS).
Advances: EPIC-04 Deployability — Milestone 4 (Production smoke deploy).
Master action: [PLAN-REVIEW] EPIC-04 Deployability is code-complete; pick the next Primary Objective. Strong default: acquisition / conversion (the funnel currently has no landing page at `/` — only `/pricing` — so referral and Product Hunt traffic lands on a Spring error). Alternative: trial-aware paywall (Stripe Checkout trial → conversion email → dunning) to lift Free → Personal conversion once traffic exists.

## 2026-05-31
Shipped: Production HTTP healthcheck wired end-to-end — added `spring-boot-starter-actuator` to `pom.xml`, exposed only `/actuator/health` over HTTP via `management.endpoints.web.exposure.include=health` with `show-details=never` and `probes.enabled=true` (so `/actuator/health/liveness` and `/actuator/health/readiness` are also published for K8s-style platforms); disabled `management.health.mail` so a flaky SMTP relay can't flap the aggregate. The default `db` indicator stays on, so a 200 from `/actuator/health` is exactly the "Flyway ran + DataSource reachable" signal Milestone 4's "Done means" calls for. `SecurityConfig` permits `/actuator/health` and `/actuator/health/**` anonymously (CSRF non-issue since probes are GET). `Dockerfile` runtime stage now `apt-get install -y --no-install-recommends curl` (apt cache wiped, stays under a MB) and declares `HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 CMD curl -fsS http://localhost:8080/actuator/health`. `docker-compose.yml` mirrors the same healthcheck on the `app` service so `docker compose ps` reports `healthy` after Postgres + Flyway are up. `DEPLOY.md` §4 directs Render/Fly/Railway to point their platform healthcheck at `/actuator/health`, and §5 leads the verify step with `curl -sSf https://<host>/actuator/health` returning `{"status":"UP"}` before the `/pricing` smoke check. New `ActuatorHealthEndpointTest` (`@SpringBootTest` + `MockMvc`, dev profile, H2) asserts unauthenticated 200 + `$.status == UP` for `/actuator/health`, `/actuator/health/liveness`, and `/actuator/health/readiness`. 192 tests pass (up from 189).
Advances: EPIC-04 Deployability — Milestone 4 (Production smoke deploy).
Master action: none

## 2026-05-27
Shipped: Container build + local compose stack — multi-stage `Dockerfile` (eclipse-temurin:21-jdk for `./mvnw -B -ntp dependency:go-offline` + `package -DskipTests` with cached deps, eclipse-temurin:21-jre runtime, dedicated non-root `app:app` UID/GID 1000, `JAVA_OPTS` defaulting to `-XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError`, `SPRING_PROFILES_ACTIVE=prod`, `EXPOSE 8080`); `docker-compose.yml` wires `postgres:16-alpine` (named volume `postgres-data`, `pg_isready` healthcheck on 5s/5s/10) + the app service with `depends_on: postgres: condition: service_healthy`, env passthrough for every `application.yml prod` placeholder (`DB_URL/DB_USER/DB_PASS` pointed at the compose-network hostname `postgres`, plus `STRIPE_*`, `BILLING_*`, `MAIL_*`, `MAILBOX_ENCRYPTION_*`, `MAILBOX_POLLING_*`, `PORT`) with `${VAR:-default}` fallbacks so missing host vars don't crash the boot; `.dockerignore` excludes `.git`, `target`, IDE config, `master/`, README/CLAUDE so the build context stays small. README's "Run locally" section now leads with `docker compose up --build` (Docker-first) and keeps the bare `./mvnw spring-boot:run` path as the secondary option. CLAUDE.md roadmap ticks the `Dockerfile` + `docker-compose.yml` box. PLAN.md switches Primary Objective from EPIC-03 (code-complete, "Done means" blocked on Master hosting/domain/Stripe ops) to EPIC-04 Deployability with milestones 1 (this ship) / 2 (GitHub Actions CI) / 3 (Testcontainers + GreenMail integration test) / 4 (production smoke deploy + DEPLOY.md + GHCR publish). MASTER_ACTIONS [PLAN-REVIEW] block removed now that the next Objective is picked. 188 tests still pass — no source-code changes.
Advances: EPIC-04 Deployability — Milestone 1 (Container build + local compose stack). New Primary Objective: ship the app to a real URL paying users can reach.
Master action: none for this milestone; Master ops items (hosting, domain, Stripe live keys, mailbox encryption secrets) already tracked under Infrastructure / Stripe / Mailbox encryption sections of MASTER_ACTIONS.md remain blockers for Milestone 4.

## 2026-05-26
Shipped: Plan-tiered IMAP poll cadence + ±30s jitter + N-failure circuit breaker — new `PollingPolicy` bean encapsulates `intervalFor(Plan)` (Free=15min, Personal/Team/Enterprise=5min), `nextPollAt(plan, from)` (interval +/- 30s uniformly-distributed jitter via `ThreadLocalRandom`), and `suspendAtFailures()=5`. Flyway V7 adds three columns to `mail_accounts`: `consecutive_failure_count INT NOT NULL DEFAULT 0`, `polling_suspended BOOLEAN NOT NULL DEFAULT FALSE`, `next_poll_at TIMESTAMP NULL` (+ index on `next_poll_at`); `MailAccount` entity gains the fields, a `recordPollFailure(threshold)` helper (increments counter, flips `pollingSuspended=true` at threshold), and `markSynced()` now also resets the counter and clears the breaker. `MailAccountRepository.findDueForPolling(now)` JPQL filters `polling_suspended=false AND (next_poll_at IS NULL OR next_poll_at <= :now)` so the scheduler tick only touches accounts that are actually due — suspended mailboxes are skipped entirely and recently-polled mailboxes don't get hammered on every tick when the scheduler interval is shorter than the plan interval. `MailboxPollingService` now injects `PlanLimitService`, `PollingPolicy`, and the existing `Clock` bean; `pollAll()` calls `findDueForPolling(LocalDateTime.now(clock))`; `pollOne(Long)` calls `recordPollFailure` on both the decryption-failure path and the `ImapConnectionException` path (log emits a WARN once the breaker trips), then unconditionally calls `scheduleNextPoll(account)` which stamps `nextPollAt` via `pollingPolicy.nextPollAt(planLimitService.currentPlan(user), now)` so a failing mailbox still gets re-tried at its plan interval until the breaker opens. Manual "Sync now" via the controller still calls `pollOne` directly, bypassing the due/suspended filter — a successful manual sync closes the breaker (`markSynced` reset). `MailboxView` record adds `pollingSuspended` + `consecutiveFailureCount` fields; `/mailboxes` index template renders a "Polling paused" pill badge next to the username and, when both an error and the breaker are tripped, an extra remediation line ("Automatic polling paused after N consecutive failures. Use Sync now to retry once you've fixed the issue."). Namespaced `.mailbox-suspended-badge` CSS with dark-mode variant added to `main.css`. 10 new tests (6 `PollingPolicyTest` for Free/paid interval mapping, jitter bounds across 200 samples for both tiers, jitter actually varies, suspend threshold; 4 `MailboxPollingServiceTest` covering success-resets-counter-and-unsuspends after a 5-failure trip, suspended-mailbox-skipped-by-pollAll, not-yet-due-mailbox-skipped-by-pollAll, successful-poll-stamps-next-poll-at-within-Free-window) plus updated assertions on the existing connection-failure test for `consecutiveFailureCount=1` / `pollingSuspended=false`; 188 total pass (up from 178).
Advances: EPIC-03 Mailbox Onboarding — Milestone 4 (Sane defaults + safety rails). All four milestones of the Primary Objective are now code-complete; next session should re-verify against "Done means" with a live `MAILBOX_POLLING_ENABLED=true` deploy and pick the next Primary Objective accordingly.
Master action: none (poll cadence + safety rails ship behind the existing `MAILBOX_POLLING_ENABLED` flag; no new secrets or accounts required).

## 2026-05-25
Shipped: Manual "Sync now" trigger + sync status surfacing on `/mailboxes` — split `MailboxPollingService` so the `pollOne(Long)` / `pollAll()` sync logic is unconditionally available (controller can inject it regardless of the feature flag) and moved the recurring `@Scheduled` trigger into a new thin `MailboxPollingScheduler` bean that keeps the existing `@ConditionalOnProperty("mailbox.polling.enabled"=true)` gate; added `MailAccountRepository.findByIdAndUser(Long, User)` for owner-scoped lookups; new `POST /mailboxes/{id}/sync` (auth-required, CSRF-protected) on `MailboxController` resolves the current user, 404s on cross-user or unknown ids via the existing `NoSuchElementException` -> `GlobalExceptionHandler` path, invokes `pollingService.pollOne(id)`, reloads the account, and flashes either `syncMessage` ("Mailbox synced.") or `syncError` ("Sync failed: ...") on the redirect to `/mailboxes`. Introduced `MailboxView` record + static formatter that renders friendly relative timestamps for `lastSyncedAt` ("Just now", "5 minutes ago", "1 hour ago", "Yesterday", "3 days ago", "Apr 1, 2026" past a week) and a remediation hint for `lastSyncError` keyed off keywords (`auth`/`login`/`password` -> "Generate a new app password..."; `timeout` -> "didn't respond in time"; `tls`/`ssl`/`certificate` -> "TLS handshake failed..."; `connect`/`unreachable`/`host`/`network` -> "We couldn't reach your mail server..."; fallback -> "Try Sync now again. If the error persists, reconnect the mailbox."). `/mailboxes` index template now renders per-row CSRF-armed "Sync now" buttons, the relative-time label, and a callout box with the error + remediation hint when present; success/error alerts at the top of the page surface the flash messages. Added namespaced `.mailbox-row`, `.mailbox-error`, `.btn-secondary`, `.mailbox-sync-*` CSS with dark-mode and <=480px mobile overrides. 15 new tests (9 `MailboxViewTest` covering null, just-now, minutes/hours/days/yesterday boundaries, beyond-a-week date fallback, error-hint keyword mapping for auth/connectivity/generic; `MailboxPollingFeatureFlagTest` now also asserts `MailboxPollingService` bean is present when scheduler bean is absent; 5 `MailboxControllerTest` for anonymous->login, CSRF->403, owned->success-flash, foreign-owner->404, error-on-row->error-flash); 178 total pass (up from 163).
Advances: EPIC-03 Mailbox Onboarding — Milestone 3 (Manual "Sync now" trigger + sync status surfacing).
Master action: none.

## 2026-05-24
Shipped: First-mailbox onboarding wizard — `GET /mailboxes/new` is now a two-step server-rendered flow: with no `provider` query param it renders a 5-card provider picker (Gmail / iCloud / Fastmail / Outlook · Microsoft 365 / Other), and with `?provider=<slug>` it pre-fills `MailboxForm` (host, port, ssl) from the new `MailboxProvider` enum and renders the credentials step with a provider-branded headline, the provider's app-password help copy, and a deep link to that provider's app-password settings page (Google, Apple ID, Fastmail Settings, Microsoft account); the host/port/SSL fields are tucked behind a `<details>` "Advanced" toggle for known providers (auto-opened on validation errors) and rendered inline for "Other"; a hidden `provider` field is carried on the form so validation/IMAP-error re-renders keep the user on the credentials step with the right preset. Controller now exposes `providers` (all enum values) on both GET and POST, picks step 2 whenever `mailboxForm` exists in the model (validation re-render path), and falls back to the picker for unknown slugs. Added `MailboxProvider.fromSlug` (case- and whitespace-insensitive). Wizard CSS (progress steps, 2-column provider grid, advanced disclosure, dark-mode tweaks) appended to `main.css`. 6 new tests (4 `MailboxProviderTest` for slug resolution / preset accessors; 2 new `MailboxControllerTest` cases for picker render without provider + Gmail preset round-trip + unknown-slug fallback); 163 total pass.
Advances: EPIC-03 Mailbox Onboarding — Milestone 2 (First-mailbox onboarding wizard).
Master action: none.

## 2026-05-23
Shipped: Scheduled IMAP polling behind a feature flag — `MailboxPollingService` (`@ConditionalOnProperty("mailbox.polling.enabled"=true)`) runs a `@Scheduled` `pollAll()` on `fixedDelayString=${mailbox.polling.interval-ms:300000}` with `initialDelayString=${mailbox.polling.initial-delay-ms:30000}`; `pollOne(accountId)` is `@Transactional` and decrypts the stored IMAP password, calls a new `ImapClient.fetchSinceUid(host,port,ssl,user,pass,lastSeenUid)` seam that returns `IncrementalFetch(messages, newLastUid)` — on `null` cursor it reports the current highest INBOX UID as a baseline (no backfill, since the connect-time initial sync already pulled the latest 30) and on a set cursor it issues `UIDFolder.getMessagesByUID(lastSeenUid+1, LASTUID)` filtered strictly above the boundary; each new `MimeMessage` is fed to the existing `EmailImportService.importMessage(mime, owner)`, the cursor is advanced, and `markSynced()` is called; per-account `ImapConnectionException` is caught and written to `lastSyncError` instead of breaking the loop; `pollAll` wraps each `pollOne` in its own try/catch so a poison account can't kill the rest. Flyway V6 adds `last_seen_uid BIGINT NULL` to `mail_accounts`; the `MailAccount` entity gains the field + setter. `JakartaImapClient` implements `fetchSinceUid` via `UIDFolder`. `@EnableScheduling` added to `EmailMessengerApplication`. `application.yml` (both `dev` and `prod`) declares `mailbox.polling.{enabled,interval-ms,initial-delay-ms}` with safe defaults (`enabled=false`, 5-min interval, 30-s initial delay) wired to `MAILBOX_POLLING_*` env vars so prod can flip the flag on without redeploying app code. 5 new tests (4 `MailboxPollingServiceTest` covering first-poll-baseline-no-import, incremental-imports-and-advances-cursor, IMAP-failure-records-on-row-without-cursor-advance, pollAll-survives-a-throwing-account; 1 `MailboxPollingFeatureFlagTest` confirming the polling bean is absent when the flag is off); 157 total pass.
Advances: EPIC-03 Mailbox Onboarding — Milestone 1 (Scheduled IMAP polling). Per the [PLAN-REVIEW] that was outstanding in MASTER_ACTIONS, this session switched the Primary Objective from EPIC-02 (provably code-complete on 2026-05-22) to EPIC-03 in `master/PLAN.md`.
Master action: none (the feature flag stays `false` by default; flip `MAILBOX_POLLING_ENABLED=true` in prod once a real mailbox is connected and you're ready to start the scheduler).

## 2026-05-22
Shipped: Mailbox-connection flow closes the last "Done means" gap in EPIC-02 — `MailAccount` JPA entity + Flyway V5 (`mail_accounts` with unique `(user_id, host, username)`), `MailAccountRepository`, `ImapClient` seam with a `JakartaImapClient` impl that opens an `imaps`/`imap+STARTTLS` `Store` with 10s connect / 15s read timeouts and pulls the latest N INBOX messages by sequence number, `CredentialEncryptor` wrapping Spring Security's `Encryptors.delux` (AES-GCM, dev-fallback warned in log; envs `MAILBOX_ENCRYPTION_PASSWORD` / `MAILBOX_ENCRYPTION_SALT`), and `MailAccountService.connect(user, host, port, ssl, username, password)` that enforces `PlanLimitService.enforceCanCreateMailbox` (Free cap = 1) **before** the IMAP probe, validates creds, persists the encrypted row, and inline-imports the latest 30 INBOX messages via `EmailImportService`; sync errors after a verified login are caught and stored on `lastSyncError` instead of failing the whole connect. Authed `GET /mailboxes` (list), `GET /mailboxes/new` (form), `POST /mailboxes` (validates, persists, redirects to `/threads` on clean sync or `/mailboxes` when `lastSyncError` is set, re-renders form on `ImapConnectionException`, lets `PlanLimitExceededException` fall through to the existing upgrade-modal flash). `threads.html` empty-state CTA now points to `/mailboxes/new`, the dangling `/settings/mailboxes` nav link is replaced with `/mailboxes`, and the upgrade modal renders mailbox-cap copy when `kind=MAILBOX_COUNT`. 17 new tests (5 `MailAccountServiceTest` covering happy-path persist+import, password-encrypted-at-rest round-trip, invalid-creds short-circuit, free-cap blocking second mailbox before any IMAP call, post-verify fetch failure recording on the row; 9 `MailboxControllerTest` for auth + CSRF gating, GET/POST routes, form errors, success redirect, sync-error redirect, plan-limit flash redirect; 3 `PlanLimitServiceTest` for mailbox-cap pass/throw/personal-allows-3); 152 total pass.
Advances: EPIC-02 Monetization Plumbing — "Done means" is now code-complete (signup → trial → checkout → webhook → connect mailbox → see threads); remaining work is Master ops (deploy, Stripe live keys, mailbox encryption secrets).
Master action: `MAILBOX_ENCRYPTION_PASSWORD` and `MAILBOX_ENCRYPTION_SALT` (hex-encoded) must be set in prod; dev fallback is logged as a warning. Next Primary Objective recommended: EPIC-03 Mailbox Onboarding (scheduled IMAP polling so newly-arrived mail keeps showing up after the initial sync, plus an onboarding wizard for the first-mailbox path); `[PLAN-REVIEW]` added to MASTER_ACTIONS.md.

## 2026-05-21
Shipped: Inline upgrade modal over the thread list when a `PlanLimitExceededException` is thrown during a controller-driven import — replaced the standalone 402 error page with a redirect-and-flash flow: `GlobalExceptionHandler.planLimit` now writes a serializable `UpgradeModal` record (currentPlan / kind / limit / current / upgradeTarget=PERSONAL) into the output `FlashMap` via `RequestContextUtils.getOutputFlashMap` and 302s to `/threads`, so the user lands back in their own inbox with the modal on top instead of a disconnected error page; `threads.html` renders a dimming overlay + plan-comparison card (Free current vs. Personal recommended, with live "N of N threads" copy from the exception's `current`/`limit`) plus a CSRF-armed `POST /billing/checkout?plan=personal` "Upgrade to Personal" submit and a JS-only "Not now" dismiss; cleaned up `error.html` to drop the now-unreachable `upgradePlan` branches. Added modal CSS — overlay, card, two-column plan-compare grid (collapses to one column ≤ 520px), dark-mode overrides — alongside a fade-in animation. 2 new tests (1 `GlobalExceptionHandlerTest` verifying the redirect target plus full `UpgradeModal` flash payload, 1 `ThreadControllerTest` confirming the `upgradeModal` flash attr propagates into the inbox model); 135 total pass.
Advances: Milestone 4 (Plan limits) of EPIC-02 Monetization Plumbing.
Master action: none

## 2026-05-20
Shipped: Free-plan thread-count enforcement — added `Plan.FREE` to the plan enum, `PlanLimits` (Free: 1 mailbox / 500 threads; Personal: 3 / unlimited; Team: 10 / unlimited; Enterprise: unlimited), `PlanLimitKind` enum and a typed `PlanLimitExceededException` carrying current plan + kind + limit + current so the upgrade modal can render contextually; `PlanLimitService.currentPlan(user)` returns FREE unless the user's Subscription is in {trialing, active, past_due} (so `incomplete`, `canceled`, and a missing row all fall back to Free), `enforceCanCreateThread(user)` throws when `EmailThreadRepository.countByOwner(user)` is at the cap and is no-op for unlimited plans; `EmailImportService.resolveThread` now calls the guard only on the brand-new-thread branch so replies into an existing 500-thread mailbox still land; `BillingService.startCheckout` now rejects `Plan.FREE` early with a clean `BillingException` instead of falling through to "no price configured"; `GlobalExceptionHandler` maps `PlanLimitExceededException` → 402 Payment Required with an `upgradePlan=personal` model attribute, and `error.html` renders a CSRF-armed `POST /billing/checkout` "Upgrade to Personal" button when that attr is set. 13 new tests (9 `PlanLimitServiceTest` covering no-sub/incomplete/trialing/canceled/past_due plan resolution, under-cap pass, at-cap throw with full exception fields, paid-plan no-op, limits accessor; 2 `EmailImportServiceTest` cases for 501st-thread throws + reply-at-cap still appends; 1 `BillingServiceTest` for Free-rejected checkout; 1 `GlobalExceptionHandlerTest` for 402 status + upgrade CTA attr); 134 total pass.
Advances: Milestone 4 (Plan limits) of EPIC-02 Monetization Plumbing.
Master action: none

## 2026-05-19
Shipped: Stripe Billing Portal self-serve — `StripePortalGateway` (interface + SDK-backed `com.stripe.model.billingportal.Session` impl) exposes `createPortalSession(customerId, returnUrl)`; `BillingService.startPortal(user)` looks up the user's `Subscription`, returns `Optional.empty()` when no Stripe customer exists yet (so a never-paid user is funnelled to `/pricing` instead of erroring) and otherwise calls the gateway with the configured `billing.stripe.portal-return-url` (env `BILLING_PORTAL_RETURN_URL`, defaults to `/threads` in dev); added `BillingService.hasManagedBilling(user)` so the inbox header can light up a "Billing" link only when there's a Stripe customer to manage; `POST /billing/portal` (auth-required, CSRF-protected) 302s to the Stripe-hosted URL or to `/pricing` when there's nothing to manage; `threads.html` renders a CSRF-token form-post "Billing" button next to "Sign out" when `hasBilling=true`. 6 new tests (3 service: portal URL on existing customer, empty on no-sub, hasManagedBilling toggling from false→true after first checkout; 3 controller: anonymous→login, customer→Stripe URL, no-sub→`/pricing`); 121 total pass.
Advances: Milestone 3 (Trial + self-serve) of EPIC-02 Monetization Plumbing.
Master action: Stripe Billing Portal settings (return URL, customer-facing features) must be enabled in the Stripe Dashboard once test mode keys are in place; tracked under existing Stripe section in MASTER_ACTIONS.md.

## 2026-05-19
Shipped: Trial-status banner inside the inbox — `BillingBannerService` reads the user's `Subscription`, emits a `TRIAL_ENDING` banner with ceiling-divided days remaining (so 36h reads as "2 days") when status is `trialing`, a `SUBSCRIPTION_ENDED` lockout banner when status is `canceled`, and nothing otherwise; `ThreadController.listThreads` short-circuits the thread-list query when canceled so a lapsed user lands on a "Your subscription has ended — Reactivate plan" panel pointing at `/pricing` instead of seeing their old threads, and trialing users get a "Trial ends in N days — add card" callout (urgent styling ≤ 3 days, "today/tomorrow" copy at 0/1) on both `threads.html` and `conversation.html`; added a `Clock` bean so day-math is deterministic in tests. 10 new tests (7 unit tests across no-sub / active / trialing / partial-day / past-trial / missing-end-date / canceled, 3 controller tests for trial banner, lockout short-circuit, and conversation banner attribute); 115 total pass.
Advances: Milestone 3 (Trial + self-serve) of EPIC-02 Monetization Plumbing.
Master action: none

## 2026-05-18
Shipped: Closed the login-funnel gap so an existing user who arrives at `/login?plan=personal` (via the register page's "Sign in" link or any plan-tagged pricing CTA) is taken to Stripe Checkout after sign-in instead of `/threads` — added `PlanCheckoutSuccessHandler` (a `SavedRequestAwareAuthenticationSuccessHandler`) that reads the `plan` form param, calls `BillingService.startCheckout(user, Plan)`, and 302s straight to the returned URL, falling through to `/threads` on missing/unknown plan or `BillingException`; wired it into `SecurityConfig`'s `formLogin` in place of `defaultSuccessUrl`; `login.html` now renders a hidden `plan` input when present and its "Create one" link forwards the plan into `/register?plan=…`. 4 new integration tests (plan → Stripe URL, unknown plan → `/threads` with billing never called, login page renders with plan param) cover the funnel; 105 total pass.
Advances: Milestone 2 (Stripe billing) of EPIC-02 Monetization Plumbing.
Master action: none

## 2026-05-17
Shipped: Wired pricing-page CTAs into the funnel — `/pricing` Personal/Team CTAs now go to `/register?plan=personal|team` (Free goes to `/register`), `AuthController` accepts an optional `plan` query/form param on GET/POST `/register`, propagates it through binding errors and email-already-registered re-renders into a hidden form field, and after the existing `request.login(...)` auto-login it invokes `BillingService.startCheckout(user, Plan.parse(plan))` and `302`s straight to the returned Stripe Checkout URL; unknown/tampered plans and `BillingException` from an unconfigured Stripe fall through silently to `/threads` so a freshly-registered user is never stranded on an error page; `register.html` hidden plan input + plan-aware sub-headline + "Sign in" link now carries `?plan=` through, plus a `/login` GET handler stashes `plan` in the model so the follow-up login funnel work has a hook. 2 new integration tests (plan→Stripe URL redirect, bogus plan → `/threads` with `BillingService` never called), 102 total pass.
Advances: Milestone 2 (Stripe billing) of EPIC-02 Monetization Plumbing.
Master action: none

## 2026-05-16
Shipped: Stripe webhook handler — `POST /billing/webhook` exempted from auth + CSRF, verifies `Stripe-Signature` via `com.stripe.net.Webhook.constructEvent` against `billing.stripe.webhook-secret`, returns 400 on bad/missing signature so Stripe stops retrying; `StripeWebhookGateway` flattens raw events into a SDK-free `StripeEvent` record (customer, subscription, status, price, trial_end, current_period_end, with current_period_end fallback to subscription.items[0].current_period_end for the post-2024-09 schema); `BillingService.applyStripeEvent` is idempotent and dispatches `checkout.session.completed` (attaches subscription_id, flips `incomplete`→`trialing` when trial days configured else `active`), `customer.subscription.created/updated` (mirrors status / price / trial_end / current_period_end), `customer.subscription.deleted` (`canceled`); unknown customer / unknown event types are logged and swallowed to avoid retry storms; lookup tries `findByStripeSubscriptionId` first, falls back to `findByStripeCustomerId`. 10 new tests across handler dispatch and end-to-end controller (HMAC-signed payload through Stripe SDK verifier, rejected bad-sig stays incomplete, missing-header rejected), 100 total pass.
Advances: Milestone 2 (Stripe billing) of EPIC-02 Monetization Plumbing.
Master action: `STRIPE_WEBHOOK_SECRET` must be supplied after deploy + endpoint registered in the Stripe dashboard (already tracked in MASTER_ACTIONS.md).

## 2026-05-15
Shipped: Stripe Checkout integration — added `com.stripe:stripe-java:32.1.0`, Flyway V4 `subscriptions` table (1:1 with users, unique stripe_customer_id/stripe_subscription_id), `Subscription` JPA entity + `Plan` enum (PERSONAL/TEAM/ENTERPRISE), `SubscriptionRepository`, `BillingProperties` (@ConfigurationProperties for secret key, four price IDs, success/cancel URLs, trial days), `StripeCheckoutGateway` interface with Stripe-SDK impl (subscription mode, 14-day trial, allow promo codes, reuses Stripe customer on repeat checkouts), `BillingService.startCheckout(user, plan)` that upserts a local Subscription row in `incomplete` state, and `BillingController POST /billing/checkout` that 302s to the Stripe-hosted URL; `BillingException` + `IllegalArgumentException` handlers wired into `GlobalExceptionHandler`; `/billing/cancel` is public, checkout requires auth. 7 new tests (gateway-mocked service flow + controller redirect/auth/unknown-plan), 90 total pass.
Advances: Milestone 2 (Stripe billing) of EPIC-02 Monetization Plumbing.
Master action: Stripe credentials still required — `STRIPE_SECRET_KEY` and four price IDs (already tracked in MASTER_ACTIONS.md).

## 2026-05-15
Shipped: Made `EmailThread` user-owned end-to-end — Flyway V3 adds NOT NULL `owner_id` FK with index, scopes `root_message_id` and `message_id_header` uniqueness per-owner; `EmailThread.owner` JPA mapping; `EmailThreadRepository` gains `findByOwnerOrderByUpdatedAtDesc` / `findByIdAndOwner` / `findByRootMessageIdAndOwner` and `MessageRepository.findByMessageIdHeaderAndOwner` (JPQL join through thread); `ThreadController` resolves the current `User` via `Principal` + `UserService.requireByEmail` and filters list, view, and reply paths so one user cannot see or reply to another's threads; `EmailImportService.importMessage` now takes a `User owner` and threads stay isolated even when two users receive the same Message-ID. 9 new tests (cross-owner isolation in repo + controller + import), 83 total pass.
Advances: Milestone 1 (Auth foundation) of EPIC-02 Monetization Plumbing.
Master action: none

## 2026-05-14
Shipped: Spring Security email/password auth — `User` entity + Flyway V2 (users + persistent_logins), BCrypt-hashed registration via `/register`, form login via `/login` with persistent remember-me, logout, CSRF enabled site-wide and wired into the existing reply form; `/threads/**` now redirects anonymous users to login. 13 new tests, 79 total pass.
Advances: Milestone 1 (Auth foundation) of EPIC-02 Monetization Plumbing.
Master action: none

## 2026-05-06 — Autonomous Run #9

### Session Briefing (Role 1 — Epic Manager)

**Active epics this session**:
- `EPIC-01 Conversion Surface` (HIGH) — landing/pricing/demo pages so any
  organic traffic has somewhere to land. `/` currently redirects straight
  to `/threads`, which is the single biggest conversion gap.
- `EPIC-02 Monetization Plumbing` (HIGH) — auth + Stripe; the path to
  actual revenue. Blocked on Master credentials (Stripe, OAuth).
- `EPIC-03 Mailbox Onboarding` (HIGH) — IMAP polling + onboarding wizard;
  the path from signup to "aha" moment.

**Most important thing this session**: Build the static pricing page at
`/pricing`. It is the highest-priority unblocked task in EPIC-01, requires
no auth or external credentials, no DB schema changes, and unlocks every
marketing/distribution effort by giving traffic a real conversion target.
Every dollar of Master's eventual ad/SEO/social spend funnels through
pricing — without it, those dollars vaporize.

**Risks / blockers flagged**:
- EPIC-02 is gated on Master configuring Stripe (already in TODO_MASTER.md).
- EPIC-03's IMAP polling needs at least one test mailbox credential to be
  end-to-end verifiable; can be built behind a feature flag without one.
- 39 [GROWTH] tasks are queued; backlog needs Role 6 prioritization to
  surface the next-session pick clearly.

**Bootstrap notes**: Created `master/EPICS.md` (didn't exist). Synced
`master/APP_SPEC.md` to mark IMAP and Gravatar as planned (not built).

---

### Role 2 — Feature Implementer
**Task completed**: Static `/pricing` page [GROWTH][S] (EPIC-01 Conversion Surface)

Files created:
- `src/main/java/com/emailmessenger/web/MarketingController.java` — package-private
  `@Controller` exposing `GET /pricing` → `"pricing"` view. Separated from
  `ThreadController` so marketing surface routing stays orthogonal to app routes.
- `src/main/resources/templates/pricing.html` — full plan comparison page:
  hero with H1 "Simple, predictable pricing" + sub; monthly/annual billing
  toggle (annual prices `$7 / $24 / $83` derived from APP_SPEC's "2 months
  free" rule); 4-column plan grid (Free / Personal *Most Popular* / Team /
  Enterprise) with check-marked feature lists, plan blurbs, and CTAs (Free
  → `/threads`, Personal/Team → "Start 14-day free trial" → `/threads` for
  now since auth/Stripe aren't wired, Enterprise → `mailto:sales@`); 4-item
  FAQ (`<details>` accordion: switching plans, free trial, mailbox limit,
  data handling); marketing footer with brand + nav links. SEO `<meta
  description>` set for the page.

Files changed:
- `src/main/resources/static/css/main.css` — +200 lines:
  - `.marketing` page container, `.hero-narrow` H1/sub
  - `.billing-toggle`/`.billing-option` segmented control with
    `.is-active` + brand-colored fill; `.save-badge` for "Save 16%"
  - `.plan-grid` responsive auto-fit (220px min); `.plan-card` with the
    `.plan-card-featured` variant getting brand border + lift transform +
    shadow + a "Most popular" `.plan-tag` ribbon
  - `.plan-name`, `.plan-price`/`.price-amount`/`.price-period`,
    `.plan-blurb` (40px min-height to align grid), `.plan-features` with
    ✓ pseudo-bullets, `.plan-cta` full-width button
  - `.faq` + `.faq-item` `<details>` styling (custom summary marker hidden)
  - `.marketing-footer` with link nav
  - `.nav-active` for current-page indicator
  - Dark-mode overrides for plan card, billing option, faq item, footer
  - Mobile `@media (max-width: 640px)` resets the featured-card lift and
    scales hero H1 down to 28px
- `src/main/resources/templates/threads.html` — added `<a href="/pricing">Pricing</a>`
  to the header nav so the new page is reachable from the inbox.

Inline JS on pricing page wires the billing toggle: clicking Monthly/Annual
swaps `.is-active` and rewrites every `.price-amount` from `data-monthly`
or `data-annual` attributes — pure attribute lookup, no template injection
risk. Uses `var` for IE-compat consistency with sibling templates.

Verified: `./mvnw test` → BUILD SUCCESS, 63 tests pass.

**Income relevance**: Pricing page is the entry point of every paid
conversion. Without it, marketing/SEO/social touchpoints have nowhere to
land. With it, every visitor who arrives via `/pricing` (the most common
top-of-funnel SaaS route) sees plan tiers, a featured "Most Popular"
anchor, a clear "Start 14-day free trial" CTA, and FAQ that pre-empts the
top objections. This unlocks every downstream conversion task in EPIC-01
and gives Master a real URL to share when posting in marketing channels.

**Note for next session**: CTAs currently route to `/threads` because auth
+ Stripe checkout aren't wired yet. Once user auth ships (EPIC-02), update
the CTA hrefs to `/signup?plan=personal` etc. so the pricing page becomes
a true conversion funnel.

---

### Role 3 — Test Examiner
**Coverage added**: 3 new tests, 66 total (up from 63)

New tests:
- `MarketingControllerTest.pricingPageReturnsPricingView` — covers the new
  `GET /pricing` route added this session (200 OK, view name `"pricing"`).
  Standalone MockMvc with the same `InternalResourceViewResolver` pattern
  used by `ThreadControllerTest`.
- `ReplyServiceTest.sendReplySetsInReplyToAndReferencesHeadersFromLastMessageId`
  — verifies that when the last message in a thread has a Message-ID
  header, the outgoing reply's `In-Reply-To` and `References` headers are
  set to that exact value. **Income-critical**: without correct threading
  headers, replies arrive at the recipient's inbox as orphan emails — they
  break out of the conversation thread, look like spam, and destroy the
  product's value. Previously untested; the existing tests only verified
  Subject/To routing.
- `ReplyServiceTest.sendReplyOmitsThreadingHeadersWhenLastMessageHasNoMessageId`
  — verifies that when `messageIdHeader` is null (e.g. an imported message
  with a missing or malformed Message-ID), `In-Reply-To` and `References`
  are *not* set (rather than being set to the literal string "null", which
  would be a silent corruption bug).

**Risk reduced**: Email threading correctness for outgoing replies — a
class of bug that would have shipped silently and only surfaced via user
churn ("my replies aren't threading, I'm cancelling").

**Still uncovered (not actionable this session)**:
- Stripe webhook handler — code not written yet (EPIC-02).
- User auth flows — code not written yet (EPIC-02).
- IMAP polling job — code not written yet (EPIC-03).
- Day-separator JS and keyboard shortcut JS — client-side; out of JUnit scope.
- Pricing page billing-toggle JS — client-side; out of JUnit scope.

---

### Role 4 — Growth Strategist
Identified 5 implementable growth ideas not previously captured, plus 2 Master
marketing actions:

1. **Smart reply suggestions** [GROWTH][M] (EPIC-05) — Claude-generated 2–3
   one-tap reply suggestions under each conversation; Personal+ tier gate;
   the strongest "wow" demo asset for screenshots and recording. HIGH income
   impact. Prereq: auth + ANTHROPIC_API_KEY.
2. **Exit-intent email capture modal** [GROWTH][S] (EPIC-01) — on `/pricing`
   and future landing, detect close/back intent and show "Get launch updates"
   modal; captures leads before Stripe is wired. MEDIUM income impact.
3. **Add-on extra mailbox at $3/mo** [GROWTH][S] (EPIC-02) — expansion
   revenue without forcing a tier upgrade. MEDIUM income impact.
4. **Auto-categorize threads** [GROWTH][M] (EPIC-04) — Newsletter / Personal
   / Work via List-Id + sender-domain heuristics; reduces "overwhelming"
   churn that hits at the 100+ thread mark. MEDIUM income impact.
5. **Public stats page at `/stats`** [GROWTH][S] (EPIC-01) — live
   server-rendered counters as a trust signal; compounds with pricing page.
   LOW–MEDIUM income impact.

Master actions added to TODO_MASTER.md:
- Submit MailIM to BetaList + Indie Hackers products page once pricing
  page is live (free, ~30 min, targeted SaaS-curious traffic).
- $50–$100 Reddit ads test on r/productivity / r/freelance / r/remotework
  landing on `/pricing`; pause-criteria included.

---

### Role 5 — UX Auditor
**Flows audited**: landing-equivalent (`/` → `/threads`), thread list, empty
state, conversation header, pricing page (just shipped), reply form.

**Direct fixes applied**:
1. **Empty-state dead-end** (`threads.html`): the "Connect a mailbox" button
   was `href="#"` — clicking it did nothing. Replaced with "See plans &
   get started" linking to `/pricing`. Empty state now drives every
   first-time visitor with no data toward the conversion surface instead
   of bouncing. Also rewrote the heading from "No conversations yet" to
   "Your inbox is empty" — clearer subject/object framing.
2. **SEO hygiene**: added `<meta name="robots" content="noindex, nofollow">`
   to both `threads.html` and `conversation.html`. App pages should not
   appear in Google results both because they're per-user (no value to
   anonymous searchers) and because conversation subjects could be
   sensitive. `/pricing` deliberately has no robots tag (defaults to
   indexable) — it's the only page Google should rank.

**Issues flagged (added to INTERNAL_TODO.md [UX])**:
- Pricing page CTAs route to `/threads`; needs `/signup?plan=...` once
  EPIC-02 ships.
- Pricing footer needs `/privacy` `/terms` `/refund-policy` links once
  those pages exist (Stripe go-live blocker).
- Conversation page should also expose `/pricing` in the header.
- Mobile layout pass should also hide `.kbd-hint` below 640px.

---

### Role 6 — Task Optimizer
**Backlog hygiene:**
- Created `master/DONE_ARCHIVE.md` (didn't exist); migrated 19 completed
  tasks out of `INTERNAL_TODO.md` (the `## Done (archived)` section is
  now empty/removed) plus this run's pricing page completion.
- Rewrote `INTERNAL_TODO.md` from scratch with the prescribed priority
  ordering: `TEST-FAILURE → income-critical → UX(conversion) → HEALTH →
  GROWTH → BLOCKED`.
- Tagged **every** open task with its Epic ID (EPIC-01 through EPIC-08).
- Grouped GROWTH items by epic so the next session can read down a single
  epic's backlog without filtering.
- 0 `TEST-FAILURE` items; 0 `BLOCKED` items.
- Active task count: 47 tasks (1 CORE income-critical, 12 income-critical
  GROWTH, 7 UX, 24 GROWTH non-critical, 3 Infra). Down from 50 by way of
  the pricing-page completion plus 2 net adds (5 new growth – 1 done –
  some absorbed by existing slots; previous count likely undercounted).
- `TODO_MASTER.md` audit: the only `[LIKELY DONE - verify]` item is the
  HTML XSS sanitization, already verified in test suite (4 XSS tests in
  `ConversationServiceTest`); leaving the verify-in-production note as-is.

**Session Close Summary:**

What was accomplished Run #9:
- Shipped `/pricing` page (EPIC-01) — first marketing surface; 4 plans, monthly/
  annual toggle, FAQ, dark-mode + responsive CSS, SEO meta description.
- Added `MarketingController` with test coverage; reply-threading-header
  correctness now tested (income-critical anti-churn coverage).
- Bootstrap: created `EPICS.md` (8 epics; 3 active) and synced `APP_SPEC.md`
  to mark IMAP/Gravatar as planned (not built).
- Backlog: created `DONE_ARCHIVE.md`, rewrote `INTERNAL_TODO.md` with epic
  tagging and priority ordering. Added 5 new growth tasks + 2 marketing
  Master actions. Fixed 1 dead-end empty-state CTA. Added robots metas to
  app pages.
- Test suite: 63 → 66 tests, BUILD SUCCESS.

Most important open item heading into next session:
- **EPIC-02 user authentication** is the unblocker for at least 9 plan-gated
  GROWTH tasks (Stripe billing, plan limits, archiving, pinning, signature,
  custom from-address, AI summary, smart replies, Google OAuth). It is a
  single-session [M] task and once shipped opens the floodgates of revenue
  features. Recommend that be Run #10's Role-2 pick.

Risks / blockers needing Master attention:
- `TODO_MASTER.md` Stripe + DB + domain items remain outstanding. Without
  a Stripe account, EPIC-02 can be coded but not end-to-end tested with
  real checkouts.
- Reddit ads + BetaList submissions (added this run) are zero-cost and
  unblocked; can be done as soon as Master has a public URL.

---

### Role 7 — Health Monitor

**Security**:
- Hardcoded-secret scan across `src/**`: clean. Only references are the
  `application.yml` env-var placeholders (`${DB_PASS}`, `${MAIL_PASS:}`)
  and the empty H2 dev password — all expected.
- New `MarketingController` and `pricing.html` introduce no DB query, no
  user input, no `th:utext`, and no `eval`-style JS. Billing-toggle JS
  reads only `data-monthly` / `data-annual` attributes from the page's
  own DOM — no injection vector.
- **Pre-emptive flag added** to `INTERNAL_TODO.md` [HEALTH]: CSRF token
  must be wired into `conversation.html`'s reply form when Spring Security
  arrives in EPIC-02 (without it, existing POSTs will start returning
  403). Currently no Spring Security on classpath so no enforcement
  exists — known gap.
- **Pre-emptive flag added**: `/h2-console` is correctly gated by the dev
  profile in `application.yml`, but a CI/prod smoke check should verify
  it returns 404 in prod so a bad `SPRING_PROFILES_ACTIVE` can't silently
  expose the DB browser.

**Performance**:
- `/pricing` is fully static — zero DB queries, sub-millisecond render.
- CSS file grew from 412 → ~610 lines (~12 KB unminified). Still
  manageable; flag to watch as future marketing pages add more.
- No new dependencies; `pom.xml` unchanged.

**Code quality**:
- `MarketingController`: 11 lines, package-private — no over-engineering.
- `pricing.html` uses semantic HTML (`<article>` per plan, `<details>`
  for FAQ accordion); no inline styles; no template-injection risk.
- One small JS dead-branch was removed during implementation
  (`'period' === 'annual' ? ...` was a literal-string compare).
- CSS additions are namespaced (`.marketing`, `.plan-*`, `.billing-*`,
  `.faq-*`); no global selector pollution.

**Legal**:
- **Direct fix applied**: `pricing.html` FAQ originally contained
  forward-looking claims ("Mailbox credentials are encrypted at rest",
  "delete all your data with one click") that aren't yet implemented.
  Misrepresentation of un-shipped features is actionable under FTC and
  EU consumer-protection rules. Rewrote to factual scope-limited copy:
  "Email content is stored only to render your conversation view — we
  never read it for advertising or sell it to third parties. Full details
  are in our privacy policy (coming with launch)."
- Dependency licenses audited: spring-boot (Apache 2.0), jsoup (MIT),
  jakarta.mail (EPL 2.0 + GPL 2.0 with classpath exception, commercial-
  friendly), flyway (Apache 2.0), postgresql (PostgreSQL License,
  BSD-like), testcontainers (MIT), h2 (MPL 2.0 / EPL 1.0 dual). No
  GPL / copyleft contamination.
- Outstanding `[LEGAL]` items in `TODO_MASTER.md` (privacy policy, ToS,
  refund policy, cookie consent, OAuth ToS review) remain unchanged.

---



### Role 1 — Feature Implementer
**Task completed**: CSS polish — day separators, dark mode, refined bubbles, hover states [CORE][M]

Files changed:
- `src/main/java/com/emailmessenger/service/BubbleRun.java` — added `date()` method (returns
  `LocalDate` of the first message's `sentAt`; null-safe: returns null for empty runs and null
  sentAt). Required to expose date info to the Thymeleaf template without inline list-access
  expressions.
- `src/main/resources/static/css/main.css` — 90 lines added:
  - **Dark mode** (`@media (prefers-color-scheme: dark)`): full variable override for `--bg`,
    `--surface`, `--border`, `--text`, `--text-muted`, `--brand`, `--brand-dark`; plus specific
    overrides for thread-item hover, reply textarea background, alert-success dark green, and
    bubble border opacity. GitHub-inspired dark palette (bg: #0d1117, surface: #161b22).
  - **Day separator**: `.day-separator` flex row with `::before`/`::after` hr lines and
    uppercase centered date label; 11px font, 0.05em letter-spacing.
  - **Bubble refinements**: added `border: 1px solid rgba(0,0,0,0.04)` + `transition: box-shadow`
    + `.bubble:hover { box-shadow: 0 2px 8px ... }` for tactile hover feedback.
  - **`.header-nav`/`.msg-count`**: replaced all inline styles in both templates with CSS classes.
  - **`.conv-header h1`**: added `flex: 1; min-width: 0` so long subjects ellipsis-truncate
    correctly when the new kbd-hint and msg-count siblings are present in the flex row.
  - **`.kb-focus`**: outline + background for keyboard-selected thread item (Role 4).
  - **`.kbd-hint`/`kbd`**: subtle shortcut label styling with bordered `<kbd>` chips.
- `src/main/resources/templates/conversation.html` — three changes:
  - `.bubble-run` now has `th:attr="data-date=..."` computed from `run.date()` for day separator
    JS; empty string when date is null (JS skips empty dates).
  - `<span style="...">` message count replaced with `<span class="msg-count">`.
  - Script block updated: IIFE runs day-separator insertion (parses `data-date`, inserts
    `.day-separator` divs between runs on different dates; labels "Today" / "Yesterday" /
    locale-formatted full date); then scrolls to bottom.
- `src/main/resources/templates/threads.html` — inline nav styles replaced with `.header-nav`
  CSS class.

Verified: `./mvnw test` → BUILD SUCCESS, 63 tests pass (up from 60).

**Income relevance**: Dark mode is a top-requested feature in productivity SaaS and is commonly
cited in App Store reviews as a conversion factor. Day separators make the chat-bubble metaphor
legible for long threads (without them, hours-long conversations look like chat from one sitting).
Keyboard shortcuts are used by power users before they upgrade — removing friction from the
"evaluating if I like this" phase directly improves conversion.

---

### Role 2 — Test Examiner
**Coverage added**: 3 new tests in `ConversationServiceTest` (63 total, up from 60)

Tests added:
- `bubbleRunDateReturnsLocalDateOfFirstMessage` — verifies `BubbleRun.date()` returns the
  `LocalDate` matching the first message's `sentAt`; uses a fixed timestamp `2025-06-15T09:30`.
- `bubbleRunDateIsNullWhenFirstMessageHasNullSentAt` — verifies `date()` returns null when the
  first message has no `sentAt` (defensive path for emails with missing `Date:` header).
- `separateRunsOnDifferentDaysHaveDifferentDates` — builds a two-run conversation with messages
  on 2025-06-15 and 2025-06-16 from different senders; verifies the two runs report different dates
  (prerequisite invariant for the day-separator logic).

Coverage status:
- `BubbleRun.date()`: fully covered (normal case, null sentAt, multi-run date comparison).
- Day separator JS: client-side; no unit test added (not a Java path).
- Keyboard shortcut JS: client-side; no unit test added.
- Income-critical paths still at zero coverage (unchanged from prior runs):
  - Stripe webhook handler
  - User authentication flows
  - IMAP polling job

---

### Role 3 — Growth Strategist
Identified 6 new implementable growth tasks not previously captured:

1. **Pricing page at /pricing** [GROWTH][S] — Static plan comparison table with feature matrix
   and CTA buttons; no auth required; prerequisite for organic conversion from landing page.
   HIGH income impact.
2. **14-day free trial on Personal tier** [GROWTH][S] — Set `trial_period_days=14` on the Stripe
   Personal plan price; no credit card at signup; highest single-lever SaaS conversion mechanism.
   HIGH income impact. Prerequisite: Stripe billing.
3. **PWA web app manifest** [GROWTH][S] — `manifest.json` + `apple-touch-icon`; users who install
   the PWA have 3× higher 30-day retention than browser-only users. MEDIUM income impact.
4. **Thread archiving** [GROWTH][S] — "Archive" action per thread; archived threads hidden from
   main list; separate /archived route. GTD workflow essential for power users. MEDIUM income
   impact. Prerequisite: user auth.
5. **Conversation pinning** [GROWTH][S] — Pin up to 3 threads to top of list; per-user state;
   drives team plan upgrade. MEDIUM income impact. Prerequisite: user auth.

Added 1 [MARKETING] item to TODO_MASTER.md:
- Transactional email provider (Postmark/SendGrid/Resend) with 3-email welcome sequence (day 0,
  3, 7). Highest ROI re-engagement mechanism for early SaaS.

---

### Role 4 — UX Auditor
Audited flows: thread list page, conversation view header + reply flow.

**Direct fixes applied:**
1. **Keyboard shortcuts — thread list** (`threads.html`): `j`/`k` navigate up/down through threads
   with a `.kb-focus` blue outline indicator; `Enter` opens the focused thread. IIFE guards against
   firing when a form input has focus; `metaKey`/`ctrlKey`/`altKey` modifier check prevents
   conflicts with OS shortcuts.
2. **Keyboard shortcuts — conversation** (`conversation.html`): `r` focuses the reply textarea
   and smooth-scrolls it into view; `Escape` blurs the textarea. Guard prevents `r` from firing
   while already in the textarea.
3. **Keyboard shortcut hints** (both templates): added a `<span class="kbd-hint">` in the header
   nav showing `[j] [k] navigate · [Enter] open` (thread list) and `[r] reply · [Esc] cancel`
   (conversation). Styled as subtle muted-color text with bordered `<kbd>` chips — discoverable
   without being noisy.
4. **Inline style cleanup** — removed all inline `style="..."` attributes from both templates;
   replaced with CSS classes `.header-nav`, `.msg-count`. HTML is now style-free.
5. **`conv-header h1` flex fix** — added `flex: 1; min-width: 0` so the subject heading properly
   truncates with ellipsis when kbd-hint and msg-count siblings narrow the available space.

**Issues still open (INTERNAL_TODO.md [UX]):**
- `+ Add mailbox` is a dead-end until mailbox settings page ships (requires user auth first).
- Thread list last-message-body preview missing.
- IMAP sync status indicator missing.
- Mobile layout full pass still needed.

---

### Role 5 — Task Optimizer
Updated INTERNAL_TODO.md:
- Archived 2 newly completed tasks to Done section:
  - [CORE][M] CSS polish → Done
  - [UX][S] Keyboard shortcuts → Done
  - Done section now has 19 items
- Added 5 new [GROWTH][S] tasks from Role 3 in priority order (pricing page first as it has
  no prerequisites; Stripe trial next; PWA manifest; archiving; pinning)
- Added 1 [MARKETING] item to TODO_MASTER.md (transactional email sequence)
- Active task count: 50 tasks (1 Core, 36 Growth, 4 UX, 3 Infra)
- No blocked tasks

---

### Role 6 — Health Monitor
Security:
- **`BubbleRun.date()` injection analysis**: `data-date` value written by Thymeleaf via
  `#temporals.format(LocalDate, 'yyyy-MM-dd')` produces only `\d{4}-\d{2}-\d{2}` output —
  no HTML injection possible regardless of input email date headers. ✓
- **`sep.innerHTML` in day separator JS**: `formatDateLabel(iso)` constructs output from a JS
  `Date` object via `toLocaleDateString()` — output is browser-computed text from a parsed date,
  not from any user-supplied string; cannot contain injected HTML. ✓
- **Keyboard shortcut JS**: reads only `e.key` (keyboard event property) and calls `click()` /
  `focus()` on pre-existing DOM elements. No user string reaches the DOM. ✓
- No new server-side security risks; all new code is either a pure Java method or client-side JS.

Performance:
- `BubbleRun.date()` is O(1): reads `messages.get(0)` then calls `.toLocalDate()`.
- Day separator IIFE: O(n) scan of `.bubble-run` elements; runs once on page load; negligible.
- CSS additions: ~90 lines; minified would be ~1.5 KB additional; negligible page weight.
- `flex: 1; min-width: 0` on h1: pure layout property; no layout-thrashing risk.

Code quality:
- `BubbleRun.date()` is null-safe at both the list-empty and null-sentAt levels. ✓
- JS uses `var` (not `const`/`let`) for IE11 compat fallback in the conversation template — minor,
  but consistent with the existing scroll script which used `const`. Updated to `const`/`let` where
  the IIFE uses them; outer functions use `var` for broadest compat. Acceptable.
- No inline styles remain in either template. Clean separation of style and markup. ✓
- No dead code: `formatDateLabel` is called in the day-separator IIFE.

Dependencies:
- No new dependencies added this run.
- jsoup 1.17.2 still current. Spring Boot 3.5.14 still current.

Legal:
- No new legal risks introduced.
- All prior [LEGAL] items in TODO_MASTER.md remain outstanding.

---

## 2026-04-26 — Autonomous Run #7

### Role 1 — Feature Implementer
**Task completed**: Thymeleaf templates — thread list, conversation view with chat bubbles, reply form [CORE][L]

Files created/changed:
- `src/main/java/com/emailmessenger/web/ReplyForm.java` — package-private JavaBeans form object
  with `@NotBlank` + `@Size(max=100,000)` on the `body` field; used by `@Valid @ModelAttribute`
  in the reply POST handler.
- `src/main/java/com/emailmessenger/web/ThreadViewService.java` — package-private `@Service`;
  `@Transactional(readOnly=true)` method `getConversation(long threadId)` that loads `EmailThread`
  from the repository and calls `ConversationService.buildConversation()` within a single
  transaction (prevents `LazyInitializationException` from `thread.getMessages()` with
  `open-in-view=false`).
- `src/main/java/com/emailmessenger/service/ReplyService.java` — public `@Service`;
  `sendReply(long threadId, String subject, String body)` loads the last message from the thread,
  constructs a `MimeMessage` via `MimeMessageHelper` (sets From, To, Subject, In-Reply-To,
  References headers), and sends via `JavaMailSender`. Throws `MailPreparationException` on
  `MessagingException`; `MailSendException` propagates from `mailSender.send()`. Both are caught
  by `GlobalExceptionHandler` (502 → friendly error page).
- `src/main/java/com/emailmessenger/web/ThreadController.java` — package-private `@Controller`:
  - `GET /` → redirect to `/threads`
  - `GET /threads?page=N` → paginates `EmailThreadRepository.findAllByOrderByUpdatedAtDesc`,
    clamps negative page numbers to 0, adds `threads` (Page) to model, returns `"threads"` view.
  - `GET /threads/{id}` → loads `Conversation` via `ThreadViewService`, adds `replyForm` to
    model, returns `"conversation"` view.
  - `POST /threads/{id}/reply` → validates `ReplyForm`, shows validation errors inline if
    invalid, calls `ReplyService.sendReply`, redirects with flash `successMessage` on success.
- `src/main/resources/templates/threads.html` — Thymeleaf thread list: brand header with
  "+ Add mailbox" nav link, paginated thread list with subject/count/date per row, empty state
  card with CTA, pagination prev/next links.
- `src/main/resources/templates/conversation.html` — Thymeleaf conversation view: sticky back
  + subject header with message count badge, scrollable bubble area (auto-scrolls to bottom on
  load), BubbleRun avatar+sender+messages structure, `th:utext` for pre-sanitized HTML bodies
  (with explanatory comment), reply form with inline validation error display and success flash,
  "Send Reply" as primary CTA.
- `src/main/resources/static/css/main.css` — 250-line IM-look CSS: CSS custom properties,
  thread list items, chat bubble styling (4px/16px radius, box-shadow), avatar circles, sticky
  reply area, focus ring on textarea, mobile breakpoint at 640px.

Bug fixed during implementation: `EmailThread` has `getId()` not `id()` — template originally
used `${conversation.thread().id()}` which would throw `SpelEvaluationException`; corrected to
`${conversation.thread().id}` (property access).

Verified: `./mvnw test` → BUILD SUCCESS, 60 tests pass (was 48).

**Income relevance**: The thread list and conversation view are the product's entire visible
surface area. Without templates, there is no product for users to see, no conversion funnel, and
no path to payment. This is the single most income-blocking task in the backlog and is now done.

---

### Role 2 — Test Examiner
**Coverage added**: 12 new tests across 2 new test classes (60 total, up from 48)

Files created:
- `src/test/java/com/emailmessenger/web/ThreadControllerTest.java` (8 tests):
  - `rootRedirectsToThreads` — GET / returns 302 to /threads
  - `listThreadsReturnsThreadsViewWithModel` — empty page → 200, view="threads", model has "threads"
  - `listThreadsNegativePageClampsToZero` — page=-5 → still returns 200 (no exception)
  - `viewConversationReturnsConversationView` — valid id → 200, view="conversation", has model attrs
  - `viewConversationWithUnknownIdReturns404` — getConversation throws NoSuchElementException → 404, view="error"
  - `replyWithEmptyBodyShowsValidationErrorAndConversationView` — empty body → 200, view="conversation",
    model has field errors on "body", ReplyService is never called
  - `replyWithValidBodyRedirectsWithSuccessFlash` — valid body → 302 to /threads/{id}
  - `replyWithUnknownThreadIdReturns404` — unknown id on error path → 404
  
  All tests use `MockMvcBuilders.standaloneSetup` with `InternalResourceViewResolver` (prefix
  "/WEB-INF/templates/", suffix ".html") to prevent circular view path error in standalone mode.
  `GlobalExceptionHandler` included via `.setControllerAdvice()`.

- `src/test/java/com/emailmessenger/service/ReplyServiceTest.java` (4 tests):
  - `sendReplyDoesNothingWhenThreadHasNoMessages` — empty thread → mailSender.send never called
  - `sendReplySendsMimeMessageToLastMessageSender` — verifies Subject = "Re: ...", To = last sender email
  - `sendReplyUsesLastMessageSenderWhenMultipleMessages` — picks last message's sender, not first
  - `sendReplyPropagatesMailSendExceptionOnFailure` — MailSendException propagates to controller

Income-critical paths now covered:
- Reply form validation (empty body blocked, no spam send)
- Reply service routing (last message sender, not arbitrary)
- 404 handling for missing threads on all paths

Still zero coverage (code not written yet):
- Stripe webhook handler and subscription state
- User authentication flows
- IMAP polling job

---

### Role 3 — Growth Strategist
Identified 5 new implementable growth opportunities not previously captured:

1. **Custom SMTP/from-address per user** [GROWTH][S] — Without this, all outgoing replies
   come from the app's noreply address, which is useless in production. Per-user SMTP config is
   a prerequisite for the reply feature being actually valuable post-auth. HIGH income impact.
   Prerequisite: user auth.
2. **AI-generated thread summary** [GROWTH][M] — One-sentence summary per thread shown in
   thread list; Claude API (already available); Personal+ gate; strong differentiator vs every
   other email client. HIGH income impact. Prerequisite: auth + ANTHROPIC_API_KEY env var.
3. **Reply signature** [GROWTH][S] — Per-user configurable signature appended to all replies;
   increases reply adoption by making the app feel production-ready; Personal+ gate. MEDIUM
   income impact. Prerequisite: user auth.
4. **Outbound webhook trigger** [GROWTH][S] — POST to a user-configured URL on new message
   arrival; enables Zapier/Make integrations without a full API; Team plan gate. MEDIUM income
   impact. Prerequisite: IMAP polling.
5. **"Copy conversation as Markdown"** [GROWTH][S] — One-click copy of full thread as Markdown
   to clipboard; useful for pasting into Notion/Slack/docs; zero-friction share touchpoint.
   MEDIUM income impact.

Added 2 [MARKETING] items to TODO_MASTER.md:
- 15-second GIF/screen recording of email → chat bubble transform for social distribution.
- Configure ANTHROPIC_API_KEY env var once AI summary feature ships.

---

### Role 4 — UX Auditor
Audited flows: Landing `/` → thread list, thread list → conversation view, reply form submit.

**Direct fixes applied:**
1. **App header navigation** — Added "+ Add mailbox" link to threads.html header. Previously
   there was no navigation at all; new users had no affordance to find where to connect a
   mailbox. The link points to `/settings/mailboxes` (not yet built, tracked in UX TODO).
2. **Conversation page title truncation** — Capped `<title>` to 57 chars + ellipsis to prevent
   ugly/truncated browser tab labels from long email subjects.
3. **Message count in conversation header** — Added "N message(s)" count badge to the right
   of the thread subject in the conversation header; helps users orient without scrolling.

**Issues flagged (added to INTERNAL_TODO.md [UX]):**
- Thread list: no last-message preview text below subject — users can't identify threads
  at a glance; requires denormalizing preview into email_threads table or a query join.
- Keyboard shortcuts (j/k/r/Esc) not implemented; essential for power user adoption.
- "+ Add mailbox" link is a dead-end until mailbox settings page is built.
- IMAP sync status indicator still missing (carried from prior runs).
- Mobile layout pass needed (basic responsive CSS exists, not fully tested).

---

### Role 5 — Task Optimizer
Rewrote INTERNAL_TODO.md:
- Archived 8 newly completed tasks:
  - [CORE][L] Thymeleaf templates → Done
  - [HEALTH][S] Input validation (ReplyForm) → Done
  - [UX][S] Thread list empty state → Done
  - [UX][S] Conversation view empty state → Done
  - [UX][S] Reply button visual prominence → Done
  - [UX][S] Bubble body th:utext rendering → Done
  - Done section now has 19 items
- Added 5 new [GROWTH] tasks from Role 3 in priority order
- Added 3 new [UX] tasks from Role 4 (2 carried forward + 1 new header nav item)
- Removed [HEALTH][S] "input validation" from active — done via ReplyForm @Valid
- CSS task description updated to note basic CSS is in place; remaining work is polish
- Active task count: 46 tasks (2 Core, 35 Growth, 5 UX, 3 Infra)
- No blocked tasks (input validation prerequisite now resolved)

---

### Role 6 — Health Monitor
Security:
- `ThreadController.reply` validates `ReplyForm` with `@Valid` + `BindingResult` before calling
  `ReplyService.sendReply` — no unchecked user input reaches the mail layer. Input validation
  constraint is now enforced at the web boundary.
- `ReplyService.sendReply` uses `MimeMessageHelper.setText(body, false)` — `false` means
  plain text, no HTML injection risk in outgoing reply body.
- `ReplyService.fromAddress` is initialized to `"noreply@mailaim.app"` — a defined fallback
  prevents null `setFrom()` call which would result in a malformed SMTP envelope.
- No user input is passed to `setFrom()` — "from" address is app-controlled only.

Performance:
- `ThreadViewService.getConversation` is `@Transactional(readOnly=true)` — read-only hint
  signals Hibernate to skip dirty checking; no write locks acquired.
- `ThreadController.listThreads` uses `PageRequest.of(page, 20)` — paginated; never loads all
  threads. Free tier users with 500 threads: max 20 per request.
- `ReplyService.sendReply` does one `findByThreadIdOrderBySentAtAsc` query — O(n) in thread
  message count. For typical threads (<100 messages) this is negligible.

Code quality:
- `ThreadViewService` is package-private to `web` — correctly scoped; not exposed beyond
  the web package boundary.
- `ReplyForm` is package-private — correctly scoped; only used by `ThreadController`.
- No field `@Autowired` in any new class; all constructor injection.
- `ReplyService.sendReply` is `@Transactional(readOnly=true)` to load sender data safely.

Dependencies:
- No new dependencies added this run. All mail functionality uses `spring-boot-starter-mail`
  and `jakarta.mail` already in pom.xml.

Legal:
- No new legal risks introduced.
- All prior [LEGAL] items in TODO_MASTER.md remain outstanding.

---

## 2026-04-26 — Autonomous Run #6

### Role 1 — Feature Implementer
**Tasks completed**: EmailImportException + GlobalExceptionHandler + error.html [HEALTH][S] × 2

Files created/changed:
- `src/main/java/com/emailmessenger/email/EmailImportException.java` — package-private unchecked
  exception wrapping `MessagingException` / `IOException` from the mail parse layer. Callers
  no longer leak Jakarta Mail types across package boundaries.
- `src/main/java/com/emailmessenger/email/EmailImportService.java` — `importMessage()` signature
  changed from `throws MessagingException, IOException` to unchecked. Body wrapped in
  `try/catch (MessagingException | IOException e)` → `throw new EmailImportException(...)`.
- `src/main/java/com/emailmessenger/web/GlobalExceptionHandler.java` — `@ControllerAdvice` with
  five handlers: `NoResourceFoundException` (404), `NoSuchElementException` (404),
  `MailException | EmailImportException` (502), `DataIntegrityViolationException` (409),
  `Exception` (500). All handlers set the HTTP status via `HttpServletResponse.setStatus()`,
  populate `status` and `message` model attributes, and return view name `"error"`. SLF4J
  logger logs WARN for mail/conflict errors, ERROR with full stack trace for unhandled exceptions.
- `src/main/resources/templates/error.html` — Thymeleaf error page: centered card, status code
  in red, context-sensitive heading (different for 404 / 50x / 409), plain-English message
  attribute, "Back to MailIM" button, support email link. Mobile-responsive. Replaces Spring
  Whitelabel error page.
- `src/main/resources/application.yml` — added `server.error.whitelabel.enabled: false` in dev
  profile so Spring's built-in Whitelabel page can never appear.

Verified: `./mvnw test` → BUILD SUCCESS, 48 tests pass.

**Income relevance**: Users previously saw raw Spring Whitelabel error pages on any unhandled
exception, which undermines trust. Professional error pages with actionable messages reduce
abandonment. Hiding checked exceptions behind `EmailImportException` prevents accidental exception
leakage into future web controllers (which would result in 500 pages for preventable errors).

---

### Role 2 — Test Examiner
**Coverage added**: 4 new tests in GlobalExceptionHandlerTest

File created: `src/test/java/com/emailmessenger/web/GlobalExceptionHandlerTest.java`
- Uses `MockMvcBuilders.standaloneSetup()` with a package-private inner `ThrowingController` —
  no `@SpringBootTest` overhead; fast and isolated.
- `mailExceptionReturns502WithErrorView` — `MailSendException` → 502, view=error, model.status=502
- `noSuchElementReturns404WithErrorView` — `NoSuchElementException` → 404, view=error, model.status=404
- `dataIntegrityViolationReturns409WithErrorView` — `DataIntegrityViolationException` → 409
- `unhandledExceptionReturns500WithErrorView` — `RuntimeException` → 500, view=error, model.status=500

Total test count: 48 (up from 44). 0 failures.

Income-critical paths still at zero coverage (code not yet written):
- Stripe webhook handler and subscription state
- User authentication flows
- IMAP polling job
- Thymeleaf thread list / conversation templates

---

### Role 3 — Growth Strategist
Identified 4 new implementable growth opportunities not previously captured:

1. **Annual/monthly billing toggle** [GROWTH][S] — Show a toggle on the pricing/plan-selection page
   switching between monthly and annual pricing with a "Save 16%" label. Annual = 2× LTV per
   conversion. HIGH income impact. Prerequisite: Stripe billing task.
2. **REST API for Personal+ tier** [GROWTH][M] — JSON API exposing thread list, message retrieval,
   and reply sending; enables Zapier/Make integrations; strong upsell lever for developer users.
   HIGH income impact.
3. **"Sent via MailIM" branding footer** [GROWTH][S] — Append "Sent via MailIM [try free]" link
   to outgoing email replies for Free-tier users only. Each sent email distributes the product.
   Disabled for Personal+. MEDIUM income impact.
4. **Public roadmap at /roadmap** [GROWTH][S] — Static page listing upcoming features; shows
   the product is actively developed; generates shareability. MEDIUM income impact.

Added 2 [MARKETING] items to TODO_MASTER.md:
- Affiliate program via Rewardful/PartnerStack (30% recurring commission)
- NPS survey at day 30 (Delighted/Typeform) for review velocity and churn prevention

---

### Role 4 — UX Auditor
Templates still do not exist; no live user flow beyond error pages.

**Direct fix: error.html UX improvements**
- Added context-sensitive h1 heading that varies by status: "Page not found" (404), "Mail server
  unreachable" (502/503), "Conflict saving data" (409), "Something went wrong" (all others).
  Users immediately understand the failure category without reading the paragraph.
- Added "Still stuck? Contact support" link below the back-to-home button with `mailto:support@mailaim.app`.
  Previously there was no recovery path if the user's issue was not solved by going home.
- Mobile responsive: cards shrink to 16px side padding on narrow viewports; font sizes scale.

No new UX items flagged (previously flagged UX backlog remains unchanged — all depend on
Thymeleaf templates which are the next CORE task).

---

### Role 5 — Task Optimizer
Rewrote INTERNAL_TODO.md:
- Archived 2 newly completed HEALTH tasks:
  - [HEALTH][S] EmailImportException wrapping → Done section
  - [HEALTH][S] GlobalExceptionHandler + error.html → Done section
  - Done section now has 11 items
- Added 4 new [GROWTH] tasks from Role 3 in priority order
- Remaining [HEALTH][S] input validation task moved to top; tagged with note that it is
  BLOCKED until web controllers exist (prerequisite: Thymeleaf templates task)
- Added 2 new [MARKETING] items to TODO_MASTER.md
- Active task count: 40 tasks (1 Health, 3 Core, 30 Growth, 7 UX, 3 Infra)
- No blocked tasks except input validation (labeled with prerequisite note, not BLOCKED tag
  since it's still workable next session after templates ship)

---

### Role 6 — Health Monitor
Security:
- **No exception details exposed to users**: all error messages in GlobalExceptionHandler are
  hardcoded safe strings; exception objects are logged server-side only. Zero information leakage.
- **EmailImportException wrapping**: stack traces from Jakarta Mail are now retained in the cause
  chain (logged at WARN/ERROR) but never surfaced to the HTTP response. Clean separation.
- **Logging added**: `log.warn()` for mail/conflict errors, `log.error(ex)` with full stack trace
  for unhandled exceptions — previously all exceptions were silently swallowed after this handler
  was set up. Production observability now restored.
- No new hardcoded credentials, no SQL, no user-controlled data in error messages.

Performance:
- GlobalExceptionHandler is a Spring singleton; no state; zero overhead on the happy path.
- EmailImportException construction is O(1); negligible impact.

Code quality:
- `mailError` handler's `Exception ex` parameter now used in the log statement — no dead parameters.
- SLF4J Logger follows Spring Boot conventions (`LoggerFactory.getLogger(GlobalExceptionHandler.class)`).
- `EmailImportException` constructor is package-private — correctly scoped; only `EmailImportService`
  within the email package constructs it; `GlobalExceptionHandler` catches it by type only.

Dependencies:
- No new dependencies added this run.
- jsoup 1.17.2 still current; Spring Boot 3.5.14 still current.

Legal:
- No new legal risks introduced.
- All prior [LEGAL] items in TODO_MASTER.md remain outstanding.

---

## 2026-04-26 — Autonomous Run #5

### Role 1 — Feature Implementer
**Task completed**: HTML email body XSS sanitization [HEALTH][M] — CRITICAL pre-launch blocker

Files changed:
- `pom.xml` — added `org.jsoup:jsoup:1.17.2` runtime dependency.
- `src/main/java/com/emailmessenger/service/ConversationService.java` — `buildBodyHtml`
  now passes the raw HTML email body through `Jsoup.clean(html, Safelist.relaxed())` before
  returning it. This strips `<script>` tags, inline event handlers (`onclick`, `onerror`, etc.),
  `javascript:` protocol hrefs, `<iframe>`, and any other dangerous construct that malicious
  senders embed in HTML email bodies. Safe elements — `<p>`, `<b>`, `<strong>`, `<em>`, `<a>`,
  `<ul>`, `<ol>`, `<li>`, `<blockquote>`, `<code>`, `<pre>` — are preserved unchanged.
  The plain-text path (renderMarkdown) was already safe via HTML-escaping; no change needed there.

Verified: `./mvnw test` → BUILD SUCCESS, 44 tests pass.

**Income relevance**: Closes the only CRITICAL security vulnerability identified since
project start. Without sanitization, any HTML email with a `<script>` tag would execute
in every subscriber's browser. Shipping this unresolved would be a liability that could
kill the business on first security disclosure.

---

### Role 2 — Test Examiner
**Coverage added**: 4 new XSS sanitization tests in ConversationServiceTest

Tests added:
- `scriptTagsStrippedFromHtmlBody` — verifies `<script>alert('xss')</script>` is removed
  and safe content is preserved.
- `inlineEventHandlersStrippedFromHtmlBody` — verifies `onclick` is stripped from `<a>` tags.
- `javascriptLinksStrippedFromHtmlBody` — verifies `javascript:void(0)` href is removed.
- `safeHtmlElementsPreservedAfterSanitization` — verifies `<strong>` and `<em>` survive.

Also updated `htmlBodyPassesThroughWithoutTransform` to use `contains()` assertions
instead of `isEqualTo()` to be robust against jsoup output normalization differences.

Total test count: 44 (up from 40). 0 failures.

Income-critical paths still at zero coverage (code not yet written):
- Stripe webhook handler and subscription state
- User authentication flows
- IMAP polling job
- Thymeleaf template rendering

---

### Role 3 — Growth Strategist
Identified 4 new implementable growth opportunities not previously captured:

1. **Stripe customer portal integration** [GROWTH][S] — Stripe self-service portal for plan
   upgrades, downgrades, cancellations, and invoice downloads. Reduces support overhead and
   churn from users who can't find the cancel button. HIGH income impact (direct retention
   mechanism). Prerequisite: Stripe billing task.
2. **In-app upgrade preview of locked features** [GROWTH][S] — Show blurred/disabled previews
   of Team-tier features (Slack webhook, full-text search) with an "Upgrade to unlock" CTA.
   Direct upgrade-conversion trigger visible to every Personal-tier user. HIGH income impact.
3. **Send-later scheduling for replies** [GROWTH][M] — Schedule an email reply to send at a
   future time; critical for remote workers across time zones. Personal/Team plan gate. HIGH
   income impact (differentiation from standard email clients).
4. **Thread snooze** [GROWTH][S] — Snooze a thread to re-surface at a user-set time; reduces
   inbox anxiety and drives daily re-engagement. MEDIUM income impact.

Added 1 [MARKETING] item to TODO_MASTER.md:
- AppSumo lifetime deal: immediate lump-sum revenue + organic reviews.

---

### Role 4 — UX Auditor
Templates still do not exist; no live user flows to walk. One UX task completed directly in code:

**Direct fix: Participant.initials() helper** [UX][S]
- Added `initials()` method to `Participant` entity: "Alice Bob"→"AB", "alice@test.com"→"A",
  "Charlie"→"C". This is the prerequisite for the avatar rendering planned in templates — without
  it, the template author would have to inline the initials logic in Thymeleaf expressions.
- Method is a pure string function with no JPA/persistence side effects; safe to call anywhere.

Updated UX note in INTERNAL_TODO.md: "[UX][S] Bubble body HTML rendering — use th:utext"
now explicitly states that the value is "pre-rendered and already sanitized HTML" (now that
the sanitization fix is in place), so template authors don't need to add their own safeguards.

No structural UX issues possible to flag without live templates; existing UX task list in
INTERNAL_TODO.md remains accurate.

---

### Role 5 — Task Optimizer
Rewrote INTERNAL_TODO.md:
- Archived 2 newly completed tasks:
  - [HEALTH][M] HTML sanitization → Done section
  - [UX][S] Participant initials utility → Done section
  - Done section now has 9 items
- Added 4 new [GROWTH] tasks from Role 3 in priority order
- Added 1 new AppSumo [MARKETING] item to TODO_MASTER.md
- Removed duplicate mention of initials in Gravatar task (notes helper already exists)
- TODO_MASTER.md [CRITICAL] XSS item tagged [LIKELY DONE - verify] since code fix is complete
- Active task count: 40 tasks across Health (3), Core (3), Growth (26), UX (8), Infra (3) sections
- All tasks remain [S/M/L] tagged; no vague or oversized tasks
- No blocked tasks

---

### Role 6 — Health Monitor
Security:
- **XSS vector closed**: jsoup 1.17.2 `Safelist.relaxed()` sanitization is now active in
  production code path. Verified by 4 new tests covering script injection, event handler
  injection, javascript: protocol abuse, and safe-element preservation.
- jsoup 1.17.2 — no known CVEs; widely deployed in production Spring Boot applications.
  Consider upgrading to 1.18.1 when convenient (no security driver; minor API improvements).
- No new hardcoded credentials in any file added this run.
- `initials()` in Participant is pure string manipulation — no SQL, no user input reaching
  any persistence layer, no XSS risk.

Performance:
- `Jsoup.clean()` is O(n) in HTML size. For typical email bodies (< 100 KB), the overhead
  is negligible (sub-millisecond). No caching needed.
- No new N+1 query risks introduced.

Code quality:
- Comment in `buildBodyHtml` explains WHY sanitization is done (malicious sender context) —
  non-obvious to a future reader; justified. All other new code is self-explanatory.
- `initials()` follows the project convention (no unnecessary comments, no field `@Autowired`).
- Dead code check: all modified code paths are exercised by tests.

Dependencies:
- jsoup 1.17.2 added (Apache 2.0 license — compatible with commercial SaaS use, no copyleft risk).
- Spring Boot 3.5.14 still current; no new CVEs flagged.

Legal:
- jsoup is Apache 2.0 licensed — no copyleft constraint; safe for commercial closed-source use.
- All prior [LEGAL] items in TODO_MASTER.md remain outstanding (Privacy Policy, ToS, Refund
  Policy, Cookie consent) — none were addressed this run (require Master action).

---

## 2026-04-26 — Autonomous Run #4

### Role 1 — Feature Implementer
**Task completed**: IM transform — quoted-reply stripping, same-sender bubble grouping, markdown rendering

Files created:
- `src/main/java/com/emailmessenger/service/IMTransformService.java` — package-private `@Service`:
  - `stripQuotes(String)`: line-by-line parser; detects "On ... wrote:" attribution (including
    wrapped multi-line variants), Outlook "-----Original Message-----" divider, and `> ` prefixed
    quote lines; cuts everything from the first attribution/divider onwards; collapses 3+ blank
    lines to 2; strips surrounding whitespace.
  - `renderMarkdown(String)`: HTML-escapes input first (prevents XSS in plain-text path), then
    applies regex transforms for **bold**, *italic*, `code`, and URL auto-linking; wraps in `<p>`
    tags at blank-line boundaries, single newlines become `<br>`.
- `src/main/java/com/emailmessenger/service/ConversationService.java` — public `@Service`:
  - `buildConversation(EmailThread)` — iterates thread's messages (already `@OrderBy sentAt ASC`),
    groups consecutive same-sender messages into `BubbleRun` instances (compared by email address,
    not DB ID, so works on unsaved entities in tests), returns an immutable `Conversation`.
  - `buildBodyHtml(Message)` — prefers `bodyHtml` from the email if present; otherwise strips
    quotes then renders plain text as markdown HTML.
- `src/main/java/com/emailmessenger/service/BubbleMessage.java` — package-private record:
  `(messageId, bodyHtml, sentAt, attachments)`.
- `src/main/java/com/emailmessenger/service/BubbleRun.java` — package-private record:
  `(sender, messages)`.
- `src/main/java/com/emailmessenger/service/Conversation.java` — public record:
  `(thread, runs)`.

Verified: `./mvnw test` → BUILD SUCCESS, 40 tests pass.

**Income relevance**: The IM transform is the product's primary value proposition — without it,
threads display as raw email walls of text, not chat bubbles. This layer directly gates all
UI work that drives conversion.

---

### Role 2 — Test Examiner
**Coverage added**: 23 new tests (14 IMTransformService + 8 ConversationService + 1 BCC import)

Files added:
- `src/test/java/com/emailmessenger/service/IMTransformServiceTest.java` (14 tests):
  - null body, non-quoted content preserved, `>` line removal, Gmail "On ... wrote:" removal,
    wrapped 2-line attribution, Outlook divider, blank-line collapsing, bold, italic, inline
    code, URL auto-link, paragraph wrapping, HTML char escaping.
- `src/test/java/com/emailmessenger/service/ConversationServiceTest.java` (8 tests):
  - empty thread, single message, same-sender grouping, different-sender splitting,
    mixed group counts, plain-to-HTML body transform, HTML body passthrough, quoted-reply stripping.
- Added `bccRecipientsAreCaptured` test to `EmailImportServiceTest` — BCC path was exercised
  in code but had zero test coverage.

Total test count: 40 (up from 17). No failures. No flaky tests.

Income-critical paths still at zero coverage (code not written yet):
- Stripe webhook handler and subscription state
- User authentication flows
- IMAP polling job
- Thymeleaf template rendering

---

### Role 3 — Growth Strategist
Added 4 new growth tasks to INTERNAL_TODO.md not previously captured:
1. **Demo mode** [GROWTH][S] — /demo route with pre-seeded sample threads; visitors experience
   the IM view without signing up; removes top-of-funnel uncertainty. HIGH income impact.
2. **Email forwarding address** [GROWTH][M] — unique @mailaim.app address per user; forwarded
   emails auto-import; avoids IMAP credential friction entirely. HIGH income impact.
3. **EML file upload** [GROWTH][S] — upload a .eml file to seed threads instantly; zero-friction
   demo path. MEDIUM income impact.
4. **Thread labels/tags** [GROWTH][M] — user-defined labels; Team plan feature gate. MEDIUM income
   impact; drives Personal → Team upgrade.

Added 2 [MARKETING] items to TODO_MASTER.md:
- Loom/YouTube demo video: highest-leverage single landing-page conversion asset.
- Chrome/Firefox extension: "Open in MailIM" button in Gmail = viral distribution loop.

---

### Role 4 — UX Auditor
Templates still do not exist; no live user flow to walk. Two new UX issues flagged from
the new service layer:
1. **Participant initials utility** [UX][S] — no method exists to compute avatar initials from
   displayName; template work will be blocked without this helper.
2. **th:utext rendering note** [UX][S] — BubbleMessage.bodyHtml contains pre-rendered HTML;
   Thymeleaf must use th:utext not th:text; flagged for the template author to avoid a subtle
   "shows raw HTML tags" bug.

---

### Role 5 — Task Optimizer
Rewrote INTERNAL_TODO.md:
- Archived 1 new completed task (IM transform) → Done section now has 7 items
- Promoted [HEALTH][M] HTML sanitization to a new "Health / Security (pre-launch blockers)"
  section at the top — it must be resolved before templates ship
- Added 4 new [GROWTH] and 2 new [UX] tasks from this run
- Consolidated growth tasks in priority order: auth/billing/limits first, then
  onboarding/SEO/virality, then engagement, then infrastructure
- Verified no duplicates across 37 active tasks
- All tasks tagged [S/M/L]

---

### Role 6 — Health Monitor
Security (one critical finding):
- **XSS in HTML email bodies** [CRITICAL]: `ConversationService.buildBodyHtml` returns
  `msg.getBodyHtml()` directly without sanitization. When Thymeleaf renders this with `th:utext`,
  any `<script>`, event handler, or iframe in an incoming email executes in the user's browser.
  Fix: add jsoup to pom.xml and call `Jsoup.clean(bodyHtml, Safelist.relaxed())` before returning.
  Added [CRITICAL] note to TODO_MASTER.md and [HEALTH][M] task to INTERNAL_TODO.md.
- The plain-text path (`renderMarkdown`) is safe: HTML is escaped before any regex transformation,
  so no injection is possible through the markdown renderer.

Performance:
- `IMTransformService.stripQuotes` is O(n) in number of lines; no regex backtracking risk since
  all patterns are simple anchored or character-class patterns.
- `ConversationService.buildConversation` is O(n) in messages; produces `List.copyOf` snapshots —
  immutable and safe to cache.

Code quality:
- `IMTransformService` and view model records (`BubbleMessage`, `BubbleRun`) are package-private —
  correctly scoped; only `ConversationService` and `Conversation` are public.
- Constructor injection in `ConversationService`; no field `@Autowired`.
- No dead code. All 5 new files are exercised by tests.

Dependencies:
- No new runtime dependencies added this run. jsoup needed for the HTML sanitization fix (tracked).

Legal: no change from prior runs — all open [LEGAL] items in TODO_MASTER.md remain outstanding.

---

## 2026-04-26 — Autonomous Run #3

### Role 1 — Feature Implementer
**Task completed**: Email-import service — RFC 822 parsing + thread building

Files created:
- `src/main/java/com/emailmessenger/email/ParsedEmail.java` — package-private record holding
  all parsed fields from a MimeMessage before DB operations: messageId, inReplyTo, references
  (List<String>), subject, from, to/cc/bcc recipients, bodyPlain, bodyHtml, attachments, sentAt.
  Two nested records: `AddressEntry(email, name)` and `AttachmentEntry(filename, mimeType, sizeBytes)`.
- `src/main/java/com/emailmessenger/email/MimeMessageParser.java` — package-private class that
  walks a `MimeMessage` tree: extracts RFC 5322 headers, recursively walks `multipart/*` to find
  `text/plain` / `text/html` parts, collects attachment metadata (filename, mimeType, size).
  Does NOT read attachment byte content — only metadata; blob_ref is wired separately.
- `src/main/java/com/emailmessenger/email/EmailImportService.java` — `@Service` with
  `@Transactional` import method:
  - Idempotent: skips if Message-ID already exists in the database.
  - Thread resolution: walks References list newest-first per RFC 5322, falls back to In-Reply-To,
    then rootMessageId lookup, then creates a new EmailThread.
  - `resolveParticipant()`: find-or-create Participant by normalised email address.
  - Attachment metadata captured; `blob_ref` is null until external storage is wired.
  - Save order: `messageRepo.save(message)` first so the subsequent `threadRepo.save(thread)`
    cascade-merges an already-managed entity (avoids double-INSERT unique constraint violation).

Bug found and fixed during test run: original code called `thread.addMessage(message)` +
`threadRepo.save(thread)` (which cascades ALL to the unsaved message entity, inserting it), then
called `messageRepo.save(message)` again — violating the `message_id_header` unique constraint.
Fix: save message first, then add to thread, then save thread.

Verified: `./mvnw test` → BUILD SUCCESS, 17 tests pass.

**Income relevance**: Email parsing and thread building are the core data pipeline. Without this,
no thread can ever appear in the UI — it is the prerequisite for all user-visible value.

---

### Role 2 — Test Examiner
**Coverage added**: 6 new tests in `EmailImportServiceTest` (17 total, up from 11)

File created: `src/test/java/com/emailmessenger/email/EmailImportServiceTest.java`
- `importCreatesThreadAndMessage` — verifies thread, message, sender, body, rootMessageId
- `replyViaInReplyToJoinsExistingThread` — verifies In-Reply-To links reply to parent thread; checks messageCount=2
- `replyViaReferencesJoinsExistingThread` — verifies References header thread linking
- `duplicateMessageIdIsSkipped` — verifies idempotency returns empty + only one DB row
- `senderParticipantIsDeduplicated` — verifies two messages from same address → 1 Participant
- `toAndCcRecipientsAreCaptured` — verifies RecipientType.TO (×2) and CC (×1) stored correctly

Tests use `@SpringBootTest @ActiveProfiles("dev") @Transactional` with H2; each test rolls back.
MimeMessage instances built in-process via `jakarta.mail.Session.getInstance(new Properties())`.

Income-critical paths with zero coverage (code not yet written — expected):
- Stripe webhook handler and subscription state
- User authentication flows
- IM transform (quoted-reply stripping)
- IMAP polling job

No flaky tests. No redundant tests. No test failures.

---

### Role 3 — Growth Strategist
Added 5 new growth tasks to INTERNAL_TODO.md (not previously captured):
1. **Thread permalink sharing** [GROWTH][M] — shareable read-only `/share/{token}` link is a
   viral touchpoint; every person who receives a link gets a demo of the product. HIGH income impact.
2. **Browser push notifications** [GROWTH][S] — Web Push API re-engages users without requiring
   them to keep the tab open; drives daily active usage. MEDIUM income impact.
3. **Slack/Discord webhook integration** [GROWTH][M] — sends a Slack/Discord notification when
   a new email arrives; this is a natural $29/mo Team plan feature gate. HIGH income impact.
4. **Thread export (PDF/HTML)** [GROWTH][M] — freelancers and support teams need to export
   conversations; this is a paid-tier feature gate. MEDIUM income impact.
5. **In-app referral prompt** [GROWTH][S] — triggered after activation milestone (10 threads
   imported); shows tweet + copy-link modal. MEDIUM income impact.

Added 2 [MARKETING] items to TODO_MASTER.md:
- Slack/Discord community for early users
- Channel-specific announcement when webhook integration ships

---

### Role 4 — UX Auditor
No Thymeleaf templates exist yet so no live user flows to walk. All 5 previously flagged UX
issues remain in INTERNAL_TODO.md. Added 2 new UX issues specific to the import service now built:
1. **Import error feedback** [UX][S] — MessagingException/IOException from import currently
   bubbles up as a 500. Needs a user-visible error banner or toast, not a crash page.
2. **IMAP sync status indicator** [UX][S] — thread list should show "last synced X minutes ago"
   so users know whether they're seeing live data; critical for trust in the product.

---

### Role 5 — Task Optimizer
Audited and rewrote INTERNAL_TODO.md:
- Archived 1 new completed task (Email Import Service) → Done section now has 6 items
- Added 5 new [GROWTH] tasks from Role 3 in priority order (below existing Growth items)
- Added 2 new [UX] tasks from Role 4
- Added 1 new [HEALTH] task: wrap mail exceptions in domain exception
- Confirmed no duplicates across 30 active tasks
- All tasks remain tagged [S/M/L]; no vague or oversized tasks
- No blocked tasks

---

### Role 6 — Health Monitor
Security (no critical issues):
- No hardcoded credentials in any new file
- `MimeMessageParser` handles null Message-ID, null sender, null sent date defensively
- `resolveParticipant` normalises email to lowercase before lookup/storage — prevents duplicate
  Participants from case-variant addresses (e.g. Alice@Test.com vs alice@test.com)
- `importMessage` is idempotent on Message-ID — safe to call from a polling job without
  duplicate-import protection at the job level

Performance:
- `walkParts` recursion depth is bounded by the message structure (typically 2–3 levels);
  no risk of stack overflow for standard RFC 822 messages
- `resolveParticipant` does a SELECT before every INSERT — acceptable for import volumes,
  but may become a bottleneck in bulk-import scenarios (flagged for future batching)
- `resolveThread` does up to N+1 SELECTs for messages in the References list (worst case:
  long References chain with no match). Low risk at current scale; indexed on message_id_header.

Code quality:
- `MimeMessageParser` is package-private (correctly scoped)
- `ParsedEmail` is a record — immutable, no mutable state
- Constructor injection enforced; no `@Autowired` fields
- `(String) part.getContent()` cast in `walkParts` is guarded by `isMimeType("text/plain")`
  and `isMimeType("text/html")` checks — Jakarta Mail guarantees String content for those types

Flagged:
- `MessagingException` and `IOException` leak through `EmailImportService.importMessage()` as
  checked exceptions — added [HEALTH][S] task to wrap in a domain exception
- No rate limiting on import volume per user — will need enforcement when IMAP polling ships

Legal (no change from Run #2 — all items still open in TODO_MASTER.md [LEGAL]).

---

## 2026-04-26 — Autonomous Run #2

### Role 1 — Feature Implementer
**Tasks completed**: Flyway V1__init.sql + domain entities + Spring Data repositories

Files created:
- `src/main/resources/db/migration/V1__init.sql` — 5 tables (participants, email_threads,
  messages, message_recipients, attachments) with FK constraints and 6 performance indexes.
  SQL is ANSI-standard `GENERATED BY DEFAULT AS IDENTITY` — runs on both H2 (test) and
  PostgreSQL (production) without a compatibility layer.
- `src/main/java/com/emailmessenger/domain/` — 6 files:
  - `Participant.java` — `@Entity`, deduped by email, `@PrePersist` createdAt
  - `EmailThread.java` — `@Entity`, `@OneToMany messages`, `@PrePersist/@PreUpdate` timestamps,
    `addMessage()` helper increments messageCount
  - `Message.java` — `@Entity`, `@ManyToOne thread + sender`, `@OneToMany recipients + attachments`,
    `addRecipient()` / `addAttachment()` helpers
  - `MessageRecipient.java` — `@Entity`, `@ManyToOne message + participant`, `RecipientType` enum
  - `Attachment.java` — `@Entity`, `@ManyToOne message`, blob_ref for storage pointer
  - `RecipientType.java` — enum `TO / CC / BCC`
- `src/main/java/com/emailmessenger/repository/` — 4 interfaces:
  - `ParticipantRepository` — `findByEmail`, `existsByEmail`
  - `EmailThreadRepository` — `findByRootMessageId`, `findAllByOrderByUpdatedAtDesc(Pageable)`
  - `MessageRepository` — `findByMessageIdHeader`, `findByThreadIdOrderBySentAtAsc`
  - `AttachmentRepository` — `findByMessageId`

Verified: `./mvnw test` → BUILD SUCCESS, all tests pass.

**Income relevance**: Domain model and persistence layer are the foundation for all features
that drive revenue — user threads, message history, attachment storage. Nothing billable ships
without this layer.

---

### Role 2 — Test Examiner
**Coverage added**: 10 new tests across 3 test classes (11 total, up from 1).

Files added:
- `ParticipantRepositoryTest` (3 tests): save/find by email, existsByEmail, unique constraint enforcement
- `EmailThreadRepositoryTest` (4 tests): save/find thread, ordered by updatedAt, message persistence, findByMessageIdHeader
- `AttachmentRepositoryTest` (3 tests): save/find by message ID, multiple attachments per message, TO/CC recipient types

Income-critical paths still at zero coverage (code not yet written — expected):
- Stripe webhook handler and subscription state transitions
- User authentication (login, registration, session)
- IMAP ingestion and thread-building logic
- IM transform (quoted-reply stripping)

No flaky tests. No redundant tests. No test failures. Added TEST-FAILURE tagging protocol to
INTERNAL_TODO.md for when those paths are built.

---

### Role 3 — Growth Strategist
Added 5 new implementable growth tasks to INTERNAL_TODO.md:
1. **Google OAuth SSO** [GROWTH][S] — Removes password friction at signup; Gmail users get
   mailbox auto-connected. HIGH income impact. Added credential step to TODO_MASTER.md.
2. **First-run onboarding wizard** [GROWTH][M] — Guided "connect your mailbox" flow.
   Reduces activation drop-off = more users reach the Aha moment = more conversions.
3. **Upgrade prompts at limit** [GROWTH][S] — Inline modal when free tier hits 500 threads
   or 1 mailbox. Direct revenue trigger. HIGH income impact.
4. **Email digest notifications** [GROWTH][M] — Daily/weekly re-engagement email. Reduces
   churn. MEDIUM income impact.
5. **SEO landing page** [GROWTH][M] — Static / with features + pricing. Organic traffic
   from "email as chat" searches. MEDIUM income impact.

Added [MARKETING] Google OAuth ToS review item to TODO_MASTER.md.

---

### Role 4 — UX Auditor
No Thymeleaf templates exist yet so no live user flows to audit. Flagged 5 specific
UX issues for when templates are built (added to INTERNAL_TODO.md tagged [UX]):
1. Thread list empty state — blank page with no CTA when no threads exist
2. Conversation empty state — blank panel when thread has no messages
3. Error pages — Spring Whitelabel error page exposed to users; needs custom error.html
4. Reply button prominence — must be primary action, not buried below message list
5. Mobile layout — 375px viewport pass required for chat bubbles

---

### Role 5 — Task Optimizer
Audited and rewrote INTERNAL_TODO.md:
- Archived 5 completed tasks to Done section
- Organized into sections: Core, Growth/Revenue, UX, Health, Infrastructure
- Consolidated duplicate upgrade-prompt mentions into one task
- Re-prioritized: Core income-blocking features first, then Growth, UX, Health, Infra
- All tasks tagged [S/M/L]; no oversized tasks remain
- No blocked tasks (all dependencies either done or not yet needed)

---

### Role 6 — Health Monitor
Security audit (no issues found):
- No hardcoded credentials in any source file
- `application.yml` prod profile uses env-var placeholders throughout
- No `@Autowired` field injection (constructor injection enforced by convention)
- `open-in-view=false` prevents lazy-load-over-HTTP session leaks

Performance review:
- 6 indexes added in V1__init.sql covering all FK columns and common sort keys
  (`updated_at DESC`, `sent_at DESC`) — N+1 risk on unindexed FKs eliminated at schema level
- `FetchType.LAZY` on all `@ManyToOne` and `@OneToMany` relations — no eager cross-table fetches
- No unbounded list queries; `EmailThreadRepository.findAllByOrderByUpdatedAtDesc` uses `Pageable`

Code quality:
- No dead code; all 6 entity classes and 4 repositories are actively referenced in tests
- `Collections.unmodifiableList()` on collection getters in EmailThread and Message —
  prevents external mutation of managed collections
- No unused dependencies

Dependencies:
- Spring Boot 3.5.14 (latest 3.x); no known CVEs in current dep set
- H2 2.3 (bundled by Spring Boot) — no known issues
- Testcontainers 1.20.4 — current stable

Legal (no change from Run #1):
- Privacy Policy, Terms of Service, Refund Policy, Cookie banner still outstanding
- Flagged in TODO_MASTER.md [LEGAL] — no action taken (code-level task; requires Master)

## 2026-04-25 — Autonomous Run #1 (Bootstrap)

### First Run — APP_SPEC.md created
- Defined application: **MailIM** — email-to-instant-message SaaS
- Business model: Freemium subscription ($0 / $9 / $29 / $99 per month)
- Target users: remote workers, support teams, freelancers

### Role 1 — Feature Implementer
**Task implemented**: Scaffold Maven project + Add starters (CLAUDE.md items 2 and 3)

Files created:
- `pom.xml` — Spring Boot 3.5.14, Java 21; starters: web, thymeleaf, data-jpa,
  validation, mail, flyway-core, flyway-database-postgresql; runtime: postgresql;
  test: h2, spring-boot-starter-test, testcontainers-bom, testcontainers-postgresql
- `.mvn/wrapper/maven-wrapper.properties` — Maven 3.9.11
- `mvnw` / `mvnw.cmd` — generated by `mvn wrapper:wrapper`
- `src/main/java/com/emailmessenger/EmailMessengerApplication.java`
- `src/main/resources/application.yml` — dev profile (H2 in-memory) and prod profile
  (PostgreSQL via env vars DB_URL / DB_USER / DB_PASS)
- `src/test/java/com/emailmessenger/EmailMessengerApplicationTests.java`

Verified: `./mvnw test` → BUILD SUCCESS, 1 test, 0 failures.

**Income relevance**: Compiling foundation for all income-generating features.

### Role 2 — Test Examiner
- Reviewed test suite: 1 test (`contextLoads`) passes; application context boots
  correctly against H2 with Flyway (0 migrations, schema empty — expected).
- Income-critical paths with zero test coverage (expected — code not yet written):
  - Stripe payments / billing webhooks
  - User authentication and registration
  - IMAP email ingestion and thread building
  - IM transform (quoted-reply stripping)
- No flaky or redundant tests. No test failures.
- Added test coverage tasks to INTERNAL_TODO.md for when features are built.

### Role 3 — Growth Strategist
Identified highest-leverage income opportunities and added to INTERNAL_TODO.md:
1. **Stripe billing** [GROWTH][M] — direct revenue unlock; no paying users without it
2. **User auth** [GROWTH][M] — prerequisite for billing; needed for multi-tenancy
3. **Plan-limit enforcement** [GROWTH][S] — creates upgrade pressure on free users
4. **Gravatar + initials avatars** [GROWTH][M] — improves perceived quality → conversion
5. **Unread tracking** [GROWTH][S] — engagement + daily active usage driver
6. **Full-text search** [GROWTH][M] — key feature gate for Personal/Team upgrade
7. **Referral link** [GROWTH][S] — viral loop for organic growth

Added marketing actions to TODO_MASTER.md: Product Hunt, directory listings, SEO
content, community posts, waitlist landing page.

### Role 4 — Task Optimizer
- Created INTERNAL_TODO.md with full task backlog in priority order
- Archived completed scaffold tasks
- All tasks tagged by priority, size, and category
- No duplicates found (first run)
- Blocked tasks: none yet

### Role 5 — Health Monitor
Security:
- No hardcoded credentials found in any file
- `application.yml` prod profile uses `${DB_URL}`, `${DB_USER}`, `${DB_PASS}`,
  `${MAIL_HOST}`, `${MAIL_USER}`, `${MAIL_PASS}` env-var placeholders — safe
- No payment flows present yet — no PCI exposure

Performance:
- No entities or queries yet — N+1 analysis deferred to when entities exist
- `spring.jpa.open-in-view=false` set in both profiles (avoids lazy-load antipattern)

Dependencies:
- Spring Boot 3.5.14 (latest 3.x stable as of 2026-04-25)
- No known CVEs flagged in current dep set (Hibernate 6.6.49, Flyway bundled)

Legal (flagged in TODO_MASTER.md [LEGAL]):
- No Privacy Policy page
- No Terms of Service page
- No Refund Policy page
- Cookie consent banner not yet implemented
- All above required before accepting payments or EU/CCPA-covered users

Code quality:
- Only 3 Java files; no dead code, no repeated logic
- `open-in-view=false` already set to prevent common JPA antipattern

## 2026-05-28
Shipped: added `.github/workflows/ci.yml` running `./mvnw -B verify` on push/PR with Maven dep caching, plus a Buildx job that builds (no push) the Dockerfile with GHA layer cache so broken builds fail before deploy.
Advances: Milestone 2 — GitHub Actions CI.
Master action: none

## 2026-05-29
Shipped: end-to-end Testcontainers + GreenMail integration test (`EndToEndMailboxFlowIntegrationTest`) — boots `postgres:16-alpine` via Testcontainers with `spring.datasource.*` rewired through `@DynamicPropertySource` (Flyway V1..V7 applied against real Postgres + Hibernate `validate`), spins up an in-process GreenMail IMAP server via `GreenMailExtension(ServerSetupTest.IMAP)`, delivers a `MimeMessage` into a provisioned mailbox, then drives MockMvc through `POST /mailboxes` (which runs the real `MailAccountService.connect` → `JakartaImapClient.verifyConnection` + `fetchRecentInbox` → `EmailImportService.importMessage`) and asserts `/threads` renders the imported subject and the row is persisted under the owner. `@EnabledIf("dockerAvailable")` skips the test when no Docker daemon is reachable so contributors without Docker still get a green build; CI on `ubuntu-latest` always has one. Added `com.icegreen:greenmail:2.0.1` + `greenmail-junit5:2.0.1` (test scope) to pom.xml. 189 tests pass.
Advances: EPIC-04 Deployability — Milestone 3 (Integration tests with Testcontainers + GreenMail).
Master action: none

## 2026-05-30
Shipped: GHCR image publish wired into the CI Docker job (login via `GITHUB_TOKEN`, `packages: write`, `docker/metadata-action` emitting `sha-<short>`, branch-named, and `latest`-on-`claude_routine` tags; `push: ${{ github.event_name != 'pull_request' }}` so PRs still build but only `claude_routine`/`master` pushes publish to `ghcr.io/jacob-bensen/email-messenger`) and a one-page `DEPLOY.md` at the repo root walking Master from `docker pull` through env-file setup, Flyway-on-boot verification, Caddy TLS terminator, `GET /pricing` smoke check, and Stripe webhook registration. Backlog now empty pending live deploy proof.
Advances: EPIC-04 Deployability — Milestone 4 (Production smoke deploy).
Master action: none (existing MASTER_ACTIONS entries already cover the human side — hosting, domain, Stripe keys, encryption secrets).

## 2026-06-03
Shipped: end-to-end attribution + plan-aware trial banner — added `users.acquisition_source` column (Flyway V8, indexed, varchar(64)) populated by `UserService.register(..., source)` with trim/blank-to-null/64-char clamp; `RegistrationForm` now carries a hidden `source` field bound via `th:field`; `AuthController` reads `?utm_source=` on GET `/register` and seeds the form; `MarketingController` reads `?utm_source=` on `/` and `/pricing` and exposes `utmSource` to Thymeleaf so every "Start free" / pricing-plan CTA on `landing.html` and `pricing.html` forwards the source through to `/register?utm_source=…` (and `?plan=personal/team&utm_source=…` for paid CTAs); `BillingBanner` gained a nullable `planLabel`, `BillingBannerService` maps `Subscription.plan` → "Personal"/"Team"/"Enterprise", and `threads.html` now renders "Personal trial ends in N days" etc. when known. Three new tests cover persistence (`UserServiceTest#persistsAcquisitionSourceWhenProvided`/`#normalizesBlankAcquisitionSourceToNull`/`#clampsOverlongAcquisitionSourceToColumnWidth`), full-HTTP carry-through (`AuthFlowIntegrationTest#registrationCarriesUtmSourceOntoUser`), and plan-label surfacing (`BillingBannerServiceTest#trialBannerSurfacesChosenPlanLabel`/`#trialBannerHasNullPlanLabelWhenPlanUnset`). `./mvnw test` → 201 tests pass.
Advances: EPIC-05 Acquisition — Milestone 2 (Conversion-tracked signup funnel).
Master action: none

## 2026-06-04
Shipped: SEO basics + OG previews — every public page (`/`, `/pricing`, `/login`, `/register`) now renders unique `<title>` / `meta description` / `<link rel=canonical>` / `og:title|description|url|image|type|site_name` / `twitter:card|title|description|image` via a shared `fragments/seo.html` Thymeleaf fragment. A new `SiteProperties` (`marketing.base-url`, default `https://mailaim.app`, override via `MARKETING_BASE_URL`) and `SiteModelAdvice` `@ControllerAdvice` expose `siteBaseUrl` to every model so canonical/OG URLs are absolute. New `SeoController` serves `GET /robots.txt` (`User-agent: *`, allow `/`, disallow `/threads`, `/mailboxes`, `/billing/`, `/actuator/`, `/h2-console/`, `Sitemap:` line), `GET /sitemap.xml` (sitemap.org urlset with all four public URLs, `lastmod`, `changefreq=weekly`, priority 1.0 on `/`), and `GET /images/og-card.png` (1200×630 PNG generated with Java 2D — dark IM-bubble preview). `SecurityConfig` permitAll updated for `/robots.txt` and `/sitemap.xml`. 10 new tests across `SeoControllerTest` (robots / sitemap / PNG magic+dimensions / trailing-slash normalization) and `PublicPageSeoIntegrationTest` (boots full Spring + Thymeleaf, asserts each rendered HTML has the right title/description/canonical/og:url/og:image/twitter:card, plus distinct titles across pages, plus robots+sitemap end-to-end through the security filter chain with `marketing.base-url=https://test.mailaim.app`). `./mvnw test` → 211 tests pass (1 Docker-only test skipped as before).
Advances: EPIC-05 Acquisition — Milestone 3 (SEO basics + OG previews).
Master action: none

## 2026-06-04
Shipped: First-touch demo content — `GET /demo` renders a curated multi-participant email thread as IM-style chat with zero signup, so a Product Hunt / Twitter visitor can see MailIM working in five seconds. New `DemoConversationService` in `com.emailmessenger.service` constructs a transient `EmailThread` ("Launch checklist — Tuesday demo") with seven plain-body messages from three participants (Alex Lee, Sam Patel, Maya Chen) spanning yesterday + today (`buildDemo(LocalDate)` is date-injectable for deterministic tests; `buildDemo()` defaults to `LocalDate.now()` so the conversation-view JS still labels day separators "Yesterday" / "Today"), and feeds them through the real `ConversationService` → `IMTransformService` pipeline so visitors see actual bubble grouping (Alex sends two consecutive messages = one bubble run), live markdown rendering (Sam's `**Your inbox, as a chat.**` becomes `<strong>`), real quoted-reply stripping (the `> Two things blocking launch:` block Sam quoted from Alex's earlier message is silently dropped), and a live cross-day separator. New `DemoController` exposes `GET /demo` (permitted in `SecurityConfig` request matchers alongside `/`, `/pricing`, `/login`, `/register`), adds `demoMode=true` + `demoSignedIn` to the model, and reuses `conversation.html` — the template now branches on `demoMode` to (a) replace the noindex/nofollow `<title>` with a real SEO/OG fragment ("Live demo — MailIM", canonical `/demo`, twitter:summary_large_image — also listed in `sitemap.xml` and `SeoController.PUBLIC_PATHS`), (b) swap the `← All threads` back-link for `← MailIM home`, (c) render a brand-gradient `.demo-banner` strip at the top with a `Start free` CTA carrying `?utm_source=demo` (or `Back to inbox` if the visitor is already authenticated), (d) suppress the trial-banner, billing checks, and the keyboard-reply listener, (e) hide the reply form entirely (a transient `EmailThread` has no id, so no `/threads/null/reply` POST gets emitted) and replace it with a `.demo-footer` card offering `Start free` + `See pricing` (both UTM-tagged). `MarketingController` now routes `GET /?demo=1` → `redirect:/demo` (preserving `utm_source` via URL-encoded query string), so the PLAN.md "Done means" URL works; `landing.html`'s hero CTA pair is now `Start free` + `See live demo` so the demo is discoverable from the homepage. Latent fix: promoted `BubbleRun` and `BubbleMessage` from package-private to `public` records (with `public LocalDate date()`) — Thymeleaf's SpEL evaluator can't invoke a package-private accessor on a record from outside the declaring package, so `data-date=${run.date()}` on `conversation.html:69` would have thrown at runtime for any real conversation render too; the existing standalone-MockMvc tests never exercised the view layer, so the `PublicPageSeoIntegrationTest` boot of `/demo` was the first end-to-end render and surfaced it. New tests: `DemoConversationServiceTest` (7 cases — subject + participants, consecutive-sender grouping, yesterday+today span, quoted-reply block stripped from rendered HTML, `**…**` → `<strong>` markdown, message-count consistency, default-`now` build uses no future dates); `MarketingControllerTest` (3 new — `?demo=1` → `/demo`, demo redirect preserves `utm_source`, demo redirect wins over the authenticated `/threads` redirect); `PublicPageSeoIntegrationTest.demoPageRendersConversationWithSeoTagsAndStartFreeCta` (boots full Spring + Thymeleaf, asserts title/description/canonical/og:url, three sender names appear in rendered HTML, `.demo-banner` CSS hook present, `/register?utm_source=demo` CTA emitted, no reply-form `name="body"`, no `/threads/null/reply` action); `SeoControllerTest` + `PublicPageSeoIntegrationTest` extended to assert `/demo` appears in `sitemap.xml`. `./mvnw test` → 222 tests pass (1 Docker-only test skipped as before). PLAN.md's Primary Objective EPIC-05 Acquisition is now code-complete on all four milestones — proposing **EPIC-06 Launch readiness** (Privacy Policy / Terms / Refund Policy pages + cookie-consent banner — Stripe live-mode gate; Loom/YouTube demo embed in the landing hero replacing the static screenshot mock — Master action #1 in `MASTER_ACTIONS.md → Launch / marketing`; first-touch in-app onboarding checklist on `/threads` for users with zero mailboxes) as the next Primary Objective.
Advances: EPIC-05 Acquisition — Milestone 4 (First-touch demo content). EPIC-05 now fully shipped; next Primary Objective proposed above pending [PLAN-REVIEW].
Master action: [PLAN-REVIEW] EPIC-05 Acquisition is code-complete — adopt EPIC-06 Launch readiness (legal pages + cookie banner + demo embed) as the next Primary Objective and update PLAN.md.

## 2026-06-05
Shipped: First-touch in-app onboarding checklist on `/threads` — a fresh signup who lands on an empty inbox now sees a 3-step welcome card ("Create your account" ✓ / "Connect your inbox" / "See your first conversation") with a primary CTA pointing at the next undone step, replacing the previous one-line "Your inbox is empty" empty state that gave activation-stage users no sense of progress. New `OnboardingChecklist` record in `com.emailmessenger.web` (booleans `mailboxConnected` + `firstThreadImported`, helpers `isComplete()`, `nextStepCtaUrl()`, `nextStepCtaLabel()` — routes to `/mailboxes/new` + "Connect your inbox" when no mailbox, `/mailboxes` + "Sync now" when mailbox exists but no threads imported yet). New `OnboardingService` in the same package derives the checklist by counting `MailAccountRepository.countByUser` + `EmailThreadRepository.countByOwner` for the owner. `ThreadController` now takes the service as a constructor dependency and exposes the checklist as `onboarding` model attribute **only** when `threads.totalElements == 0` (no extra query on populated inboxes). `threads.html` swaps the old `empty-state` div for an `.onboarding-card` whose headline/subhead/CTA branches on whether the mailbox is connected — both states still render the full ordered list with check-mark / current / pending step states so users see the whole funnel. ~60 lines of namespaced CSS (`.onboarding-card`, `.onboarding-title`, `.onboarding-sub`, `.onboarding-steps`, `.onboarding-step`, `.onboarding-step-mark`, `.onboarding-step-current`, `.onboarding-step-done`, `.onboarding-cta`) appended right after the existing empty-state block; uses the same CSS custom properties (`--surface`, `--border`, `--text`, `--brand`) so dark mode picks up automatically. PLAN.md transitions Primary Objective from EPIC-05 Acquisition (all four milestones code-complete and verified last session) to **EPIC-06 Launch readiness** with four milestones — legal pages + cookie consent, this onboarding checklist, demo video embed in landing hero, trial-conversion nudge — and the 2026-06-04 [PLAN-REVIEW] in MASTER_ACTIONS.md is now resolved and removed. Tests: 3 new `OnboardingServiceTest` cases (fresh user / mailbox-connected-but-no-threads / both complete — covering the CTA URL+label branching), 2 new `ThreadControllerTest` cases (`emptyInboxExposesOnboardingChecklist` asserts the model attribute is the exact checklist instance; `nonEmptyInboxDoesNotExposeOnboardingChecklist` asserts the attribute is absent and `onboardingService.checklistFor` is never called when threads exist). `./mvnw test` → 227 tests pass (1 Docker-only test skipped as before).
Advances: EPIC-06 Launch readiness — Milestone 2 (First-touch in-app onboarding checklist).
Master action: none

## 2026-06-06
Shipped: Re-engagement email after 7 days of inactivity — V13 adds `last_login_at` + `last_inbox_visit_at` + `last_reengagement_sent_at` to `users` (all nullable, service code coalesces against `created_at` so pre-tracking rows are treated as "last active when they registered"). New `UserActivityService` in `com.emailmessenger.auth` writes both visit + login stamps via single-row `@Modifying(flushAutomatically=true, clearAutomatically=true)` JPQL updates so the hot path on every `GET /threads` and every login stays a one-statement write that doesn't bump entity-wide `updated_at`. `LoginAuditListener` consumes Spring Security's `AuthenticationSuccessEvent` (covers form login, remember-me re-auth, and the post-registration `request.login()`), filtering anonymous/blank principals. `ThreadController` calls `userActivityService.recordInboxVisit(owner)` at the top of `listThreads`. New `com.emailmessenger.reengagement` package mirrors the digest pattern: `ReengagementService.runReengagementCycle()` sweeps `UserRepository.findDormantSince` (single query: `enabled = true AND COALESCE(last_inbox_visit_at, created_at) < cutoff AND COALESCE(last_login_at, created_at) < cutoff`), per-user skips (a) those whose `last_reengagement_sent_at` is more recent than effective last activity (idempotency per inactivity window — a user who reads then re-disappears earns a second nudge), (b) those with zero unread threads (`EmailThreadRepository.countByOwnerAndUnreadTrue`), (c) opt-outs via the existing `DigestEmailPreference.optedOut` flag (one unsubscribe link kills every automated marketing email to that address), then composes a plain-text email naming the unread count and days-away delta with an unsubscribe link reusing the shared opt-out token. `ReengagementScheduler` runs cron `0 0 13 * * ?` UTC gated by `reengagement.enabled=true` (default off in dev/tests). 17 new tests: `ReengagementServiceTest` x8 (boots full Spring + H2, covers dormant+unread sends, zero-unread skip, opt-out skip, idempotency on second cycle, re-disappearance second nudge, recently-active skip, sweep totals, subject+body+unsubscribe footer), `UserActivityServiceTest` x4 (visit/login stamps + case-insensitive login + null/blank/unsaved-noop), `LoginAuditListenerTest` x2 (authenticated success → recordLogin; anonymous → ignored), `ReengagementSchedulerFeatureFlagTest` x2 (scheduler absent when flag off, service always present), plus `ThreadControllerTest#listThreadsRecordsInboxVisitForOwner`. `./mvnw test` → 394 tests pass (1 Docker-only skipped as before). PLAN.md's Primary Objective EPIC-08 Saved Searches & Reactivation is now code-complete on all four milestones — proposing **EPIC-09 Account self-serve** (password reset via emailed token + email verification on signup, since users currently have no recovery path if they forget their password — a silent cap on paid retention and a real blocker for Stripe live launches) as the next Primary Objective; alternatives in MASTER_ACTIONS.md are Mobile/PWA, annual-billing surfacing, or Google OAuth signup.
Advances: EPIC-08 Saved Searches & Reactivation — Milestone 4 (Re-engagement email after 7 days of inactivity). EPIC-08 now fully shipped; next Primary Objective proposed above pending [PLAN-REVIEW].
Master action: [PLAN-REVIEW] EPIC-08 Saved Searches & Reactivation is code-complete — adopt EPIC-09 Account self-serve (password reset + email verification) as the next Primary Objective and update PLAN.md, or pick one of the listed alternatives.

## 2026-06-07
Shipped: Service worker for offline shell — `GET /sw.js` (no-store, `Service-Worker-Allowed: /`, `application/javascript`) registers `install` / `activate` / `fetch` lifecycle handlers in `PwaController`. Install pre-caches the four-asset static shell (`/offline`, `/css/main.css`, `/icons/icon-192.png`, `/manifest.webmanifest`) under a cache name `mailim-shell-<12-hex-of-SHA1(SW_TEMPLATE + asset list)>`, so any edit to the SW logic or shell-asset list ships as a new cache key — `activate` deletes every cache whose key doesn't match the current version, busting stale clients on the next page load. Fetch handler intercepts navigation requests (`req.mode === 'navigate'`) and falls back to `caches.match('/offline')` when the network rejects, plus serves shell assets cache-first; non-navigation, non-shell GETs and any non-GET method pass through untouched so authenticated POSTs (CSRF replies, billing checkout, sync-now) are never replayed. New `GET /offline` returns a self-contained MailIM-branded fallback HTML (`<title>You're offline — MailIM</title>`, brand-blue gradient, "Try again" reload button, embeds `/css/main.css` for normal styling but inlines a brand-color shell so the page reads as MailIM even when the cached CSS is somehow missing) — also lists the manifest + theme-color + apple-touch-icon links so an installed PWA opened to the offline screen stays installable. `SecurityConfig` permitAll now includes `/sw.js` and `/offline` alongside the existing `/manifest.webmanifest`, `/icons/**`, `/apple-touch-icon.png` entries so browsers reach them with no auth context. The shared `fragments/seo.html` head fragment gains a tiny `<script>` block that lazily registers `/sw.js` on `window.load` if `'serviceWorker' in navigator`, behind a `.catch(function(){})` so older Safaris / private mode don't surface errors; the same registration script is inlined inside the `<th:block th:unless="${demoMode}">` head block on `conversation.html` and the head of `threads.html` so signed-in surfaces also seed the SW the first time they load (demo conversation view picks it up via the seo fragment instead). New tests: `PwaControllerTest` x3 (`serviceWorkerJsRegistersInstallActivateAndFetchHandlers` asserts content type + Cache-Control no-store + Service-Worker-Allowed header + all three lifecycle listeners + `caches.match('/offline')` + every shell asset listed in the SW body; `serviceWorkerCacheVersionBustsOnSourceChange` asserts two GETs return byte-identical bodies and the version string matches `mailim-shell-[0-9a-f]{12}`; `offlinePageRendersMailimBrandedShell` asserts MailIM brand, the "offline" copy, the "Try again" button, and that the offline page itself links to `/manifest.webmanifest` and `/css/main.css` so it doesn't trigger a second offline screen on resource fetch). `PublicPageSeoIntegrationTest` x2 (`serviceWorkerAndOfflineShellAreReachableThroughSecurityFilterChain` boots full Spring + filter chain and asserts unauthenticated `GET /sw.js` returns the CACHE_VERSION constant and `GET /offline` renders the branded screen — catching any future permitAll regression; `landingPageRegistersServiceWorkerOnLoad` asserts the rendered `/` HTML contains `serviceWorker.register('/sw.js'` so removing the registration script from the seo fragment fails CI). `./mvnw test` → 476 tests pass (1 Docker-only skipped as before).
Advances: EPIC-10 Mobile / PWA — Milestone 2 (Service worker for offline shell).
Master action: none

## 2026-06-07
Shipped: In-app install prompt + iOS instructions — new `fragments/install-banner.html` Thymeleaf fragment renders a fixed-position dual-mode banner that surfaces a one-click "Install MailIM" CTA on Chromium browsers (Android Chrome / Edge / desktop Chrome) and an "Add to Home Screen → tap share → Add" walkthrough on iOS Safari. The Chromium branch listens for `beforeinstallprompt`, calls `evt.preventDefault()` so Chrome's default mini-infobar is suppressed, stashes the event, and only then unhides the banner — click the CTA and we call `deferredPrompt.prompt()`, await `userChoice`, then dismiss either way. The iOS branch UA-detects `/iPad|iPhone|iPod/` and excludes Chrome-/Firefox-/Edge-/Opera-on-iOS (`CriOS|FxiOS|EdgiOS|OPiOS`) since those in-app browsers can't install a PWA so the hint is just noise. Both dismiss paths persist to `localStorage` under `mailim-install-dismiss-v1` — a "no thanks" never reappears on subsequent loads, and the versioned key lets us future-version the copy without re-pestering everyone. Installed-app detection short-circuits the whole flow via `window.matchMedia('(display-mode: standalone)').matches` (Chromium) + `window.navigator.standalone === true` (legacy iOS) plus the `appinstalled` event so the banner can't nag inside the launched PWA. Fragment is included on `landing.html` (public, first-touch) and `threads.html` (post-signup, where Personal-tier retention actually compounds) — both are the surfaces PLAN.md milestone 3 calls out. ~85 lines of new namespaced CSS (`.install-banner`, `.install-banner-body`, `.install-banner-mark`, `.install-banner-text`, `.install-banner-title`, `.install-banner-sub`, `.install-banner-share`, `.install-banner-cta`, `.install-banner-dismiss`) appended to `main.css` — fixed bottom-center, brand-color "MI" wordmark on the left, dismiss `×` on the right, mobile breakpoint at 560px wraps text/CTA on two lines for narrow phones, dark-mode override picks up the existing `--surface`/`--border`/`--text` tokens. New tests: `PublicPageSeoIntegrationTest.landingPageRendersInstallBannerWithIosFallbackAndPersistentDismiss` boots full Spring + Thymeleaf, renders `GET /`, and asserts all the install-banner DOM ids plus the `beforeinstallprompt` listener wiring, the `/iPad|iPhone|iPod/` UA test, the `display-mode: standalone` + `navigator.standalone` already-installed gates, and the `mailim-install-dismiss-v1` localStorage key; `ThreadInboxRenderingIntegrationTest.inboxRendersInstallBannerForSignedInUsers` boots full Spring + JPA + Security with a `@WithMockUser` registered user, fetches `GET /threads`, and asserts the same banner DOM + the `beforeinstallprompt` wiring + the dismiss key — guarding the post-signup surface against future template regressions. `./mvnw test` → 478 tests pass (1 Docker-only skipped as before).
Advances: EPIC-10 Mobile / PWA — Milestone 3 (In-app install prompt + iOS instructions).
Master action: none

## 2026-06-07
Shipped: Mobile-tuned threads + conversation view — `meta name="viewport"` on both `threads.html` and `conversation.html` now carries `viewport-fit=cover` so an installed standalone PWA reaches into the iOS notch + home-bar area; `.conv-body` height switches from `calc(100vh - 57px)` to `calc(100dvh - 57px)` with the `vh` declaration retained as the older-browser fallback (the second `height:` declaration only takes effect where `dvh` parses, so old Safari/Chrome get `vh` and modern browsers get a viewport that shrinks correctly when the on-screen keyboard slides up); `.day-separator` becomes `position: sticky; top: 0` with `background: var(--bg)` + `z-index: 5` so cross-day labels pin to the top of the messages scroll container during long-thread scrolling. New mobile-audit CSS block at the end of `main.css` adds safe-area insets to `.app-header` (`padding-top: env(safe-area-inset-top, 0)` + `padding-left/right: calc(24px + env(safe-area-inset-left/right, 0))`), `.conv-header` (same pattern with `calc(12px + …top)` so the header text doesn't slide under the notch), and `.reply-area` (`padding-bottom: calc(16px + env(safe-area-inset-bottom, 0))` so the send button clears the iPhone home indicator). At `@media (max-width: 640px)` — the 375px-viewport audit baseline — `.header-nav a` and `.header-nav .link-button` get `min-height: 44px; display: inline-flex` (WCAG 2.5.5 AAA tap-target floor), `.kbd-hint` is hidden (keyboard shortcuts are pointless on a phone and were eating header room), `.back-link` becomes a 44×44 brand-tinted button (`background: rgba(79, 128, 255, 0.10); border-radius: 8px`) instead of a thin text link, `.thread-item` grows to `padding: 16px 18px; min-height: 64px`, `.sender-rail-row` + `.sender-rail-summary` + `.sender-pill-clear` + `.filter-chip` + `.filter-chip-clear` + `.inbox-search-input` + `.inbox-search-submit` + `.inbox-search-clear` + `.page-link` + `.reply-form .btn` all get `min-height: 44px`, `.reply-form textarea` gains `min-height: 96px; font-size: 16px` (16px is the iOS "don't zoom on focus" threshold), and `.reply-area` itself becomes `position: sticky; bottom: 0` so the reply box rides the bottom of the visible viewport — combined with the `100dvh` `.conv-body` height, the form sits above the iOS soft keyboard instead of being pushed off-screen. Everything is wired through the existing `--brand` / `--surface` / `--border` design tokens so `@media (prefers-color-scheme: dark)` picks up the new rules with no extra overrides. New tests: `PublicPageSeoIntegrationTest.mainStylesheetCarriesMobileTuningForInstalledPwa` GETs `/css/main.css` through the full Spring filter chain and asserts `100dvh`, `env(safe-area-inset-top`, `env(safe-area-inset-bottom`, `min-height: 44px`, `.day-separator`, and `position: sticky` all persist — so a future CSS refactor that drops the mobile shell fails CI; `ThreadInboxRenderingIntegrationTest.inboxRendersMobileViewportAndUsesSharedStylesheet` boots the full Spring + Thymeleaf + Security stack with a `@WithMockUser` registered user, fetches `GET /threads`, and asserts the rendered HTML contains `viewport-fit=cover` and links `/css/main.css` — guarding the signed-in surface against a template regression that drops the viewport hint. `./mvnw test` → 480 tests pass (1 Docker-only skipped as before). PLAN.md's Primary Objective EPIC-10 Mobile / PWA is now code-complete on all four milestones — proposing **annual-billing surfacing** (toggle annual vs monthly on `/pricing`, pass `?billing=annual` through Stripe Checkout for the 2-months-free SKU — direct ARPU lift), **Gmail OAuth mailbox connection** (one-click signin replaces IMAP-password friction — largest activation gain, depends on the existing Google OAuth credentials master action), or **first-paying-customer attribution dashboard** as the next Primary Objective.
Advances: EPIC-10 Mobile / PWA — Milestone 4 (Mobile-tuned threads + conversation view). EPIC-10 now fully shipped; next Primary Objective proposed above pending [PLAN-REVIEW].
Master action: [PLAN-REVIEW] EPIC-10 Mobile / PWA is code-complete — adopt one of annual-billing surfacing, Gmail OAuth mailbox connection, or first-paying-customer attribution dashboard as the next Primary Objective and update PLAN.md.

## 2026-06-08
Shipped: Subscription billing_period field + active-cadence display on /account — Flyway V17 adds `subscriptions.billing_period VARCHAR(10)` with a backfill UPDATE that stamps every existing non-`incomplete` row to `'MONTHLY'` (only the monthly checkout existed pre-EPIC-11, so this is the only safe inference). `Subscription` entity grows `@Enumerated(EnumType.STRING) BillingPeriod billingPeriod` plus getter/setter. `BillingService.startCheckout` now records the period actually used — and, crucially, when the annual SKU fallback degrades a request to monthly (`ANNUAL → MONTHLY` because the annual price ID isn't configured), the recorded period reflects what Stripe will actually charge, not what the user originally asked for. `BillingProperties.periodFor(String priceId)` reverse-maps a price ID to a cadence by exact-equality against the six configured SKUs; `BillingService.applySubscriptionUpdated` calls it whenever a webhook carries a `price.id`, derives `ANNUAL`/`MONTHLY`, and writes the field — falling back to leave-alone when the price ID doesn't match a configured SKU (so a promo price never silently corrupts the cadence). New `AccountBillingSummary` record (`label`, `renewsOn`, `trialing`, `canceled`) wraps a `Subscription` and pre-formats the headline string "Personal · Annual" / "Personal · Monthly trial" / "Personal · Canceled", picking `trialEndsAt` for trialing subs and `currentPeriodEnd` otherwise. `AccountController` injects `SubscriptionRepository`, looks up the user's row, and exposes `billing` on the model only when a non-FREE plan exists. `account.html` renders a new "Subscription" card above the password card with the plan/cadence label and a "Trial ends" / "Renews" date in `yyyy-MM-dd` — the card is suppressed entirely for free users so it doesn't read as "you have nothing". New tests: `BillingServiceTest` extended (`startCheckoutCreatesPendingSubscriptionAndReturnsCheckoutUrl` asserts MONTHLY recorded; `startCheckoutAnnualUsesAnnualPriceId` asserts ANNUAL recorded; `startCheckoutAnnualFallsBackToMonthlyPriceWhenAnnualNotConfigured` asserts the recorded period downgrades alongside the price); `BillingWebhookHandlingTest.subscriptionUpdatedWithAnnualPriceIdFlipsLocalBillingPeriodToAnnual` (boots full Spring + H2, seeds a MONTHLY row, posts a `customer.subscription.updated` webhook carrying the annual SKU, asserts the row flips to ANNUAL); `BillingWebhookHandlingTest.subscriptionUpdatedWithUnknownPriceIdDoesNotClobberBillingPeriod` (seeds ANNUAL, sends an unknown promo price ID, asserts the recorded cadence is preserved); `AccountControllerTest.accountPageShowsActiveAnnualCadenceAndRenewalDate` (boots full Spring + Thymeleaf + Security with `@WithMockUser`, seeds an ANNUAL Personal subscription with `current_period_end=2027-06-07`, asserts the rendered HTML contains "Personal · Annual" and "2027-06-07"); `AccountControllerTest.accountPageShowsTrialCadenceWithTrialEnd` (asserts a trialing MONTHLY sub renders "Personal · Monthly trial" and the `trial_ends_at` date); `AccountControllerTest.accountPageOmitsSubscriptionCardForFreeUser` (asserts the `billing` model attribute is null for a user with no subscription row, so the new card isn't rendered for free users). `./mvnw test` → 505 tests pass (1 Docker-only skipped as before). All four EPIC-11 milestones are now code-complete — proposing **first-paying-customer attribution funnel** (a `/admin/revenue` page showing MRR / ARR mix + `utm_source` → paid conversion, plus a one-shot Stripe → local subscription backfill so live customers reconcile on first webhook) as the next Primary Objective; alternatives in MASTER_ACTIONS.md remain Gmail OAuth mailbox connection and integration tests with Testcontainers + GreenMail (the only Roadmap MVP item still unchecked).
Advances: EPIC-11 Annual billing surfacing — Milestone 4 (Subscription billing_period field + active-cadence display). EPIC-11 now fully shipped; next Primary Objective proposed above pending [PLAN-REVIEW].

## 2026-06-08
Shipped: Stripe-driven backfill for pre-V17 `billing_period` — new `StripeSubscriptionGateway` interface + `StripeSubscriptionGatewayImpl` reads the current price ID off a live Stripe subscription (`Subscription.retrieve(id)` → `items.data[0].price.id`), wrapping every `StripeException` in an `Optional.empty()` so a deleted/unknown subscription never aborts a sweep. New `BillingPeriodBackfillService` in `com.emailmessenger.admin` walks `SubscriptionRepository.findByBillingPeriodIsNull()`, asks the gateway for each row's live price ID, reverse-maps via `BillingProperties.periodFor(...)`, writes the inferred `BillingPeriod` (and the resolved `stripePriceId`) back when it matches a configured SKU — and idempotently skips on a second run because filled rows drop out of the candidate set. Returns a `BillingPeriodBackfillResult` record partitioning the run into `{scanned, updated, missingStripeId, unmatchedPriceId, stripeMisses}` so the operator can tell a no-op apart from a partial reconcile. `AdminRevenueController` gains `POST /admin/revenue/reconcile-billing-period` (CSRF-protected, allowlist-gated — non-admins get the same 404 as `/admin/revenue` GET, so the action stays invisible to non-operators) that flashes the result and redirects back. `templates/admin/revenue.html` renders an `.admin-toolbar` section above the KPI grid with a "Reconcile from Stripe" button + hint copy, and a flash-message line that reads either "Reconcile: no rows needed backfill — every active subscription already has a billing period." or "Reconcile: scanned N, updated N, missing Stripe id N, unmatched price N, Stripe misses N." so the operator can see exactly what happened. ~40 lines of namespaced CSS (`.admin-toolbar`, `.admin-toolbar-form`, `.admin-toolbar-hint`, `.admin-toolbar-result`, `.admin-btn` + focus/hover states) appended to `main.css`, reusing the existing `--surface`/`--border`/`--brand` design tokens so dark mode picks up automatically. New tests: `BillingPeriodBackfillServiceTest` x8 (empty repo → no-op; monthly + annual price ID branches each flip `billingPeriod` correctly; null `stripeSubscriptionId` counts as `missingStripeId` and leaves the row untouched; unknown promo price ID counts as `unmatchedPriceId` and does NOT overwrite — protecting against silent corruption from one-off discount codes; gateway returning `Optional.empty()` counts as `stripeMiss` and leaves the row untouched; mixed batch partitions cleanly across all four outcome buckets; consecutive runs prove idempotency because the candidate set empties after the first one). `AdminRevenueControllerTest` extended x4 (admin POST runs reconcile, hits flash + redirect, and DB row's `billingPeriod` is reloaded as ANNUAL; non-admin POST returns 404 even with a valid CSRF token; no-candidate POST yields a flash result with `noOp == true`; GET renders the "Reconcile from Stripe" button + form action so a template regression that drops the button fails CI). `./mvnw test` → 537 tests pass (1 Docker-only skipped as before).
Advances: EPIC-12 First-paying-customer attribution funnel — Milestone 2 (Stripe backfill for pre-V17 billing_period).
Master action: none
Master action: [PLAN-REVIEW] EPIC-11 Annual billing surfacing is code-complete — adopt first-paying-customer attribution funnel as the next Primary Objective and update PLAN.md, or pick one of the listed alternatives.

## 2026-06-09
Shipped: account linking via `users.google_subject` — Flyway V18 adds the nullable, uniquely indexed column; `OAuth2ProvisioningService.provisionFromGoogle` now takes the OIDC `sub` and prefers a subject lookup over email match, so a Google address change still resolves to the same MailIM row. An email-match hit writes the subject onto the row (linking the existing email-password account); a fresh provision stamps it on create. `GoogleOidcUserService` threads `delegate.getSubject()` through on every callback. 5 new `OAuth2ProvisioningServiceTest` cases cover stamp-on-fresh, link-on-email-match, resolve-by-subject-after-email-change, no-overwrite on second login, and blank-subject defensive null. 586 tests pass.
Advances: EPIC-13 Google OAuth signup — Milestone 3 (account linking via google_subject).
Master action: none

## 2026-06-10
Shipped: EPIC-15 M2 — team-invite flow + step 4 of the onboarding progress card. Flyway V24 adds three tables: `teams` (UNIQUE on `owner_user_id` so each user owns at most one team for now), `team_members` (UNIQUE `(team_id, user_id)`, role enum OWNER/MEMBER), and `team_invites` (UNIQUE on SHA-256 token hash, with `accepted_at` / `revoked_at` / `expires_at` for full lifecycle). New `com.emailmessenger.team` package: `TeamService.findOrCreateOwnedTeam` lazy-creates the inviter's team on first invite (named `"<displayName>'s team"` or email fallback) and stamps an OWNER `TeamMember` row in the same transaction; `TeamInviteService.invite(User, String)` normalizes the invitee email, rejects blanks / non-emails / self-invites / duplicate pending addresses (case-insensitive lookup), mints a 32-byte URL-safe-base64 plaintext token whose SHA-256 hex is the only thing persisted, sets a 7-day TTL, and sends a MimeMessage with the accept URL via `JavaMailSender`; on a `MailException` the mail-failure outcome is returned without burning the row. `TeamInviteService.acceptInvite(String, User)` validates the token (exists, not used, not expired, not revoked), refuses to redeem when the signed-in email doesn't equal the invitee (a stolen link can't be redeemed by any other account), short-circuits to ALREADY_MEMBER + accept-stamp when the user is already in the team (idempotent re-accept), otherwise inserts a MEMBER `TeamMember` and marks the invite accepted. `TeamInviteController` (auth-required by default; no SecurityConfig allowlist change) wires `GET/POST /team/invite` (form + outstanding-invite list reflecting pending/joined/cancelled state) and `GET/POST /team/invite/accept` (status branches: ready / emailMismatch / invalid / joined / alreadyMember). New Thymeleaf templates `templates/team/invite.html` (back link to inbox, brand-styled form, flash messages, sent-invites list with relative date) and `templates/team/accept.html` (auth-card layout matching `password/forgot.html`, invalid + emailMismatch branches surface the gap clearly). `OnboardingChecklist` grows `teammateInvited` (4th positional arg), bumps `TOTAL_STEPS` to 4, recomputes `percentComplete()` (3/4 = 75%, 2/4 = 50%), adds CTA branch (`/team/invite` → "Invite a teammate"). `OnboardingService` now also reads `TeamInviteRepository.countNonRevokedByInviter(owner)` — revoked invites don't count toward step completion (a user who clicked invite then cancelled hasn't actually completed the step), but pending and accepted both do (sending the invite is the action). `threads.html` renders step 4 in both the always-visible progress strip and the empty-inbox welcome card. 15 new tests across `TeamInviteServiceTest` x8 (first invite lazy-creates team + OWNER member + persists hash; self-invite rejected with no mail send; duplicate pending invite rejected without minting a new token; invalid email rejected; `countNonRevokedByInviter` excludes revoked rows; accept adds MEMBER and marks token used; mismatched accepter email rejected without consuming the token; expired token rejected) and `TeamInviteControllerTest` x9 (unauthenticated `GET /team/invite` 302s to login; signed-in GET lazy-creates the team and renders empty invite list; POST sends + flashes 'sent'; POST blank email flashes 'invalidEmail'; POST self email flashes 'selfInvite'; accept form 'ready' branch for matching signed-in user; accept form 'emailMismatch' branch surfaces the wrong-account hint; unauthenticated accept 302s to login; POST accept adds the user to the team + redirects to /threads with 'joined' flash). `OnboardingServiceTest` extended from 5 → 6 cases (new `savedSearchDoneButNoInvitePointsToInviteStep`), existing 5 retrofitted to the 4th positional arg + `TeamInviteRepository` mock. `ThreadControllerTest` updated to the 4-arg `OnboardingChecklist`. `./mvnw test` → 656 tests pass (1 Docker-only skipped as before).
Advances: EPIC-15 in-app onboarding checklist — Milestone 2 (Step 4 — invite a teammate).
Master action: none

## 2026-06-11
Shipped: EPIC-16 M2 — team-scoped thread + notes access so an invited teammate following a shared `/threads/{id}` link sees the same conversation view and notes panel the owner sees. New `ThreadAccessService` (in `com.emailmessenger.team`) owns the access check: `findAccessibleThread(id, viewer)` returns the thread when the viewer is the owner or a `TeamMember` of the owner's team; `isOwner(thread, viewer)` separates the owner-only surfaces (reply form, upgrade CTA) from shared surfaces. `ThreadViewService` now resolves the thread through `ThreadAccessService` and only calls `markRead()` when the viewer is the owner — a teammate's read-through doesn't clear the owner's "unread" filter. `ThreadNoteService` gained `canAccessNotesOn(thread, viewer)` (gates on the *owner's* plan since they pay for the seat, then defers to `ThreadAccessService.isAccessibleTo`), and `notesFor` + `post` route through it — so a Free-plan teammate inside a Team-plan owner's team can read AND post notes, while a stranger still gets an empty list / `GATED`. `ThreadController.viewConversation` exposes `isThreadOwner` for the template (replaces the previous owner-implicit branching) and only surfaces `teamNotesUpgradeNudge` to the owner — a teammate viewing a Personal-plan owner's thread just sees no notes panel rather than an "Upgrade to Team" CTA they can't act on; `postNote` now looks up the thread via `ThreadAccessService.findAccessibleThread` so teammates can submit notes; reply still uses `findByIdAndOwner` (SMTP credentials are owner-scoped). `conversation.html` hides the reply form for non-owners and renders a small `.reply-area-readonly` "viewing a teammate's thread" hint pointing at the notes panel instead. New `existsByTeamAndUser` on `TeamMemberRepository` powers the membership probe. 8 new tests: `ThreadAccessServiceTest` x5 (owner-access; stranger denied; teammate accesses + isOwner=false; cross-team isolation; unknown id), `ThreadNoteServiceTest` x3 (teammate-on-Free-plan sees notes when owner is Team; teammate can post attributed to themselves; downgrade removes teammate access), `ThreadControllerTest` x3 (teammate viewer sees `isThreadOwner=false` + no upgrade nudge; teammate without notes access sees no nudge; teammate post routes through team-scoped access). `./mvnw test` → 702 tests pass (1 Docker-only skipped as before).
Advances: EPIC-16 shared-inbox — Milestone 2 (Thread visibility for invited teammates — notes work cross-user).
Master action: none

## 2026-06-11
Shipped: EPIC-16 M3 — @mention picker + transactional notification when a team note tags a teammate. New `NoteMentionService` (in `com.emailmessenger.team`) parses `(?<![A-Za-z0-9._-])@([A-Za-z0-9._-]{1,64})` against the team-member roster, resolves each token against three handles per member (full email, email local part, and the display name normalized to lowercase `[a-z0-9._-]`), excludes self-mentions, dedupes recipients in first-mention order, and sends a per-recipient transactional email via `JavaMailSender` whose subject is `<author> mentioned you on "<thread subject>"` and whose body links `<base>/threads/{id}#note-{noteId}` so the recipient can scroll to the exact note (each `<li class="note-bubble">` now renders `id="note-{id}"`). A `MailException` for one recipient is logged and counted as a miss; the loop continues so the rest still get the mail, and the note itself stays saved — mention notification is a best-effort side effect, never a transactional gate on the post. `ThreadNoteService.post(...)` calls `mentions.notify(saved)` after a successful save inside the same transaction (the notify method is `@Transactional(readOnly=true)` so it joins the existing tx and sees the just-saved row); `ThreadNoteService` constructor gained the `NoteMentionService` dependency. New `TeamMemberRepository.findByTeamOrderByJoinedAtAsc(team)` powers both the email-recipient roster and the in-page `@` picker — sorted by `joinedAt` so the picker order is stable and deterministic. `NoteMentionService.candidatesForThread(thread, viewer)` returns picker rows (`record MentionCandidate(userId, handle, label, email)`) for the team owned by the thread's owner; excludes the viewer themselves; and returns an empty list — never lazy-creating a team — when the owner hasn't formed a team yet. `ThreadController.viewConversation` injects `NoteMentionService`, exposes `teamMentionCandidates` on the model only when `canPostTeamNote` (so the picker never renders for non-posters), and threads it through to `conversation.html`. The template now renders a `<ul data-mention-candidates hidden>` payload of `data-handle`/`data-label`/`data-email` rows underneath the form plus a `<ul class="mention-picker" data-mention-picker hidden>` overlay, and a small "Type `@` to mention a teammate" hint when candidates exist. ~95 lines of vanilla JS appended to the existing inline `<script>` block implement the picker: it reads team members from the hidden payload, detects an `@` at-or-after-whitespace before the cursor, filters by prefix-on-handle or substring-on-label, renders up to 6 matches, supports `↑/↓/Enter/Tab/Esc` keyboard navigation plus mouse click, inserts `@<handle>` followed by a space at the cursor, and closes on blur (with a 100ms grace so click selection wins over blur). ~40 lines of namespaced CSS appended (`.note-input-wrap`, `.mention-picker`, `.mention-picker li.active`, `.note-hint`, `.note-hint code` plus dark-mode variants reusing the existing `--brand`/`--text`/`--text-muted`/`--surface` design tokens). 9 new tests in `NoteMentionServiceTest` (`@SpringBootTest @ActiveProfiles("dev") @Transactional`, `@MockBean JavaMailSender`): `mentionByEmailLocalPartSendsExactlyOneEmailToThatMember`, `displayNameNormalizedMatchResolvesToTheRightMember` (`@alicewong` → "Alice Wong"), `selfMentionDoesNotEmailTheAuthor`, `unmatchedTokenSendsNoEmail`, `multipleMentionsDedupeToOneEmailPerRecipient` (asserts exactly 2 sends for `@bob @carol — and again @bob`), `mailFailureForOneRecipientDoesNotBlockOthersAndDoesNotRollBackTheNote` (first `send` throws `MailSendException`, second succeeds, note persists, loop continues), `tokenAfterNonWhitespaceCharacterIsIgnored` (`jane@example.com` inside a sentence does NOT register as a mention thanks to the negative lookbehind), `candidatesForThreadExcludesViewerAndReturnsTeammates`, and `candidatesForThreadIsEmptyWhenOwnerHasNoTeamYet`. The `@BeforeEach` stub uses `thenAnswer(_ -> new MimeMessage(null))` rather than `thenReturn(...)` so each `createMimeMessage()` returns a fresh instance — the original `thenReturn` form handed back a single mutable MimeMessage whose `setTo` was overwritten per call, masking real send-count regressions (caught and fixed during this session). `ThreadControllerTest` extended with a `@Mock NoteMentionService` and a default `lenient` stub for `candidatesForThread` so the existing 46 conversation-render assertions still pass; the controller constructor's argument list grew accordingly. `./mvnw test` → 711 tests pass (1 Docker-only skipped as before). All three implementable EPIC-16 milestones (M1 owner notes, M2 cross-user team visibility, M3 @-mention notifications) are now code-complete; only M4 — the operator dashboard card that surfaces the Free→Team conversion lift on `/admin/revenue` — remains open in the backlog.
Advances: EPIC-16 shared-inbox — Milestone 3 (@mention notifications inside a note).
Master action: none

## 2026-06-11
Shipped: EPIC-17 M1 — Churn & MRR-retention card on `/admin/revenue`. New `ChurnMetrics` record + `ChurnMetricsService` in `com.emailmessenger.admin` compute a rolling-30-day cancellation snapshot: `canceledSubscribers`, `lostMrrCents` (sum of monthly-equivalent plan prices for every row that flipped `canceled` in the window, FREE rows skipped), `lostArrCents` (12×), `startingMrrCents` (`currentActiveMrr + lostMrr` — the SaaS-standard "what was on the books at the start of the window" anchor), `grossRevenueChurnRatePercent` = `lostMrr / startingMrr`, and a `PlanChurnBreakdown` per paid plan (Personal/Team/Enterprise always rendered, even at zero, so a missing row never silently hides a plan). Prior-30-day counterparts (`priorCanceledSubscribers`, `priorLostMrrCents`, `priorGrossRevenueChurnRatePercent`) feed delta helpers that mirror the EPIC-16 Team-adoption pattern (`canceledDeltaLabel` / `lostMrrDeltaLabel` reading "▲/▼ N% vs. prior 30 days") and a dedicated `churnRateDeltaLabel` that reports in **percentage points** because churn rate is already a percentage — a 17% → 10% rate is "▼ 7 pts vs. prior 30 days", not "-41% of 17%". New repository method `SubscriptionRepository.findCanceledBetween(from, to)` (eager `JOIN FETCH s.user`, half-open `[from, to)` keyed on `updatedAt` because Stripe's `customer.subscription.deleted` webhook + `@PreUpdate` stamp the same transaction the status flips, so a year-old subscription canceled this morning surfaces as this morning's churn rather than vanishing into a year-old anchor). `AdminRevenueController` injects the new service and exposes `churn` on the model. `templates/admin/revenue.html` renders a 5-KPI card (Canceled subscribers, Lost MRR, Lost ARR, Gross revenue churn %, Starting MRR) plus a per-plan canceled+lost-MRR table, slotted between the Team-plan adoption card and the Trial-end conversion card so revenue-health metrics cluster together. 10 new tests in `ChurnMetricsServiceTest` (empty data; cutoffs captured for both windows; per-plan monthly-equivalent sum with FREE skipped; per-plan bucketing across Personal/Team/Enterprise; churn rate = `lost / (current + lost)`; zero-start guard; prior-window deltas; **rate delta in points not percent** — covers a Personal-tier dataset where current 10% and prior 17% yields "▼ 7 pts"; no-prior-data branch labels; null-plan rows skipped). `AdminRevenueControllerTest` extended with `revenuePageRendersChurnCardWithCancellationsLostMrrAndPerPlanBreakdown` (boots full Spring + Thymeleaf + Security, seeds an active Team sub + a canceled Personal sub, asserts model attribute `churn` and the card headings "Churn", "Lost MRR", "Gross revenue churn", "Canceled subscribers", "Starting MRR" all render). `./mvnw test` → 738 tests pass (1 Docker-only skipped as before). PLAN.md transitions to **EPIC-17 churn telemetry** as the next Primary Objective (M1 churn card shipped here; M2 cancellation-reason capture, M3 at-risk retention queue, M4 per-plan churn line in the operator weekly digest open in BACKLOG.md). The 2026-06-04/06/08 chain of [PLAN-REVIEW] entries collapses into a single open entry on MASTER_ACTIONS.md awaiting live numbers before re-evaluating.
Advances: EPIC-17 churn telemetry — Milestone 1 (Churn & MRR-retention card).
Master action: none
