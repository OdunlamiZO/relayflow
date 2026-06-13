# RelayFlow

RelayFlow is an omnichannel customer messaging platform with developer-grade workflow automation. Telegram is the first adapter, WhatsApp Business Cloud API support has started, and Instagram is planned. The core model is channel-agnostic.

## Repository Layout

```text
apps/
  api/    Java 21 + Spring Boot backend
  web/    Next.js frontend
docs/
  prd/    Product and technical planning documents
infra/
  docker/ Local infrastructure notes and config
```

## Local Development

Prerequisites:

- Java 21
- Maven 3.9+
- Node.js 20+
- Docker

Start infrastructure:

```bash
docker compose up -d postgres redis
```

Flyway is disabled at runtime, so migrations do not run automatically when the API starts. Run migrations manually before starting the API against a fresh or changed database:

```bash
cd apps/api
mvn flyway:migrate \
  -Dflyway.url=jdbc:postgresql://localhost:5432/relayflow \
  -Dflyway.user=relayflow \
  -Dflyway.password=relayflow
```

Set a credential encryption key (required — the API refuses to start without it):

```bash
export RELAYFLOW_ENCRYPTION_KEY=$(openssl rand -base64 32)
```

Run the API:

```bash
cd apps/api
mvn spring-boot:run
```

Enable Google login locally:

```bash
export GOOGLE_AUTH_ENABLED=true
export GOOGLE_CLIENT_ID=your-google-client-id
export GOOGLE_CLIENT_SECRET=your-google-client-secret
export RELAYFLOW_WEB_BASE_URL=http://localhost:3000
```

Enable the shared Telegram demo bot locally:

```bash
export SHARED_TELEGRAM_BOT_TOKEN=your-telegram-bot-token
export RELAYFLOW_API_BASE_URL=http://localhost:8080
```

Per-channel-account Telegram bots are registered with a generated `secret_token` and verified automatically via the `X-Telegram-Bot-Api-Secret-Token` header. To verify the shared bot's webhook, set `SHARED_TELEGRAM_WEBHOOK_SECRET` to the same value passed as `secret_token` when calling Telegram's `setWebhook` for the shared bot:

```bash
export SHARED_TELEGRAM_WEBHOOK_SECRET=your-shared-webhook-secret
```

Configure this redirect URI in Google Cloud:

```text
http://localhost:8080/login/oauth2/code/google
```

Run the web app:

```bash
cd apps/web
npm install
npm run dev
```

Set the shared Telegram demo bot's username (used to build the guest deep link, e.g. `https://t.me/your_bot_username?start=...`) in `apps/web/.env.local`:

```bash
NEXT_PUBLIC_SHARED_BOT_USERNAME=your_bot_username
```

## Subscription & Plan Configuration

Plan limits and pricing are stored in Redis and take effect immediately without restarting the API. Seed these keys before the API handles subscription or plan-limited requests.

### Key schema

Two separate Redis keys per plan — keeping limits/pricing provider-neutral and payment-provider config isolated:

| Key | Value | Description |
|---|---|---|
| `relayflow:plan:{PLAN}:config` | JSON object | Limits and pricing. `{PLAN}` is `FREE`, `PRO_MONTHLY`, or `PRO_ANNUAL` (uppercase). |
| `relayflow:plan:{PLAN}:provider` | string | Active payment provider for this plan (e.g. `PAYSTACK`). Defaults to `PAYSTACK` when absent. |
| `relayflow:paystack:plan:{PLAN}:code` | string | Paystack plan code for recurring billing (e.g. `PLN_abc123`). |

**`relayflow:plan:{PLAN}:config` fields:**

| Field | Type | Description |
|---|---|---|
| `maxChannelAccounts` | integer | Maximum channel connections per workspace. Use `2147483647` for unlimited. |
| `maxWorkflows` | integer | Maximum workflow definitions per workspace. Use `2147483647` for unlimited. |
| `maxMembersPerWorkspace` | integer | Maximum members per workspace. Use `2147483647` for unlimited. |
| `priceNgn` | number | Price in Nigerian naira for this plan's billing cycle. `0` for free plans. |
| `billingInterval` | string \| null | `"monthly"` or `"annual"`. `null` for free plans. |

### Set plan config

```bash
redis-cli SET relayflow:plan:FREE:config \
  '{"maxChannelAccounts":1,"maxWorkflows":1,"maxMembersPerWorkspace":1,"priceNgn":0,"billingInterval":null}'

redis-cli SET relayflow:plan:PRO_MONTHLY:config \
  '{"maxChannelAccounts":2,"maxWorkflows":5,"maxMembersPerWorkspace":15,"priceNgn":5000,"billingInterval":"monthly"}'

redis-cli SET relayflow:plan:PRO_ANNUAL:config \
  '{"maxChannelAccounts":2,"maxWorkflows":5,"maxMembersPerWorkspace":15,"priceNgn":50000,"billingInterval":"annual"}'

# Set the payment provider for paid plans (omit to keep the default of PAYSTACK)
redis-cli SET relayflow:plan:PRO_MONTHLY:provider 'PAYSTACK'
redis-cli SET relayflow:plan:PRO_ANNUAL:provider 'PAYSTACK'

# Set the Paystack recurring plan codes
redis-cli SET relayflow:paystack:plan:PRO_MONTHLY:code 'PLN_xxx'
redis-cli SET relayflow:paystack:plan:PRO_ANNUAL:code 'PLN_yyy'
```

Replace `PLN_xxx` / `PLN_yyy` with the real Paystack plan codes from your dashboard (create one monthly plan and one annual plan). Changes apply on the next API call — no restart required.

### Paystack setup

1. Set `PAYSTACK_SECRET_KEY` to your Paystack secret key. The checkout and webhook endpoints are inactive when this variable is absent.
2. Create recurring plans in the Paystack dashboard, copy the monthly and annual plan codes, and write them to `relayflow:paystack:plan:PRO_MONTHLY:code` and `relayflow:paystack:plan:PRO_ANNUAL:code` (see above).
3. Register the webhook URL in your Paystack dashboard:

```
https://{your-api-domain}/paystack/webhook
```

## API Reference

### Health

- `GET /health`

### Authentication

- `GET  /auth/me`
- `POST /auth/signup`
- `POST /auth/verify-email`
- `POST /auth/login`
- `POST /auth/login/2fa`
- `POST /auth/logout`
- `POST /auth/guest`
- `GET  /oauth2/authorization/google`
- `GET  /login/oauth2/code/google`

The app supports verified email/password login, Google OAuth, TOTP two-factor login, and temporary anonymous guest sessions. Guest sessions create a workspace automatically and can use the shared Telegram bot when `SHARED_TELEGRAM_BOT_TOKEN` is configured. Guest workspaces cannot connect additional channels — `POST /channel-accounts` returns `403` for them. When a guest account signs up and converts to a real account, the shared Telegram channel account and everything that happened over it (conversations, messages, workflow runs, external identities, and any contacts left with no other channel) are dropped; workflow definitions are preserved.

### Profile

- `GET    /profile`
- `PATCH  /profile`
- `POST   /profile/change-password`
- `DELETE /profile`
- `POST   /profile/2fa/setup`
- `POST   /profile/2fa/enable`
- `POST   /profile/2fa/disable`

### Messaging

- `GET    /workspaces`
- `POST   /workspaces`
- `GET    /workspaces/{workspaceId}/members`
- `POST   /workspaces/{workspaceId}/members`
- `PATCH  /workspaces/{workspaceId}/members/{memberId}`
- `DELETE /workspaces/{workspaceId}/members/{memberId}`
- `PUT    /workspaces/{workspaceId}/owner?memberId={memberId}`
- `GET    /workspaces/{workspaceId}/invites`
- `POST   /workspaces/{workspaceId}/invites`
- `DELETE /workspaces/{workspaceId}/invites/{inviteId}`
- `GET    /invites/{token}`
- `POST   /invites/{token}/accept`
- `GET    /channel-accounts?workspaceId={workspaceId}`
- `POST   /channel-accounts` — `403` for guest workspaces (guests are limited to the shared Telegram bot).
- `DELETE /channel-accounts/{id}?workspaceId={workspaceId}`
- `POST   /channel-accounts/{id}/reconnect?workspaceId={workspaceId}`
- `GET    /contacts?workspaceId={workspaceId}`
- `POST   /contacts`
- `GET    /contacts/{id}?workspaceId={workspaceId}`
- `POST   /contacts/{id}/merge?workspaceId={workspaceId}`
- `DELETE /contacts/{id}?workspaceId={workspaceId}`
- `POST   /external-identities`
- `POST   /conversations`
- `GET    /conversations?workspaceId={workspaceId}`
- `GET    /conversations/{id}?workspaceId={workspaceId}`
- `PATCH  /conversations/{id}?workspaceId={workspaceId}`
- `PATCH  /conversations/{id}/assignee?workspaceId={workspaceId}` — assign or unassign (`assigneeId: null`) a conversation to a workspace member.
- `GET    /conversations/{id}/messages?workspaceId={workspaceId}`
- `POST   /conversations/{id}/messages?workspaceId={workspaceId}`

### Integrations

- `GET    /workspaces/{workspaceId}/api-keys`
- `POST   /workspaces/{workspaceId}/api-keys`
- `DELETE /workspaces/{workspaceId}/api-keys/{keyId}`
- `GET    /workspaces/{workspaceId}/webhook`
- `PUT    /workspaces/{workspaceId}/webhook`
- `DELETE /workspaces/{workspaceId}/webhook`
- `POST   /workspaces/{workspaceId}/webhook/rotate-secret`

Public API requests authenticate with `X-Api-Key`:

- `GET  /public/v1/conversations`
- `GET  /public/v1/conversations/{id}`
- `GET  /public/v1/conversations/{id}/messages`
- `POST /public/v1/conversations/{id}/messages`

### Workflows

- `GET    /workflows?workspaceId={workspaceId}`
- `POST   /workflows`
- `GET    /workflows/{id}?workspaceId={workspaceId}`
- `PATCH  /workflows/{id}?workspaceId={workspaceId}`
- `DELETE /workflows/{id}?workspaceId={workspaceId}`
- `GET    /workflows/{id}/runs?workspaceId={workspaceId}` — paginated run history.
- `GET    /workflows/{id}/runs/{runId}?workspaceId={workspaceId}` — run detail with per-step input/output snapshots.

### Subscription

- `GET  /plans` — public; returns all plans with live limits, pricing, and `upgradeAvailable`
- `GET  /workspaces/{workspaceId}/subscription`
- `POST /workspaces/{workspaceId}/subscription/checkout`
- `DELETE /workspaces/{workspaceId}/subscription`

`POST /checkout` returns a Paystack authorization URL. Redirect the user there to complete payment. `DELETE /subscription` schedules cancellation and keeps paid access until the current billing period ends. Recurring billing is handled automatically by Paystack; the API listens for `charge.success`, `subscription.create`, `subscription.not_renew`, `subscription.disable`, and `invoice.update` events at `/paystack/webhook`.

### Telegram

- `POST /telegram/webhook/{channelAccountId}`
- `POST /telegram/webhook/shared`

The regular webhook path is for a dedicated bot token per channel account. The shared webhook path handles the guest bot flow using a Telegram `/start {workspaceId}` deep link. Both paths verify the `X-Telegram-Bot-Api-Secret-Token` header before processing the payload.

### WhatsApp

- `GET  /whatsapp/webhook/{channelAccountId}`
- `POST /whatsapp/webhook/{channelAccountId}`

The `GET` path handles Meta webhook verification using the channel account's stored verify token. The `POST` path receives WhatsApp Business Cloud API message events.

### SSE

- `GET /sse/workspace/{workspaceId}` — real-time event stream for the inbox

Events pushed: `message.created`, `conversation.updated`, `workspace.updated`.

## Implementation Status

### Messaging core
- [x] Channel-agnostic persistence model — workspaces, channel accounts, contacts, external identities, conversations, messages.
- [x] REST endpoints for creating and reading core messaging records.
- [x] Telegram adapter — inbound webhook ingestion, outbound relay, shared bot `/start {workspaceId}` deep-link flow, and webhook authenticity verification via `X-Telegram-Bot-Api-Secret-Token`.
- [x] WhatsApp Business Cloud API adapter — channel connection form, webhook verification, inbound text ingestion, outbound text relay, and per-channel webhook URL display.
- [x] Outbound message delivery guarantee — Telegram and WhatsApp sends are retried once; final failure rolls back the transaction so the message is never saved and the caller receives a descriptive error.
- [x] Shared bot message routing to the most-recently linked guest workspace.
- [x] Channel account disconnect — sets status to `DISABLED`; inbound and outbound are gated on `ACTIVE` so messages stop immediately. History is preserved.
- [x] Closed conversation reopening — when a contact messages a closed conversation it is set back to `OPEN` and workflow automation fires again.
- [x] Contact management — paginated contacts list, detail panel, delete, and guarded contact merge that moves identities/conversations to the target contact.
- [x] Per-channel-account identities — external identities are scoped to a channel account/bot so the same Telegram user can appear in separate connected bots without collision.
- [x] Conversation workflow lock — active workflows own the conversation and agent replies return `409 Conflict` until the workflow finishes, fails, or closes the conversation.
- [x] Conversation assignment — conversations can be assigned to (or unassigned from) a workspace member via a dropdown in the message thread.
- [x] Workspace member permissions — owners manage members, transfer ownership, and grant granular access for inbox, contacts, workflows, channels, API keys, and webhooks.
- [x] Workspace invites — owners create/revoke expiring email invites; authenticated users can preview and accept matching invites.
- [x] API keys and public API — workspace API keys can list conversations/messages and send outbound agent messages through `/public/v1`.
- [x] Workspace webhooks — configurable signed webhooks currently emit `contact.created` with retry/backoff delivery. SSRF protection rejects webhook URLs that resolve to loopback, link-local, private, multicast, or wildcard addresses, both when saving the URL and at delivery time.
- [x] Subscription billing foundation — FREE, PRO monthly, and PRO annual plans, Redis-backed plan limits/pricing, Paystack checkout, scheduled cancellation, Paystack webhooks, downgrade locking, and plan caps for channels, workflows, and workspace members.

### Authentication
- [x] Email/password signup and login.
- [x] Email verification before first email/password login.
- [x] Google OAuth2 login with user provisioning.
- [x] Profile management — display name, email update preference, password change, account deletion, and profile page.
- [x] TOTP two-factor authentication — setup QR code, enable/disable, and 2FA login challenge.
- [x] Split user model — identities, preferences, and MFA methods live outside the core `users` table.
- [x] Anonymous guest session flow with auto-created workspace. Guest data purged after 24 hours (configurable via `relayflow.guest.expiry-hours`).
- [x] Credential encryption — bot tokens, MFA secrets, and webhook secrets are encrypted at rest with AES-256-GCM (`RELAYFLOW_ENCRYPTION_KEY`, required at startup).
- [x] Session cookie hardening — configurable cookie name, `HttpOnly`, `Secure`, `SameSite`, and idle timeout (`SESSION_COOKIE_NAME`, `SESSION_COOKIE_SECURE`, `SESSION_COOKIE_SAME_SITE`, `SESSION_TIMEOUT`); logout expires the cookie with matching attributes.
- [x] Swagger UI / OpenAPI docs can be disabled outside of development via `SWAGGER_ENABLED=false`.
- [x] Rate limiting — Redis-backed, fixed-window, returns `429` with a `Retry-After` header. Covers auth endpoints (`/auth/login`, `/login/2fa`, `/signup`, `/guest`, `/verify-email`, keyed by client IP), the public API (`/public/v1/**`, keyed by API key), and inbound webhooks (Telegram, WhatsApp, Paystack, keyed by client IP). Configurable via `RATE_LIMIT_ENABLED`, `RATE_LIMIT_LOGIN_LIMIT`/`RATE_LIMIT_LOGIN_WINDOW_SECONDS`, `RATE_LIMIT_SIGNUP_LIMIT`/`RATE_LIMIT_SIGNUP_WINDOW_SECONDS`, `RATE_LIMIT_PUBLIC_API_LIMIT`/`RATE_LIMIT_PUBLIC_API_WINDOW_SECONDS`, and `RATE_LIMIT_WEBHOOK_LIMIT`/`RATE_LIMIT_WEBHOOK_WINDOW_SECONDS`.

### Inbox UI
- [x] Conversation list, message thread, and outbound composer.
- [x] Contacts page with channel badges, contact detail panel, inbox deep link, delete confirmation, and merge modal.
- [x] Settings UI for member permissions, invites, API keys, webhook configuration, and owner-only billing management.
- [x] Channel settings can connect Telegram or WhatsApp and display provider-specific webhook URLs.
- [x] Real-time updates via SSE — `message.created`, `conversation.updated`, and `workspace.updated` events pushed after commit.
- [x] Google login entry point and session status panel.
- [x] Guest mode banner with a "Create account" prompt and Telegram-connected empty state.
- [x] Channel settings show the shared Telegram bot with a "Guest mode only" badge, plus a persistent deep link and copy button (`https://t.me/{bot}?start={workspaceId}`) so it can be reshared with additional testers. Non-shared channels also get a copy button for their webhook URL. "Telegram"/"WhatsApp" brand colors are defined as `telegram`/`whatsapp` design tokens.

### Workflow engine
- [x] Workflow definition data model — `workflow_definitions`, `workflow_runs`, `workflow_run_steps`.
- [x] Node graph executor — walks the graph step by step, records per-step input/output snapshots, duration, and status.
- [x] Variable interpolation — `{{variable}}` placeholders resolved at execution time in all text fields.
- [x] **Trigger node** — fires on `conversation_opened`; multiple concurrent workflows supported per conversation.
- [x] **Send Message node** — sends an outbound message through the active channel adapter.
- [x] **Condition node** — multi-branch with configurable variable, operator, and value per branch; `is_set` / `is_not_set` operators need no value.
- [x] **HTTP Request node** — method, URL, headers, body, content-type, timeout; response status and JSON path mappings saved to workflow variables.
- [x] **Set Variable node** — creates or overwrites a named workflow variable.
- [x] **Ask Question node** — sends a question and pauses the run (`WAITING`); resumes when the contact replies. Two modes: open-ended (saves reply to a variable) or defined options (routes by exact match or option number, can save the selected value, falls back to "Other"). Telegram renders options as reply keyboards; WhatsApp uses native buttons for up to three options.
- [x] **Jump To node** — redirects execution to another node by ID with a configurable max-jump limit to prevent loops.
- [x] **End Conversation node** — sends an optional closing message and sets the conversation to `CLOSED`.
- [x] Workflow graph validator — enforces structural rules at publish time (one trigger, no orphaned nodes, all condition and option branches connected, valid Jump To targets).
- [x] Workflow run logs — every run and every step persisted with full observability data, exposed via `GET /workflows/{id}/runs` and `GET /workflows/{id}/runs/{runId}`. Runs older than `relayflow.workflow.run-retention-days` (default 90, configurable via `WORKFLOW_RUN_RETENTION_DAYS`) are purged hourly.
- [x] Workspace membership authorization and permission checks on mutating workspace-scoped endpoints.

### Workflow builder UI
- [x] React Flow drag-and-drop canvas.
- [x] Per-node config panel with variable picker (`{{…}}` button) supporting both built-in and user-defined variables.
- [x] Save draft and Publish / Unpublish toggle with validation error banner.
- [x] Node palette: Trigger, Send Message, Condition, HTTP Request, Set Variable, Ask Question, Jump To, End Conversation.
- [x] Run logs UI — `/workflows/{id}/runs` lists run history with status, error preview, and a step-by-step breakdown of input/output snapshots and durations.

### Planned
- [ ] AI agent — an LLM-powered agent that can manage conversations directly (read history, draft/send replies) and trigger a workflow when needed.
- [ ] Decide upgrade proration policy for moving to a plan above PRO — wait for the current subscription to expire before switching, or start the new plan immediately and credit the unused balance from the current one.
- [ ] Backend integration tests (Testcontainers, existing IT profile).
- [ ] Frontend component tests for inbox states and composer.
- [ ] Playwright end-to-end tests (CI only, not pre-commit).
- [ ] Structured request/run ID logging and Sentry integration.
- [ ] Deployment configuration (Render / Fly.io / Railway).

## Git Hooks

The repository includes `hooks/pre-commit`. It runs:

- Backend Java formatting check: `mvn spotless:check -Pdevelopment`
- Backend unit tests: `mvn test`
- Frontend lint: `npm run lint`
- Frontend typecheck: `npm run typecheck`
- Frontend format check: `npm run format:check`
- Frontend unit/component tests: `npm run test`
- Marketing lint, typecheck, and format check (`apps/marketing`)

Install with:

```bash
cd apps/api
mvn install -Pdevelopment
```

## Tests

### Frontend

```bash
cd apps/web
npm run test          # unit / component tests (pre-commit)
npm run test:watch    # watch mode
```

Playwright end-to-end tests should be added later for the inbox and workflow builder flows. Keep them out of pre-commit; run in CI or a pre-push hook.

### Backend

```bash
cd apps/api
mvn install -Pdevelopment          # lightweight tests, no Docker needed
mvn verify -Pintegration-test      # full integration tests against PostgreSQL (requires Docker)
```
