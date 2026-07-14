import { randomBytes } from "node:crypto";
import { beforeAll, describe, expect, it } from "vitest";

// MARKETING_DB_PATH is read at module-load time, so it must be set before
// database.ts is first imported — dynamic import guarantees that order.
let db: typeof import("@/lib/database");

beforeAll(async () => {
  process.env.MARKETING_DB_PATH = ":memory:";
  db = await import("@/lib/database");
});

function newOrder() {
  return {
    reference: randomBytes(12).toString("base64url"),
    email: "buyer@example.com",
    githubUsername: "buyer",
    currency: "NGN",
    amountMinorUnits: 5_000_000,
  };
}

describe("order state machine", () => {
  it("inserts a pending order", () => {
    const order = newOrder();
    db.insertPendingOrder(order);

    const found = db.findOrderByReference(order.reference);

    expect(found?.status).toBe("pending");
    expect(found?.githubUsername).toBe("buyer");
    expect(found?.licenseKey).toBeNull();
  });

  it("transitions pending -> awaiting_access_grant exactly once", () => {
    const order = newOrder();
    db.insertPendingOrder(order);

    const accessGrantToken = randomBytes(12).toString("base64url");
    const firstAttempt = db.markOrderAwaitingAccessGrant({
      reference: order.reference,
      licenseKey: "fake-license-key",
      licenseExpiresAt: 4_070_908_800,
      accessGrantToken,
    });
    const secondAttempt = db.markOrderAwaitingAccessGrant({
      reference: order.reference,
      licenseKey: "a-different-key",
      licenseExpiresAt: 1,
      accessGrantToken: randomBytes(12).toString("base64url"),
    });

    expect(firstAttempt).toBe(true);
    expect(secondAttempt).toBe(false);

    const found = db.findOrderByReference(order.reference);

    expect(found?.status).toBe("awaiting_access_grant");
    expect(found?.licenseKey).toBe("fake-license-key");
  });

  it("only grantAccessAndCompleteOrder's first call with a valid token succeeds", () => {
    const order = newOrder();
    db.insertPendingOrder(order);

    const accessGrantToken = randomBytes(12).toString("base64url");
    db.markOrderAwaitingAccessGrant({
      reference: order.reference,
      licenseKey: "fake-license-key",
      licenseExpiresAt: 4_070_908_800,
      accessGrantToken,
    });

    const firstAttempt = db.grantAccessAndCompleteOrder(accessGrantToken);
    const secondAttempt = db.grantAccessAndCompleteOrder(accessGrantToken);

    expect(firstAttempt?.status).toBe("paid");
    expect(firstAttempt?.licenseKey).toBe("fake-license-key");
    expect(secondAttempt).toBeUndefined();

    const found = db.findOrderByReference(order.reference);

    expect(found?.status).toBe("paid");
  });

  it("rejects an unknown access-grant token", () => {
    const result = db.grantAccessAndCompleteOrder("no-such-token");

    expect(result).toBeUndefined();
  });

  it("cannot grant access to an order that never reached awaiting_access_grant", () => {
    const order = newOrder();
    db.insertPendingOrder(order);

    // No markOrderAwaitingAccessGrant call — the order has no access_grant_token yet.
    const result = db.grantAccessAndCompleteOrder("irrelevant");

    expect(result).toBeUndefined();
    expect(db.findOrderByReference(order.reference)?.status).toBe("pending");
  });

  it("only marks a pending order failed, not an awaiting_access_grant one", () => {
    const order = newOrder();
    db.insertPendingOrder(order);
    db.markOrderAwaitingAccessGrant({
      reference: order.reference,
      licenseKey: "fake-license-key",
      licenseExpiresAt: 4_070_908_800,
      accessGrantToken: randomBytes(12).toString("base64url"),
    });

    db.markOrderFailed(order.reference);

    expect(db.findOrderByReference(order.reference)?.status).toBe(
      "awaiting_access_grant"
    );
  });
});
