# PLAN

## What this is

**email-messenger** (product name: MailIM) is a Spring Boot web app that
imports email threads over IMAP and renders them as a modern IM-style chat
view — bubbles, avatars, day separators, dark mode — instead of the
nested-quote wall most mail clients show. It is positioned as a freemium
SaaS: Free (1 mailbox, 30-day history), Personal $9/mo (3 mailboxes,
unlimited history), Team $29/mo (10 mailboxes, sharing), Enterprise $99/mo
(SSO, audit). Annual billing offers 2 months free. Money comes from
recurring subscriptions, with natural Free → Personal → Team upgrades as
mailbox count and history needs grow. EPIC-02 Monetization, EPIC-03
Mailbox Onboarding, and EPIC-04 Deployability are all code-complete in
`claude_routine`; live deploy is gated on Master ops (hosting, domain,
Stripe live keys, encryption secrets).

## Primary Objective

**Ship EPIC-05 Acquisition.** Even with prod live, every Product-Hunt /
referral / SEO visitor currently hits a Spring 404 at `/` because the
funnel only has `/pricing`, `/login`, `/register` — no marketing home,
no story, no above-the-fold conversion path. Until `/` sells the product
and routes warm traffic into `/register`, the only signups will be
people who already know the URL of `/register`, which is nobody. This
Objective ends when a cold visitor landing on `mailaim.app/` sees a real
landing page, can pick a plan, and reach the registration form in two
clicks — and we can credibly post the URL to Product Hunt without
embarrassment.

## Milestones

1. **Public landing page at `/`.** Real marketing home: hero + tagline +
   primary CTA, IM-conversation preview, feature grid, how-it-works,
   pricing recap with link to `/pricing`, final CTA, footer. Anonymous
   visitors see the page; logged-in users redirect to `/threads` so the
   marketing site doesn't intercept the app.
2. **Conversion-tracked signup funnel.** `/register` (and `/register?plan=…`)
   land with the plan + UTM source preserved through Stripe Checkout
   success, and the trial-banner copy on `/threads` reflects which plan
   was chosen. One source of truth for "where did this signup come from"
   so we can read it back per-cohort.
3. **SEO basics + OG previews.** `/`, `/pricing`, `/login`, `/register`
   render unique `<title>`, `meta description`, canonical URL,
   OpenGraph + Twitter card tags. `sitemap.xml` and `robots.txt` served
   from the app so search engines and Slack/Twitter unfurls work without
   manual asset uploads.
4. **First-touch demo content.** A `?demo=1` mode (or shared demo user)
   that shows a curated thread rendered as chat — no signup needed — so
   Product Hunt visitors and Twitter clickers can see the product
   actually working in 5 seconds.

## Done means

A cold visitor opens `https://mailaim.app/`, sees a polished landing page
with hero, IM preview, features, pricing recap, and a "Start free" CTA;
clicking it takes them to `/register`; UTM source and chosen plan are
preserved through Stripe Checkout to the trial-banner on `/threads`;
`view-source:` on every public page shows distinct title, description,
canonical, and OG tags; `sitemap.xml` and `robots.txt` are served; and
`/?demo=1` renders a working conversation view without an account.
