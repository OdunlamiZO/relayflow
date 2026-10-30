// Docs: docs/codebase-components.md#server-messaging-apits
import { cookies } from "next/headers";

import { serverApiBaseUrl } from "@/lib/api-base-url";
import { ApiError, apiErrorMessage, readResponseBody } from "@/lib/api-client";
import { type InvitePreview, type Workspace } from "@/lib/messaging-api";

export class ServerMessagingApiClient {
  private readonly baseUrl: string;

  constructor(baseUrl = serverApiBaseUrl()) {
    this.baseUrl = baseUrl.replace(/\/$/, "");
  }

  async listWorkspaces(): Promise<Workspace[]> {
    try {
      return await this.request<Workspace[]>("/workspaces");
    } catch {
      return [];
    }
  }

  async getInvitePreview(token: string): Promise<InvitePreview | null> {
    try {
      return await this.request<InvitePreview>(
        `/invites/${encodeURIComponent(token)}`
      );
    } catch {
      return null;
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

export const serverMessagingApi = new ServerMessagingApiClient();
