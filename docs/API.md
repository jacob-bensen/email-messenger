# API.md — canonical API contract

Status (2026-10-18): **no JSON API exists.** The mobile client (`conexus-mail-mobile`) has no implementation either.
This file is the place the contract must be defined *before* code is written (routine step "contract first").

## Existing non-HTML endpoints
| Method | Path | Auth | Notes |
|---|---|---|---|
| GET | `/chats/events` | session | SSE stream of new-mail events (`ChatEventStream`) |
| POST | `/billing/webhook` | Stripe signature, CSRF-exempt | Stripe events |
| GET | `/actuator/health` | public | health |

## Proposed `/api/v1` (TBD — not implemented)
Prerequisite decision (see INTERNAL_TODO): token auth for mobile (e.g. `POST /api/v1/auth/login` → bearer token;
Google sign-in via native ID-token exchange). Error envelope: `{ "error": { "code": string, "message": string } }`.
Candidate first slice: `GET /api/v1/conversations`, `GET /api/v1/conversations/{key}`, `POST /api/v1/conversations/{key}/send`,
mirroring `ChatService` / `ReplyService`. Shapes should derive from the records `ConversationListItem`, `ChatConversation`, `BubbleRun`.
