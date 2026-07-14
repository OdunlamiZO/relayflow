/**
 * In-memory fixed-window rate limiter, keyed by client IP. Correct as long
 * as this app runs as a single replica (true today, since it has no shared
 * store like Redis by design — see apps/marketing/README.md). Revisit if
 * marketing traffic ever justifies horizontal scaling.
 */

type WindowState = {
  count: number;
  windowStartedAt: number;
};

const windowsByKey = new Map<string, WindowState>();

export function isRateLimited(
  key: string,
  limit: number,
  windowSeconds: number
): boolean {
  const now = Date.now();
  const windowMilliseconds = windowSeconds * 1000;
  const existing = windowsByKey.get(key);

  if (!existing || now - existing.windowStartedAt >= windowMilliseconds) {
    windowsByKey.set(key, { count: 1, windowStartedAt: now });

    return false;
  }

  existing.count += 1;

  return existing.count > limit;
}

export function clientIpFromRequest(request: Request): string {
  const forwardedFor = request.headers.get("x-forwarded-for");

  if (forwardedFor) {
    return forwardedFor.split(",")[0].trim();
  }

  return "unknown";
}
