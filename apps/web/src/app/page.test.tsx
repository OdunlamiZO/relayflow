import { beforeEach, describe, expect, it, vi } from "vitest";

// Imported after mocks so the mock is in place.
import {
  getInstanceStatus,
  getServerAuthenticationStatus,
} from "@/lib/server-authentication";

import Home from "./page";

// next/navigation redirect is a throw-based API; mock it as a plain spy.
const mockRedirect = vi.fn();

vi.mock("next/navigation", () => ({
  redirect: (url: string) => {
    mockRedirect(url);
  },
}));

vi.mock("@/lib/server-authentication", () => ({
  getInstanceStatus: vi.fn(),
  getServerAuthenticationStatus: vi.fn(),
}));

const mockGetInstanceStatus = vi.mocked(getInstanceStatus);
const mockGetStatus = vi.mocked(getServerAuthenticationStatus);

beforeEach(() => {
  mockRedirect.mockReset();
  mockGetInstanceStatus.mockReset();
  mockGetStatus.mockReset();
  mockGetInstanceStatus.mockResolvedValue({ bootstrapped: true });
});

describe("Home (root route)", () => {
  it("redirects to /setup when the instance hasn't been bootstrapped yet", async () => {
    mockGetInstanceStatus.mockResolvedValue({ bootstrapped: false });
    mockGetStatus.mockResolvedValue({ authenticated: false });

    await Home();

    expect(mockRedirect).toHaveBeenCalledWith("/setup");
  });

  it("redirects authenticated users to /inbox", async () => {
    mockGetStatus.mockResolvedValue({ authenticated: true });

    await Home();

    expect(mockRedirect).toHaveBeenCalledWith("/inbox");
  });

  it("redirects unauthenticated visitors to /login", async () => {
    mockGetStatus.mockResolvedValue({ authenticated: false });

    await Home();

    expect(mockRedirect).toHaveBeenCalledWith("/login");
  });
});
