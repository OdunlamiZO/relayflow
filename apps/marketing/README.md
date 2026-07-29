# apps/marketing

RelayFlow's marketing site — and, since it's the only piece of RelayFlow
that stays hosted by RelayFlow itself (see the BYOC pivot notes), the
self-hosted license checkout and issuance service.

This is an internal operator runbook for the checkout/issuance side. For the
customer-facing self-hosted product, see `docs/self-hosting.md` — this file
is intentionally **not** linked from there.

## How it works

The self-hosted Docker images are in a **private** GHCR registry (not public —
see below), so a purchase has two parts: issuing a license key (automated)
and granting the buyer's GitHub account read access to pull the images
(manual, notified via Telegram). Both must happen before the buyer gets
anything — see the `awaiting_access_grant` step below.

1. A visitor lands on `/pricing`, enters their email and GitHub username, and
   clicks "Buy license." `POST /api/checkout` verifies the GitHub username
   actually exists, creates a `pending` order (SQLite), starts a Paystack
   transaction, and redirects to Paystack's hosted checkout page.
2. After payment, Paystack redirects the browser back to
   `/checkout/complete?reference=...`, which polls
   `GET /api/orders/[reference]/status` until the order is `paid`.
3. In parallel, Paystack calls `POST /api/webhooks/paystack`. After verifying
   the HMAC signature and a server-to-server `transaction/verify` call, a
   license key is signed (`src/lib/license.ts`) and stored, and the order
   moves to `awaiting_access_grant` — **not `paid` yet**. A Telegram message
   (`src/lib/telegram.ts`) is sent with the order details and a one-click
   confirmation link; the key is not emailed at this point.
4. You add the buyer's GitHub account as a collaborator on the
   `relayflow-api` / `relayflow-web` / `relayflow-migrate` **packages**
   (GitHub's package settings UI, not the repo's — package-level grants
   apply immediately, no invitation the buyer has to accept; repo-level
   ones would also leak source code, which package grants don't). This step
   has no API and can't be automated. Then tap the confirmation link in the
   Telegram message.
5. That link (`GET /api/admin/grant-access?token=...`, token-authenticated —
   no login) transitions the order to `paid` and, only now, emails the
   license key via Resend (falls back to a console log when `RESEND_API_KEY`
   is unset). The buyer's still-open `/checkout/complete` tab picks this up
   on its next poll.
6. The license key is a JWS verified offline by the self-hosted product's
   `LicenseKeyValidator` — see the "Cross-language verification" section
   below before touching `src/lib/license.ts`.

## Going live: first-time setup checklist

Do these once, in order, before the first real sale.

### 1. Provision a host

A VPS with Docker Engine + Docker Compose v2 installed, and a domain name
with an `A` record pointed at it. If you're using the bundled Caddy (see
Deployment below), open ports 80/443; if fronting with your own nginx,
whatever ports that needs.

### 2. Generate the license signing keypair

```bash
node scripts/generate-license-keypair.mjs
```

Prints two values:

- `LICENSE_SIGNING_PRIVATE_KEY_B64` — goes in this app's own env only. Never
  commit it, never give it to a customer. Signs every license key this
  instance will ever issue — store it in a secrets manager, not just the
  VPS's `.env.marketing` file.
- `RELAYFLOW_LICENSE_SIGNING_PUBLIC_KEY` — set as a GitHub Actions secret on
  this repo (Settings → Secrets and variables → Actions). `publish-api.yml`
  passes it as a build arg that gets baked into every `relayflow-api` image
  at build time (see `LicenseKeyValidator`'s Javadoc) — customers never see
  or configure this value.

Run this once. Re-running it invalidates every previously issued license
key — the public key baked into already-published images would no longer
match.

### 3. Get a Paystack account

Create a Paystack account, grab the secret key (`PAYSTACK_SECRET_KEY`) from
the dashboard, and set your flat price in `PAYSTACK_PRICE_MINOR_UNITS`
(minor units of `PAYSTACK_CURRENCY` — e.g. kobo for NGN).

### 4. Create a Telegram bot and get your chat ID

This is how you get notified when someone pays — the purchase flow is
stuck in `awaiting_access_grant` forever without it.

1. Message [@BotFather](https://t.me/BotFather) on Telegram, send `/newbot`,
   follow the prompts (pick a display name and a username ending in `bot`).
   BotFather replies with a token — that's `TELEGRAM_BOT_TOKEN`.
2. Send your new bot any message (e.g. "hi") so it has a chat to talk back
   to.
3. Visit `https://api.telegram.org/bot<TOKEN>/getUpdates` in a browser
   (substitute your real token). Find `"chat":{"id":...}` in the JSON —
   that number is `TELEGRAM_CHAT_ID`.

### 5. Make the GHCR packages private

`relayflow-api` / `relayflow-web` / `relayflow-migrate` on
`ghcr.io/odunlamizo/...` need to be **private**, or step 4 of "How it
works" above is pointless — anyone could pull the images without paying.
Package visibility is a separate setting from repo visibility: GitHub
profile → Packages → each package → Package settings → Danger Zone →
Change visibility.

### 6. Fill in the env file

```bash
cp .env.example .env.marketing
```

Fill in every value from steps 2–4 above, plus `MARKETING_BASE_URL` (this
app's own public URL) and `RESEND_API_KEY`/`RESEND_FROM` if you want real
emails instead of console-logged ones locally. See `.env.example` for the
full list — everything without a default is required, and the app refuses
to start without it (see `configuration.ts`).

### 7. Deploy and verify

See "Deployment" below for the actual command. Once it's up:

- `curl https://<your-domain>/api/health` → `{"status":"ok"}`
- Visit `/pricing`, confirm the price renders correctly.
- Visit `/docs/self-hosting`, confirm the embedded `docker-compose.yml` /
  `.env.example` / Caddyfile blocks render (they're read from the real
  files at build time — see `src/lib/deployment-artifacts.ts`).
- Do one real (or Paystack test-mode) purchase end to end and confirm the
  Telegram notification arrives.

## Env vars

See `.env.example` for the full list with defaults. `PAYSTACK_SECRET_KEY`,
`LICENSE_SIGNING_PRIVATE_KEY_B64`, `TELEGRAM_BOT_TOKEN`, and
`RESEND_API_KEY` are secrets — pass them as container env vars at deploy
time, never as Docker build args (see `Dockerfile`).

`CHECKOUT_RATE_LIMIT` / `CHECKOUT_RATE_LIMIT_WINDOW_SECONDS` (default `5` per
`3600` seconds) fixed-window rate limit the checkout endpoint per client IP —
see `configuration.ts`.

## Database

A single SQLite file (`MARKETING_DB_PATH`, default
`./data/marketing.sqlite`), holding one `orders` table. No migration
framework — the schema is created idempotently on first connection
(`src/lib/database.ts`).

Uses Node's built-in `node:sqlite` (unflagged since Node 22.13.0, which is
exactly the Docker base image tag) instead of a native-compiled driver —
expect a benign `ExperimentalWarning` on startup.

**Backups**: periodically run

```bash
sqlite3 data/marketing.sqlite ".backup data/backup-$(date +%F).sqlite"
```

or snapshot the `marketing_data` named volume directly.

## Deployment

This runs as its own profile in the root `docker-compose.yml`, entirely
separate from the customer-facing `prod` profile — see the comment block at
the top of that file. Deployed by building in place on the VPS from a git
checkout, the same pattern as `../mstore-admin` — no image is published to a
registry for this service (unlike the customer-facing `api`/`web`/`migrate`
images, which are, via `publish-api.yml` — which also publishes `migrate` — and `publish-web.yml`).

```bash
docker compose --env-file .env.marketing up --build -d marketing
```

This starts only the `marketing` app container, not `marketing-caddy` — naming the service
explicitly starts it without activating the whole `marketing` profile. `marketing-caddy`
(automatic Let's Encrypt TLS via `RELAYFLOW_MARKETING_DOMAIN`) is for a bare VPS with no existing
reverse proxy; skip it if you already front other apps with your own nginx, as on this VPS.

`marketing` publishes to `127.0.0.1:4000` only (not the public interface), so point nginx at
that. Example server block:

```nginx
server {
    listen 443 ssl;
    server_name sell.example.com;

    ssl_certificate     /etc/letsencrypt/live/sell.example.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/sell.example.com/privkey.pem;

    location / {
        proxy_pass http://127.0.0.1:4000;
        proxy_set_header Host $host;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

Obtain the certificate with `certbot --nginx -d sell.example.com` (or your usual ACME client)
— unlike Caddy, nginx doesn't provision/renew TLS automatically.

**Updates**: run `.github/workflows/deploy.yml` manually (Actions tab → Deploy marketing
→ Run workflow) whenever you want to ship a change — it's not triggered automatically on push. It
SSHes into the VPS and runs `git pull && docker compose --env-file .env.marketing up --build -d
marketing`. Requires `VPS_HOST`/`VPS_USER`/`VPS_PASSWORD` secrets on this repo (same values as
`../mstore-admin`'s, since it's the same VPS) and a git checkout of this repo at `/root/relayflow`
on that VPS — adjust the `cd` path in the workflow if yours differs.

## Cross-language verification

The license key format is a hard cross-language contract with
`apps/api/src/main/java/com/relayflow/api/license/LicenseKeyValidator.java`.
If you touch `src/lib/license.ts`, verify both:

1. **Automated**: `apps/api/src/test/java/com/relayflow/api/license/LicenseKeyValidatorNodeCompatibilityTest.java`
   hardcodes a Node-generated key/public-key fixture and asserts the Java
   validator accepts it. Run `mvn test` in `apps/api`.
2. **Manual smoke test**: run `apps/api` locally with
   `mvn spring-boot:run -Pselfhosted -Drelayflow.license.publicKey=<public key
from generate-license-keypair.mjs>` (the `selfhosted` Maven profile is what
   activates `LicenseKeyValidator` — a plain `mvn spring-boot:run` never does,
   by design; the public key is baked the same way, not read from `.env`), a
   `RELAYFLOW_LICENSE_KEY` signed by that same keypair via `src/lib/license.ts`,
   and confirm clean startup and the "license key verified" log line. Run
   `mvn clean` afterward before going back to a plain `mvn spring-boot:run` —
   Maven doesn't remove the baked files when the profile isn't active, so a
   stale one left in `target/` will keep getting picked up.
