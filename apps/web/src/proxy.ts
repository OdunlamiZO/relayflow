import { type NextRequest, NextResponse } from "next/server";

// Spring Security's default session cookie name.
// If you customise it via server.servlet.session.cookie.name, update this.
const SESSION_COOKIE = "JSESSIONID";

export function proxy(request: NextRequest) {
  const { pathname } = request.nextUrl;
  const hasSession = request.cookies.has(SESSION_COOKIE);

  // Fast-path: no cookie at all → definitely not logged in, bounce to login.
  // Do NOT redirect users WITH a cookie away from login/signup here — the cookie
  // may be stale (e.g. after an API server restart) and that would create an
  // infinite loop (inbox → 401 → login → cookie present → inbox → …).
  // Redirecting away from auth pages for live sessions is handled server-side
  // in the (auth) layout via redirectIfAuthenticated(), which actually validates
  // the session against the API.
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
    "/login",
    "/signup",
    "/inbox/:path*",
    "/workflows/:path*",
    "/settings/:path*",
  ],
};
