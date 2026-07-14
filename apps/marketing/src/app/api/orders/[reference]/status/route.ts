import { findOrderByReference } from "@/lib/database";
import { clientIpFromRequest, isRateLimited } from "@/lib/rate-limit";

export const dynamic = "force-dynamic";

export async function GET(
  request: Request,
  { params }: { params: Promise<{ reference: string }> }
) {
  const clientIp = clientIpFromRequest(request);

  if (isRateLimited(`order-status:${clientIp}`, 30, 60)) {
    return Response.json({ error: "Too many requests." }, { status: 429 });
  }

  const { reference } = await params;
  const order = findOrderByReference(reference);

  if (!order) {
    return Response.json({ error: "Not found." }, { status: 404 });
  }

  if (order.status === "paid") {
    return Response.json({
      status: "paid",
      licenseKey: order.licenseKey,
      expiresAt: order.licenseExpiresAt,
    });
  }

  return Response.json({ status: order.status });
}
