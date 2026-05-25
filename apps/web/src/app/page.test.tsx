import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import Home from "./page";

describe("Home", () => {
  it("renders the product direction", () => {
    render(<Home />);

    expect(
      screen.getByRole("heading", {
        name: /channel-agnostic messaging automation/i,
      })
    ).toBeInTheDocument();
    expect(
      screen.getByText(/telegram is the pilot adapter/i)
    ).toBeInTheDocument();
  });
});
