# Self-Hosting RelayFlow

This is the production deployment guide for running RelayFlow on your own infrastructure
(BYOC — Bring Your Own Cloud). For local development, see `infra/docker/README.md` instead.

## Prerequisites

- Docker Engine + Docker Compose v2.
- A domain name with two DNS `A` records pointed at the host: one for the web app, one for the
  API (e.g. `app.example.com` and `api.example.com`). The bundled reverse proxy (Caddy) needs
  these to provision TLS certificates automatically.
- Ports 80 and 443 open on the host.
- A RelayFlow license key (see [Licensing](#licensing) below).
- A GitHub account with access to RelayFlow's private image packages (granted after purchase) and
  a [Personal Access Token](https://github.com/settings/tokens) with `read:packages` scope — the
  images can't be pulled without authenticating first.

## Quick start

```bash
cp .env.example .env
```

Generate the two secrets that have no safe default:

```bash
openssl rand -base64 24   # → POSTGRES_PASSWORD
openssl rand -base64 32   # → RELAYFLOW_ENCRYPTION_KEY
```

Fill in `.env`: the two generated secrets, your domains (`RELAYFLOW_WEB_DOMAIN`,
`RELAYFLOW_API_DOMAIN`, and the matching `RELAYFLOW_WEB_BASE_URL`/`RELAYFLOW_API_BASE_URL`), and
your license key (`RELAYFLOW_LICENSE_KEY`).

The images are in a private registry, so authenticate before pulling — use the Personal Access
Token from [Prerequisites](#prerequisites), not your GitHub password:

```bash
docker login ghcr.io -u <your-github-username>
```

Then start the stack:

```bash
docker compose --profile prod up -d
```

## Required vs optional env vars

| Variable | Required? | Notes |
|---|---|---|
| `POSTGRES_PASSWORD` | Yes | No default — generate with `openssl rand -base64 24` |
| `RELAYFLOW_ENCRYPTION_KEY` | Yes | No default — generate with `openssl rand -base64 32`. API refuses to start without it. |
| `RELAYFLOW_LICENSE_KEY` | Yes | Issued at purchase — see [Licensing](#licensing) |
| `RELAYFLOW_WEB_DOMAIN` / `RELAYFLOW_API_DOMAIN` | Yes | Used by Caddy for automatic TLS |
| `RELAYFLOW_WEB_BASE_URL` / `RELAYFLOW_API_BASE_URL` | Yes | Must match the domains above exactly — see [Troubleshooting](#troubleshooting) |
| `GOOGLE_AUTH_ENABLED` / `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` | No | Off by default |
| `ANTHROPIC_API_KEY` / `OPENAI_API_KEY` / `GROQ_API_KEY` | No | At least one needed for AI agent features |
| `SMTP_HOST` / `SMTP_PORT` / `SMTP_USERNAME` / `SMTP_PASSWORD` / `SMTP_FROM` | No | Works with any SMTP provider (Resend, SES, Mailgun, Postmark, Gmail, self-hosted, etc.). Blank host runs email in no-op/log mode |
| Rate limiting, timeouts, retention vars | No | All have safe defaults — see `.env.example` |

## First-run walkthrough

1. Confirm the one-shot `migrate` service applied all migrations and exited cleanly:
   ```bash
   docker compose --profile prod logs migrate
   ```
2. Confirm every service reports healthy:
   ```bash
   docker compose --profile prod ps
   ```
3. Visit `https://<RELAYFLOW_WEB_DOMAIN>` and complete the first admin signup.

## Licensing

RelayFlow's self-hosted image requires a signed license key (`RELAYFLOW_LICENSE_KEY`), checked
at API startup. Verification happens **entirely offline** — the API never calls out to
RelayFlow's servers to check your license, so your deployment has no runtime dependency on them
being reachable.

- **Obtaining a key**: purchase a self-hosted plan through the marketing site; a key is issued to
  you directly.
- **Expiry**: an expired key is tolerated for a 14-day grace period (the API logs a warning but
  keeps running) before startup fails — a late renewal won't cause an outage.
- **Missing or invalid key**: the API refuses to start, with a log message pointing back here.

## Backups

Back up the Postgres volume regularly:

```bash
docker compose --profile prod exec postgres \
  pg_dump -U relayflow relayflow > backup-$(date +%F).sql
```

Restore into a fresh instance with `psql -U relayflow relayflow < backup.sql`, or snapshot the
`postgres_data` named volume directly at the infrastructure level.

## Upgrades

1. Bump `RELAYFLOW_VERSION` in `.env` to the new release tag.
2. Re-run migrations before restarting the app:
   ```bash
   docker compose --profile prod run --rm migrate
   ```
3. Pull and restart:
   ```bash
   docker compose --profile prod pull
   docker compose --profile prod up -d
   ```

Migrations are forward-only — there are no down-migrations, so plan upgrades accordingly.

## Using your own reverse proxy

If you already run nginx, Traefik, or similar, skip the `caddy` service by naming the other
services explicitly instead of using `--profile prod`:

```bash
docker compose up -d postgres redis migrate api web
```

and point your existing proxy at `web:3000` and `api:8080` on the compose network instead.

## Troubleshooting

- **Browser console shows CORS errors**: `RELAYFLOW_WEB_BASE_URL` must exactly match the public
  origin the browser is loading the web app from (scheme + host, no trailing slash) — the API
  uses it as the allowed CORS origin.
- **AI agent features return errors or do nothing**: no LLM provider key is configured — set at
  least one of `ANTHROPIC_API_KEY`, `OPENAI_API_KEY`, or `GROQ_API_KEY`.
- **API container exits immediately on a fresh install**: check `docker compose --profile prod
  logs api` — this is almost always a missing `RELAYFLOW_ENCRYPTION_KEY`
  or an invalid/missing license key, both of which fail fast with a clear message.
