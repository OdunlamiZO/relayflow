import { type NextRequest, NextResponse } from "next/server";

// Spring Security's default session cookie name.
// If you customise it via server.servlet.session.cookie.name, update this.
const SESSION_COOKIE = "JSESSIONID";

export function proxy(request: NextRequest) {
  const { pathname } = request.nextUrl;
  const hasSession = request.cookies.has(SESSION_COOKIE);

  // Authenticated users don't need the landing page or auth pages.
  if (
    hasSession &&
    (pathname === "/" || pathname === "/login" || pathname === "/signup")
  ) {
    return NextResponse.redirect(new URL("/inbox", request.url));
  }

  // Unauthenticated users can't access the inbox.
  // The inbox layout also calls requireAuthentication() as a belt-and-suspenders
  // server-side check that validates the session against the API.
  if (!hasSession && pathname.startsWith("/inbox")) {
    return NextResponse.redirect(new URL("/login", request.url));
  }

  return NextResponse.next();
}

export const config = {
  matcher: ["/", "/login", "/signup", "/inbox/:path*"],
};
