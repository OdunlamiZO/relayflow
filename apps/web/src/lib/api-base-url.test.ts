// @vitest-environment node
import { afterEach, describe, expect, it, vi } from "vitest";

import {
  BROWSER_API_BASE_URL,
  apiBaseUrl,
  serverApiBaseUrl,
} from "./api-base-url";

describe("api base URL", () => {
  afterEach(() => {
    vi.unstubAllEnvs();
    vi.unstubAllGlobals();
  });

  it("uses the internal API URL on the server, without a trailing slash", () => {
    vi.stubEnv("RELAYFLOW_API_INTERNAL_URL", "http://api:8080/");

    expect(serverApiBaseUrl()).toBe("http://api:8080");
    expect(apiBaseUrl()).toBe("http://api:8080");
  });

  it("falls back to the local API when the internal URL isn't set", () => {
    vi.stubEnv("RELAYFLOW_API_INTERNAL_URL", undefined);

    expect(serverApiBaseUrl()).toBe("http://localhost:8080");
  });

  it("uses the same-origin proxy path in the browser", () => {
    vi.stubGlobal("window", {});

    expect(apiBaseUrl()).toBe(BROWSER_API_BASE_URL);
    expect(BROWSER_API_BASE_URL).toBe("/backend");
  });
});
