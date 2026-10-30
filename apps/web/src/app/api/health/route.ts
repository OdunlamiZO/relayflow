// Docs: docs/codebase-components.md#health-route

export const dynamic = "force-dynamic";

export function GET() {
  return Response.json({ status: "ok" });
}
