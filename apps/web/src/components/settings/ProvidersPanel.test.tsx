// Docs: docs/codebase-components.md#providerspaneltesttsx
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";

import type { ProviderSettings } from "@/lib/messaging-api";

import { ProvidersPanel } from "./ProvidersPanel";

const mockUpdate = vi.fn();

const settings: ProviderSettings = {
  chatModel: {
    selected: null,
    instanceDefault: "ANTHROPIC",
    providers: [
      {
        id: "ANTHROPIC",
        displayName: "Anthropic",
        configurationProblem: null,
        model: null,
        instanceModel: "claude-haiku-4-5",
      },
      {
        id: "OPENAI_COMPATIBLE",
        displayName: "OpenAI-compatible",
        configurationProblem: null,
        model: null,
        instanceModel: null,
      },
    ],
  },
  transcription: {
    selected: "WHISPER_LOCAL",
    instanceDefault: null,
    providers: [],
  },
  storage: {
    selected: null,
    instanceDefault: null,
    providers: [
      {
        id: "LOCAL_DISK",
        displayName: "Local disk",
        configurationProblem: null,
        model: null,
        instanceModel: null,
      },
    ],
  },
};

vi.mock("@/hooks/use-provider-settings", () => ({
  useProviderSettings: vi.fn(() => ({ data: settings, isLoading: false })),
}));

vi.mock("@/hooks/use-update-provider-settings", () => ({
  useUpdateProviderSettings: vi.fn(() => ({
    mutate: mockUpdate,
    isPending: false,
  })),
}));

vi.mock("@/components/providers/ToastProvider", () => ({
  useToast: vi.fn(() => ({ showToast: vi.fn() })),
}));

describe("ProvidersPanel", () => {
  it("warns when a provider type has nothing chosen", () => {
    render(<ProvidersPanel workspaceId="workspace-1" />);

    expect(
      screen.getByText(
        "No storage provider is chosen, so recordings aren't kept."
      )
    ).toBeInTheDocument();
  });

  it("marks the instance default and shows its model as the placeholder", () => {
    render(<ProvidersPanel workspaceId="workspace-1" />);

    expect(
      screen.getByRole("radio", { name: "Instance default (Anthropic)" })
    ).toHaveAttribute("aria-checked", "true");
    expect(screen.getByLabelText("Model for Anthropic")).toHaveAttribute(
      "placeholder",
      "claude-haiku-4-5"
    );
  });

  it("flags a plugin whose settings are missing", () => {
    settings.chatModel.providers[0].configurationProblem =
      "Set ANTHROPIC_API_KEY to use the Anthropic provider";

    render(<ProvidersPanel workspaceId="workspace-1" />);

    expect(
      screen.getByRole("radio", { name: "Anthropic (not configured)" })
    ).toBeInTheDocument();
    expect(
      screen.getByText(
        "Anthropic isn't ready: Set ANTHROPIC_API_KEY to use the Anthropic provider. Until then, the AI agent can't reply."
      )
    ).toBeInTheDocument();

    settings.chatModel.providers[0].configurationProblem = null;
  });

  it("flags a chosen plugin that isn't installed and an empty provider type", () => {
    render(<ProvidersPanel workspaceId="workspace-1" />);

    expect(
      screen.getByRole("radio", { name: "WHISPER_LOCAL (not installed)" })
    ).toHaveAttribute("aria-checked", "true");
    expect(
      screen.getByText(
        "No transcription plugin is installed, so voice messages wait for a person."
      )
    ).toBeInTheDocument();
  });

  it("saves the chosen provider and its model", async () => {
    render(<ProvidersPanel workspaceId="workspace-1" />);

    await userEvent.click(
      screen.getByRole("radio", { name: "OpenAI-compatible" })
    );
    await userEvent.type(
      screen.getByLabelText("Model for OpenAI-compatible"),
      "llama-3.3-70b"
    );
    await userEvent.click(screen.getByRole("button", { name: "Save" }));

    expect(mockUpdate).toHaveBeenCalledWith(
      {
        llmProvider: "OPENAI_COMPATIBLE",
        llmModels: { OPENAI_COMPATIBLE: "llama-3.3-70b" },
        transcriptionProvider: "WHISPER_LOCAL",
        transcriptionModels: {},
        storageProvider: null,
      },
      expect.anything()
    );
  });
});
