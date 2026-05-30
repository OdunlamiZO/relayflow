import { beforeEach, describe, expect, it, vi } from "vitest";

// Imported after mocks so the mock is in place.
import { getServerAuthenticationStatus } from "@/lib/server-authentication";

import Home from "./page";

// next/navigation redirect is a throw-based API; mock it as a plain spy.
const mockRedirect = vi.fn();

vi.mock("next/navigation", () => ({
  redirect: (url: string) => {
    mockRedirect(url);
  },
}));

vi.mock("@/lib/server-authentication", () => ({
  getServerAuthenticationStatus: vi.fn(),
}));

// TryItButton is a client component that uses hooks — stub it out for SSR tests.
vi.mock("@/components/landing/TryItButton", () => ({
  TryItButton: () => null,
}));

const mockGetStatus = vi.mocked(getServerAuthenticationStatus);

beforeEach(() => {
  mockRedirect.mockReset();
  mockGetStatus.mockReset();
});

describe("Home (root route)", () => {
  it("redirects authenticated users to /inbox", async () => {
    mockGetStatus.mockResolvedValue({ authenticated: true });

    await Home();

    expect(mockRedirect).toHaveBeenCalledWith("/inbox");
  });

  it("redirects anonymous users to /inbox", async () => {
    mockGetStatus.mockResolvedValue({ authenticated: true, anonymous: true });

    await Home();

    expect(mockRedirect).toHaveBeenCalledWith("/inbox");
  });

  it("renders the landing page for unauthenticated visitors (no redirect)", async () => {
    mockGetStatus.mockResolvedValue({ authenticated: false });

    await Home();

    expect(mockRedirect).not.toHaveBeenCalled();
  });
});
