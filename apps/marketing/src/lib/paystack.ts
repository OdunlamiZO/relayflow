import { createHmac, timingSafeEqual } from "node:crypto";

import { configuration } from "@/lib/configuration";

const PAYSTACK_API_BASE_URL = "https://api.paystack.co";

type InitializeTransactionResult = {
  authorizationUrl: string;
};

export async function initializeTransaction(input: {
  email: string;
  reference: string;
  amountMinorUnits: number;
  currency: string;
  callbackUrl: string;
}): Promise<InitializeTransactionResult> {
  const response = await fetch(
    `${PAYSTACK_API_BASE_URL}/transaction/initialize`,
    {
      method: "POST",
      headers: {
        Authorization: `Bearer ${configuration.paystack.secretKey}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        email: input.email,
        amount: String(input.amountMinorUnits),
        currency: input.currency,
        reference: input.reference,
        channels: ["card"],
        callback_url: input.callbackUrl,
      }),
    }
  );

  if (!response.ok) {
    throw new Error(
      `Paystack transaction initialization failed with status ${response.status}`
    );
  }

  const body = (await response.json()) as {
    data: { authorization_url: string };
  };

  return { authorizationUrl: body.data.authorization_url };
}

export type VerifyTransactionResult = {
  status: string;
  amountMinorUnits: number;
  currency: string;
};

export async function verifyTransaction(
  reference: string
): Promise<VerifyTransactionResult> {
  const response = await fetch(
    `${PAYSTACK_API_BASE_URL}/transaction/verify/${encodeURIComponent(reference)}`,
    {
      headers: {
        Authorization: `Bearer ${configuration.paystack.secretKey}`,
      },
    }
  );

  if (!response.ok) {
    throw new Error(
      `Paystack transaction verification failed with status ${response.status}`
    );
  }

  const body = (await response.json()) as {
    data: { status: string; amount: number; currency: string };
  };

  return {
    status: body.data.status,
    amountMinorUnits: body.data.amount,
    currency: body.data.currency,
  };
}

/**
 * Verifies the `x-paystack-signature` header: HMAC-SHA512 over the raw
 * request body, keyed by the Paystack secret key (the same key used for the
 * Bearer auth header — Paystack has no separate webhook secret). Must be
 * called with the exact raw body string, never a re-serialized object.
 */
export function verifyWebhookSignature(
  rawBody: string,
  signatureHeader: string
): boolean {
  const expectedSignature = createHmac(
    "sha512",
    configuration.paystack.secretKey
  )
    .update(rawBody, "utf8")
    .digest("hex");

  const expectedBuffer = Buffer.from(expectedSignature, "utf8");
  const actualBuffer = Buffer.from(signatureHeader, "utf8");

  if (expectedBuffer.length !== actualBuffer.length) {
    return false;
  }

  return timingSafeEqual(expectedBuffer, actualBuffer);
}
