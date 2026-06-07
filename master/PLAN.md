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
count, history, and saved-search counts grow. EPICs 02–09 (Monetization,
Mailbox Onboarding, Deployability, Acquisition, Launch readiness, Inbox
Search, Saved Searches & Reactivation, Account self-serve) are
code-complete in `claude_routine`; live deploy is gated on Master ops
(hosting, domain, Stripe live keys, encryption secrets, demo video URL).

## Primary Objective

**Ship EPIC-10 Mobile / PWA.** Responsive `/threads` + `/conversation`
layouts already work on a phone browser, but MailIM is not installable.
The mobile experience caps Personal-tier conversion because the only
way to read mail "on my phone" today is to open the browser, tab over,
sign in again, and type the URL — the natural retention loop ("ping me
on Slack, I'll glance at my MailIM IM-view") needs a home-screen icon
and an app-shell launch. PWA install + offline shell is the cheapest
path: no App Store review, no iOS dev account, no Play Store fees, and
the same Spring Boot codebase serves it. This Objective ends when a
visitor on iOS Safari or Android Chrome can install MailIM from
`/threads`, launch it from the home screen into a standalone app shell
that opens to `/threads` without browser chrome, see a sensible "you're
offline" screen if the network drops, and (where supported) get a
native install banner the first time they visit signed-in.

## Milestones

1. **Web app manifest + PWA icons + theme color.** `GET
   /manifest.webmanifest` returns a valid manifest (name, short_name,
   description, start_url=`/threads`, scope=`/`, display=`standalone`,
   theme_color, background_color, icons[192/512/maskable]). `GET
   /icons/icon-{192,512,512-maskable}.png` and `GET
   /apple-touch-icon.png` (180px, iOS Safari) are generated server-side
   from the brand mark. `<link rel="manifest">` + `<meta
   name="theme-color">` + `<link rel="apple-touch-icon">` are injected
   into every public page (via the SEO fragment) and the authenticated
   `/threads` + `/conversation` views so the browser install prompt
   triggers from both pre-signup and post-signup surfaces.
2. **Service worker for offline shell.** `GET /sw.js` registers a
   service worker that pre-caches the static shell (CSS, brand mark,
   `/threads` HTML skeleton) on install, serves cached responses for
   navigation requests when the network fails, and ships a small
   `/offline` HTML fallback so an installed PWA opened in airplane
   mode shows MailIM-branded "you're offline" copy instead of the iOS
   "Safari can't open the page" screen. Cache versioning busts on
   deploy via a build-time hash.
3. **In-app install prompt + iOS instructions.** On `/threads` and
   `/`, listen for `beforeinstallprompt`, stash the event, and render
   a dismissable "Install MailIM" banner with a button that triggers
   the native prompt. Safari ignores `beforeinstallprompt`, so detect
   iOS Safari and render an "Add to Home Screen → tap share → Add"
   help card instead. Both banners persist a dismiss cookie so a user
   who said no isn't pestered again.
4. **Mobile-tuned threads + conversation view.** Audit `/threads` and
   `/conversation` on a 375px viewport: tap targets ≥44px, safe-area
   insets for iOS notch, sticky reply form above the on-screen keyboard,
   swipe-to-back gesture surfaced as a visible back arrow, day-separator
   sticky headers, and a `viewport-fit=cover` meta so the standalone PWA
   uses the full screen. Wire the new mobile-first CSS through the
   existing `--brand` / `--surface` design tokens so dark mode still
   applies.

## Done means

A visitor on iOS Safari or Android Chrome can `GET /threads`, accept the
install prompt (Android) or follow the "Add to Home Screen" card (iOS),
launch MailIM from the home screen as a standalone app — no browser
chrome — landing on `/threads`. The service worker pre-caches enough of
the static shell that opening the installed app in airplane mode shows a
MailIM-branded offline screen instead of a generic browser error. The
mobile-tuned layout passes a 375px-viewport audit on `/threads` and
`/conversation` (tap targets, safe-area, sticky reply form). The install
banner dismisses persistently so a "no thanks" doesn't reappear.
