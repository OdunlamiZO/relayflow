import { render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { DesktopOnly } from "./DesktopOnly";

function mockScreenWidth(isDesktop: boolean) {
  vi.stubGlobal(
    "matchMedia",
    vi.fn(() => ({
      matches: isDesktop,
      addEventListener: vi.fn(),
      removeEventListener: vi.fn(),
    }))
  );
}

function renderDesktopOnly() {
  return render(
    <DesktopOnly
      title="Needs a bigger screen"
      message="Open RelayFlow on a computer."
      back={{ href: "/workflows", label: "Back to workflows" }}
    >
      <p>Editor</p>
    </DesktopOnly>
  );
}

describe("DesktopOnly", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("shows the content on wide screens", () => {
    mockScreenWidth(true);

    renderDesktopOnly();

    expect(screen.getByText("Editor")).toBeInTheDocument();
    expect(screen.queryByText("Needs a bigger screen")).not.toBeInTheDocument();
  });

  it("shows the message and back link instead of the content on narrow screens", () => {
    mockScreenWidth(false);

    renderDesktopOnly();

    expect(screen.queryByText("Editor")).not.toBeInTheDocument();
    expect(screen.getByText("Needs a bigger screen")).toBeInTheDocument();
    expect(
      screen.getByRole("link", { name: "Back to workflows" })
    ).toHaveAttribute("href", "/workflows");
  });
});
