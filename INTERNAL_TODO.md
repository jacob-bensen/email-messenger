# Internal TODO (backend)

Ordered by severity. Last audit: 2026-11-04.

## Contract breakage
- No `/api/v1` REST API and no token auth exist; the Flutter client has nothing to call. Define in `docs/API.md` first, then implement.
- CSRF is enforced on every POST outside `/billing/webhook`; the API path must be exempted or use bearer auth.

## Logic drift / architecture
- `ConversationController` embeds business orchestration (mailbox scoping, signature append, new-vs-reply send, attachment conversion, recipient resolution). Extract into a service (e.g. `ChatSendService`) before the API reuses it.
- Persistence is JPA, not jOOQ as the generic routine assumes; no codegen needed.

## Missing tests / verification
- Live IMAP IDLE unverified against a real server (see CLAUDE.md roadmap).

## Roadmap items from CLAUDE.md still open
Support email; Google OAuth consent publish; Web Push; outbound delivery states; signature contact card; shared files (attachments not downloadable, `blob_ref` null); reply-to-specific-bubble.
