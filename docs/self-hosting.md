# Self-Hosting RelayFlow

This is the production deployment guide for running RelayFlow on your own infrastructure
(BYOC — Bring Your Own Cloud). For local development, see `infra/docker/README.md` instead.

## Prerequisites

- Docker Engine + Docker Compose v2.
- A domain name with two DNS `A` records pointed at the host: one for the web app, one for the
  API (e.g. `app.example.com` and `api.example.com`). The bundled reverse proxy (Caddy) needs
  these to provision TLS certificates automatically.
- Ports 80 and 443 open on the host.

## Quick start

```bash
cp .env.example .env
```

Generate the two secrets that have no safe default:

```bash
openssl rand -base64 24   # → POSTGRES_PASSWORD
openssl rand -base64 32   # → RELAYFLOW_ENCRYPTION_KEY
```

Fill in `.env`: the two generated secrets and your domains (`RELAYFLOW_WEB_DOMAIN`,
`RELAYFLOW_API_DOMAIN`, and the matching `RELAYFLOW_WEB_BASE_URL`/`RELAYFLOW_API_BASE_URL`).

Then start the stack:

```bash
docker compose --profile prod up -d
```

## Required vs optional env vars

| Variable | Required? | Notes |
|---|---|---|
| `POSTGRES_PASSWORD` | Yes | No default — generate with `openssl rand -base64 24` |
| `RELAYFLOW_ENCRYPTION_KEY` | Yes | No default — generate with `openssl rand -base64 32`. API refuses to start without it. |
| `RELAYFLOW_WEB_DOMAIN` / `RELAYFLOW_API_DOMAIN` | Yes | Used by Caddy for automatic TLS |
| `RELAYFLOW_WEB_BASE_URL` / `RELAYFLOW_API_BASE_URL` | Yes | Must match the domains above exactly — see [Troubleshooting](#troubleshooting) |
| `GOOGLE_AUTH_ENABLED` / `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` | No | Off by default. In Google Cloud, add the redirect URI `<RELAYFLOW_WEB_BASE_URL>/backend/login/oauth2/code/google` |
| `ANTHROPIC_API_KEY` / `OPENAI_API_KEY` / `GROQ_API_KEY` | No | At least one needed for AI agent features |
| `SMTP_HOST` / `SMTP_PORT` / `SMTP_USERNAME` / `SMTP_PASSWORD` / `SMTP_FROM` | No | Works with any SMTP provider (Resend, SES, Mailgun, Postmark, Gmail, self-hosted, etc.). Blank host runs email in no-op/log mode |
| `LOG_FORMAT` / `LOG_LEVEL` | No | `json` (default in the prod stack), `ecs`, `gelf`, `logfmt` or `text`; `INFO` by default — see [Logging](#logging) |
| `JAVA_OPTS` | No | Extra JVM flags for the API. Defaults to `-XX:MaxRAMPercentage=75`, so the heap can use 75% of the API container's 1 GB memory limit; raise the limit in `docker-compose.yml` for heavier workloads |
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

## Logging

The API writes logs to stdout, one event per line, in the format set by `LOG_FORMAT` in `.env`:

| `LOG_FORMAT` | Output | Use with |
|---|---|---|
| `json` (default in the prod stack) | JSON with Datadog field names | Datadog, Fluent Bit, Vector, Splunk, any JSON-reading agent |
| `ecs` | JSON in [Elastic Common Schema](https://www.elastic.co/guide/en/ecs/current/index.html) | Elasticsearch / Kibana, Elastic Agent, Filebeat |
| `gelf` | JSON in [GELF 1.1](https://go2docs.graylog.org/current/getting_in_log_data/gelf.html) | Graylog |
| `logfmt` | `key=value` pairs | Grafana Loki, Heroku-style tooling |
| `text` | Human-readable lines | Reading with `docker compose --profile prod logs api` |

Apply a change with `docker compose --profile prod up -d api`. The API refuses to start if
`LOG_FORMAT` is not one of these values. A `json` line looks like this:

```json
{"timestamp":"2026-10-18T09:12:44.103Z","message":"Channel account created workspaceId=… channelAccountId=… provider=TELEGRAM","logger.name":"io.odunlamizo.relayflow.channel.ChannelAccountService","logger.thread_name":"http-nio-8080-exec-3","level":"INFO","requestId":"5f0c…","workspaceId":"…","channelAccountId":"…","provider":"TELEGRAM"}
```

- Every key/value in a message is also a top-level field (`workspaceId`, `conversationId`,
  `workflowRunId`, `channelAccountId`, …), so you can filter and facet on it.
- `requestId` ties together every line written while handling one HTTP request, including
  asynchronous work it starts. The API reuses an incoming `X-Request-Id` header and returns the
  ID in the `X-Request-Id` response header.
- Exceptions appear as `error.kind`, `error.message` and `error.stack` (`error.type`,
  `error.message`, `error.stack_trace` in `ecs`; `full_message`, `_error_kind`, `_error_message`
  in `gelf`).
- In `gelf`, every additional field is prefixed with `_` (`_requestId`, `_workspaceId`, …), as
  GELF requires. In `logfmt` and `gelf`, the message omits the key/value pairs, which appear only
  as fields.
- At `INFO` and above, logs carry IDs only. `LOG_LEVEL=DEBUG` adds personal data such as email
  addresses.

### Datadog

1. Run the Datadog Agent on the Docker host with container log collection enabled
   (`DD_LOGS_ENABLED=true`, `DD_LOGS_CONFIG_CONTAINER_COLLECT_ALL=true`).
2. Label the `api` service so the Agent tags its logs:

   ```yaml
   api:
     labels:
       com.datadoghq.ad.logs: '[{"source": "java", "service": "relayflow-api"}]'
   ```

3. For trace correlation, attach the Datadog Java tracer and turn on log injection. The tracer
   adds `dd.trace_id` and `dd.span_id` to each log line:

   ```yaml
   api:
     environment:
       JAVA_TOOL_OPTIONS: -javaagent:/dd/dd-java-agent.jar
       DD_LOGS_INJECTION: "true"
       DD_SERVICE: relayflow-api
       DD_ENV: production
       DD_AGENT_HOST: datadog-agent
     volumes:
       - ./dd-java-agent.jar:/dd/dd-java-agent.jar:ro
   ```

Any agent that reads JSON from container stdout (Fluent Bit, Vector, Grafana Alloy, the
Elastic Agent) can collect these logs the same way.

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
  logs api` — this is almost always a missing `RELAYFLOW_ENCRYPTION_KEY`, which fails fast with
  a clear message.
