# PLAN

## What this is

**email-messenger** (product name: MailIM) is a Spring Boot web app that
imports email threads over IMAP and renders them as a modern IM-style chat
view — bubbles, avatars, day separators, dark mode — instead of the
nested-quote wall most mail clients show. It is positioned as a freemium
SaaS: Free (1 mailbox, 500 threads), Personal $9/mo (3 mailboxes,
unlimited history), Team $29/mo (10 mailboxes, sharing), Enterprise $99/mo
(SSO, audit). Annual billing offers 2 months free. Money comes from
recurring subscriptions, with natural Free → Personal → Team upgrades as
mailbox count and history needs grow. EPICs 02–06 (Monetization, Mailbox
Onboarding, Deployability, Acquisition, Launch readiness) are
code-complete in `claude_routine`; live deploy is gated on Master ops
(hosting, domain, Stripe live keys, encryption secrets, demo video URL).

## Primary Objective

**Ship EPIC-07 Inbox Search & Discovery.** With unlimited history on
paid plans and a 500-thread cap on Free, finding a specific message gets
painful fast — the inbox is a flat list ordered by recency, with no
search box, no sender drill-down, and no way to filter. That hurts
retention on Free (users churn before hitting the upgrade trigger) and
hurts paid stickiness (the more history you have, the more you need to
search). Search is also the foundation for the next batch of monetizable
features (saved searches, AI semantic search, sender-grouped sidebars).
This Objective ends when a user with hundreds of imported threads can
find any conversation in under five seconds using subject, sender, or
body text — and the Free-vs-Personal gap pivots from "thread cap" to
"unlimited search depth", giving the upgrade a second compelling reason.

## Milestones

1. **Subject + participant search on `/threads`.** A search bar above the
   inbox accepts a query and filters threads by case-insensitive
   substring of subject, participant email, or participant display name.
   Pagination preserves the query; an empty-results state offers a quick
   way back to the full inbox. _(Shipped 2026-06-05 — `EmailThreadRepository.search`
   JPQL drives a `?q=` filter on `GET /threads` with the search input
   rendered in `threads.html` and styled inline in `main.css`. 9 new
   tests; 268 total pass.)_
2. **Full-text body search (Personal+).** Postgres `tsvector` over
   `messages.body_plain` powers a body-text search path. Free-tier users
   running a query that only hits body content see an upgrade nag
   ("Search across message bodies is a Personal feature") that links to
   `/billing/checkout?plan=personal`. H2-friendly fallback for tests.
3. **Sender-grouped sidebar drill-down.** Group threads by participant
   (avatar + name + thread count) so users can drill from "everyone" to
   "everything from acme.com". Lives in the same `/threads` surface as a
   collapsible left rail; integrates with the search query so a sender
   pick narrows the result set without a full reload.
4. **Filter chips: date range, has-attachment, unread.** Compact chip
   row above the thread list that ANDs onto the active search/sender
   filter. Each chip toggles a `?since=`/`?attachments=true`/`?unread=true`
   query param. Filters are bookmarkable.

## Done means

A user with 200+ imported threads can type a sender's last name into the
inbox search and see every thread they're on in well under five seconds;
a Free-tier user trying to body-search hits a clearly-worded paid prompt
that opens a checkout; a paid user can drill to a single sender via the
sidebar and combine that with a date-range chip to find "everything from
Ada in the last 30 days"; and the bookmarked filter URL renders the same
result set on reload.
