import { randomBytes } from "node:crypto";

import { configuration } from "@/lib/configuration";
import { insertPendingOrder, markOrderFailed } from "@/lib/database";
import { githubUserExists } from "@/lib/github";
import { initializeTransaction } from "@/lib/paystack";
import { clientIpFromRequest, isRateLimited } from "@/lib/rate-limit";

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
    currency: configuration.paystack.currency,
    amountMinorUnits: configuration.paystack.priceMinorUnits,
  });

  try {
    const { authorizationUrl } = await initializeTransaction({
      email,
      reference,
      amountMinorUnits: configuration.paystack.priceMinorUnits,
      currency: configuration.paystack.currency,
      callbackUrl: `${configuration.marketingBaseUrl}/checkout/complete?reference=${reference}`,
    });

    return Response.json({ authorizationUrl });
  } catch (error) {
    markOrderFailed(reference);
    console.error("Failed to initialize a Paystack transaction:", error);

    return Response.json(
      { error: "Could not start checkout. Please try again." },
      { status: 502 }
    );
  }
}
