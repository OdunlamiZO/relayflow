import { generateKeyPairSync, verify } from "node:crypto";
import { beforeAll, describe, expect, it } from "vitest";

import { signLicenseKey } from "@/lib/license";

let publicKeyBytes: Buffer;

beforeAll(() => {
  const { publicKey, privateKey } = generateKeyPairSync("ed25519");

  publicKeyBytes = publicKey.export({ type: "spki", format: "der" });
  process.env.LICENSE_SIGNING_PRIVATE_KEY_B64 = privateKey
    .export({ type: "pkcs8", format: "der" })
    .toString("base64");
});

describe("signLicenseKey", () => {
  it("produces three unpadded base64url segments", () => {
    const licenseKey = signLicenseKey({
      customerId: "acme",
      issuedAt: new Date("2026-01-01T00:00:00Z"),
      expiresAt: new Date("2027-01-01T00:00:00Z"),
    });

    const segments = licenseKey.split(".");

    expect(segments).toHaveLength(3);
    for (const segment of segments) {
      expect(segment).not.toContain("=");
      expect(segment).not.toContain("+");
      expect(segment).not.toContain("/");
    }
  });

  it("encodes a header that decodes to {alg: EdDSA}", () => {
    const licenseKey = signLicenseKey({
      customerId: "acme",
      issuedAt: new Date("2026-01-01T00:00:00Z"),
      expiresAt: new Date("2027-01-01T00:00:00Z"),
    });

    const [header] = licenseKey.split(".");
    const decoded = JSON.parse(Buffer.from(header, "base64url").toString());

    expect(decoded).toEqual({ alg: "EdDSA" });
  });

  it("round-trips the claims through the payload segment", () => {
    const issuedAt = new Date("2026-01-01T00:00:00Z");
    const expiresAt = new Date("2027-01-01T00:00:00Z");
    const licenseKey = signLicenseKey({
      customerId: "acme",
      issuedAt,
      expiresAt,
    });

    const [, payload] = licenseKey.split(".");
    const decoded = JSON.parse(Buffer.from(payload, "base64url").toString());

    expect(decoded).toEqual({
      sub: "acme",
      iat: Math.floor(issuedAt.getTime() / 1000),
      exp: Math.floor(expiresAt.getTime() / 1000),
    });
  });

  it("produces a signature verifiable with raw Ed25519 over header.payload", () => {
    const licenseKey = signLicenseKey({
      customerId: "acme",
      issuedAt: new Date("2026-01-01T00:00:00Z"),
      expiresAt: new Date("2027-01-01T00:00:00Z"),
    });

    const [header, payload, signature] = licenseKey.split(".");
    const signingInput = Buffer.from(`${header}.${payload}`, "ascii");

    const publicKeyObject = {
      key: publicKeyBytes,
      format: "der" as const,
      type: "spki" as const,
    };

    const isValid = verify(
      null,
      signingInput,
      publicKeyObject,
      Buffer.from(signature, "base64url")
    );

    expect(isValid).toBe(true);
  });
});
