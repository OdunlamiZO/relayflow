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

Run the API:

```bash
cd apps/api
mvn spring-boot:run
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
