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
count, history, and saved-search counts grow. EPICs 02–08 (Monetization,
Mailbox Onboarding, Deployability, Acquisition, Launch readiness, Inbox
Search, Saved Searches & Reactivation) are code-complete in
`claude_routine`; live deploy is gated on Master ops (hosting, domain,
Stripe live keys, encryption secrets, demo video URL).

## Primary Objective

**Ship EPIC-09 Account self-serve.** Authenticated MailIM users currently
have no recovery path if they forget their password — the only login form
demands the password, the database stores a one-way bcrypt hash, and the
codebase has no password-reset flow. For a paid product this silently
caps retention (a churned user can't recover, just abandons), and it is a
hard blocker for any real-world Stripe live launch (a paying customer
locked out of their own account is a chargeback). Email verification on
signup hardens the same surface — without it, anyone can register with
any address (real or stolen), the reset flow becomes a free
account-takeover vector, and transactional emails (digest, re-engagement)
go to unconfirmed addresses. In-app password/email change and a basic
login-throttle audit close the loop so the account UX feels like a paid
product. This Objective ends when a user who forgot their password can
recover it from `/login` end-to-end via a tokenized email, signup emails
verify the address before the first paid feature unlocks, and
authenticated users can change their own password and email from a
self-serve account page.

## Milestones

1. **Password reset via emailed token.** `/login` gains a "Forgot
   password?" link → `/password/forgot` form takes an email → service
   emails a tokenized `/password/reset?token=…` URL → `/password/reset`
   form sets a new password and revokes any other outstanding tokens for
   the same user. Tokens are 32 random bytes URL-safe base64, stored as
   SHA-256 hash, single-use (consumed → `used_at`), and expire in 1 hour.
   `POST /password/forgot` always renders the same generic "if we know
   that email, you'll get a link" screen to avoid account enumeration.
2. **Email verification on signup.** New users get a `email_verifications`
   token in a confirmation email; until verified, a soft banner on
   `/threads` prompts them to verify; `GET /verify-email?token=…` flips
   the `users.email_verified_at` column and clears the banner. Resend
   link from the banner. Optional unverified-account grace window before
   paid feature gates kick in.
3. **In-app change password / change email.** Authenticated `/account`
   page lets the user change password (requires current password) and
   change email (re-verifies the new address via the same token flow).
   Bcrypt round-trip on every change. All outstanding password-reset
   and email-verification tokens are revoked on password/email change.
4. **Login throttling + auth audit log.** Brute-force protection on
   `/login` (progressive delays / temporary lockout after N failures per
   email or IP), and a minimal `auth_events` table backing a "recent
   account activity" panel on `/account` (last 10 logins, password
   changes, email changes, resets).

## Done means

A user who forgot their password can recover it from `/login` end-to-end:
"Forgot password?" → enter email → receive token URL → set new password →
sign in with the new password. A newly-registered user receives a
verification email on signup and the unverified-account banner disappears
after they click through. An authenticated user can change their own
password (requiring the current one) and email (re-verifying the new
address) from `/account` without contacting support. Five failed login
attempts in a row temporarily blocks further attempts for that account
or IP, and the account page lists the most recent auth events.
