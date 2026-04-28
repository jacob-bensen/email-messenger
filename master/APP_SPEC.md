# MailIM — Application Specification

## Overview

**MailIM** is a SaaS web application that transforms email inboxes into a
modern instant-message-style conversation interface. Users connect one or more
IMAP/SMTP mailboxes and see their email threads rendered as chat bubbles —
similar to iMessage or Slack — rather than the traditional top-down wall-of-text
format.

## Target Users

- Remote workers overwhelmed by high-volume email chains
- Support teams that use email but want a chat-like UX
- Freelancers who want a unified view of client conversations

## Business Model

**Freemium SaaS** with monthly/annual billing:

| Plan       | Price     | Limits                                   |
|------------|-----------|------------------------------------------|
| Free       | $0        | 1 mailbox, 30-day history, 500 threads   |
| Personal   | $9/mo     | 3 mailboxes, unlimited history           |
| Team       | $29/mo    | 10 mailboxes, team sharing, priority support |
| Enterprise | $99/mo    | Unlimited mailboxes, SSO, audit logs     |

Annual billing: 2 months free (16% discount).

## Core Features (MVP)

1. **IMAP connection** — connect any IMAP/SMTP mailbox (Gmail, Outlook, etc.)
2. **Thread view** — email threads displayed as IM-style chat bubbles
3. **Quoted-reply stripping** — removes `> ` and "On … wrote:" noise
4. **Same-sender grouping** — consecutive messages from the same person
   are collapsed into a bubble run
5. **Reply inline** — compose and send replies from the conversation view
6. **Participant avatars** — Gravatar-based or initials fallback
7. **Dark mode** — full CSS dark-mode support

## Planned Features (Post-MVP)

- Attachment preview (images inline, download link for others)
- Unread badge and read/unread tracking per thread
- Search across all threads
- Label/folder navigation
- Mobile-responsive layout
- Webhook notifications (Slack, Discord)
- Team sharing / shared inbox view
- SSO (Google OAuth, SAML)
- Usage analytics dashboard for admins

## Tech Stack

- Java 21, Spring Boot 3.x
- PostgreSQL 16 (production), H2 (tests)
- Thymeleaf (server-rendered views)
- Maven (with wrapper)
- Flyway migrations
- JUnit 5 + Testcontainers

## Revenue Relevance

- Recurring subscription revenue (MRR)
- Upgrade paths: free → personal → team are natural as usage grows
- Low churn expected: deep mailbox integration = high switching cost
- Potential for B2B / team plan upsells

## Deployment Target

- Docker + Docker Compose (app + postgres)
- Heroku / Render / Railway for hosted SaaS
- GitHub Actions CI/CD

_Last synced: 2026-04-28 (Run #18 — verified against codebase: landing/pricing/demo/waitlist/privacy/terms/refund pages live; IMAP polling + Testcontainers + GreenMail integration tests in place; security headers filter active; gzip on; auth + Stripe + AI summary still planned)_
