# Deploy

One-page guide for taking the `claude_routine` build to a public URL.

## Prereqs

- A host with Docker + Docker Compose v2 (Render, Fly Machines, Railway,
  a $5 VPS — anywhere you can run a container).
- A public DNS name pointing at the host (e.g. `mailaim.app`).
- A Postgres 16 database. The bundled `docker-compose.yml` ships one for
  small deploys; for anything serious use a managed Postgres
  (Supabase / Neon / RDS).
- Stripe account with the Pro monthly + annual prices created
  and a webhook configured at `https://<host>/billing/webhook`.
  (Business is sales-assisted — no Stripe price is needed.)

## 1. Pull the image

CI publishes `ghcr.io/jacob-bensen/email-messenger` on every push to
`claude_routine`. Tags:

- `latest` — head of `claude_routine`
- `claude_routine` — same, branch-named
- `sha-<short>` — pin a specific commit

The package is public; `docker pull` needs no auth.

```bash
docker pull ghcr.io/jacob-bensen/email-messenger:latest
```

## 2. Configure environment

Copy this into a `.env` next to `docker-compose.yml` (or set them in your
hosting platform's dashboard). Anything left blank degrades gracefully —
**except** the DB creds and the mailbox encryption pair, which are
required for any non-trivial deploy.

```env
# Postgres
POSTGRES_USER=mailim
POSTGRES_PASSWORD=<rand>

# Mailbox credential encryption — REQUIRED in prod.
# openssl rand -base64 32   # password
# openssl rand -hex 8       # salt
MAILBOX_ENCRYPTION_PASSWORD=
MAILBOX_ENCRYPTION_SALT=

# SMTP for outbound replies
MAIL_HOST=smtp.postmarkapp.com
MAIL_PORT=587
MAIL_USER=
MAIL_PASS=

# Stripe — live or test keys
STRIPE_SECRET_KEY=sk_live_...
STRIPE_WEBHOOK_SECRET=whsec_...
STRIPE_PRO_PRICE_ID=price_...
STRIPE_PRO_ANNUAL_PRICE_ID=price_...
BILLING_SUCCESS_URL=https://<host>/billing/success?session_id={CHECKOUT_SESSION_ID}
BILLING_CANCEL_URL=https://<host>/billing/cancel
BILLING_PORTAL_RETURN_URL=https://<host>/threads

# Background IMAP polling
MAILBOX_POLLING_ENABLED=true
```

## 3. Run the stack

For the bundled Postgres setup:

```bash
docker compose up -d
```

Flyway applies `V1..V7` against Postgres on first boot; the logs show
`Successfully applied 7 migrations`. The app listens on `:8080`.

To run just the app against an external Postgres:

```bash
docker run -d --name email-messenger \
  --env-file .env \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e DB_URL=jdbc:postgresql://<db-host>:5432/email_messenger \
  -e DB_USER=$POSTGRES_USER -e DB_PASS=$POSTGRES_PASSWORD \
  -p 8080:8080 \
  ghcr.io/jacob-bensen/email-messenger:latest
```

## 4. HTTPS terminator

Put any TLS-terminating reverse proxy in front of `:8080`. Minimal
Caddyfile:

```
mailaim.app {
  reverse_proxy 127.0.0.1:8080
}
```

`caddy run` (or `caddy reload`) gets you Let's Encrypt automatically. On
Render / Fly / Railway the platform terminates TLS for you and you point
their HTTP service at the container's port; configure their healthcheck
path to `/actuator/health` so traffic isn't routed before Postgres +
Flyway are ready.

## 5. Verify

The image's Docker `HEALTHCHECK` and the compose stack both poll
`/actuator/health` every 30s after a 60s start-period. To smoke-check
manually:

```bash
curl -sSf https://<host>/actuator/health
# {"status":"UP"}

curl -sSf https://<host>/pricing | grep -o '<title>.*</title>'
```

A 200 from `/actuator/health` confirms Flyway ran and the DataSource is
reachable; a 200 from `/pricing` confirms the JVM came up and the
Thymeleaf engine renders. Then walk the live path: register → connect a
mailbox → see imported threads in `/threads` → Upgrade → land back from
Stripe Checkout.

## 6. Configure Stripe webhook

After the host is public:

1. Stripe Dashboard → Developers → Webhooks → Add endpoint
   `https://<host>/billing/webhook`.
2. Subscribe to `checkout.session.completed`,
   `customer.subscription.updated`, `customer.subscription.deleted`,
   `invoice.payment_failed`.
3. Copy the signing secret into `STRIPE_WEBHOOK_SECRET` and restart the
   app.

## Upgrades

```bash
docker compose pull && docker compose up -d
```

Flyway runs any new migrations on boot; rolling restart is safe because
`spring.jpa.hibernate.ddl-auto=validate` will fail-fast if schema and
entities drift.
