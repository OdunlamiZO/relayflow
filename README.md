# RelayFlow

RelayFlow is an omnichannel customer messaging platform with developer-grade workflow automation. Telegram is the first adapter, with WhatsApp and Instagram planned. The core model is channel-agnostic.

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
export SHARED_TELEGRAM_BOT_USERNAME=your_bot_username
export RELAYFLOW_API_BASE_URL=http://localhost:8080
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

## API Reference

### Health

- `GET /api/health`

### Authentication

- `GET  /api/auth/me`
- `POST /api/auth/signup`
- `POST /api/auth/verify-email`
- `POST /api/auth/login`
- `POST /api/auth/login/2fa`
- `POST /api/auth/logout`
- `POST /api/auth/guest`
- `GET  /oauth2/authorization/google`
- `GET  /login/oauth2/code/google`

The app supports verified email/password login, Google OAuth, TOTP two-factor login, and temporary anonymous guest sessions. Guest sessions create a workspace automatically and can use the shared Telegram bot when `SHARED_TELEGRAM_BOT_TOKEN` is configured.

### Profile

- `GET    /api/profile`
- `PATCH  /api/profile`
- `POST   /api/profile/change-password`
- `DELETE /api/profile`
- `POST   /api/profile/2fa/setup`
- `POST   /api/profile/2fa/enable`
- `POST   /api/profile/2fa/disable`

### Messaging

- `GET    /api/workspaces`
- `POST   /api/workspaces`
- `GET    /api/workspaces/{workspaceId}/members`
- `POST   /api/workspaces/{workspaceId}/members`
- `PATCH  /api/workspaces/{workspaceId}/members/{memberId}`
- `DELETE /api/workspaces/{workspaceId}/members/{memberId}`
- `GET    /api/workspaces/{workspaceId}/invites`
- `POST   /api/workspaces/{workspaceId}/invites`
- `DELETE /api/workspaces/{workspaceId}/invites/{inviteId}`
- `GET    /api/invites/{token}`
- `POST   /api/invites/{token}/accept`
- `GET    /api/channel-accounts?workspaceId={workspaceId}`
- `POST   /api/channel-accounts`
- `DELETE /api/channel-accounts/{id}?workspaceId={workspaceId}`
- `POST   /api/channel-accounts/{id}/reconnect?workspaceId={workspaceId}`
- `GET    /api/contacts?workspaceId={workspaceId}`
- `POST   /api/contacts`
- `GET    /api/contacts/{id}?workspaceId={workspaceId}`
- `POST   /api/contacts/{id}/merge?workspaceId={workspaceId}`
- `DELETE /api/contacts/{id}?workspaceId={workspaceId}`
- `POST   /api/external-identities`
- `POST   /api/conversations`
- `GET    /api/conversations?workspaceId={workspaceId}`
- `GET    /api/conversations/{id}?workspaceId={workspaceId}`
- `PATCH  /api/conversations/{id}?workspaceId={workspaceId}`
- `GET    /api/conversations/{id}/messages?workspaceId={workspaceId}`
- `POST   /api/conversations/{id}/messages?workspaceId={workspaceId}`

### Integrations

- `GET    /api/workspaces/{workspaceId}/api-keys`
- `POST   /api/workspaces/{workspaceId}/api-keys`
- `DELETE /api/workspaces/{workspaceId}/api-keys/{keyId}`
- `GET    /api/workspaces/{workspaceId}/webhook`
- `PUT    /api/workspaces/{workspaceId}/webhook`
- `DELETE /api/workspaces/{workspaceId}/webhook`
- `POST   /api/workspaces/{workspaceId}/webhook/rotate-secret`

Public API requests authenticate with `X-Api-Key`:

- `GET  /public/v1/conversations`
- `GET  /public/v1/conversations/{id}`
- `GET  /public/v1/conversations/{id}/messages`
- `POST /public/v1/conversations/{id}/messages`

### Workflows

- `GET    /api/workflows?workspaceId={workspaceId}`
- `POST   /api/workflows`
- `GET    /api/workflows/{id}?workspaceId={workspaceId}`
- `PATCH  /api/workflows/{id}?workspaceId={workspaceId}`
- `DELETE /api/workflows/{id}?workspaceId={workspaceId}`

### Telegram

- `POST /api/telegram/webhook/{channelAccountId}`
- `POST /api/telegram/webhook/shared`

The regular webhook path is for a dedicated bot token per channel account. The shared webhook path handles the guest bot flow using a Telegram `/start {workspaceId}` deep link.

### SSE

- `GET /api/sse/workspace/{workspaceId}` — real-time event stream for the inbox

Events pushed: `message.created`, `conversation.updated`, `workspace.updated`.

## Implementation Status

### Messaging core
- [x] Channel-agnostic persistence model — workspaces, channel accounts, contacts, external identities, conversations, messages.
- [x] REST endpoints for creating and reading core messaging records.
- [x] Telegram adapter — inbound webhook ingestion, outbound relay, shared bot `/start {workspaceId}` deep-link flow.
- [x] Outbound message delivery guarantee — Telegram send retried once; final failure rolls back the transaction so the message is never saved and the caller receives a descriptive error.
- [x] Shared bot message routing to the most-recently linked guest workspace.
- [x] Channel account disconnect — sets status to `DISABLED`; inbound and outbound are gated on `ACTIVE` so messages stop immediately. History is preserved.
- [x] Closed conversation reopening — when a contact messages a closed conversation it is set back to `OPEN` and workflow automation fires again.
- [x] Contact management — paginated contacts list, detail panel, delete, and guarded contact merge that moves identities/conversations to the target contact.
- [x] Per-channel-account identities — external identities are scoped to a channel account/bot so the same Telegram user can appear in separate connected bots without collision.
- [x] Conversation workflow lock — active workflows own the conversation and agent replies return `409 Conflict` until the workflow finishes, fails, or closes the conversation.
- [x] Workspace member permissions — owners manage members and grant granular access for inbox, contacts, workflows, channels, API keys, and webhooks.
- [x] Workspace invites — owners create/revoke expiring email invites; authenticated users can preview and accept matching invites.
- [x] API keys and public API — workspace API keys can list conversations/messages and send outbound agent messages through `/public/v1`.
- [x] Workspace webhooks — configurable signed webhooks currently emit `contact.created` with retry/backoff delivery.

### Authentication
- [x] Email/password signup and login.
- [x] Email verification before first email/password login.
- [x] Google OAuth2 login with user provisioning.
- [x] Profile management — display name, email update preference, password change, account deletion, and profile page.
- [x] TOTP two-factor authentication — setup QR code, enable/disable, and 2FA login challenge.
- [x] Split user model — identities, preferences, and MFA methods live outside the core `users` table.
- [x] Anonymous guest session flow with auto-created workspace. Guest data purged after 24 hours (configurable via `relayflow.guest.expiry-hours`).

### Inbox UI
- [x] Conversation list, message thread, and outbound composer.
- [x] Contacts page with channel badges, contact detail panel, inbox deep link, delete confirmation, and merge modal.
- [x] Settings UI for member permissions, invites, API keys, and webhook configuration.
- [x] Real-time updates via SSE — `message.created` and `workspace.updated` events pushed after commit.
- [x] Google login entry point and session status panel.
- [x] Guest mode banner with "Create account" prompt and Telegram-connected empty state.

### Workflow engine
- [x] Workflow definition data model — `workflow_definitions`, `workflow_runs`, `workflow_run_steps`.
- [x] Node graph executor — walks the graph step by step, records per-step input/output snapshots, duration, and status.
- [x] Variable interpolation — `{{variable}}` placeholders resolved at execution time in all text fields.
- [x] **Trigger node** — fires on `conversation_opened`; multiple concurrent workflows supported per conversation.
- [x] **Send Message node** — sends an outbound message through the active channel adapter.
- [x] **Condition node** — multi-branch with configurable variable, operator, and value per branch; `is_set` / `is_not_set` operators need no value.
- [x] **HTTP Request node** — method, URL, headers, body, content-type, timeout; response status and JSON path mappings saved to workflow variables.
- [x] **Set Variable node** — creates or overwrites a named workflow variable.
- [x] **Ask Question node** — sends a question and pauses the run (`WAITING`); resumes when the contact replies. Two modes: open-ended (saves reply to a variable) or defined options (routes by exact match, falls back to "Other").
- [x] **Jump To node** — redirects execution to another node by ID with a configurable max-jump limit to prevent loops.
- [x] **End Conversation node** — sends an optional closing message and sets the conversation to `CLOSED`.
- [x] Workflow graph validator — enforces structural rules at publish time (one trigger, no orphaned nodes, all condition and option branches connected, valid Jump To targets).
- [x] Workflow run logs — every run and every step persisted with full observability data.

### Workflow builder UI
- [x] React Flow drag-and-drop canvas.
- [x] Per-node config panel with variable picker (`{{…}}` button) supporting both built-in and user-defined variables.
- [x] Save draft and Publish / Unpublish toggle with validation error banner.
- [x] Node palette: Trigger, Send Message, Condition, HTTP Request, Set Variable, Ask Question, Jump To, End Conversation.

### Planned
- [ ] Workflow run logs UI — list runs per workflow; step-by-step breakdown with input/output snapshots. Accessible to workspace members.
- [x] Workspace membership authorization and permission checks on mutating workspace-scoped endpoints.
- [ ] Telegram webhook verification (`X-Telegram-Bot-Api-Secret-Token`).
- [ ] Broader role model beyond owner/member permissions.
- [ ] Conversation assignment to workspace members.
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
