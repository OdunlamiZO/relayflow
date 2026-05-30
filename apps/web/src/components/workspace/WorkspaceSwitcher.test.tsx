import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";

import { WorkspaceSwitcher } from "./WorkspaceSwitcher";

const mockPush = vi.fn();

vi.mock("next/navigation", () => ({
  useRouter: vi.fn(() => ({ push: mockPush })),
}));

const mockCreateWorkspace = vi.fn();

vi.mock("@/hooks/use-create-workspace", () => ({
  useCreateWorkspace: vi.fn(() => ({
    mutate: mockCreateWorkspace,
    isPending: false,
  })),
}));

vi.mock("@/hooks/use-workspaces", () => ({
  useWorkspaces: vi.fn(() => ({
    data: [
      { id: "ws-1", name: "Support", createdAt: "2026-01-01T00:00:00Z" },
      { id: "ws-2", name: "Sales", createdAt: "2026-01-02T00:00:00Z" },
    ],
  })),
}));

function renderSwitcher(workspaceId = "ws-1") {
  return render(<WorkspaceSwitcher workspaceId={workspaceId} />);
}

describe("WorkspaceSwitcher", () => {
  it("shows the current workspace name", () => {
    renderSwitcher("ws-1");

    expect(screen.getByText("Support")).toBeInTheDocument();
  });

  it("opens the dropdown when clicked", async () => {
    renderSwitcher("ws-1");

    await userEvent.click(
      screen.getByRole("button", { name: /switch workspace/i })
    );

    const listbox = screen.getByRole("listbox", { name: /workspaces/i });

    expect(listbox).toBeInTheDocument();
    expect(
      screen.getByRole("option", { name: /support/i })
    ).toBeInTheDocument();
    expect(screen.getByRole("option", { name: /sales/i })).toBeInTheDocument();
  });

  it("navigates to selected workspace", async () => {
    renderSwitcher("ws-1");

    await userEvent.click(
      screen.getByRole("button", { name: /switch workspace/i })
    );
    await userEvent.click(screen.getByRole("option", { name: /sales/i }));

    expect(mockPush).toHaveBeenCalledWith("/inbox?workspaceId=ws-2");
  });

  it("does not navigate when the current workspace is re-selected", async () => {
    mockPush.mockClear();
    renderSwitcher("ws-1");

    await userEvent.click(
      screen.getByRole("button", { name: /switch workspace/i })
    );
    await userEvent.click(screen.getByRole("option", { name: /support/i }));

    expect(mockPush).not.toHaveBeenCalled();
  });

  it("shows the create form when New workspace is clicked", async () => {
    renderSwitcher("ws-1");

    await userEvent.click(
      screen.getByRole("button", { name: /switch workspace/i })
    );
    await userEvent.click(
      screen.getByRole("button", { name: /new workspace/i })
    );

    expect(screen.getByPlaceholderText(/workspace name/i)).toBeInTheDocument();
  });

  it("calls createWorkspace with the entered name", async () => {
    mockCreateWorkspace.mockImplementation(
      (
        _req: unknown,
        { onSuccess }: { onSuccess: (ws: { id: string }) => void }
      ) => {
        onSuccess({ id: "ws-3" });
      }
    );

    renderSwitcher("ws-1");

    await userEvent.click(
      screen.getByRole("button", { name: /switch workspace/i })
    );
    await userEvent.click(
      screen.getByRole("button", { name: /new workspace/i })
    );
    await userEvent.type(
      screen.getByPlaceholderText(/workspace name/i),
      "Engineering"
    );
    await userEvent.click(screen.getByRole("button", { name: /^create$/i }));

    await waitFor(() => {
      expect(mockCreateWorkspace).toHaveBeenCalledWith(
        { name: "Engineering" },
        expect.any(Object)
      );
    });

    expect(mockPush).toHaveBeenCalledWith("/inbox?workspaceId=ws-3");
  });

  it("closes the dropdown on Escape", async () => {
    renderSwitcher("ws-1");

    await userEvent.click(
      screen.getByRole("button", { name: /switch workspace/i })
    );

    expect(screen.getByRole("listbox")).toBeInTheDocument();

    await userEvent.keyboard("{Escape}");

    expect(screen.queryByRole("listbox")).not.toBeInTheDocument();
  });
});
