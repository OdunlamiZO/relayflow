// Docs: docs/codebase-components.md#knowledgesourcepickertesttsx
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";

import type {
  KnowledgeSource,
  KnowledgeSourceFields,
} from "@/lib/messaging-api";

import { KnowledgeSourcePicker } from "./KnowledgeSourcePicker";

const sources: KnowledgeSource[] = [
  {
    id: "source-1",
    name: "rates",
    url: "https://api.example.com/rates",
    headers: [],
    cacheSeconds: 60,
    createdAt: "2026-11-20T10:00:00Z",
    updatedAt: "2026-11-20T10:00:00Z",
  },
];

const fields: KnowledgeSourceFields = {
  fields: [
    { path: "rates.NGN", sample: "1520.5" },
    { path: "[0].city", sample: "Lagos" },
  ],
  error: null,
};

vi.mock("@/hooks/use-knowledge-sources", () => ({
  useKnowledgeSources: vi.fn(() => ({ data: sources, isLoading: false })),
}));

vi.mock("@/hooks/use-knowledge-source-fields", () => ({
  useKnowledgeSourceFields: vi.fn((_workspaceId: string, sourceId) => ({
    data: sourceId ? fields : undefined,
    isLoading: false,
    error: null,
  })),
}));

async function openSource() {
  const onSelect = vi.fn();
  const user = userEvent.setup();
  render(
    <KnowledgeSourcePicker workspaceId="workspace-1" onSelect={onSelect} />
  );

  await user.click(screen.getByTitle("Insert a knowledge source value"));
  await user.click(screen.getByText("rates"));

  return { onSelect, user };
}

describe("KnowledgeSourcePicker", () => {
  it("lists a source's fields with their current values", async () => {
    await openSource();

    expect(screen.getByText("rates.NGN")).toBeInTheDocument();
    expect(screen.getByText("1520.5")).toBeInTheDocument();
  });

  it("gives the full placeholder path for a picked field", async () => {
    const { onSelect, user } = await openSource();

    await user.click(screen.getByText("rates.NGN"));

    expect(onSelect).toHaveBeenCalledWith("sources.rates.rates.NGN");
  });

  it("joins an array path without a dot", async () => {
    const { onSelect, user } = await openSource();

    await user.click(screen.getByText("[0].city"));

    expect(onSelect).toHaveBeenCalledWith("sources.rates[0].city");
  });
});
