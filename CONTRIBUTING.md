# Contributing to RelayFlow

Thanks for considering a contribution. This document covers how the repo is laid out, how to get
a local environment running, and the conventions pull requests are expected to follow.

## Repository layout

- `apps/api` — Java 21 + Spring Boot backend
- `apps/web` — Next.js frontend (inbox, settings, workflow builder)
- `apps/marketing` — static marketing site (GitHub Pages)
- `packages/contracts` — shared OpenAPI spec and JSON schemas
- `docs/` — product/technical planning docs and the self-hosting guide

See the root [`README.md`](README.md) for local development setup (prerequisites, running
infrastructure, migrations, starting each app).

## Before you start

For anything beyond a small fix, open an issue first describing what you want to change and why.
This avoids duplicated work and lets us agree on the approach before you invest time in an
implementation.

## Code style

These rules are enforced by formatters/linters where possible, and checked in review otherwise.

- **No abbreviations** — spell out full words in every identifier (class names, variables,
  methods, files, hooks): `configuration` not `config`, `message` not `msg`, `conversation` not
  `conv`, `workspace` not `ws`.
- **Java**: a blank line before the first field in a class and between every field; a blank line
  before every `return` that isn't the sole statement in its block; blank lines separating
  distinct logical phases within a method body (fetch → build → persist).
- **TypeScript/TSX**: the same blank-line-before-`return` and logical-grouping rules apply.
  Components call hooks, hooks call the API client — never call the API client directly from a
  component. Import order is enforced by Prettier (`npm run format` to auto-fix).
- **Comments**: default to none. Well-named functions and variables should carry the intent.
  Only add a comment for something the code genuinely can't express — an external constraint, a
  workaround, a non-obvious invariant — and keep it to one flat, descriptive line.

## Before opening a pull request

Install the pre-commit hook once:

```bash
cd apps/api
mvn install -Pdevelopment
```

It runs backend formatting/tests and frontend/marketing lint, typecheck, format, and unit tests
on every commit. A PR won't be merged if any of these fail in CI:

```bash
# Backend
cd apps/api && mvn spotless:check -Pdevelopment && mvn test

# Frontend
cd apps/web && npm run lint && npm run typecheck && npm run format:check && npm run test

# Marketing
cd apps/marketing && npm run lint && npm run typecheck && npm run format:check && npm run test
```

Update `packages/contracts/openapi.yaml` in the same change set as any API shape change.

## Pull requests

- Branch off `main`.
- Keep PRs focused — one change per PR is easier to review than several bundled together.
- Describe what changed and why in the PR description; link the issue it addresses.
- Update relevant docs (`README.md`, `docs/`) when behavior or setup steps change.

## Reporting bugs and security issues

Open a GitHub issue for bugs. For a security vulnerability, please do not open a public issue —
contact the maintainer directly instead.
