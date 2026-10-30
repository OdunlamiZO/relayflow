// Docs: docs/codebase-components.md#server-messaging-apitestts
import { afterEach, describe, expect, it, vi } from "vitest";

import { ServerMessagingApiClient } from "./server-messaging-api";

vi.mock("next/headers", () => ({
  cookies: async () => ({
    getAll: () => [
      { name: "SESSION", value: "session-1" },
      { name: "theme", value: "dark" },
    ],
  }),
}));

describe("ServerMessagingApiClient", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("lists workspaces with the request's cookies forwarded", async () => {
    const fetchMock = vi.spyOn(globalThis, "fetch").mockResolvedValue(
      new Response(JSON.stringify([{ id: "workspace-1", name: "RelayFlow" }]), {
        status: 200,
      })
    );

    const client = new ServerMessagingApiClient("http://api:8080/");
    const workspaces = await client.listWorkspaces();

    expect(workspaces).toEqual([{ id: "workspace-1", name: "RelayFlow" }]);
    expect(fetchMock).toHaveBeenCalledWith("http://api:8080/workspaces", {
      headers: { Cookie: "SESSION=session-1; theme=dark" },
      cache: "no-store",
    });
  });

  it("returns no workspaces when the request fails", async () => {
    vi.spyOn(globalThis, "fetch").mockResolvedValue(
      new Response("", { status: 401 })
    );

    const client = new ServerMessagingApiClient("http://api:8080");

    await expect(client.listWorkspaces()).resolves.toEqual([]);
  });

  it("returns no invite preview when the request fails", async () => {
    vi.spyOn(globalThis, "fetch").mockRejectedValue(new Error("offline"));

    const client = new ServerMessagingApiClient("http://api:8080");

    await expect(client.getInvitePreview("token-1")).resolves.toBeNull();
  });
});
