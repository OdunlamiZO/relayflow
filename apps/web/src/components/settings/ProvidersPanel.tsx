"use client";

// Docs: docs/codebase-components.md#providerspanel
import { useId, useState } from "react";

import { LoadingButton } from "@/components/common/LoadingButton";
import { Spinner } from "@/components/common/Spinner";
import { useToast } from "@/components/providers/ToastProvider";
import { useProviderSettings } from "@/hooks/use-provider-settings";
import { useUpdateProviderSettings } from "@/hooks/use-update-provider-settings";
import { errorMessage } from "@/lib/error-message";
import {
  type ProviderSettings,
  type ProviderTypeSettings,
  type UpdateProviderSettingsRequest,
} from "@/lib/messaging-api";

type Props = {
  workspaceId: string;
};

export function ProvidersPanel({ workspaceId }: Props) {
  const { data: settings, isLoading } = useProviderSettings(workspaceId);
  const updateProviderSettings = useUpdateProviderSettings(workspaceId);
  const { showToast } = useToast();

  const [edited, setEdited] = useState<UpdateProviderSettingsRequest | null>(
    null
  );

  if (isLoading || !settings) {
    return (
      <div className="flex justify-center py-16">
        <Spinner size="lg" />
      </div>
    );
  }

  const saved = toRequest(settings);
  const draft = edited ?? saved;
  const isUnchanged = JSON.stringify(draft) === JSON.stringify(saved);

  function patch(next: Partial<UpdateProviderSettingsRequest>) {
    setEdited({ ...draft, ...next });
  }

  function handleSave() {
    updateProviderSettings.mutate(draft, {
      onSuccess: () => {
        setEdited(null);
        showToast({ kind: "success", message: "Providers saved" });
      },
      onError: (error) => {
        showToast({ kind: "error", message: errorMessage(error) });
      },
    });
  }

  return (
    <div className="mx-auto max-w-2xl px-6 py-8 sm:px-8">
      <div className="mb-8">
        <h1 className="text-xl font-bold text-primary">Providers</h1>
        <p className="mt-1.5 text-sm text-neutral-500">
          Choose the plugins this workspace uses. Anything left on the instance
          default follows the server&apos;s setting. Plugins are installed on
          the server.
        </p>
      </div>

      <ProviderTypeSection
        title="Chat model"
        description="The LLM behind the AI agent. An agent can still pick its own in AI Agent settings."
        pluginType="chat model"
        consequence="the AI agent can't reply"
        settings={settings.chatModel}
        selected={draft.llmProvider}
        models={draft.llmModels}
        onSelect={(llmProvider) => patch({ llmProvider })}
        onModelChange={(llmModels) => patch({ llmModels })}
      />

      <ProviderTypeSection
        title="Transcription"
        description="Turns voice messages into text. A voice message that isn't transcribed waits for a person."
        pluginType="transcription"
        consequence="voice messages wait for a person"
        settings={settings.transcription}
        selected={draft.transcriptionProvider}
        models={draft.transcriptionModels}
        onSelect={(transcriptionProvider) => patch({ transcriptionProvider })}
        onModelChange={(transcriptionModels) => patch({ transcriptionModels })}
      />

      <ProviderTypeSection
        title="Storage"
        description="Where voice message recordings are kept. Recordings already saved stay with the plugin that saved them."
        pluginType="storage"
        consequence="recordings aren't kept"
        settings={settings.storage}
        selected={draft.storageProvider}
        onSelect={(storageProvider) => patch({ storageProvider })}
      />

      <div className="mt-8 flex justify-end">
        <LoadingButton
          type="button"
          onClick={handleSave}
          isLoading={updateProviderSettings.isPending}
          disabled={isUnchanged}
          className="rounded-lg bg-secondary px-4 py-2 text-sm font-semibold text-neutral-100 transition-colors hover:bg-secondary-dark disabled:cursor-not-allowed disabled:opacity-60"
        >
          Save
        </LoadingButton>
      </div>
    </div>
  );
}

type ProviderTypeSectionProps = {
  title: string;
  description: string;
  pluginType: string;
  consequence: string;
  settings: ProviderTypeSettings;
  selected: string | null;
  models?: Record<string, string>;
  onSelect: (providerId: string | null) => void;
  onModelChange?: (models: Record<string, string>) => void;
};

function ProviderTypeSection({
  title,
  description,
  pluginType,
  consequence,
  settings,
  selected,
  models,
  onSelect,
  onModelChange,
}: ProviderTypeSectionProps) {
  const modelInputId = useId();
  const instanceDefault = settings.providers.find(
    (provider) => provider.id === settings.instanceDefault
  );
  const effectiveId = selected ?? settings.instanceDefault;
  const effective = settings.providers.find(
    (provider) => provider.id === effectiveId
  );

  const warning =
    settings.providers.length === 0
      ? `No ${pluginType} plugin is installed, so ${consequence}.`
      : !effective
        ? `No ${pluginType} provider is chosen, so ${consequence}.`
        : effective.configurationProblem
          ? `${effective.displayName} isn't ready: ${effective.configurationProblem}. Until then, ${consequence}.`
          : models && !models[effective.id] && !effective.instanceModel
            ? `No model is set for ${effective.displayName}, so ${consequence}.`
            : null;

  const options = [
    {
      value: null,
      label: `Instance default (${instanceDefault?.displayName ?? "none"})`,
    },
    ...settings.providers.map((provider) => ({
      value: provider.id,
      label: provider.configurationProblem
        ? `${provider.displayName} (not configured)`
        : provider.displayName,
    })),
    ...(selected !== null &&
    !settings.providers.some((provider) => provider.id === selected)
      ? [{ value: selected, label: `${selected} (not installed)` }]
      : []),
  ];

  return (
    <section className="mt-8">
      <h2 className="mb-1 text-xs font-semibold uppercase tracking-wide text-neutral-400">
        {title}
      </h2>
      <p className="mb-3 text-xs text-neutral-500">{description}</p>

      {warning && <p className="mb-3 text-xs text-yellow-text">{warning}</p>}

      <div
        role="radiogroup"
        aria-label={`${title} provider`}
        className="flex flex-wrap gap-2"
      >
        {options.map(({ value, label }) => (
          <button
            key={value ?? "instance-default"}
            type="button"
            role="radio"
            aria-checked={selected === value}
            onClick={() => onSelect(value)}
            className={`rounded-full border px-3 py-1.5 text-xs font-medium transition-colors ${
              selected === value
                ? "border-secondary bg-secondary/10 text-secondary"
                : "border-neutral-300 bg-neutral-100 text-neutral-600 hover:bg-neutral-200"
            }`}
          >
            {label}
          </button>
        ))}
      </div>

      {models && onModelChange && effective && (
        <div className="mt-3">
          <label
            htmlFor={modelInputId}
            className="mb-1 block text-xs font-medium text-neutral-600"
          >
            Model for {effective.displayName}
          </label>
          <input
            id={modelInputId}
            type="text"
            value={models[effective.id] ?? ""}
            onChange={(event) =>
              onModelChange(withModel(models, effective.id, event.target.value))
            }
            placeholder={effective.instanceModel ?? "Required"}
            maxLength={200}
            className="w-full rounded-lg border border-neutral-300 bg-neutral-100 px-3 py-2 font-mono text-sm text-neutral-800 outline-none placeholder:text-neutral-400 hover:border-neutral-400 focus:border-secondary focus:ring-2 focus:ring-secondary/20"
          />
          {effective.instanceModel && (
            <p className="mt-1 text-xs text-neutral-500">
              Leave empty to use the instance&apos;s model.
            </p>
          )}
        </div>
      )}
    </section>
  );
}

function toRequest(settings: ProviderSettings): UpdateProviderSettingsRequest {
  return {
    llmProvider: settings.chatModel.selected,
    llmModels: savedModels(settings.chatModel),
    transcriptionProvider: settings.transcription.selected,
    transcriptionModels: savedModels(settings.transcription),
    storageProvider: settings.storage.selected,
  };
}

function savedModels(settings: ProviderTypeSettings) {
  return Object.fromEntries(
    settings.providers
      .filter((provider) => provider.model !== null)
      .map((provider) => [provider.id, provider.model as string])
  );
}

function withModel(
  models: Record<string, string>,
  providerId: string,
  model: string
) {
  const next = { ...models };

  if (model === "") {
    delete next[providerId];
  } else {
    next[providerId] = model;
  }

  return next;
}
