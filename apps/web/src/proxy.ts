// Docs: docs/codebase-components.md#proxyts
import { type NextRequest, NextResponse } from "next/server";

import { BROWSER_API_BASE_URL, serverApiBaseUrl } from "@/lib/api-base-url";

// Spring Security's default session cookie name; update it if server.servlet.session.cookie.name changes.
const SESSION_COOKIE = "JSESSIONID";

export function apiProxyTarget(request: NextRequest): URL {
  const path =
    request.nextUrl.pathname.slice(BROWSER_API_BASE_URL.length) || "/";

  return new URL(`${serverApiBaseUrl()}${path}${request.nextUrl.search}`);
}

export function proxy(request: NextRequest) {
  const { pathname } = request.nextUrl;

  if (
    pathname === BROWSER_API_BASE_URL ||
    pathname.startsWith(`${BROWSER_API_BASE_URL}/`)
  ) {
    return NextResponse.rewrite(apiProxyTarget(request));
  }

  const hasSession = request.cookies.has(SESSION_COOKIE);

  // Only bounces visitors without a cookie; redirectIfAuthenticated() validates a stale cookie server-side to avoid a redirect loop.
  if (
    !hasSession &&
    (pathname.startsWith("/inbox") ||
      pathname.startsWith("/workflows") ||
      pathname.startsWith("/settings"))
  ) {
    return NextResponse.redirect(new URL("/login", request.url));
  }

  return NextResponse.next();
}

export const config = {
  matcher: [
    "/backend/:path*",
    "/login",
    "/signup",
    "/inbox/:path*",
    "/workflows/:path*",
    "/settings/:path*",
  ],
};
