import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { QueryProvider } from "@/components/providers/QueryProvider";
import { ToastProvider } from "@/components/providers/ToastProvider";

import Home from "./page";

vi.mock("next/navigation", () => ({
  useRouter: vi.fn(() => ({
    push: vi.fn(),
    replace: vi.fn(),
    prefetch: vi.fn(),
    back: vi.fn(),
    forward: vi.fn(),
  })),
  useSearchParams: vi.fn(() => new URLSearchParams()),
  usePathname: vi.fn(() => "/"),
}));

vi.mock("@/hooks/use-authentication", () => ({
  useAuthentication: vi.fn(() => ({
    user: null,
    isLoading: false,
    isError: false,
    isAuthenticated: false,
    isAnonymous: false,
  })),
}));

vi.mock("@/hooks/use-logout", () => ({
  useLogout: vi.fn(() => ({
    mutate: vi.fn(),
    isPending: false,
  })),
}));

function renderHome() {
  render(
    <QueryProvider>
      <ToastProvider>
        <Home />
      </ToastProvider>
    </QueryProvider>
  );
}

describe("Home", () => {
  it("renders the hero headline", () => {
    renderHome();

    expect(screen.getByRole("heading", { level: 1 })).toHaveTextContent(
      /all your customer conversations/i
    );
  });

  it("renders the open inbox link", () => {
    renderHome();

    expect(
      screen.getByRole("link", { name: /open inbox/i })
    ).toBeInTheDocument();
  });

  it("renders the workflow flow nodes", () => {
    renderHome();

    expect(screen.getByText("Trigger")).toBeInTheDocument();
    expect(screen.getByText("Send reply")).toBeInTheDocument();
    expect(screen.getByText("Condition")).toBeInTheDocument();
  });

  it("renders get started links pointing to signup", () => {
    renderHome();

    const links = screen.getAllByRole("link", { name: /get started/i });

    expect(links.length).toBeGreaterThanOrEqual(1);
    links.forEach((link) => {
      expect(link).toHaveAttribute("href", "/signup");
    });
  });

  it("renders the feature grid", () => {
    renderHome();

    expect(screen.getByText("Omnichannel inbox")).toBeInTheDocument();
    expect(screen.getByText("Visual workflow automation")).toBeInTheDocument();
    expect(screen.getByText("Transparent execution logs")).toBeInTheDocument();
  });

  it("renders nav log in link when unauthenticated", () => {
    renderHome();

    expect(screen.getByRole("link", { name: /log in/i })).toBeInTheDocument();
  });
});
