import { mkdirSync } from "node:fs";
import { dirname } from "node:path";
import { DatabaseSync } from "node:sqlite";

import { configuration } from "@/lib/configuration";

const CREATE_ORDERS_TABLE = `
  CREATE TABLE IF NOT EXISTS orders (
    reference           TEXT PRIMARY KEY,
    email                TEXT NOT NULL,
    github_username      TEXT NOT NULL,
    status               TEXT NOT NULL CHECK (status IN ('pending','awaiting_access_grant','paid','failed')) DEFAULT 'pending',
    currency             TEXT NOT NULL,
    amount_minor_units   INTEGER NOT NULL,
    license_key          TEXT,
    license_expires_at   INTEGER,
    access_grant_token   TEXT,
    access_granted_at    INTEGER,
    created_at           INTEGER NOT NULL,
    updated_at           INTEGER NOT NULL
  )
`;

const CREATE_EMAIL_INDEX =
  "CREATE INDEX IF NOT EXISTS idx_orders_email ON orders(email)";

const CREATE_ACCESS_GRANT_TOKEN_INDEX =
  "CREATE UNIQUE INDEX IF NOT EXISTS idx_orders_access_grant_token ON orders(access_grant_token) WHERE access_grant_token IS NOT NULL";

declare global {
  var __relayflowMarketingDatabase: DatabaseSync | undefined;
}

function openDatabase(): DatabaseSync {
  mkdirSync(dirname(configuration.databasePath), { recursive: true });

  const database = new DatabaseSync(configuration.databasePath);

  database.exec("PRAGMA journal_mode = WAL");
  database.exec("PRAGMA busy_timeout = 5000");
  database.exec(CREATE_ORDERS_TABLE);
  database.exec(CREATE_EMAIL_INDEX);
  database.exec(CREATE_ACCESS_GRANT_TOKEN_INDEX);

  return database;
}

// Cached on globalThis so Next.js dev-mode hot reload doesn't open a second
// handle on the same file.
export function getDatabase(): DatabaseSync {
  if (!globalThis.__relayflowMarketingDatabase) {
    globalThis.__relayflowMarketingDatabase = openDatabase();
  }

  return globalThis.__relayflowMarketingDatabase;
}

// pending -> awaiting_access_grant (paid, key signed, waiting on a manual
// GitHub access grant) -> paid (key delivered). Or pending -> failed.
export type OrderStatus =
  | "pending"
  | "awaiting_access_grant"
  | "paid"
  | "failed";

export type Order = {
  reference: string;
  email: string;
  githubUsername: string;
  status: OrderStatus;
  currency: string;
  amountMinorUnits: number;
  licenseKey: string | null;
  licenseExpiresAt: number | null;
  accessGrantToken: string | null;
  accessGrantedAt: number | null;
  createdAt: number;
  updatedAt: number;
};

type OrderRow = {
  reference: string;
  email: string;
  github_username: string;
  status: string;
  currency: string;
  amount_minor_units: number;
  license_key: string | null;
  license_expires_at: number | null;
  access_grant_token: string | null;
  access_granted_at: number | null;
  created_at: number;
  updated_at: number;
};

function toOrder(row: OrderRow): Order {
  return {
    reference: row.reference,
    email: row.email,
    githubUsername: row.github_username,
    status: row.status as OrderStatus,
    currency: row.currency,
    amountMinorUnits: row.amount_minor_units,
    licenseKey: row.license_key,
    licenseExpiresAt: row.license_expires_at,
    accessGrantToken: row.access_grant_token,
    accessGrantedAt: row.access_granted_at,
    createdAt: row.created_at,
    updatedAt: row.updated_at,
  };
}

export function insertPendingOrder(input: {
  reference: string;
  email: string;
  githubUsername: string;
  currency: string;
  amountMinorUnits: number;
}): void {
  const now = Math.floor(Date.now() / 1000);

  getDatabase()
    .prepare(
      `INSERT INTO orders
         (reference, email, github_username, status, currency, amount_minor_units, created_at, updated_at)
       VALUES (?, ?, ?, 'pending', ?, ?, ?, ?)`
    )
    .run(
      input.reference,
      input.email,
      input.githubUsername,
      input.currency,
      input.amountMinorUnits,
      now,
      now
    );
}

export function markOrderFailed(reference: string): void {
  const now = Math.floor(Date.now() / 1000);

  getDatabase()
    .prepare(
      "UPDATE orders SET status = 'failed', updated_at = ? WHERE reference = ? AND status = 'pending'"
    )
    .run(now, reference);
}

/**
 * Transitions pending -> awaiting_access_grant and attaches the issued
 * license. Returns true only if this call performed the transition — guards
 * against re-notifying on a duplicate webhook delivery.
 */
export function markOrderAwaitingAccessGrant(input: {
  reference: string;
  licenseKey: string;
  licenseExpiresAt: number;
  accessGrantToken: string;
}): boolean {
  const now = Math.floor(Date.now() / 1000);

  const result = getDatabase()
    .prepare(
      `UPDATE orders
       SET status = 'awaiting_access_grant', license_key = ?, license_expires_at = ?,
           access_grant_token = ?, updated_at = ?
       WHERE reference = ? AND status = 'pending'`
    )
    .run(
      input.licenseKey,
      input.licenseExpiresAt,
      input.accessGrantToken,
      now,
      input.reference
    );

  return result.changes > 0;
}

/**
 * Transitions awaiting_access_grant -> paid. Returns the order only if this
 * call performed the transition — guards against a re-clicked link
 * completing (and re-emailing) twice.
 */
export function grantAccessAndCompleteOrder(
  accessGrantToken: string
): Order | undefined {
  const now = Math.floor(Date.now() / 1000);

  const result = getDatabase()
    .prepare(
      `UPDATE orders
       SET status = 'paid', access_granted_at = ?, updated_at = ?
       WHERE access_grant_token = ? AND status = 'awaiting_access_grant'`
    )
    .run(now, now, accessGrantToken);

  if (result.changes === 0) {
    return undefined;
  }

  const row = getDatabase()
    .prepare("SELECT * FROM orders WHERE access_grant_token = ?")
    .get(accessGrantToken) as OrderRow | undefined;

  return row ? toOrder(row) : undefined;
}

export function findOrderByReference(reference: string): Order | undefined {
  const row = getDatabase()
    .prepare("SELECT * FROM orders WHERE reference = ?")
    .get(reference) as OrderRow | undefined;

  return row ? toOrder(row) : undefined;
}
