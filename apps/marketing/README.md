# apps/marketing

RelayFlow's marketing site — static content only, no backend. It links out
to the GitHub repo and to `docs/self-hosting.md` for the free, open-source,
self-hosted product.

## How it's built

A Next.js App Router site with `output: "export"` (`next.config.mjs`) — pure
static HTML/CSS/JS, no server, no API routes. `src/lib/deployment-artifacts.ts`
reads `docker-compose.yml`, `.env.example`, and the Caddyfile from the repo
root **at build time** and embeds them in `/docs/self-hosting` so that page
never drifts from the real deployment files.

## Deployment

Hosted on GitHub Pages, deployed by `.github/workflows/deploy-pages.yml` on
every push to `main` that touches this app. `basePath: "/relayflow"` in
`next.config.mjs` matches the project-page URL
(`https://<user>.github.io/relayflow/`) — update it if this ever moves to a
custom domain (drop the `basePath` and add a `CNAME` file instead).

## Local development

```bash
npm run dev    # http://localhost:4000
npm run build  # static export to out/
```
