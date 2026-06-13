import { afterEach, describe, expect, it, vi } from "vitest";

import { ApiError, MessagingApiClient } from "./messaging-api";

describe("MessagingApiClient", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("creates a workspace with session credentials", async () => {
    const fetchMock = vi.spyOn(globalThis, "fetch").mockResolvedValue(
      new Response(
        JSON.stringify({
          id: "workspace-1",
          name: "RelayFlow",
          createdAt: "2026-05-26T10:00:00Z",
        }),
        {
          headers: {
            "Content-Type": "application/json",
          },
          status: 201,
        }
      )
    );

    const client = new MessagingApiClient("http://localhost:8080/");
    const workspace = await client.createWorkspace({ name: "RelayFlow" });

    expect(workspace.name).toBe("RelayFlow");
    expect(fetchMock).toHaveBeenCalledWith("http://localhost:8080/workspaces", {
      body: JSON.stringify({ name: "RelayFlow" }),
      credentials: "include",
      headers: {
        "Content-Type": "application/json",
      },
      method: "POST",
    });
  });

  it("builds message list requests with the workspace query parameter", async () => {
    const fetchMock = vi.spyOn(globalThis, "fetch").mockResolvedValue(
      new Response(JSON.stringify([]), {
        headers: {
          "Content-Type": "application/json",
        },
        status: 200,
      })
    );

    const client = new MessagingApiClient("http://localhost:8080");

    await client.listMessages("workspace 1", "conversation-1");

    expect(fetchMock).toHaveBeenCalledWith(
      "http://localhost:8080/conversations/conversation-1/messages?workspaceId=workspace%201",
      {
        credentials: "include",
        headers: undefined,
        method: "GET",
        body: undefined,
      }
    );
  });

  it("throws API errors with backend error details", async () => {
    vi.spyOn(globalThis, "fetch").mockResolvedValue(
      new Response(JSON.stringify({ message: "Conversation not found" }), {
        headers: {
          "Content-Type": "application/json",
        },
        status: 404,
      })
    );

    const client = new MessagingApiClient("http://localhost:8080");

    await expect(
      client.getConversation("workspace-1", "missing")
    ).rejects.toMatchObject({
      details: { message: "Conversation not found" },
      message: "Conversation not found",
      name: "ApiError",
      status: 404,
    } satisfies Partial<ApiError>);
  });
});
