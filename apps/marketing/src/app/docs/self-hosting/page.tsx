import Link from "next/link";

import { CopyableCodeBlock } from "@/components/CopyableCodeBlock";
import { Footer } from "@/components/Footer";
import {
  caddyfile,
  dockerComposeYml,
  envExample,
} from "@/lib/deployment-artifacts";

const ENV_VAR_ROWS: Array<{
  name: string;
  required: boolean;
  notes: string;
}> = [
  {
    name: "POSTGRES_PASSWORD",
    required: true,
    notes: "No default — generate with openssl rand -base64 24",
  },
  {
    name: "RELAYFLOW_ENCRYPTION_KEY",
    required: true,
    notes:
      "No default — generate with openssl rand -base64 32. The API refuses to start without it.",
  },
  {
    name: "RELAYFLOW_WEB_DOMAIN / RELAYFLOW_API_DOMAIN",
    required: true,
    notes: "Used by Caddy for automatic TLS",
  },
  {
    name: "RELAYFLOW_WEB_BASE_URL / RELAYFLOW_API_BASE_URL",
    required: true,
    notes: "Must match the domains above exactly — see Troubleshooting",
  },
  {
    name: "GOOGLE_AUTH_ENABLED / GOOGLE_CLIENT_ID / GOOGLE_CLIENT_SECRET",
    required: false,
    notes: "Off by default",
  },
  {
    name: "ANTHROPIC_API_KEY / OPENAI_API_KEY / GROQ_API_KEY",
    required: false,
    notes: "At least one needed for AI agent features",
  },
  {
    name: "SMTP_HOST / SMTP_PORT / SMTP_USERNAME / SMTP_PASSWORD / SMTP_FROM",
    required: false,
    notes:
      "Works with any SMTP provider (Resend, SES, Mailgun, Postmark, Gmail, self-hosted, etc.). Blank host runs email in no-op/log mode",
  },
  {
    name: "Rate limiting, timeouts, retention vars",
    required: false,
    notes: "All have safe defaults — see .env.example below",
  },
];

export default function SelfHostingDocsPage() {
  return (
    <div className="min-h-screen bg-neutral-200 text-neutral-800">
      <header className="sticky top-0 z-10 border-b border-neutral-300 bg-neutral-100/95 backdrop-blur-sm">
        <div className="mx-auto flex max-w-6xl items-center justify-between px-6 py-4 sm:px-8">
          <Link
            href="/"
            className="flex items-center gap-2 text-sm font-bold tracking-tight text-accent"
          >
            <span
              className="material-symbols-rounded text-[20px]"
              aria-hidden="true"
            >
              account_tree
            </span>
            RelayFlow
          </Link>

          <Link
            href="https://github.com/OdunlamiZO/relayflow"
            target="_blank"
            rel="noopener noreferrer"
            className="rounded-lg bg-secondary px-3 py-2 text-sm font-semibold text-neutral-100 transition-colors hover:bg-secondary-dark"
          >
            GitHub
          </Link>
        </div>
      </header>

      <main className="mx-auto max-w-3xl px-6 py-16 sm:px-8 sm:py-20">
        <h1 className="text-[1.9rem] font-bold leading-[1.15] tracking-tight text-primary sm:text-[2.5rem]">
          Self-hosting RelayFlow
        </h1>
        <p className="mt-5 text-base leading-7 text-neutral-600">
          This is the production deployment guide for running RelayFlow on your
          own infrastructure. It covers everything from a fresh host to a
          running instance with TLS.
        </p>

        <section className="mt-12">
          <h2 className="m-0 text-xl font-bold tracking-tight text-primary">
            Prerequisites
          </h2>
          <ul className="mt-4 flex flex-col gap-2 text-sm leading-6 text-neutral-600">
            <li>Docker Engine + Docker Compose v2.</li>
            <li>
              A domain name with two DNS <code>A</code> records pointed at the
              host: one for the web app, one for the API (e.g.{" "}
              <code>app.example.com</code> and <code>api.example.com</code>).
              The bundled reverse proxy (Caddy) needs these to provision TLS
              certificates automatically.
            </li>
            <li>Ports 80 and 443 open on the host.</li>
          </ul>
        </section>

        <section className="mt-12">
          <h2 className="m-0 text-xl font-bold tracking-tight text-primary">
            Quick start
          </h2>
          <p className="mt-4 text-sm leading-6 text-neutral-600">
            Save these three files into a directory on your host, keeping them
            in the same relative layout shown here (the Caddyfile goes in{" "}
            <code>infra/docker/caddy/Caddyfile</code>, next to the compose
            file).
          </p>

          <div className="mt-6 flex flex-col gap-4">
            <CopyableCodeBlock
              filename="docker-compose.yml"
              content={dockerComposeYml}
            />
            <CopyableCodeBlock filename=".env.example" content={envExample} />
            <CopyableCodeBlock
              filename="infra/docker/caddy/Caddyfile"
              content={caddyfile}
            />
          </div>

          <p className="mt-6 text-sm leading-6 text-neutral-600">Then:</p>
          <pre className="mt-3 overflow-auto rounded-lg bg-neutral-100 p-4 text-xs leading-5 text-neutral-700">
            <code>cp .env.example .env</code>
          </pre>

          <p className="mt-4 text-sm leading-6 text-neutral-600">
            Generate the two secrets that have no safe default:
          </p>
          <pre className="mt-3 overflow-auto rounded-lg bg-neutral-100 p-4 text-xs leading-5 text-neutral-700">
            <code>{`openssl rand -base64 24   # → POSTGRES_PASSWORD
openssl rand -base64 32   # → RELAYFLOW_ENCRYPTION_KEY`}</code>
          </pre>

          <p className="mt-4 text-sm leading-6 text-neutral-600">
            Fill in <code>.env</code>: the two generated secrets and your
            domains (<code>RELAYFLOW_WEB_DOMAIN</code>,{" "}
            <code>RELAYFLOW_API_DOMAIN</code>, and the matching{" "}
            <code>RELAYFLOW_WEB_BASE_URL</code>/
            <code>RELAYFLOW_API_BASE_URL</code>).
          </p>

          <p className="mt-4 text-sm leading-6 text-neutral-600">
            Then start the stack:
          </p>
          <pre className="mt-3 overflow-auto rounded-lg bg-neutral-100 p-4 text-xs leading-5 text-neutral-700">
            <code>docker compose --profile prod up -d</code>
          </pre>
        </section>

        <section className="mt-12">
          <h2 className="m-0 text-xl font-bold tracking-tight text-primary">
            Required vs optional env vars
          </h2>
          <div className="mt-4 overflow-x-auto rounded-xl border border-neutral-300">
            <table className="w-full border-collapse text-left text-sm">
              <thead>
                <tr className="border-b border-neutral-300 bg-neutral-100">
                  <th className="px-4 py-2.5 font-semibold text-primary">
                    Variable
                  </th>
                  <th className="px-4 py-2.5 font-semibold text-primary">
                    Required?
                  </th>
                  <th className="px-4 py-2.5 font-semibold text-primary">
                    Notes
                  </th>
                </tr>
              </thead>
              <tbody>
                {ENV_VAR_ROWS.map((row) => (
                  <tr
                    key={row.name}
                    className="border-b border-neutral-300 last:border-b-0"
                  >
                    <td className="px-4 py-2.5 font-mono text-xs text-neutral-700">
                      {row.name}
                    </td>
                    <td className="px-4 py-2.5 text-neutral-600">
                      {row.required ? "Yes" : "No"}
                    </td>
                    <td className="px-4 py-2.5 text-neutral-600">
                      {row.notes}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>

        <section className="mt-12">
          <h2 className="m-0 text-xl font-bold tracking-tight text-primary">
            First-run walkthrough
          </h2>
          <ol className="mt-4 flex flex-col gap-3 text-sm leading-6 text-neutral-600">
            <li>
              Confirm the one-shot <code>migrate</code> service applied all
              migrations and exited cleanly:
              <pre className="mt-2 overflow-auto rounded-lg bg-neutral-100 p-4 text-xs leading-5 text-neutral-700">
                <code>docker compose --profile prod logs migrate</code>
              </pre>
            </li>
            <li>
              Confirm every service reports healthy:
              <pre className="mt-2 overflow-auto rounded-lg bg-neutral-100 p-4 text-xs leading-5 text-neutral-700">
                <code>docker compose --profile prod ps</code>
              </pre>
            </li>
            <li>
              Visit <code>https://&lt;RELAYFLOW_WEB_DOMAIN&gt;</code> and
              complete the first admin signup.
            </li>
          </ol>
        </section>

        <section className="mt-12">
          <h2 className="m-0 text-xl font-bold tracking-tight text-primary">
            Backups
          </h2>
          <p className="mt-4 text-sm leading-6 text-neutral-600">
            Back up the Postgres volume regularly:
          </p>
          <pre className="mt-3 overflow-auto rounded-lg bg-neutral-100 p-4 text-xs leading-5 text-neutral-700">
            <code>{`docker compose --profile prod exec postgres \\
  pg_dump -U relayflow relayflow > backup-$(date +%F).sql`}</code>
          </pre>
          <p className="mt-4 text-sm leading-6 text-neutral-600">
            Restore into a fresh instance with{" "}
            <code>psql -U relayflow relayflow &lt; backup.sql</code>, or
            snapshot the <code>postgres_data</code> named volume directly at the
            infrastructure level.
          </p>
        </section>

        <section className="mt-12">
          <h2 className="m-0 text-xl font-bold tracking-tight text-primary">
            Upgrades
          </h2>
          <ol className="mt-4 flex flex-col gap-3 text-sm leading-6 text-neutral-600">
            <li>
              Bump <code>RELAYFLOW_VERSION</code> in <code>.env</code> to the
              new release tag.
            </li>
            <li>
              Re-run migrations before restarting the app:
              <pre className="mt-2 overflow-auto rounded-lg bg-neutral-100 p-4 text-xs leading-5 text-neutral-700">
                <code>docker compose --profile prod run --rm migrate</code>
              </pre>
            </li>
            <li>
              Pull and restart:
              <pre className="mt-2 overflow-auto rounded-lg bg-neutral-100 p-4 text-xs leading-5 text-neutral-700">
                <code>{`docker compose --profile prod pull
docker compose --profile prod up -d`}</code>
              </pre>
            </li>
          </ol>
          <p className="mt-4 text-sm leading-6 text-neutral-600">
            Migrations are forward-only — there are no down-migrations, so plan
            upgrades accordingly.
          </p>
        </section>

        <section className="mt-12">
          <h2 className="m-0 text-xl font-bold tracking-tight text-primary">
            Using your own reverse proxy
          </h2>
          <p className="mt-4 text-sm leading-6 text-neutral-600">
            If you already run nginx, Traefik, or similar, skip the{" "}
            <code>caddy</code> service by naming the other services explicitly
            instead of using <code>--profile prod</code>:
          </p>
          <pre className="mt-3 overflow-auto rounded-lg bg-neutral-100 p-4 text-xs leading-5 text-neutral-700">
            <code>docker compose up -d postgres redis migrate api web</code>
          </pre>
          <p className="mt-4 text-sm leading-6 text-neutral-600">
            and point your existing proxy at <code>web:3000</code> and{" "}
            <code>api:8080</code> on the compose network instead.
          </p>
        </section>

        <section className="mt-12">
          <h2 className="m-0 text-xl font-bold tracking-tight text-primary">
            Troubleshooting
          </h2>
          <ul className="mt-4 flex flex-col gap-3 text-sm leading-6 text-neutral-600">
            <li>
              <strong>Browser console shows CORS errors</strong> —{" "}
              <code>RELAYFLOW_WEB_BASE_URL</code> must exactly match the public
              origin the browser is loading the web app from (scheme + host, no
              trailing slash) — the API uses it as the allowed CORS origin.
            </li>
            <li>
              <strong>AI agent features return errors or do nothing</strong> —
              no LLM provider key is configured — set at least one of{" "}
              <code>ANTHROPIC_API_KEY</code>, <code>OPENAI_API_KEY</code>, or{" "}
              <code>GROQ_API_KEY</code>.
            </li>
            <li>
              <strong>
                API container exits immediately on a fresh install
              </strong>{" "}
              — check <code>docker compose --profile prod logs api</code> — this
              is almost always a missing <code>RELAYFLOW_ENCRYPTION_KEY</code>,
              which fails fast with a clear message.
            </li>
          </ul>
        </section>
      </main>

      <Footer />
    </div>
  );
}
