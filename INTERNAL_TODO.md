# Internal TODO (ordered by severity)

## Contract breakage
- No `/api/v1` exists and no bearer-token auth (session+CSRF only); mobile client cannot talk to the server. Finalize docs/API.md v1, add token auth in `SecurityConfig` (separate stateless filter chain for `/api/**`, CSRF off there).

## Logic drift risk
- `ConversationController.send` / `compose` hold send orchestration inline (recipients, signature append, asReply vs new thread, outbound recording). Extract to a service before adding the JSON endpoint so web and API share it.
- `ConversationController.populateList` builds list model in the controller; extract a list+counts service call.

## Missing tests
- No API tests (none exist). Add MockMvc slice tests with the new API.

## Style / docs
- CLAUDE.md says Spring Boot/JPA; sync routine assumes jOOQ — it is JPA here (no codegen step).
- Attachments are not downloadable (`blob_ref` null) — mobile needs this for files (see CLAUDE.md roadmap).
