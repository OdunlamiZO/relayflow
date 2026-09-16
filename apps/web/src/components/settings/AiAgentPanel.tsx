"use client";

import { useState } from "react";

import { ConfirmModal } from "@/components/common/ConfirmModal";
import { LoadingButton } from "@/components/common/LoadingButton";
import { Select } from "@/components/common/Select";
import { Spinner } from "@/components/common/Spinner";
import { useToast } from "@/components/providers/ToastProvider";
import { useAiAgentConfigurations } from "@/hooks/use-ai-agent-configurations";
import { useCreateAiAgentConfiguration } from "@/hooks/use-create-ai-agent-configuration";
import { useDeleteAiAgentConfiguration } from "@/hooks/use-delete-ai-agent-configuration";
import { useSetDefaultAiAgentConfiguration } from "@/hooks/use-set-default-ai-agent-configuration";
import { useUpdateAiAgentConfiguration } from "@/hooks/use-update-ai-agent-configuration";
import { useWorkflows } from "@/hooks/use-workflows";
import { errorMessage } from "@/lib/error-message";
import type {
  AiAgentConfiguration,
  AutonomyCeiling,
  ExtractionField,
  KnowledgeEntry,
  LlmProvider,
  UpdateAiAgentConfigurationRequest,
  WorkflowMapping,
} from "@/lib/messaging-api";

const LLM_PROVIDER_OPTIONS: { value: "" | LlmProvider; label: string }[] = [
  { value: "", label: "Platform default" },
  { value: "ANTHROPIC", label: "Anthropic" },
  { value: "OPENAI", label: "OpenAI" },
  { value: "GROQ", label: "Groq" },
  { value: "OLLAMA", label: "Ollama" },
];

const INSTRUCTIONS_SAMPLE = `# ABOUT YOUR BUSINESS
We're a small online store selling handmade crafts.

# TONE & STYLE
Be warm, friendly, and brief. Always ask what the customer needs before acting.

# WHAT TO DO
When a customer wants to place an order, ask what item they want and their delivery address before confirming anything.
When a customer wants to return or exchange an item, ask for their order number, then let them know a team member will follow up.

# NEVER DO
- Never respond without first asking what the customer needs
- Never make promises you can't keep
- Never guess at information you don't have — check the knowledge base below, or say you're not sure
`;

const INSTRUCTIONS_TEMPLATE = `# ABOUT YOUR BUSINESS


# TONE & STYLE


# WHAT TO DO


# NEVER DO
- Never guess — if something isn't covered above, say you're not sure and offer to connect them with a team member.
`;

type FormState = {
  agentName: string;
  enabled: boolean;
  autonomyCeiling: AutonomyCeiling;
  llmProvider: LlmProvider | null;
  instructions: string;
  knowledgeBase: KnowledgeEntry[];
  escalationKeywords: string[];
  workflowMappings: WorkflowMapping[];
  extractionFields: ExtractionField[];
};

function formFromConfiguration(configuration: AiAgentConfiguration): FormState {
  return {
    agentName: configuration.name ?? "AI Agent",
    enabled: configuration.enabled,
    autonomyCeiling: configuration.autonomyCeiling,
    llmProvider: configuration.llmProvider,
    instructions: configuration.instructions ?? "",
    knowledgeBase: configuration.knowledgeBase,
    escalationKeywords: configuration.escalationKeywords,
    workflowMappings: configuration.workflowMappings,
    extractionFields: configuration.extractionFields,
  };
}

const BLANK_CONFIGURATION_ID = "new";

function blankConfiguration(workspaceId: string): AiAgentConfiguration {
  return {
    id: BLANK_CONFIGURATION_ID,
    workspaceId,
    name: "",
    isDefault: false,
    enabled: false,
    autonomyCeiling: "DRAFT_ONLY",
    llmProvider: null,
    instructions: "",
    knowledgeBase: [],
    escalationKeywords: [],
    workflowMappings: [],
    extractionFields: [],
    createdAt: "",
    updatedAt: "",
  };
}

type Props = {
  workspaceId: string;
};

export function AiAgentPanel({ workspaceId }: Props) {
  const { data: configurations = [], isLoading } =
    useAiAgentConfigurations(workspaceId);
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [isCreatingNew, setIsCreatingNew] = useState(false);

  const selected =
    configurations.find((c) => c.id === selectedId) ??
    configurations.find((c) => c.isDefault) ??
    configurations[0] ??
    null;

  const showingNew = isCreatingNew || (!selected && !isLoading);

  return (
    <div className="mx-auto max-w-2xl px-6 py-8 sm:px-8">
      <div className="mb-6">
        <h1 className="text-xl font-bold text-primary">AI Agent</h1>
        <p className="mt-1.5 text-sm text-neutral-500">
          Configure one or more AI agents, then assign which one each channel
          uses from Settings → Channels.
        </p>
      </div>

      {isLoading ? (
        <div className="flex justify-center py-12">
          <Spinner />
        </div>
      ) : (
        <>
          {configurations.length > 0 && (
            <div className="mb-6 flex flex-wrap gap-2 border-b border-neutral-200 pb-5">
              {configurations.map((configuration) => (
                <AgentTab
                  key={configuration.id}
                  label={configuration.name || "AI Agent"}
                  isDefault={configuration.isDefault}
                  active={!showingNew && configuration.id === selected?.id}
                  onClick={() => {
                    setSelectedId(configuration.id);
                    setIsCreatingNew(false);
                  }}
                />
              ))}

              <button
                type="button"
                onClick={() => setIsCreatingNew(true)}
                className={`flex items-center gap-1 rounded-full border border-dashed px-3 py-1.5 text-xs font-medium transition-colors ${
                  showingNew
                    ? "border-secondary bg-secondary/10 text-secondary"
                    : "border-neutral-300 text-neutral-500 hover:border-neutral-400 hover:text-neutral-700"
                }`}
              >
                <span
                  className="material-symbols-rounded text-[14px] leading-none"
                  aria-hidden="true"
                >
                  add
                </span>
                New agent
              </button>
            </div>
          )}

          {showingNew ? (
            <NewAgentSection
              key="new"
              workspaceId={workspaceId}
              onCreated={(id) => {
                setSelectedId(id);
                setIsCreatingNew(false);
              }}
            />
          ) : (
            selected && (
              <ExistingAgentSection
                key={selected.id}
                workspaceId={workspaceId}
                configuration={selected}
                canDelete={configurations.length > 1}
              />
            )
          )}
        </>
      )}
    </div>
  );
}

function AgentTab({
  label,
  isDefault,
  active,
  onClick,
}: {
  label: string;
  isDefault: boolean;
  active: boolean;
  onClick: () => void;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={`flex items-center gap-1.5 rounded-full border px-3 py-1.5 text-xs font-medium transition-colors ${
        active
          ? "border-secondary bg-secondary/10 text-secondary"
          : "border-neutral-300 bg-neutral-100 text-neutral-600 hover:bg-neutral-200"
      }`}
    >
      {label}
      {isDefault && (
        <span className="rounded-full bg-neutral-200 px-1.5 py-0.5 text-[10px] font-semibold text-neutral-500">
          Default
        </span>
      )}
    </button>
  );
}

function NewAgentSection({
  workspaceId,
  onCreated,
}: {
  workspaceId: string;
  onCreated: (id: string) => void;
}) {
  const createConfiguration = useCreateAiAgentConfiguration(workspaceId);
  const { showToast } = useToast();

  return (
    <AiAgentForm
      workspaceId={workspaceId}
      configuration={blankConfiguration(workspaceId)}
      isSaving={createConfiguration.isPending}
      onSave={(request, options) =>
        createConfiguration.mutate(request, {
          onSuccess: (data) => {
            showToast({ kind: "success", message: "AI agent created" });
            options?.onSuccess?.(data);
            onCreated(data.id);
          },
          onError: (err) => {
            showToast({ kind: "error", message: errorMessage(err) });
            options?.onError?.(err);
          },
        })
      }
    />
  );
}

function ExistingAgentSection({
  workspaceId,
  configuration,
  canDelete,
}: {
  workspaceId: string;
  configuration: AiAgentConfiguration;
  canDelete: boolean;
}) {
  const updateConfiguration = useUpdateAiAgentConfiguration(
    workspaceId,
    configuration.id
  );
  const deleteConfiguration = useDeleteAiAgentConfiguration(
    workspaceId,
    configuration.id
  );
  const setDefaultConfiguration =
    useSetDefaultAiAgentConfiguration(workspaceId);
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
  const { showToast } = useToast();

  return (
    <>
      <AiAgentForm
        workspaceId={workspaceId}
        configuration={configuration}
        onSave={updateConfiguration.mutate}
        isSaving={updateConfiguration.isPending}
        extraAction={
          <div className="flex items-center gap-2">
            {!configuration.isDefault && (
              <LoadingButton
                type="button"
                isLoading={setDefaultConfiguration.isPending}
                onClick={() =>
                  setDefaultConfiguration.mutate(configuration.id, {
                    onSuccess: () =>
                      showToast({
                        kind: "success",
                        message: `${configuration.name || "Agent"} is now the default`,
                      }),
                    onError: (err) =>
                      showToast({ kind: "error", message: errorMessage(err) }),
                  })
                }
                className="rounded-lg border border-neutral-200 px-3 py-2 text-xs font-medium text-neutral-600 transition-colors hover:border-secondary hover:bg-secondary/10 hover:text-secondary disabled:opacity-60"
              >
                Set as default
              </LoadingButton>
            )}

            {canDelete && (
              <button
                type="button"
                onClick={() => setShowDeleteConfirm(true)}
                className="rounded-lg px-3 py-2 text-xs font-medium text-neutral-500 transition-colors hover:text-red-500"
              >
                Delete
              </button>
            )}
          </div>
        }
      />

      {showDeleteConfirm && (
        <ConfirmModal
          title="Delete this AI agent?"
          description={
            configuration.isDefault
              ? "This is the workspace default — another agent will automatically become the default. Any channel using it directly will fall back to that new default."
              : "Any channel assigned to this agent will fall back to the workspace default."
          }
          confirmLabel="Delete"
          destructive
          isPending={deleteConfiguration.isPending}
          onConfirm={() =>
            deleteConfiguration.mutate(undefined, {
              onSuccess: () => setShowDeleteConfirm(false),
            })
          }
          onCancel={() => setShowDeleteConfirm(false)}
        />
      )}
    </>
  );
}

type AiAgentFormProps = {
  workspaceId: string;
  configuration: AiAgentConfiguration;
  onSave: (
    request: UpdateAiAgentConfigurationRequest,
    options?: {
      onSuccess?: (data: AiAgentConfiguration) => void;
      onError?: (err: unknown) => void;
    }
  ) => void;
  isSaving: boolean;
  extraAction?: React.ReactNode;
};

function AiAgentForm({
  workspaceId,
  configuration,
  onSave,
  isSaving,
  extraAction,
}: AiAgentFormProps) {
  const { data: workflows = [] } = useWorkflows(workspaceId);
  const { showToast } = useToast();

  const [form, setForm] = useState<FormState>(() =>
    formFromConfiguration(configuration)
  );
  const [newKeyword, setNewKeyword] = useState("");

  const {
    agentName,
    enabled,
    autonomyCeiling,
    llmProvider,
    instructions,
    knowledgeBase,
    escalationKeywords,
    workflowMappings,
    extractionFields,
  } = form;

  const isUnchanged =
    (configuration.name ?? "AI Agent") === agentName &&
    configuration.enabled === enabled &&
    configuration.autonomyCeiling === autonomyCeiling &&
    configuration.llmProvider === llmProvider &&
    (configuration.instructions ?? "") === instructions &&
    JSON.stringify(configuration.knowledgeBase) ===
      JSON.stringify(knowledgeBase) &&
    JSON.stringify(configuration.escalationKeywords) ===
      JSON.stringify(escalationKeywords) &&
    JSON.stringify(configuration.workflowMappings) ===
      JSON.stringify(workflowMappings) &&
    JSON.stringify(configuration.extractionFields) ===
      JSON.stringify(extractionFields);

  function patch(partial: Partial<FormState>) {
    setForm((prev) => ({ ...prev, ...partial }));
  }

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();

    onSave(
      {
        name: agentName.trim() || "AI Agent",
        enabled,
        autonomyCeiling,
        llmProvider,
        instructions: instructions.trim() || null,
        knowledgeBase,
        escalationKeywords,
        workflowMappings,
        extractionFields,
      },
      {
        onSuccess: () =>
          showToast({ kind: "success", message: "AI agent saved" }),
        onError: (err) =>
          showToast({ kind: "error", message: errorMessage(err) }),
      }
    );
  }

  function addKnowledgeEntry() {
    patch({ knowledgeBase: [...knowledgeBase, { question: "", answer: "" }] });
  }

  function updateKnowledgeEntry(
    index: number,
    field: keyof KnowledgeEntry,
    value: string
  ) {
    patch({
      knowledgeBase: knowledgeBase.map((entry, i) =>
        i === index ? { ...entry, [field]: value } : entry
      ),
    });
  }

  function removeKnowledgeEntry(index: number) {
    patch({ knowledgeBase: knowledgeBase.filter((_, i) => i !== index) });
  }

  function addKeyword() {
    const trimmed = newKeyword.trim();
    if (!trimmed || escalationKeywords.includes(trimmed)) return;
    patch({ escalationKeywords: [...escalationKeywords, trimmed] });
    setNewKeyword("");
  }

  function removeKeyword(keyword: string) {
    patch({
      escalationKeywords: escalationKeywords.filter((k) => k !== keyword),
    });
  }

  function addWorkflowMapping() {
    patch({
      workflowMappings: [
        ...workflowMappings,
        { workflowId: "", name: "", triggerDescription: "" },
      ],
    });
  }

  function updateWorkflowMapping(
    index: number,
    field: keyof WorkflowMapping,
    value: string
  ) {
    patch({
      workflowMappings: workflowMappings.map((mapping, i) => {
        if (i !== index) return mapping;
        if (field === "workflowId") {
          const workflow = workflows.find((w) => w.id === value);
          return {
            ...mapping,
            workflowId: value,
            name: workflow?.name ?? mapping.name,
          };
        }
        return { ...mapping, [field]: value };
      }),
    });
  }

  function removeWorkflowMapping(index: number) {
    patch({ workflowMappings: workflowMappings.filter((_, i) => i !== index) });
  }

  function addExtractionField() {
    patch({
      extractionFields: [...extractionFields, { key: "", description: "" }],
    });
  }

  function updateExtractionField(
    index: number,
    field: keyof ExtractionField,
    value: string
  ) {
    patch({
      extractionFields: extractionFields.map((entry, i) =>
        i === index ? { ...entry, [field]: value } : entry
      ),
    });
  }

  function removeExtractionField(index: number) {
    patch({ extractionFields: extractionFields.filter((_, i) => i !== index) });
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-8">
      {/* Name */}
      <section>
        <h2 className="mb-1 text-xs font-semibold uppercase tracking-wide text-neutral-400">
          Agent name
        </h2>
        <p className="mb-2 text-xs text-neutral-500">
          Shown as the assignee when the agent handles a conversation.
        </p>
        <input
          type="text"
          value={agentName}
          onChange={(e) => patch({ agentName: e.target.value })}
          maxLength={100}
          placeholder="AI Agent"
          className="w-full rounded-lg border border-neutral-300 bg-neutral-100 px-3 py-2 text-sm text-neutral-800 outline-none transition-colors hover:border-neutral-400 focus:border-secondary focus:ring-2 focus:ring-secondary/20"
        />
      </section>

      {/* Enable */}
      <section>
        <label className="flex cursor-pointer items-center gap-3">
          <input
            type="checkbox"
            checked={enabled}
            onChange={(e) => patch({ enabled: e.target.checked })}
            className="h-4 w-4 rounded border-neutral-300 accent-secondary"
          />
          <span className="text-sm font-medium text-neutral-800">
            Enable AI agent
          </span>
        </label>
        <p className="mt-1.5 pl-7 text-xs text-neutral-500">
          When enabled, the agent handles inbound messages that are not owned by
          a workflow.
        </p>
      </section>

      {/* Autonomy */}
      <section>
        <h2 className="mb-3 text-xs font-semibold uppercase tracking-wide text-neutral-400">
          Autonomy
        </h2>

        <div className="space-y-2.5">
          {(
            [
              {
                value: "DRAFT_ONLY" as AutonomyCeiling,
                label: "Draft only",
                description:
                  "The agent proposes a reply for a human to review and send.",
              },
              {
                value: "AUTO_SEND" as AutonomyCeiling,
                label: "Auto-send",
                description:
                  "High-confidence replies are sent automatically. Low-confidence replies become drafts.",
              },
            ] satisfies {
              value: AutonomyCeiling;
              label: string;
              description: string;
            }[]
          ).map(({ value, label, description }) => (
            <label
              key={value}
              className="flex cursor-pointer items-start gap-3"
            >
              <input
                type="radio"
                name="autonomyCeiling"
                value={value}
                checked={autonomyCeiling === value}
                onChange={() => patch({ autonomyCeiling: value })}
                className="mt-0.5 h-4 w-4 accent-secondary"
              />
              <span>
                <span className="block text-sm font-medium text-neutral-800">
                  {label}
                </span>
                <span className="text-xs text-neutral-500">{description}</span>
              </span>
            </label>
          ))}
        </div>
      </section>

      {/* LLM provider */}
      <section>
        <h2 className="mb-2 text-xs font-semibold uppercase tracking-wide text-neutral-400">
          LLM Provider
        </h2>
        <p className="mb-2.5 text-xs text-neutral-500">
          Which LLM this agent uses — Platform default follows your global
          setting, or pick one to override it just for this agent.
        </p>
        <div className="flex flex-wrap gap-2">
          {LLM_PROVIDER_OPTIONS.map(({ value, label }) => {
            const active = (llmProvider ?? "") === value;

            return (
              <button
                key={value || "default"}
                type="button"
                onClick={() =>
                  patch({ llmProvider: value ? (value as LlmProvider) : null })
                }
                className={`rounded-full border px-3 py-1.5 text-xs font-medium transition-colors ${
                  active
                    ? "border-secondary bg-secondary/10 text-secondary"
                    : "border-neutral-300 bg-neutral-100 text-neutral-600 hover:bg-neutral-200"
                }`}
              >
                {label}
              </button>
            );
          })}
        </div>
      </section>

      {/* Instructions */}
      <section>
        <div className="mb-2 flex items-center justify-between">
          <h2 className="text-xs font-semibold uppercase tracking-wide text-neutral-400">
            Instructions
          </h2>
          <div className="flex gap-3">
            {!instructions && (
              <button
                type="button"
                onClick={() => patch({ instructions: INSTRUCTIONS_SAMPLE })}
                className="text-xs font-medium text-secondary hover:text-secondary-dark"
              >
                Load sample
              </button>
            )}
            {instructions && (
              <button
                type="button"
                onClick={() => patch({ instructions: "" })}
                className="text-xs font-medium text-neutral-400 hover:text-neutral-600"
              >
                Reset
              </button>
            )}
          </div>
        </div>
        <p className="mb-2 text-xs text-neutral-500">
          Tell the agent about your business, its tone, and what to do — what to
          ask for and when to hand a conversation off. Facts go in the knowledge
          base below; escalation triggers go in escalation keywords.
        </p>
        <textarea
          value={instructions || INSTRUCTIONS_TEMPLATE}
          onChange={(e) => patch({ instructions: e.target.value })}
          rows={12}
          className="w-full rounded-lg border border-neutral-300 bg-neutral-100 px-3 py-2.5 font-mono text-xs text-neutral-800 outline-none placeholder:text-neutral-400 focus:border-secondary focus:ring-2 focus:ring-secondary/20"
        />
      </section>

      {/* Knowledge base */}
      <section>
        <h2 className="mb-1 text-xs font-semibold uppercase tracking-wide text-neutral-400">
          Knowledge base
        </h2>
        <p className="mb-3 text-xs text-neutral-500">
          Q&amp;A pairs the agent uses to answer common questions accurately.
          Keep each entry to one specific question — split bundled topics into
          separate entries so the agent can match and answer precisely.
        </p>

        <div className="space-y-3">
          {knowledgeBase.map((entry, i) => (
            <div
              key={i}
              className="rounded-lg border border-neutral-200 bg-neutral-100 p-3"
            >
              <div className="mb-2 flex items-center justify-between">
                <span className="text-xs font-medium text-neutral-500">
                  Entry {i + 1}
                </span>
                <button
                  type="button"
                  onClick={() => removeKnowledgeEntry(i)}
                  className="text-xs text-red-500 hover:text-red-700"
                >
                  Remove
                </button>
              </div>
              <input
                type="text"
                placeholder="Question"
                value={entry.question}
                onChange={(e) =>
                  updateKnowledgeEntry(i, "question", e.target.value)
                }
                className="mb-2 w-full rounded-lg border border-neutral-300 bg-neutral-100 px-3 py-2 text-sm text-neutral-800 outline-none transition-colors hover:border-neutral-400 focus:border-secondary focus:ring-2 focus:ring-secondary/20"
              />
              <textarea
                placeholder="Answer"
                value={entry.answer}
                onChange={(e) =>
                  updateKnowledgeEntry(i, "answer", e.target.value)
                }
                rows={2}
                className="w-full rounded-lg border border-neutral-300 bg-neutral-100 px-3 py-2 text-sm text-neutral-800 outline-none transition-colors hover:border-neutral-400 focus:border-secondary focus:ring-2 focus:ring-secondary/20"
              />
            </div>
          ))}
        </div>

        <button
          type="button"
          onClick={addKnowledgeEntry}
          className="mt-3 text-xs font-medium text-secondary hover:text-secondary-dark"
        >
          + Add entry
        </button>
      </section>

      {/* Escalation keywords */}
      <section>
        <h2 className="mb-1 text-xs font-semibold uppercase tracking-wide text-neutral-400">
          Escalation keywords
        </h2>
        <p className="mb-3 text-xs text-neutral-500">
          Messages containing these words are immediately routed to a human,
          before the LLM is called.
        </p>

        <div className="mb-3 flex flex-wrap gap-2">
          {escalationKeywords.map((kw) => (
            <span
              key={kw}
              className="flex items-center gap-1.5 rounded-full border border-neutral-300 bg-neutral-100 px-2.5 py-0.5 text-xs text-neutral-700"
            >
              {kw}
              <button
                type="button"
                onClick={() => removeKeyword(kw)}
                aria-label={`Remove ${kw}`}
                className="text-neutral-400 hover:text-neutral-700"
              >
                ×
              </button>
            </span>
          ))}
        </div>

        <div className="flex gap-2">
          <input
            type="text"
            placeholder="e.g. cancel, refund"
            value={newKeyword}
            onChange={(e) => setNewKeyword(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === "Enter") {
                e.preventDefault();
                addKeyword();
              }
            }}
            className="flex-1 rounded-lg border border-neutral-300 bg-neutral-100 px-3 py-2 text-sm text-neutral-800 outline-none transition-colors hover:border-neutral-400 focus:border-secondary focus:ring-2 focus:ring-secondary/20"
          />
          <button
            type="button"
            onClick={addKeyword}
            className="rounded-lg border border-neutral-300 bg-neutral-100 px-3 py-2 text-sm font-medium text-neutral-700 transition-colors hover:border-neutral-400 hover:bg-neutral-200"
          >
            Add
          </button>
        </div>
      </section>

      {/* Workflow mappings */}
      <section>
        <h2 className="mb-1 text-xs font-semibold uppercase tracking-wide text-neutral-400">
          Workflow triggers
        </h2>
        <p className="mb-3 text-xs text-neutral-500">
          Describe when the agent should trigger each workflow. Only workflows
          with the{" "}
          <strong className="font-medium text-neutral-600">Manual</strong>{" "}
          trigger event appear here. The agent matches the customer&apos;s
          intent against your descriptions.
        </p>

        <div className="space-y-3">
          {workflowMappings.map((mapping, i) => (
            <div
              key={i}
              className="rounded-lg border border-neutral-200 bg-neutral-100 p-3"
            >
              <div className="mb-2 flex items-center justify-between">
                <span className="text-xs font-medium text-neutral-500">
                  Mapping {i + 1}
                </span>
                <button
                  type="button"
                  onClick={() => removeWorkflowMapping(i)}
                  className="text-xs text-red-500 hover:text-red-700"
                >
                  Remove
                </button>
              </div>
              <div className="mb-2">
                <Select
                  value={mapping.workflowId}
                  onChange={(value) =>
                    updateWorkflowMapping(i, "workflowId", value)
                  }
                  placeholder="Select a workflow…"
                  options={workflows
                    .filter((wf) => {
                      const nodes =
                        (
                          wf.draftGraph as {
                            nodes?: {
                              type?: string;
                              data?: { event?: string };
                            }[];
                          }
                        )?.nodes ?? [];
                      return nodes.some(
                        (n) =>
                          n.type === "trigger" && n.data?.event === "manual"
                      );
                    })
                    .map((wf) => ({ value: wf.id, label: wf.name }))}
                />
              </div>
              <input
                type="text"
                placeholder="Trigger description (e.g. Customer wants to track their order)"
                value={mapping.triggerDescription}
                onChange={(e) =>
                  updateWorkflowMapping(i, "triggerDescription", e.target.value)
                }
                className="w-full rounded-lg border border-neutral-300 bg-neutral-100 px-3 py-2 text-sm text-neutral-800 outline-none transition-colors hover:border-neutral-400 focus:border-secondary focus:outline-none focus:ring-2 focus:ring-secondary/20"
              />
            </div>
          ))}
        </div>

        <button
          type="button"
          onClick={addWorkflowMapping}
          className="mt-3 text-xs font-medium text-secondary hover:text-secondary-dark"
        >
          + Add workflow trigger
        </button>
      </section>

      {/* Extraction fields */}
      <section>
        <h2 className="mb-1 text-xs font-semibold uppercase tracking-wide text-neutral-400">
          Data extraction
        </h2>
        <p className="mb-3 text-xs text-neutral-500">
          Fields the agent pulls out of the conversation and passes to any
          workflow it triggers, as <code>agent.data.&lt;key&gt;</code> workflow
          variables. Use case varies per workflow — leave empty if none apply.
        </p>

        <div className="space-y-3">
          {extractionFields.map((field, i) => (
            <div
              key={i}
              className="rounded-lg border border-neutral-200 bg-neutral-100 p-3"
            >
              <div className="mb-2 flex items-center justify-between">
                <span className="text-xs font-medium text-neutral-500">
                  Field {i + 1}
                </span>
                <button
                  type="button"
                  onClick={() => removeExtractionField(i)}
                  className="text-xs text-red-500 hover:text-red-700"
                >
                  Remove
                </button>
              </div>
              <input
                type="text"
                placeholder="Key (e.g. orderNumber)"
                value={field.key}
                onChange={(e) =>
                  updateExtractionField(i, "key", e.target.value)
                }
                className="mb-2 w-full rounded-lg border border-neutral-300 bg-neutral-100 px-3 py-2 font-mono text-sm text-neutral-800 outline-none transition-colors hover:border-neutral-400 focus:border-secondary focus:ring-2 focus:ring-secondary/20"
              />
              <input
                type="text"
                placeholder="Description (e.g. The customer's order number, if mentioned)"
                value={field.description}
                onChange={(e) =>
                  updateExtractionField(i, "description", e.target.value)
                }
                className="w-full rounded-lg border border-neutral-300 bg-neutral-100 px-3 py-2 text-sm text-neutral-800 outline-none transition-colors hover:border-neutral-400 focus:border-secondary focus:ring-2 focus:ring-secondary/20"
              />
            </div>
          ))}
        </div>

        <button
          type="button"
          onClick={addExtractionField}
          className="mt-3 text-xs font-medium text-secondary hover:text-secondary-dark"
        >
          + Add field
        </button>
      </section>

      {/* Save */}
      <div className="flex items-center justify-end gap-2 border-t border-neutral-200 pt-6">
        {extraAction}
        <LoadingButton
          type="submit"
          isLoading={isSaving}
          disabled={isUnchanged}
          className="rounded-lg bg-secondary px-5 py-2 text-sm font-semibold text-neutral-100 transition-colors hover:bg-secondary-dark disabled:cursor-not-allowed disabled:opacity-60"
        >
          Save
        </LoadingButton>
      </div>
    </form>
  );
}
