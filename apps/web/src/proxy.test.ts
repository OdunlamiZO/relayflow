// @vitest-environment node
import { NextRequest } from "next/server";

import { afterEach, describe, expect, it, vi } from "vitest";

import { apiProxyTarget, proxy } from "./proxy";

describe("proxy", () => {
  afterEach(() => {
    vi.unstubAllEnvs();
  });

  it("maps /backend paths to the internal API, keeping the query string", () => {
    vi.stubEnv("RELAYFLOW_API_INTERNAL_URL", "http://api:8080");

    const request = new NextRequest(
      "https://app.example.com/backend/conversations/1/messages?workspaceId=w1"
    );

    expect(apiProxyTarget(request).toString()).toBe(
      "http://api:8080/conversations/1/messages?workspaceId=w1"
    );
  });

  it("maps /backend itself to the API root", () => {
    vi.stubEnv("RELAYFLOW_API_INTERNAL_URL", "http://api:8080");

    const request = new NextRequest("https://app.example.com/backend");

    expect(apiProxyTarget(request).toString()).toBe("http://api:8080/");
  });

  it("rewrites /backend requests instead of redirecting them", () => {
    vi.stubEnv("RELAYFLOW_API_INTERNAL_URL", "http://api:8080");

    const response = proxy(
      new NextRequest("https://app.example.com/backend/auth/me")
    );

    expect(response.headers.get("x-middleware-rewrite")).toBe(
      "http://api:8080/auth/me"
    );
  });

  it("still sends visitors without a session to the login page", () => {
    const response = proxy(new NextRequest("https://app.example.com/inbox"));

    expect(response.status).toBe(307);
    expect(response.headers.get("location")).toBe(
      "https://app.example.com/login"
    );
  });
});
