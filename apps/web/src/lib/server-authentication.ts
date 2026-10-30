// Docs: docs/codebase-components.md#server-authenticationts
import { cookies } from "next/headers";
import { redirect } from "next/navigation";

import { serverApiBaseUrl } from "@/lib/api-base-url";
import { ApiError, apiErrorMessage, readResponseBody } from "@/lib/api-client";

// Server-only; do not import from client components.

type AuthenticationStatus = { authenticated: boolean };
type InstanceStatus = { bootstrapped: boolean };

export class ServerAuthenticationClient {
  private readonly baseUrl: string;

  constructor(baseUrl = serverApiBaseUrl()) {
    this.baseUrl = baseUrl.replace(/\/$/, "");
  }

  /** Treats the visitor as unauthenticated on any error. */
  async getAuthenticationStatus(): Promise<AuthenticationStatus> {
    try {
      return await this.request<AuthenticationStatus>("/auth/me");
    } catch {
      return { authenticated: false };
    }
  }

  /** Treats the instance as bootstrapped on any error so an API outage never traps visitors on /setup. */
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
      .map((requestCookie) => `${requestCookie.name}=${requestCookie.value}`)
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

export async function requireAuthentication(): Promise<void> {
  const { authenticated } =
    await serverAuthenticationApi.getAuthenticationStatus();

  if (!authenticated) {
    redirect("/login");
  }
}

export async function redirectIfAuthenticated(
  destination = "/inbox"
): Promise<void> {
  const { authenticated } =
    await serverAuthenticationApi.getAuthenticationStatus();

  if (authenticated) {
    redirect(destination);
  }
}

export async function redirectIfNotBootstrapped(): Promise<void> {
  const { bootstrapped } = await serverAuthenticationApi.getInstanceStatus();

  if (!bootstrapped) {
    redirect("/setup");
  }
}

export async function redirectIfBootstrapped(): Promise<void> {
  const { bootstrapped } = await serverAuthenticationApi.getInstanceStatus();

  if (bootstrapped) {
    redirect("/login");
  }
}
