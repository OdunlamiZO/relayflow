#!/usr/bin/env node
// One-off operator script — never run in a build or deploy path. Generates
// the Ed25519 keypair used to sign/verify self-hosted license keys.
import { generateKeyPairSync } from "node:crypto";

const { publicKey, privateKey } = generateKeyPairSync("ed25519");

const privateKeyBase64 = privateKey
  .export({ type: "pkcs8", format: "der" })
  .toString("base64");
const publicKeyBase64 = publicKey
  .export({ type: "spki", format: "der" })
  .toString("base64");

console.log("Generated a new Ed25519 license signing keypair.\n");
console.log(`LICENSE_SIGNING_PRIVATE_KEY_B64=${privateKeyBase64}`);
console.log(`RELAYFLOW_LICENSE_SIGNING_PUBLIC_KEY=${publicKeyBase64}`);
console.log(
  "\nStore the private key in a secrets manager and never commit it — it " +
    "signs every license key this instance will ever issue.\n\n" +
    "Set the public key as the RELAYFLOW_LICENSE_SIGNING_PUBLIC_KEY GitHub " +
    "Actions secret in this repo (Settings → Secrets and variables → " +
    "Actions). docker-publish.yml passes it as a build arg so it's baked " +
    "into every relayflow-api image — customers never see or configure it."
);
