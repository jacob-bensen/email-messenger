# BACKLOG

Up to 10 items, each advancing a PLAN.md milestone. Pick from the top.

- **Account linking via `users.google_subject`.**
  Flyway V18 adds the column; provisioner writes it on first OAuth
  login; existing email-password rows match by email and gain the link.
  PLAN milestone 3.

- **Hide /password/forgot for Google-only users + helpful nudges.**
  Provisioned-via-Google flag drives a "Sign in with Google" hint on
  `/login?error` and on `/password/forgot` when the email is recognised
  as Google-linked. PLAN milestone 4.
