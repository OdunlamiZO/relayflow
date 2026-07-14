import { createPrivateKey, sign } from "node:crypto";

import { configuration } from "@/lib/configuration";

/**
 * Produces a license key byte-compatible with the self-hosted product's
 * verifier (`apps/api/.../license/LicenseKeyValidator.java`): a compact JWS
 * `base64url(header).base64url(payload).base64url(signature)`, all
 * unpadded, signed with raw Ed25519 (pure EdDSA, no pre-hash). Any change
 * here must stay in lockstep with that Java class — see
 * `LicenseKeyValidatorNodeCompatibilityTest.java` for the cross-language
 * guard.
 */

function base64url(input: Buffer): string {
  return input.toString("base64url");
}

export function signLicenseKey(input: {
  customerId: string;
  issuedAt: Date;
  expiresAt: Date;
}): string {
  const header = base64url(Buffer.from(JSON.stringify({ alg: "EdDSA" })));

  const payload = base64url(
    Buffer.from(
      JSON.stringify({
        sub: input.customerId,
        iat: Math.floor(input.issuedAt.getTime() / 1000),
        exp: Math.floor(input.expiresAt.getTime() / 1000),
      })
    )
  );

  const signingInput = `${header}.${payload}`;
  const privateKey = createPrivateKey({
    key: Buffer.from(configuration.license.signingPrivateKeyBase64, "base64"),
    format: "der",
    type: "pkcs8",
  });
  const signature = sign(null, Buffer.from(signingInput, "ascii"), privateKey);

  return `${signingInput}.${base64url(signature)}`;
}

export function issueLicenseKey(customerId: string): {
  licenseKey: string;
  expiresAt: Date;
} {
  const issuedAt = new Date();
  const expiresAt = new Date(
    issuedAt.getTime() + configuration.license.termDays * 24 * 60 * 60 * 1000
  );

  const licenseKey = signLicenseKey({
    customerId,
    issuedAt,
    expiresAt,
  });

  return { licenseKey, expiresAt };
}
