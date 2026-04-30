# Internal TODO

Format: `[STATUS] [TAG] [SIZE] Description (EPIC-N)`

Statuses: TODO, IN-PROGRESS, DONE, BLOCKED
Tags: CORE, GROWTH, HEALTH, TEST-FAILURE, UX
Sizes: S=< 2h, M=2-4h, L=4-8h
Epics: see master/EPICS.md
Done items get archived to master/DONE_ARCHIVE.md

Priority order (top to bottom):
TEST-FAILURE → income-critical → UX-conversion → HEALTH → GROWTH → BLOCKED

---

## Test Failures

_(none open this session — full suite green, 131 passing, 6 skipped without Docker)_

---

## Income-Critical Path (highest leverage public funnel and trust)

- [ ] TODO [GROWTH] [S] Admin notification email on new waitlist signup: after WaitlistController saves a new entry, fire Spring Mail to `${ADMIN_NOTIFY_EMAIL:}` (no-op if blank) with subject "New MailIM waitlist signup: {email}". MEDIUM impact. No prerequisites beyond existing Spring Mail dep. (EPIC-1)
- [ ] TODO [GROWTH] [M] EML + .mbox drag-and-drop import zone on demo page: enhance /demo with a drag-and-drop zone that accepts .eml files, parses them via a POST /demo/upload endpoint, and previews the email as a chat bubble; lowers the barrier to "aha moment" for first-time visitors. HIGH conversion impact. No prerequisites beyond existing DemoService + EmailImportService. (EPIC-1)
- [ ] TODO [GROWTH] [S] EML file upload: upload a raw .eml file to seed threads; useful for demos and offline testing. MEDIUM impact. (EPIC-1)
- [ ] TODO [GROWTH] [M] .mbox file import: upload a raw .mbox archive (Google Takeout / Thunderbird export) to import all threads in bulk; removes IMAP credential requirement for first-time users. HIGH impact, no prerequisites. (EPIC-1)
- [ ] TODO [GROWTH] [S] Comparison landing page at /compare: static Thymeleaf page with a feature-comparison table (MailIM vs Superhuman vs HEY vs Gmail). MEDIUM SEO + conversion impact. (EPIC-1)
- [ ] TODO [GROWTH] [S] UTM-source capture on /waitlist signup: extend WaitlistEntry with a `source` column (V3__waitlist_source.sql), read `?utm_source=` query param into a hidden form field, persist. MEDIUM growth-analytics impact. (EPIC-1)
- [ ] TODO [GROWTH] [S] Exit-intent waitlist modal on /demo and /pricing. MEDIUM conversion impact (5-15% lift typical). (EPIC-1)
- [ ] TODO [GROWTH] [S] Sticky "Get early access" CTA bar on /pricing (`position: sticky; bottom: 0`). MEDIUM conversion impact. (EPIC-1)
- [ ] TODO [GROWTH] [M] Thread permalink sharing: generate a shareable read-only link (e.g. /share/{token}); viral touchpoint. HIGH income impact. (EPIC-1)
- [ ] TODO [GROWTH] [S] "Self-serve embed" widget for the demo: emit `<iframe src="/demo/{id}/embed">` HTML on /demo with a "Copy embed code" button. MEDIUM-HIGH distribution impact. (EPIC-1)
- [ ] TODO [GROWTH] [S] Waitlist position + launch ETA on success state: show "You're #N on the waitlist" using waitlist count and `APP_LAUNCH_DATE` env var. MEDIUM retention impact. (EPIC-1)
- [ ] TODO [GROWTH] [S] Waitlist success "Share this" CTA: pre-filled copy-able share URL `/waitlist` with a "📋 Copy link to share" button. MEDIUM virality impact. (EPIC-1)
- [ ] TODO [GROWTH] [S] Show waitlist count milestones on landing hero: switch hero subhead at 100 / 500 / 1000 to "Join 1,000+ people getting early access". LOW-MEDIUM impact. (EPIC-1)
- [ ] TODO [GROWTH] [S] "Only takes 30 seconds" trust microcopy on the waitlist form CTA. LOW-MEDIUM impact. (EPIC-1)
- [ ] TODO [GROWTH] [S] Demo "Share this demo" button: "Copy link" button on demo.html conversation view. MEDIUM viral impact. (EPIC-1)
- [ ] TODO [GROWTH] [S] Hero video/GIF embed slot on landing page: add `<div class="hero-media">` placeholder; one-line update once asset is recorded. (EPIC-1)
- [ ] TODO [GROWTH] [S] PWA web app manifest: manifest.json + apple-touch-icon; installs as PWA → 3× higher 30-day retention. MEDIUM impact. (EPIC-1)
- [ ] TODO [GROWTH] [S] Public roadmap page at /roadmap: static HTML listing upcoming features with rough ETA; reduces "is this abandoned?" churn. MEDIUM impact. (EPIC-1)
- [ ] TODO [GROWTH] [S] Press kit page at /press: static Thymeleaf page with founder bio, product screenshots, logo files (light + dark), brand colors, one-line elevator pitch, contact email. Drives organic backlinks from journalists, podcasters, and roundup-post authors. LOW-MEDIUM impact. Prerequisite: Master uploads founder photo + logo SVG (see TODO_MASTER.md). (EPIC-1)
- [ ] TODO [GROWTH] [S] Twitter/X share-card meta tags on demo conversation pages: per-conversation `og:title`, `og:description`, `twitter:card=summary_large_image`, `twitter:image` so shared `/demo/{id}` URLs preview as eye-catching cards rather than plain links. LOW-MEDIUM virality. (EPIC-1)
- [ ] TODO [GROWTH] [S] Public status page at /status: static "All systems operational" page with last-deploy timestamp and `/health` check result. Even before real monitoring exists, a status page is a trust signal that the team is operating professionally. LOW impact, but cheap to ship. (EPIC-2)
- [ ] TODO [GROWTH] [S] Submit /sitemap.xml to Google Search Console + Bing Webmaster: now that the sitemap endpoint exists, register the property and submit it. Without registration, the sitemap is invisible to search engines for weeks. MEDIUM SEO impact. Master action — added to TODO_MASTER.md. (EPIC-1)
- [ ] TODO [GROWTH] [S] Auto-include each new public page in `SeoController.PUBLIC_PATHS` when shipping it (/compare, /roadmap, /press, /status). Cheap discipline — every additional indexable URL is a long-tail SEO surface. (EPIC-1)
- [ ] TODO [GROWTH] [S] Add `<link rel="sitemap" type="application/xml" href="/sitemap.xml">` and `<link rel="canonical">` to landing/pricing/demo/waitlist `<head>` (canonical already on `/`; missing on others). Helps crawlers and de-dupes URL variants (e.g. ?utm_source=). LOW-MEDIUM SEO impact. (EPIC-1)
- [ ] TODO [GROWTH] [S] Visible "Why now?" urgency copy on /waitlist hero: "Spots in the early-access cohort are limited" or "Beta cohort #1 closes when we hit 500 signups". Scarcity is the cheapest conversion lever before billing exists. LOW-MEDIUM impact. (EPIC-1)
- [ ] TODO [GROWTH] [S] Pricing page social-proof bar above plan cards: "Trusted by 500+ early-access users" (read live from `WaitlistEntryRepository.count()`); falls back gracefully when count < 100. LOW-MEDIUM impact, no prerequisites. (EPIC-1)
- [ ] TODO [GROWTH] [S] Referral leaderboard endpoint at /waitlist/leaderboard: top 10 entries by `referrals_count` (with email anonymized to first letter + domain) shown as a public scoreboard. Public scoreboards 2-3× referral activity by adding social competition. LOW-MEDIUM impact. Prerequisite: pre-launch referral feature (now shipped Run #21). (EPIC-1)
- [ ] TODO [GROWTH] [S] Referral-credit milestone copy on /waitlist success state: when `referralsCount >= 3`, swap "Refer 3 friends to jump 100 places" with "🎉 You've referred {N} friends — {N*100} places skipped". Concrete progress beats abstract incentive. LOW-MEDIUM impact, no prerequisites. (EPIC-1)
- [ ] TODO [GROWTH] [S] UTM-source capture on inbound referrals: extend WaitlistController to also persist `?utm_source=` per signup, and include it on the referrer credit so we can see whether referrals from Twitter convert at higher rates than referrals from email. MEDIUM analytics impact. Prerequisite: existing UTM-source-capture task (already in backlog) + V4 migration. (EPIC-1)
- [ ] TODO [GROWTH] [S] Referral OG share-card generator at /waitlist/share-card.png: dynamic image (Java BufferedImage / SVG) showing "I just joined the MailIM waitlist — turn email into chat" with the user's referral URL as a QR code. When the user posts the link to Twitter, the link unfurls as the card. HIGH virality. Prerequisite: pre-launch referral feature (now shipped Run #21). (EPIC-1)
- [ ] TODO [GROWTH] [S] Auto-incremented "share count" microcopy on success state: append "← {totalShares} people have already shared their link" below the Copy button (track via a `share_clicks` table on POST /waitlist/share-tracked, fire-and-forget from the JS). Even an inflated counter creates a herd-behavior nudge. LOW impact. (EPIC-1)

---

## UX (conversion-blocking surface polish)

- [ ] TODO [UX] [S] Testimonials on landing page and pricing page: 2-3 placeholder quote blocks in index.html (between "How it works" and pricing preview) and below plan cards in pricing.html. HIGH conversion impact. (EPIC-1)
- [ ] TODO [UX] [M] Mobile layout pass: ensure thread list and conversation view are usable on 375px screens; bubbles must not overflow viewport. (EPIC-3)
- [ ] TODO [UX] [S] Thread list: show last-message-body preview (first 80 chars) below subject line — denormalize via query or add `last_message_preview` column to email_threads. MEDIUM impact. (EPIC-3)
- [ ] TODO [UX] [S] IMAP sync status indicator: show "last synced X minutes ago" in thread list header. (EPIC-3)
- [ ] TODO [UX] [S] Replace legal notice placeholder in /privacy and /terms with real legal copy before accepting any payments or EU users. No code needed — requires legal content generation (see TODO_MASTER.md). (EPIC-2)
- [ ] TODO [UX] [S] Validate `?ref=` token on GET /waitlist before rendering the "🎁 A friend invited you" banner: if the token doesn't resolve to a known referrer, hide the banner (still keep the hidden form field — credit-attempt is silently ignored server-side). Prevents a misleading banner from rendering for malformed/typo'd referral links. LOW impact, no prerequisites. (EPIC-1)

---

## Health (security, perf, deps)

- [ ] TODO [HEALTH] [M] Content-Security-Policy header: SecurityHeadersFilter is missing a CSP. Prerequisite: move all inline `<script>` blocks from threads.html, pricing.html, conversation.html into `/static/js/` external files; then add `Content-Security-Policy: default-src 'self'; style-src 'self' 'unsafe-inline'; script-src 'self'; img-src 'self' data: https:` in SecurityHeadersFilter. Removes largest XSS escalation vector. (EPIC-2)
- [ ] TODO [HEALTH] [S] Upgrade jsoup from 1.17.2 to latest release (1.19.x or newer): jsoup is the sole HTML sanitization dependency; staying current is critical. (EPIC-2)
- [ ] TODO [HEALTH] [S] Attachment N+1 query: Message.attachments loaded lazily per message; add `@BatchSize(size=50)` to Message.attachments. Low priority until threads with many attachments are common. (EPIC-2)
- [ ] TODO [HEALTH] [S] WaitlistReferralService.creditReferrer race: two concurrent signups crediting the same referrer can lost-update each other (read 0, both write 1). Replace the load-and-save pattern with a single atomic `@Modifying @Query("UPDATE WaitlistEntry e SET e.referralsCount = e.referralsCount + 1 WHERE e.referralToken = :token AND LOWER(e.email) <> LOWER(:newSignupEmail)")` invocation, or add `@Version` to WaitlistEntry and retry on optimistic-lock failure. Low impact at current volume (would need two referrals within the same millisecond) but a correctness gap. (EPIC-1)

---

## Growth — SEO & polish

- [ ] TODO [GROWTH] [S] JSON-LD FAQPage schema on /pricing: `<script type="application/ld+json">` block; enables Google rich-result accordion in SERPs. MEDIUM SEO impact. (EPIC-1)
- [ ] TODO [GROWTH] [S] Demo page SEO: add keyword-rich h2 sub-heading, feature bullet list, JSON-LD SoftwareApplication schema to /demo. MEDIUM impact. (EPIC-1)
- [ ] TODO [GROWTH] [S] Open Graph + meta description tags on threads.html, conversation.html, error.html (already done on waitlist/pricing/demo). MEDIUM impact. (EPIC-1)
- [ ] TODO [GROWTH] [S] SEO tags on legal pages (privacy, terms, refund): add og:title, og:description, og:type, meta description, canonical to all three. LOW individual impact. (EPIC-1)
- [ ] TODO [GROWTH] [S] Canonical URL `<link rel="canonical">` on remaining public pages (threads.html, conversation.html, demo.html, waitlist.html, pricing.html, error.html). LOW impact. (EPIC-1)

---

## Growth — Core IM Reading Experience (EPIC-3)

- [ ] TODO [GROWTH] [S] Basic thread search at GET /threads?q=: JPA LIKE query on email_threads.subject and participants.email. MEDIUM impact. (EPIC-3)
- [ ] TODO [GROWTH] [M] Add Gravatar + initials avatar fallback for Participant display in conversation view. MEDIUM impact. (EPIC-3)
- [ ] TODO [GROWTH] [M] SSE live conversation refresh: Spring SseEmitter pushes "new-message" event to the open conversation page. MEDIUM impact. (EPIC-3)
- [ ] TODO [GROWTH] [S] Add unread thread tracking: mark-as-read on view, unread count badge in thread list. MEDIUM impact. (EPIC-3)
- [ ] TODO [GROWTH] [S] "Copy conversation as Markdown" button: one-click copy of full thread to clipboard. MEDIUM impact. (EPIC-3)
- [ ] TODO [GROWTH] [S] Keyboard shortcut `?` to show help modal listing all keyboard shortcuts (j/k/Enter/r/Esc); power-user delight. LOW-MEDIUM impact. (EPIC-3)

---

## Growth — Plan/Tier monetization hooks

- [ ] TODO [GROWTH] [S] In-app upgrade preview of locked features: show disabled/blurred Team-tier features with "Upgrade to unlock" CTA. HIGH income impact. (EPIC-5 prep)
- [ ] TODO [GROWTH] [S] Outbound webhook trigger on new message: POST to configured URL when new thread message arrives (Zapier/Make integration); Team plan gate. MEDIUM impact. (EPIC-6)
- [ ] TODO [GROWTH] [S] "Sent via MailIM" branding footer in outgoing replies for Free plan users; disabled for Personal+. MEDIUM impact. (EPIC-1)

---

## Growth — Blocked on transactional email provider (TODO_MASTER.md)

- [ ] TODO [GROWTH] [S] Waitlist confirmation email: send a "you're on the list" transactional email immediately after a successful waitlist signup. HIGH income impact. Prerequisite: transactional email provider credentials. (EPIC-1)

---

## Auth-Gated Growth (EPIC-4 prerequisite)

- [ ] TODO [GROWTH] [M] User registration and authentication (Spring Security + email/password + remember-me) — prerequisite for billing, multi-tenancy, all features below. (EPIC-4)
- [ ] TODO [GROWTH] [M] First-run onboarding wizard: guided "connect your first mailbox" flow after signup. HIGH impact. (EPIC-4)
- [ ] TODO [GROWTH] [S] Add Google OAuth single sign-on: lower signup friction, auto-populate Gmail mailbox. HIGH impact. (EPIC-4)
- [ ] TODO [GROWTH] [S] Custom SMTP/from-address settings per user: configure "From" email for outgoing replies. HIGH impact. (EPIC-4)
- [ ] TODO [GROWTH] [M] AI-generated thread summary: one-sentence summary per thread; Claude API; Personal+ tier gate. HIGH impact. (EPIC-4 + EPIC-5)
- [ ] TODO [GROWTH] [S] Reply signature: per-user configurable HTML/text signature. MEDIUM impact. (EPIC-4)
- [ ] TODO [GROWTH] [S] Referral link "Invite a teammate" — awards 1 month free on conversion. (EPIC-4)
- [ ] TODO [GROWTH] [S] In-app referral prompt after user imports 10+ threads. MEDIUM impact. (EPIC-4)
- [ ] TODO [GROWTH] [M] Email digest notifications (daily/weekly unread thread summary). (EPIC-4)
- [ ] TODO [GROWTH] [M] Thread labels/tags: Team plan gate. (EPIC-4)
- [ ] TODO [GROWTH] [S] Thread snooze: re-surface thread at a set time. (EPIC-4)
- [ ] TODO [GROWTH] [S] Thread archiving: "Archive" action per thread; /archived route. (EPIC-4)
- [ ] TODO [GROWTH] [S] Conversation pinning: pin up to 3 threads to top of list. (EPIC-4)
- [ ] TODO [GROWTH] [S] Browser push notifications via Web Push API: notify on new email in watched thread. (EPIC-4)

---

## Stripe-Gated Growth (EPIC-5)

- [ ] TODO [GROWTH] [M] Add Stripe billing integration: subscription plans (Free/Personal/Team), checkout flow, webhook handler. HIGH income impact. Prerequisite: user auth. (EPIC-5)
- [ ] TODO [GROWTH] [S] Stripe customer portal integration: self-service upgrade/downgrade/cancellation. HIGH impact. (EPIC-5)
- [ ] TODO [GROWTH] [S] Plan-limit enforcement: max mailboxes per plan, max thread history, upgrade prompt at limit. (EPIC-5)
- [ ] TODO [GROWTH] [S] Upgrade prompt inline in thread list when user hits free tier limit. HIGH impact. (EPIC-5)
- [ ] TODO [GROWTH] [S] Annual/monthly billing toggle on pricing/settings page with "Save 16%" label. (EPIC-5)
- [ ] TODO [GROWTH] [S] 14-day free trial on Personal tier. HIGH impact. (EPIC-5)

---

## Larger Post-Auth Features (EPIC-6)

- [ ] TODO [GROWTH] [M] REST API for Personal+ tier: JSON endpoints for thread/message/reply; Zapier/Make integrations. HIGH impact. (EPIC-6)
- [ ] TODO [GROWTH] [M] Full-text search across threads (PostgreSQL tsvector) — Personal/Team upgrade gate. (EPIC-6)
- [ ] TODO [GROWTH] [M] Slack/Discord webhook integration: POST to Slack when new email arrives. HIGH impact. (EPIC-6)
- [ ] TODO [GROWTH] [M] Email forwarding address: assign each user a unique @mailaim.app address. HIGH impact. (EPIC-6)
- [ ] TODO [GROWTH] [M] Thread export (PDF/HTML): clean printable file; Personal/Team plan gate. MEDIUM impact. (EPIC-6)
- [ ] TODO [GROWTH] [M] Send-later scheduling for replies. HIGH impact. (EPIC-6)

---

## Blocked

- [ ] BLOCKED [UX] [S] Thread list "+ Add mailbox" nav link points to /settings/mailboxes (404). [BLOCKED] until user auth/onboarding route ships. (EPIC-4)
- [ ] BLOCKED [HEALTH] [S] Add CSRF protection: CSRF filter must be enabled on reply and waitlist POST endpoints. [BLOCKED] until Spring Security / user auth task starts. (EPIC-4)
- [ ] BLOCKED [HEALTH] [S] Rate-limit POST /threads/{id}/reply and POST /waitlist. [BLOCKED] until auth ships (per-user) or via nginx/Cloudflare (per-IP, no code needed). (EPIC-2)
