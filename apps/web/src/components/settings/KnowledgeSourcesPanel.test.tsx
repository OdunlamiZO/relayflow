// Docs: docs/codebase-components.md#knowledgesourcespaneltesttsx
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";

import type {
  KnowledgeSource,
  TestKnowledgeSourceResponse,
} from "@/lib/messaging-api";

import { KnowledgeSourcesPanel } from "./KnowledgeSourcesPanel";

const mockSave = vi.fn();

const mockTest = vi.fn();

let testResult: TestKnowledgeSourceResponse | undefined;

const sources: KnowledgeSource[] = [
  {
    id: "source-1",
    name: "rates",
    url: "https://api.example.com/rates",
    headers: [{ key: "Authorization", value: "Bearer {{secrets.RATES_KEY}}" }],
    cacheSeconds: 60,
    createdAt: "2026-11-20T10:00:00Z",
    updatedAt: "2026-11-20T10:00:00Z",
  },
];

vi.mock("@/hooks/use-knowledge-sources", () => ({
  useKnowledgeSources: vi.fn(() => ({ data: sources, isLoading: false })),
}));

vi.mock("@/hooks/use-save-knowledge-source", () => ({
  useSaveKnowledgeSource: vi.fn(() => ({ mutate: mockSave, isPending: false })),
}));

vi.mock("@/hooks/use-delete-knowledge-source", () => ({
  useDeleteKnowledgeSource: vi.fn(() => ({
    mutate: vi.fn(),
    isPending: false,
  })),
}));

vi.mock("@/hooks/use-secrets", () => ({
  useSecrets: vi.fn(() => ({
    data: [{ id: "secret-1", name: "FEES_KEY" }],
  })),
}));

vi.mock("@/hooks/use-test-knowledge-source", () => ({
  useTestKnowledgeSource: vi.fn(() => ({
    mutate: mockTest,
    isPending: false,
    data: testResult,
    error: null,
  })),
}));

describe("KnowledgeSourcesPanel", () => {
  it("lists each source with how often it's fetched", () => {
    render(<KnowledgeSourcesPanel workspaceId="workspace-1" />);

    expect(screen.getByText("rates")).toBeInTheDocument();
    expect(
      screen.getByText("Fetched at most every 60 seconds")
    ).toBeInTheDocument();
  });

  it("saves a new source without blank headers", async () => {
    const user = userEvent.setup();
    render(<KnowledgeSourcesPanel workspaceId="workspace-1" />);

    await user.click(screen.getByRole("button", { name: /New source/ }));
    await user.type(screen.getByLabelText("Name"), "fees");
    await user.type(
      screen.getByLabelText("URL"),
      "https://api.example.com/fees"
    );
    await user.click(screen.getByRole("button", { name: "+ Add header" }));
    await user.click(screen.getByRole("button", { name: "Create" }));

    expect(mockSave).toHaveBeenCalledWith(
      {
        name: "fees",
        url: "https://api.example.com/fees",
        headers: [],
        cacheSeconds: 60,
      },
      expect.anything()
    );
  });

  it("inserts a secret into a header value from the picker", async () => {
    const user = userEvent.setup();
    render(<KnowledgeSourcesPanel workspaceId="workspace-1" />);

    await user.click(screen.getByRole("button", { name: /New source/ }));
    await user.click(screen.getByRole("button", { name: "+ Add header" }));
    await user.type(screen.getByLabelText("Header 1 value"), "Bearer ");
    await user.click(screen.getByTitle("Insert variable"));
    await user.click(screen.getByText("{{secrets.FEES_KEY}}"));

    expect(screen.getByLabelText("Header 1 value")).toHaveValue(
      "Bearer {{secrets.FEES_KEY}}"
    );
  });

  it("shows the test response so a field can be picked", async () => {
    testResult = { statusCode: 200, body: '{\n  "usd" : 1500\n}', error: null };
    const user = userEvent.setup();
    render(<KnowledgeSourcesPanel workspaceId="workspace-1" />);

    await user.click(screen.getByRole("button", { name: "Edit rates" }));
    await user.click(screen.getByRole("button", { name: "Test" }));

    expect(mockTest).toHaveBeenCalledWith({
      url: "https://api.example.com/rates",
      headers: [
        { key: "Authorization", value: "Bearer {{secrets.RATES_KEY}}" },
      ],
    });
    expect(screen.getByText(/"usd" : 1500/)).toBeInTheDocument();
    expect(
      screen.getByText(
        "HTTP 200. Use a field below as {{sources.rates.field}}."
      )
    ).toBeInTheDocument();

    testResult = undefined;
  });
});
