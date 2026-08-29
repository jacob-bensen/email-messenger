# INTERNAL_TODO — email-messenger

Running list of known gaps, tech debt, and deferred work. Consume from
and append to this every run. Order by severity: contract breakage >
logic drift > missing tests > style.

The product roadmap for web-only features lives in `CLAUDE.md`
§Roadmap; this file is for cross-cutting gaps, especially the ones
touching the mobile sibling repo `conexus-mail-mobile`.

## Contract-level

- [ ] **DECISION NEEDED — mobile client strategy.** The sibling repo
      `conexus-mail-mobile` exists (Flutter, per its `.gitignore`) but
      is empty. The backend today has no REST API, no bearer-token
      auth, and no CORS allow-list — every controller is Thymeleaf +
      session cookie. Before a mobile client can do anything real, a
      human needs to answer:
      1. Is mobile a first-class client, or a thin webview / PWA
         wrapper? Today "Live delivery, part 2" in the backend
         `CLAUDE.md` roadmap only mentions Web Push through the PWA
         service worker — so the working assumption has been PWA.
      2. If first-class: which auth model — OAuth2 password grant,
         Google-only OIDC first-party token exchange, or personal
         tokens? Add the answer to `docs/BACKEND.md` §Auth model and
         `docs/API.md` §Contract conventions.
      3. Which endpoint set is the MVP for the mobile app? Log in +
         list conversations + open one + send a reply is the minimum;
         everything else can come later.
      Until this is decided, `docs/API.md` stays empty and no
      `@RestController` classes get added to the backend. Recording
      the strategic gap here is the right move — do not silently
      invent a REST surface on autopilot.

## Documentation

- [ ] `docs/BACKEND.md` §"Data model (schema)" is a hand-written list of
      entity classes. Regenerate it from an actual schema dump the next
      time Flyway migrations change substantially, so it can't drift.
- [ ] `docs/WEB.md` was reverse-engineered by grep on 2026-08-29 — if
      new controllers land, add them here in the same commit.

## Testing

- [ ] IMAP IDLE not yet verified end-to-end against a real server
      (already tracked in `CLAUDE.md` §Roadmap for "Live delivery,
      part 1"). Keep the note here so a mobile-side test plan can
      reference it once a mobile client exists.

## History

- 2026-08-29 — Phase 0 bootstrap: this file created; `docs/BACKEND.md`,
  `docs/API.md`, `docs/WEB.md` and `CHANGELOG.md` created; CLAUDE.md
  §"Related repositories" added.
