import { randomBytes } from "node:crypto";

import { configuration } from "@/lib/configuration";
import {
  insertPendingOrder,
  markOrderAwaitingAccessGrant,
} from "@/lib/database";
// import { markOrderFailed } from "@/lib/database"; // PAYSTACK PATH — uncomment when re-enabling payment
import { githubUserExists } from "@/lib/github";
import { issueLicenseKey } from "@/lib/license";
// import { initializeTransaction } from "@/lib/paystack"; // PAYSTACK PATH — uncomment when re-enabling payment
import { clientIpFromRequest, isRateLimited } from "@/lib/rate-limit";
import { notifyAccessGrantNeeded } from "@/lib/telegram";

export const dynamic = "force-dynamic";

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const GITHUB_USERNAME_PATTERN =
  /^[a-zA-Z\d](?:[a-zA-Z\d]|-(?=[a-zA-Z\d])){0,38}$/;

export async function POST(request: Request) {
  const clientIp = clientIpFromRequest(request);

  if (
    isRateLimited(
      `checkout:${clientIp}`,
      configuration.rateLimit.checkoutLimit,
      configuration.rateLimit.checkoutWindowSeconds
    )
  ) {
    return Response.json({ error: "Too many requests." }, { status: 429 });
  }

  const body = (await request.json().catch(() => null)) as {
    email?: string;
    githubUsername?: string;
  } | null;
  const email = body?.email?.trim();
  const githubUsername = body?.githubUsername?.trim();

  if (!email || !EMAIL_PATTERN.test(email)) {
    return Response.json(
      { error: "A valid email is required." },
      { status: 400 }
    );
  }

  if (!githubUsername || !GITHUB_USERNAME_PATTERN.test(githubUsername)) {
    return Response.json(
      { error: "A valid GitHub username is required." },
      { status: 400 }
    );
  }

  if (!(await githubUserExists(githubUsername))) {
    return Response.json(
      { error: "That GitHub username doesn't exist. Check the spelling." },
      { status: 400 }
    );
  }

  const reference = randomBytes(24).toString("base64url");

  insertPendingOrder({
    reference,
    email,
    githubUsername,
    // FREE MODE — restore to configuration.paystack.currency / .priceMinorUnits
    // when re-enabling payment.
    currency: "FREE",
    amountMinorUnits: 0,
  });

  // ─── FREE MODE (temporary — early access/testing phase, no payment collected) ───
  // Skips Paystack entirely and issues the license key immediately, then drops
  // into the same manual access-grant step a paid order uses (GHCR packages
  // stay private either way). To re-enable payment enforcement: delete this
  // block, restore the two commented-out imports above, restore the
  // insertPendingOrder currency/amountMinorUnits above, and uncomment the
  // PAYSTACK PATH block below.
  const { licenseKey, expiresAt } = issueLicenseKey(email);
  const accessGrantToken = randomBytes(24).toString("base64url");

  markOrderAwaitingAccessGrant({
    reference,
    licenseKey,
    licenseExpiresAt: Math.floor(expiresAt.getTime() / 1000),
    accessGrantToken,
  });

  await notifyAccessGrantNeeded({
    reference,
    email,
    githubUsername,
    accessGrantToken,
  });

  return Response.json({
    authorizationUrl: `${configuration.marketingBaseUrl}/checkout/complete?reference=${reference}`,
  });

  // ─── PAYSTACK PATH (disabled during free mode — uncomment to restore) ───────────
  // try {
  //   const { authorizationUrl } = await initializeTransaction({
  //     email,
  //     reference,
  //     amountMinorUnits: configuration.paystack.priceMinorUnits,
  //     currency: configuration.paystack.currency,
  //     callbackUrl: `${configuration.marketingBaseUrl}/checkout/complete?reference=${reference}`,
  //   });
  //
  //   return Response.json({ authorizationUrl });
  // } catch (error) {
  //   markOrderFailed(reference);
  //   console.error("Failed to initialize a Paystack transaction:", error);
  //
  //   return Response.json(
  //     { error: "Could not start checkout. Please try again." },
  //     { status: 502 }
  //   );
  // }
}
