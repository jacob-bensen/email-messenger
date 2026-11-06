# INTERNAL_TODO (ordered by severity)

## Contract breakage
1. No JSON API / token auth for the mobile client (`docs/API.md` has only a proposal). Blocks all mobile work.
2. Routine assumed jOOQ; repo uses JPA. No codegen step needed. Routine assumed `master`; this session is
   restricted to branches `claude/gifted-carson-ouzffi` (backend) and `claude/trusting-cori-ouzffi` (mobile) — pushed there.

## Logic drift
3. Chat logic lives behind HTML-model controllers (`ConversationController` builds model attributes);
   extract a reusable query/DTO layer before adding `/api/v1` so web and API share one path.
4. Attachments are not downloadable (`blob_ref` always null) — client cannot show files (see CLAUDE.md roadmap).

## Missing tests / tooling
5. Quality gate not run this session: `./mvnw` needs network for Maven deps (see summary).
6. Mobile repo has no Flutter project (`pubspec.yaml` absent); `flutter create .` required; flutter/dart not installed here.

## Style / docs
7. `master/` directory holds legacy BACKLOG/CHANGELOG/PLAN; new routine entries go to root `CHANGELOG.md`.
