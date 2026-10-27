# Local Infrastructure

> **Looking to deploy RelayFlow, not just develop it?** This file only covers local dev infra.
> See [`docs/self-hosting.md`](../../docs/self-hosting.md) and the root `docker-compose.yml`'s
> `prod` profile for the production self-hosting stack — it's the same file, gated by
> `--profile`, not a separate one. See the comment block at the top of that file for the full
> profile breakdown (`dev` / `prod` / `marketing`).

The root `docker-compose.yml`'s `dev` profile provides:

- PostgreSQL 16
- Redis 7
- Ollama (opt-in via `--profile ollama`, additive to `dev`)

PostgreSQL is the primary application database. Redis handles rate limiting, live plan configuration, and LLM provider config. Ollama is a self-hosted LLM runtime for running open-source models locally at no API cost.

## Starting services

```bash
# Core only (postgres + redis)
docker compose up -d dev-postgres dev-redis

# Core + Ollama (pulls llama3.2 on first run — ~2 GB)
docker compose --profile dev --profile ollama up -d
```

## Ollama

The `ollama` service runs the model server at `http://localhost:11434`. The `ollama-pull` service runs once on startup to download `llama3.2` into the `ollama_data` volume so it persists across container restarts.

To pull a different model after startup:

```bash
docker compose exec ollama ollama pull qwen2.5:3b
```

To switch the model RelayFlow uses, update the Redis key (no restart required):

```bash
redis-cli SET 'platform:llm:ollama:model' 'qwen2.5:3b'
redis-cli SET  platform:llm:provider       'OLLAMA'
```

NVIDIA GPU passthrough on Linux is supported — uncomment the `deploy.resources` block in `docker-compose.yml`.
