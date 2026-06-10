# BACKLOG

Up to 10 items, each advancing a PLAN.md milestone. Pick from the top.

- **Lift thread access to team-scope so invited teammates see notes.**
  Replace `findByIdAndOwner` in `ThreadController.viewConversation`
  with a team-scoped check (owner-or-fellow-member-of-owner's-team);
  `ThreadNoteService.notesFor` widens the same way so a teammate
  following a shared link sees and contributes to notes.
  PLAN milestone 2.

- **@mention picker + email notification on a note.**
  Parse `@token` against team members on save; matched users get a
  transactional email pointing at `/threads/{id}#note-{noteId}`.
  Textarea opens a `@` picker bound to the team membership.
  PLAN milestone 3.
