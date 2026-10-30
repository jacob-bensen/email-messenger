# API contract

Canonical contract between the backend and the mobile client
(`conexus-mail-mobile`). Last audited 2026-10-30.

> **Status: no mobile REST API exists yet.** The server is a server-rendered
> web app with cookie sessions. §1 documents what is actually reachable
> programmatically today; §2 is the *planned* `/api/v1` surface, which is
> **not implemented** and must be finalized here before any code is written.

Conventions (current): UTC; timestamps in view models are epoch millis.

## 1. Existing non-HTML endpoints (implemented)

| Method | Path | Auth | Request | Response |
|---|---|---|---|---|
| POST | `/chats/refresh` | session + CSRF | none | `200 {"imported": <int>}` — synchronously polls the user's mailboxes |
| GET | `/chats/events` | session | none | `text/event-stream`. Event `new-mail`, data `{"imported": <int>}`; `: ping` comment every 25 s; emitter times out at 30 min (client must reconnect) |
| POST | `/billing/webhook` | public, Stripe signature header | raw Stripe JSON | 200 on handled; invalid signature rejected (`InvalidStripeSignatureException`); CSRF-exempt. **Server-to-server only — not for the client.** |
| GET | `/actuator/health` | public | — | Spring Boot health |

Errors today: `NoSuchElementException` → 404 (HTML error page); `MailException`
/ `EmailImportException` / `BillingException` → 502 (HTML error page); upload
over limit → 413 (10 MB/file, 25 MB/request). No JSON error envelope.

## 2. Planned `/api/v1` (TBD — not implemented)

Proposed order, each needing a service-layer entry point shared with the web
controllers:

1. Token auth (`POST /api/v1/auth/login` → bearer token; refresh/revoke).
2. `GET /api/v1/chats` (backed by `ConversationListService.list`, record `ConversationListItem`).
3. `GET /api/v1/chats/{key}` (backed by `ChatService.buildFor`, record `ChatConversation`).
4. `POST /api/v1/chats/{key}/messages`, `POST /api/v1/chats` (new conversation).
5. Mailbox list/connect/sync, billing/plan status.

Existing view-model shapes to reuse (Java records, `com.emailmessenger.service`):

- `ConversationListItem {key, title, initials, group, memberCount, preview, lastActivity(+epochMilli), unread, threadCount, accounts[], senderEmail}`
- `ChatConversation {key, title, initials, members[ChatMember{email, displayName, initials, label, signatureHtml}], threadCount, runs[SenderBubbleRun]}`
- `BubbleMessage {messageId, bodyHtml, sentAt(+epochMilli), attachments[], outbound, replyToMessageId}`

Open decisions: `bodyHtml` is pre-sanitized HTML (a native client needs a
renderer or a plain-text field); attachments have no download endpoint.
