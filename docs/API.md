# API — ConexusMail REST contract

This is the canonical, cross-repo contract between the backend
(`email-messenger`) and the mobile client (`conexus-mail-mobile`).

> **Status: bootstrap.** Almost none of the surface below is implemented
> yet. The backend today serves Thymeleaf pages plus one JSON endpoint
> (`POST /chats/refresh`) and one SSE stream (`GET /chats/events`); see
> "What exists today" at the end. Everything in the `/api/v1/**` section
> is the target, added here so both repos plan against the same shape.
> Do not code against unimplemented endpoints without also implementing
> them in the same run.

## Conventions

- Base path: `/api/v1`. Any breaking change bumps to `/api/v2`.
- Request/response bodies: JSON, UTF-8. Field names are `camelCase`.
- Timestamps: ISO-8601 in UTC, e.g. `2026-08-29T14:03:22Z`.
- IDs: 64-bit integers (server-assigned).
- Error envelope:
  ```json
  { "error": { "code": "invalid_credentials", "message": "…" } }
  ```
- Auth: cookie session (`JSESSIONID`) issued by the form-login POST.
  A JSON bearer-token / refresh-token endpoint is planned — see
  `docs/DESIGN.md §ROADMAP` in the mobile repo.
- CSRF: the form-login POST requires the standard Spring Security CSRF
  token. `/api/v1/**` endpoints that mutate state will be exempted and
  authenticated purely by session cookie (planned).

## Endpoints (target)

### Session

#### `POST /api/v1/auth/login`
```json
Request:  { "email": "ada@example.com", "password": "…" }
Response: { "user": { "id": 42, "email": "…", "displayName": "…" } }
Set-Cookie: JSESSIONID=…; HttpOnly; Secure; SameSite=Lax
Errors:   401 invalid_credentials, 429 throttled
```

#### `POST /api/v1/auth/logout`
```
Response: 204 No Content
```

#### `GET /api/v1/me`
```json
Response: {
  "id": 42, "email": "…", "displayName": "…",
  "plan": "PRO", "emailVerifiedAt": "2026-08-01T09:00:00Z"
}
```

### Chats (conversation-key rail)

#### `GET /api/v1/chats?mailbox=&filter=&q=&page=&size=`
```json
Response: {
  "page": 0, "size": 25, "total": 137,
  "chats": [
    { "key": "abc123…", "subject": "Re: launch",
      "participants": [ { "email": "…", "displayName": "…" } ],
      "lastMessagePreview": "sounds good, will do",
      "lastMessageAt": "2026-08-29T13:12:00Z",
      "unread": true, "messageCount": 8 }
  ]
}
```

#### `GET /api/v1/chats/{key}?mailbox=`
```json
Response: {
  "key": "…", "subject": "…",
  "participants": [ … ],
  "messages": [
    { "id": 991, "sender": { "email": "…", "displayName": "…" },
      "sentAt": "…", "outbound": false,
      "bodyPlain": "…", "bodyHtml": "…",
      "messageIdHeader": "<…@mail>", "inReplyTo": "<…>",
      "attachments": [ { "id": 5, "filename": "spec.pdf",
                         "mimeType": "application/pdf", "sizeBytes": 12345 } ]
    }
  ],
  "unread": false, "updatedAt": "…"
}
```

#### `POST /api/v1/chats/{key}/messages`
```json
Request:  { "body": "…", "subject": "…", "asReply": true, "mailbox": "…" }
Response: 201 { "message": { …Message… } }
Errors:   403 mailbox_not_owned, 422 empty_body
```

#### `POST /api/v1/chats/{key}/mark-read`
```
Response: 204
```

#### `POST /api/v1/chats/refresh`
Force-poll all connected mailboxes for the current user.
```json
Response: { "imported": 3 }
```
*(This is the one endpoint that already exists in JSON form today, at
`POST /chats/refresh`. When it moves under `/api/v1`, the current path
stays for the Thymeleaf pages until they migrate.)*

#### `GET /api/v1/chats/events` — SSE
Same wire format as today's `/chats/events`. Events:
```
event: new-mail
data: { "imported": 2 }

event: keep-alive
data: {}
```

### Mailboxes

#### `GET /api/v1/mailboxes`
```json
Response: {
  "mailboxes": [
    { "id": 12, "email": "ada@gmail.com",
      "provider": "GMAIL", "authType": "XOAUTH2",
      "connected": true, "lastSyncedAt": "…",
      "lastSyncError": null, "signature": "— Ada" }
  ]
}
```

#### `POST /api/v1/mailboxes` (IMAP + password)
```json
Request:  { "host": "…", "port": 993, "ssl": true,
            "username": "…", "password": "…", "provider": "IMAP" }
Response: 201 { "mailbox": { … } }
```

#### `POST /api/v1/mailboxes/{id}/sync` — force poll
#### `POST /api/v1/mailboxes/{id}/signature` — `{ "signature": "…" }`
#### `DELETE /api/v1/mailboxes/{id}`

Gmail OAuth stays browser-driven (`/mailboxes/gmail/connect`); the app
should open it in a system webview.

### Account

#### `GET /api/v1/account`
```json
{ "email": "…", "displayName": "…",
  "syncFrequencyMinutes": 5, "defaultMailAccountId": 12,
  "plan": "PRO", "billingPeriod": "MONTHLY",
  "trialEndsAt": null, "emailVerifiedAt": "…" }
```

#### `POST /api/v1/account/password` — `{ currentPassword, newPassword }`
#### `POST /api/v1/account/email` — `{ currentPassword, newEmail }`
#### `POST /api/v1/account/appearance` — `{ theme, accent }`

### Saved searches

#### `GET /api/v1/searches`
#### `POST /api/v1/searches` — `{ name, query, senderEmail?, sincePreset?, requireUnread?, requireAttachments? }`
#### `DELETE /api/v1/searches/{id}`

### Teams

#### `GET /api/v1/team`
#### `POST /api/v1/team/invites` — `{ "email": "…" }`
#### `POST /api/v1/team/invites/accept` — `{ "token": "…" }`

### Billing

Browser-driven (Stripe Checkout / Portal). The mobile app should open
the checkout URL in a system webview and read subscription state back
through `GET /api/v1/me`.

## Entities

Field names for the JSON shapes above are the same as the JPA entities
in `com.emailmessenger.domain.*`. `snake_case` DB columns become
`camelCase` fields.

| Entity | Included in JSON |
|---|---|
| `User` | id, email, displayName, plan, emailVerifiedAt |
| `MailAccount` | id, email (from `username`), provider, authType, connected (derived), lastSyncedAt, lastSyncError, signature |
| `EmailThread` | subsumed by chat responses (`key` = `conversationKey`) |
| `Message` | id, sender, sentAt, outbound, subject, bodyPlain, bodyHtml, messageIdHeader, inReplyTo, attachments |
| `Participant` | email, displayName |
| `Attachment` | id, filename, mimeType, sizeBytes (no `blobRef` yet — no download endpoint) |
| `SavedSearch` | id, name, query, senderEmail, sincePreset, requireUnread, requireAttachments, lastViewedAt |
| `Team`, `TeamMember`, `TeamInvite` | as needed by team endpoints |
| `Subscription` | plan, status, billingPeriod, trialEndsAt, currentPeriodEnd |

Enums are serialized as their `name()` string (e.g. `"PRO"`,
`"XOAUTH2"`, `"TO"`).

## What exists today (2026-08-29)

Every one of these is a Thymeleaf-page controller unless flagged JSON.
Full endpoint-level catalog including auth model and file:line
references is in the exploration output archived under
`master/CHANGELOG.md` (Phase 0 run, 2026-08-29).

- **JSON now:** `POST /chats/refresh` → `{ "imported": int }`.
- **SSE now:** `GET /chats/events` → `event: new-mail` / `keep-alive`.
- **Webhook (RestController):** `POST /billing/webhook` (Stripe).
- **Everything else** returns HTML views or `redirect:` strings.

Auth is cookie-session with CSRF; there is no token endpoint. Anything
under "target" above needs to be built. Track the gap in
`INTERNAL_TODO.md`.
