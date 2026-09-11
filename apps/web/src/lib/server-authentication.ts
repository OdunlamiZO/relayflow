import { cookies } from "next/headers";
import { redirect } from "next/navigation";

import { ApiError, apiErrorMessage, readResponseBody } from "@/lib/api-client";

// Server-only — do NOT import this from client components.
// Forwards the session cookie to the backend so Server Components can
// check auth status without an extra client round-trip.

type AuthenticationStatus = { authenticated: boolean };
type InstanceStatus = { bootstrapped: boolean };

const defaultBaseUrl =
  process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";

export class ServerAuthenticationClient {
  private readonly baseUrl: string;

  constructor(baseUrl = defaultBaseUrl) {
    this.baseUrl = baseUrl.replace(/\/$/, "");
  }

  /**
   * Checks whether the current request's session is authenticated. Fails safe on any error
   * (network failure, non-2xx, malformed body) — treats the visitor as unauthenticated rather
   * than surfacing the error to a Server Component.
   */
  async getAuthenticationStatus(): Promise<AuthenticationStatus> {
    try {
      return await this.request<AuthenticationStatus>("/auth/me");
    } catch {
      return { authenticated: false };
    }
  }

  /**
   * Checks whether this self-hosted instance has completed initial setup. Fails safe on any
   * error — treats the instance as already bootstrapped so a transient API outage never traps
   * every visitor on `/setup`.
   */
  async getInstanceStatus(): Promise<InstanceStatus> {
    try {
      return await this.request<InstanceStatus>("/auth/bootstrap-status");
    } catch {
      return { bootstrapped: true };
    }
  }

  private async request<T>(path: string): Promise<T> {
    const cookieStore = await cookies();
    const cookieHeader = cookieStore
      .getAll()
      .map((c) => `${c.name}=${c.value}`)
      .join("; ");

    const response = await fetch(`${this.baseUrl}${path}`, {
      headers: { Cookie: cookieHeader },
      cache: "no-store",
    });

    if (!response.ok) {
      const details = await readResponseBody(response);

      throw new ApiError(
        response.status,
        apiErrorMessage(response.status, details),
        details
      );
    }

    return (await response.json()) as T;
  }
}

export const serverAuthenticationApi = new ServerAuthenticationClient();

/** Redirects to /login if the user is not authenticated. */
export async function requireAuthentication(): Promise<void> {
  const { authenticated } =
    await serverAuthenticationApi.getAuthenticationStatus();

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
  const { authenticated } =
    await serverAuthenticationApi.getAuthenticationStatus();

  if (authenticated) {
    redirect(destination);
  }
}

/** Redirects to /setup if this instance hasn't completed initial setup yet. */
export async function redirectIfNotBootstrapped(): Promise<void> {
  const { bootstrapped } = await serverAuthenticationApi.getInstanceStatus();

  if (!bootstrapped) {
    redirect("/setup");
  }
}

/** Redirects to /login if this instance has already completed initial setup. */
export async function redirectIfBootstrapped(): Promise<void> {
  const { bootstrapped } = await serverAuthenticationApi.getInstanceStatus();

  if (bootstrapped) {
    redirect("/login");
  }
}
