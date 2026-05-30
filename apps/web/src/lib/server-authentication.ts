import { cookies } from "next/headers";
import { redirect } from "next/navigation";

// Server-only — do NOT import this from client components.
// Forwards the session cookie to the backend so Server Components can
// check auth status without an extra client round-trip.

const apiBaseUrl =
  process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";

type AuthenticationStatus = { authenticated: boolean; anonymous?: boolean };

export async function getServerAuthenticationStatus(): Promise<AuthenticationStatus> {
  const cookieStore = await cookies();
  const cookieHeader = cookieStore
    .getAll()
    .map((c) => `${c.name}=${c.value}`)
    .join("; ");

  try {
    const res = await fetch(`${apiBaseUrl}/api/auth/me`, {
      headers: { Cookie: cookieHeader },
      cache: "no-store",
    });

    if (!res.ok) {
      return { authenticated: false };
    }

    return (await res.json()) as AuthenticationStatus;
  } catch {
    return { authenticated: false };
  }
}

/** Redirects to /login if the user is not authenticated. */
export async function requireAuthentication(): Promise<void> {
  const { authenticated } = await getServerAuthenticationStatus();

  if (!authenticated) {
    redirect("/login");
  }
}

/**
 * Redirects to {@code destination} (default: /inbox) if the user is already authenticated.
 * Pass a validated returnUrl to preserve the post-login destination.
 */
export async function redirectIfAuthenticated(
  destination = "/inbox"
): Promise<void> {
  const { authenticated } = await getServerAuthenticationStatus();

  if (authenticated) {
    redirect(destination);
  }
}
