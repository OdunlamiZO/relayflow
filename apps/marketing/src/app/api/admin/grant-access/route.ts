import { grantAccessAndCompleteOrder } from "@/lib/database";
import { sendLicenseKeyEmail } from "@/lib/email";

export const dynamic = "force-dynamic";

// GET so it works as a one-click Telegram link. The token itself is the
// auth — no login.
export async function GET(request: Request) {
  const token = new URL(request.url).searchParams.get("token");

  if (!token) {
    return new Response("Missing token.", { status: 400 });
  }

  const order = grantAccessAndCompleteOrder(token);

  if (!order || !order.licenseKey || order.licenseExpiresAt === null) {
    return new Response("This link is invalid or has already been used.", {
      status: 404,
    });
  }

  await sendLicenseKeyEmail({
    to: order.email,
    licenseKey: order.licenseKey,
    expiresAt: new Date(order.licenseExpiresAt * 1000),
  });

  return new Response(
    `Access granted for ${order.email} (order ${order.reference}). ` +
      "The license key has been emailed to them.",
    { status: 200 }
  );
}
