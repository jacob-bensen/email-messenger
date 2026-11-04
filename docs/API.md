# API.md — canonical API contract

> **Status 2026-11-04: no mobile REST API exists.** The backend is a server-rendered Thymeleaf app with
> session-cookie auth and CSRF. This file documents (A) the machine-readable endpoints that exist today and
> (B) the planned `/api/v1` surface. Section B is a **proposal, not implemented**; it becomes binding the run
> that implements it (update this file first, then both repos).

## A. Existing non-HTML endpoints
| Method/path | Auth | Request | Response | Errors |
|---|---|---|---|---|
| `POST /chats/refresh` | session + CSRF | none | `200 {"imported": int}` | 302→/login if unauthenticated; 403 bad CSRF |
| `GET /chats/events` | session | — | `text/event-stream`; events published from `NewMailEvent` (event names/payload **TBD** — read `ChatEventStream`) | 302→/login |
| `POST /billing/webhook` | Stripe signature header | raw Stripe event JSON | 200 | 400 invalid signature |
| `GET /actuator/health` | public | — | Spring health JSON | — |
| `GET /manifest.webmanifest` | public | — | PWA manifest | — |

## B. Planned `/api/v1` (proposal — NOT IMPLEMENTED)
Auth: bearer access token (short-lived) + refresh token; no cookies, no CSRF. Errors: `{ "error": code, "message": str }`
with 400/401/403/404/409/422 (plan-limit → 403 `plan_limit_exceeded` with `kind`).

| Endpoint | Purpose | Backing service |
|---|---|---|
| `POST /api/v1/auth/login` `{email,password}` → `{accessToken,refreshToken,expiresIn}` | sign-in | UserService / DbUserDetailsService |
| `POST /api/v1/auth/refresh` | rotate tokens | new |
| `GET /api/v1/chats?mailbox&filter&q&page` → page of `ConversationListItem` | chat list | ConversationListService |
| `GET /api/v1/chats/{key}?mailbox` → `ChatConversation` (marks read) | open chat | ChatService |
| `POST /api/v1/chats/{key}/messages` `{body, subject?, asReply?}` | send | OutboundMessageService |
| `GET /api/v1/mailboxes` | list accounts | MailAccountService |

DTO source of truth (Java records): `ConversationListItem` (key, title, initials, group, memberCount, preview,
lastActivity, unread, threadCount, accounts, senderEmail), `ChatConversation` (key, title, initials, members,
threadCount, runs[BubbleRun→BubbleMessage{messageId, bodyHtml, sentAt, attachments, outbound, replyToMessageId}]).
