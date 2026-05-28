# RelayFlow

RelayFlow is an omnichannel customer messaging platform with developer-grade workflow automation. Telegram is the first pilot adapter, but the core model is channel-agnostic.

## Repository Layout

```text
apps/
  api/          Java 21 + Spring Boot backend
  web/          Next.js frontend
packages/
  contracts/    OpenAPI and JSON schemas shared across frontend/backend
docs/
  prd/          Product and technical planning documents
infra/
  docker/       Local infrastructure notes/config
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

Run database migrations manually:

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

## Contracts

The backend and frontend share contracts through `packages/contracts`.

- `openapi.yaml` defines the public API surface.
- `schemas/` contains JSON schemas for workflow definitions and messaging events.
- Generated clients should be committed only after the generation flow is standardized.

## Messaging API

The first backend slice is the channel-agnostic messaging persistence API. It stores workspaces, channel accounts, contacts, external channel identities, conversations, and messages.

Current endpoints:

- `GET /api/workspaces`
- `POST /api/workspaces`
- `GET /api/channel-accounts?workspaceId={workspaceId}`
- `POST /api/channel-accounts`
- `DELETE /api/channel-accounts/{id}?workspaceId={workspaceId}`
- `POST /api/channel-accounts/{id}/reconnect?workspaceId={workspaceId}`
- `POST /api/contacts`
- `POST /api/external-identities`
- `POST /api/conversations`
- `GET /api/conversations?workspaceId={workspaceId}`
- `GET /api/conversations/{conversationId}?workspaceId={workspaceId}`
- `GET /api/conversations/{conversationId}/messages?workspaceId={workspaceId}`
- `POST /api/conversations/{conversationId}/messages?workspaceId={workspaceId}`

Flyway is disabled at runtime, so the application will not apply migrations automatically on startup. Run migrations explicitly before starting the API against a fresh database.

## Authentication API

Current endpoints:

- `GET /api/auth/me`
- `POST /api/auth/signup`
- `POST /api/auth/login`
- `POST /api/auth/logout`
- `POST /api/auth/guest`
- `GET /oauth2/authorization/google`
- `GET /login/oauth2/code/google`

The app supports email/password login, Google OAuth login, and temporary anonymous guest sessions. Guest sessions create a workspace automatically and can use the shared Telegram bot when `SHARED_TELEGRAM_BOT_TOKEN` is configured.

## Telegram API

Current endpoints:

- `POST /api/telegram/webhook/{channelAccountId}`
- `POST /api/telegram/webhook/shared`

The regular webhook path is for a dedicated bot token per channel account. The shared webhook path is for the guest bot flow using a Telegram `/start {workspaceId}` deep link.

## Implementation Roadmap

#### Messaging core (milestone 1)
- [x] Backend messaging persistence model for workspaces, channel accounts, contacts, external identities, conversations, and messages.
- [x] Backend REST endpoints for creating and reading the core messaging records.
- [x] Telegram adapter — inbound webhook ingestion, outbound relay, and shared bot `/start {workspaceId}` deep-link flow.
- [x] Outbound message delivery guarantee: Telegram send retried once; on final failure the transaction rolls back, so the message is never saved and the frontend receives a 502 with the error text.
- [x] Shared bot message routing to the most-recently linked guest workspace when a Telegram user has connected across multiple sessions.
- [x] Channel account disconnect — sets status to `DISABLED`; inbound webhooks and outbound sends are gated on `ACTIVE` status so messages stop flowing immediately. Conversation history is preserved.

#### Authentication
- [x] Email/password signup and login.
- [x] Google login/signup with OAuth2 session login and user provisioning.
- [x] Anonymous guest session flow with auto-created workspace and shared bot channel account. Guest data is purged after 24 hours (configurable via `relayflow.guest.expiry-hours`).

#### Inbox UI
- [x] Frontend API client code for the messaging endpoints.
- [x] Inbox screen: conversation list, message thread, and outbound message composer.
- [x] Real-time inbox updates via SSE (`message.created`, `workspace.updated` events). SSE push fires only after the originating transaction commits.
- [x] Frontend Google login entry point and session status panel.
- [x] Guest mode banner with "Create account" prompt and "Telegram connected" empty state after linking.

#### Security and quality
- [ ] Workspace membership authorization checks on all workspace-scoped endpoints.
- [ ] Telegram webhook verification — validate `X-Telegram-Bot-Api-Secret-Token` before trusting public webhook calls.
- [ ] Backend integration tests for persistence against PostgreSQL (Testcontainers, existing IT profile).
- [ ] Frontend component tests for the inbox states and message composer.
- [ ] Playwright end-to-end tests for the guest onboarding and inbox flows (run in CI, not pre-commit).

#### Workflow runtime (milestone 2)
- [ ] Workflow data model — `workflow_definitions`, `workflow_versions`, `workflow_runs`, `workflow_run_steps`, and `workflow_variables` tables.
- [ ] Workflow execution service — run a published version, advance step by step, write structured per-step log records.
- [ ] Inbound message trigger node — start a workflow run when a normalized message is stored.
- [ ] Set variable node — create or override a scoped workflow variable with explicit type handling.
- [ ] Send message node — send a channel-appropriate outbound message through the active adapter.
- [ ] Run status and step log API — expose run timeline, step inputs/outputs, branch decisions, and variable snapshots.

#### HTTP automation node (milestone 3)
- [ ] HTTP request node — configurable method, URL, headers, query params, JSON body, and auth.
- [ ] Per-step timeout, retry count, retry delay, success branch, and error branch.
- [ ] Response body and status code mapping to workflow variables via JSON path extraction.
- [ ] Secret masking in step log output.

#### Workflow builder UI (milestone 4)
- [ ] React Flow editor for building workflow graphs.
- [ ] Save a draft and publish a workflow version.
- [ ] Manual test-run mode — execute a workflow without sending real customer messages.
- [ ] Run log timeline UI — step cards showing inputs, outputs, errors, retries, branch decisions, and variable snapshots.

#### Production readiness (milestone 5)
- [ ] Workspace roles — owner, admin, and agent — with role-based access control in the API.
- [ ] Conversation assignment to workspace members.
- [ ] Structured request/run ID logging and Sentry integration.
- [ ] Deployment configuration for Render / Fly.io / Railway.


## Git Hooks

The repository includes `hooks/pre-commit`. It runs:

- Backend Java formatting check: `mvn spotless:check -Pdevelopment`
- Backend unit tests: `mvn test`
- Frontend lint: `npm run lint`
- Frontend typecheck: `npm run typecheck`
- Frontend format check: `npm run format:check`
- Frontend unit/component tests: `npm run test`

The hook is installed into `.git/hooks/pre-commit` in this scaffold. It can also be reinstalled by running the API Maven build with the `development` profile once dependencies are available.


## Frontend Tests

Frontend tests use Vitest with React Testing Library. Default frontend tests are fast unit/component tests and run in pre-commit.

```bash
cd apps/web
npm run test
```

Use watch mode while developing:

```bash
cd apps/web
npm run test:watch
```

Playwright should be added later for browser-based end-to-end flows once the inbox and workflow builder have real user paths. Keep Playwright out of pre-commit; run it in CI or a pre-push workflow.


## Backend Tests

Default Maven test/install runs only lightweight tests and does not require Docker:

```bash
cd apps/api
mvn install -Pdevelopment
```

Run integration tests against PostgreSQL Testcontainers when Docker Desktop is running:

```bash
cd apps/api
mvn verify -Pintegration-test
```
