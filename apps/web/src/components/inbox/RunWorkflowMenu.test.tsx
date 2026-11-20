// Docs: docs/codebase-components.md#runworkflowmenutesttsx
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";

import type { WorkflowMapping } from "@/lib/messaging-api";

import { RunWorkflowMenu } from "./RunWorkflowMenu";

const mockRun = vi.fn();
let workflows: WorkflowMapping[] = [];

vi.mock("@/hooks/use-conversation-agent-workflows", () => ({
  useConversationAgentWorkflows: vi.fn(() => ({ data: workflows })),
}));

vi.mock("@/hooks/use-run-conversation-agent-workflow", () => ({
  useRunConversationAgentWorkflow: vi.fn(() => ({
    mutate: mockRun,
    isPending: false,
    error: null,
    reset: vi.fn(),
  })),
}));

describe("RunWorkflowMenu", () => {
  beforeEach(() => {
    mockRun.mockReset();
  });

  it("renders nothing when the agent has no published workflows", () => {
    workflows = [];

    const { container } = render(
      <RunWorkflowMenu workspaceId="w1" conversationId="c1" />
    );

    expect(container).toBeEmptyDOMElement();
  });

  it("runs the workflow a person picks", async () => {
    workflows = [
      {
        workflowId: "wf1",
        name: "Loan request",
        triggerDescription: "The member wants a loan",
      },
    ];

    render(<RunWorkflowMenu workspaceId="w1" conversationId="c1" />);

    await userEvent.click(
      screen.getByRole("button", { name: /run workflow/i })
    );

    expect(screen.getByText("The member wants a loan")).toBeInTheDocument();

    await userEvent.click(
      screen.getByRole("menuitem", { name: /loan request/i })
    );

    expect(mockRun).toHaveBeenCalledWith("wf1");
  });
});
