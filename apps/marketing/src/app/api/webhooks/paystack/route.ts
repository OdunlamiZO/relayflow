import { randomBytes } from "node:crypto";

import {
  findOrderByReference,
  markOrderAwaitingAccessGrant,
} from "@/lib/database";
import { issueLicenseKey } from "@/lib/license";
import { verifyTransaction, verifyWebhookSignature } from "@/lib/paystack";
import { notifyAccessGrantNeeded } from "@/lib/telegram";

export const dynamic = "force-dynamic";

type PaystackChargeEvent = {
  event: string;
  data: {
    reference: string;
  };
};

export async function POST(request: Request) {
  const rawBody = await request.text();
  const signature = request.headers.get("x-paystack-signature");

  if (!signature || !verifyWebhookSignature(rawBody, signature)) {
    return Response.json({ error: "Invalid signature." }, { status: 401 });
  }

  const payload = JSON.parse(rawBody) as PaystackChargeEvent;

  if (payload.event !== "charge.success") {
    return Response.json({ status: "ignored" });
  }

  const reference = payload.data.reference;
  const order = findOrderByReference(reference);

  if (!order || order.status !== "pending") {
    return Response.json({ status: "ignored" });
  }

  const verification = await verifyTransaction(reference);
  const paymentConfirmed =
    verification.status === "success" &&
    verification.amountMinorUnits === order.amountMinorUnits &&
    verification.currency === order.currency;

  if (!paymentConfirmed) {
    return Response.json({ status: "ignored" });
  }

  const { licenseKey, expiresAt } = issueLicenseKey(order.email);
  const accessGrantToken = randomBytes(24).toString("base64url");

  const transitioned = markOrderAwaitingAccessGrant({
    reference,
    licenseKey,
    licenseExpiresAt: Math.floor(expiresAt.getTime() / 1000),
    accessGrantToken,
  });

  // Notify, don't email — the key isn't delivered until access is granted.
  if (transitioned) {
    await notifyAccessGrantNeeded({
      reference,
      email: order.email,
      githubUsername: order.githubUsername,
      accessGrantToken,
    });
  }

  return Response.json({ status: "processed" });
}
