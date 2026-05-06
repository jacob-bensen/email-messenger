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

## Core Features

### Built (current state)

1. **Thread view** — email threads rendered as IM-style chat bubbles
2. **Quoted-reply stripping** — removes `> ` and "On … wrote:" noise via `IMTransformService`
3. **Same-sender grouping** — consecutive messages from the same person collapsed into bubble runs
4. **Reply inline** — compose and send replies from the conversation view (validated, sanitized)
5. **Participant initials avatar** — `Participant.initials()` helper drives a circle avatar fallback
6. **Dark mode** — CSS `prefers-color-scheme: dark` with full palette override
7. **Day separators** — JS-inserted "Today" / "Yesterday" / dated separators between bubble runs
8. **Keyboard shortcuts** — `j`/`k`/`Enter` thread navigation, `r`/`Esc` reply focus
9. **Email import (programmatic)** — `EmailImportService` parses RFC 822 via Jakarta Mail and
   threads via `Message-ID` / `In-Reply-To` / `References`
10. **HTML sanitization** — jsoup `Safelist.relaxed()` prevents XSS in HTML email bodies
11. **Validation + global error pages** — `ReplyForm` `@Valid`, `GlobalExceptionHandler` 404/502/500

### Planned (not yet built)

- **IMAP polling** — automatic mailbox sync via a `@Scheduled` job (only programmatic import works)
- **User authentication** — Spring Security login (prerequisite for billing & multi-tenancy)
- **Stripe billing** — subscription plans, checkout flow, webhook handler
- **Gravatar avatars** — currently only initials are rendered
- **Pricing / landing page** — `/` redirects straight to `/threads` (no marketing surface yet)

## Other Planned Features (Post-MVP)

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

_Last synced: 2026-05-06_
