# Changelog

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
