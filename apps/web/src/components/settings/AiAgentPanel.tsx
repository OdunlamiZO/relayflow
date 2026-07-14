"use client";

import { useState } from "react";

import { Select } from "@/components/common/Select";
import { Spinner } from "@/components/common/Spinner";
import { useToast } from "@/components/providers/ToastProvider";
import { useAiAgentConfiguration } from "@/hooks/use-ai-agent-configuration";
import { useUpdateAiAgentConfiguration } from "@/hooks/use-update-ai-agent-configuration";
import { useWorkflows } from "@/hooks/use-workflows";
import { errorMessage } from "@/lib/error-message";
import type {
  AiAgentConfiguration,
  AutonomyCeiling,
  ExtractionField,
  KnowledgeEntry,
  WorkflowMapping,
} from "@/lib/messaging-api";

const INSTRUCTIONS_SAMPLE = `# ABOUT YOUR BUSINESS
We're a small online store selling handmade crafts.

# TONE & STYLE
Be warm, friendly, and brief. Always ask what the customer needs before acting.

# WHAT YOU CAN ANSWER DIRECTLY
- Business hours: Monday to Saturday, 9am–5pm
- How to place an order: visit our website and add items to your cart
- Delivery: 3–5 business days

# ESCALATION
Escalate if the customer is upset or wants a refund.

# NEVER DO
- Never respond without first asking what the customer needs
- Never make promises you can't keep
`;

const INSTRUCTIONS_TEMPLATE = `# ABOUT YOUR BUSINESS


# TONE & STYLE


# WHAT YOU CAN ANSWER DIRECTLY


# ESCALATION


# NEVER DO
`;

type FormState = {
  agentName: string;
  enabled: boolean;
  autonomyCeiling: AutonomyCeiling;
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
    instructions: configuration.instructions ?? "",
    knowledgeBase: configuration.knowledgeBase,
    escalationKeywords: configuration.escalationKeywords,
    workflowMappings: configuration.workflowMappings,
    extractionFields: configuration.extractionFields,
  };
}

type Props = {
  workspaceId: string;
};

export function AiAgentPanel({ workspaceId }: Props) {
  const { data: configuration, isLoading } =
    useAiAgentConfiguration(workspaceId);

  if (isLoading) {
    return (
      <div className="flex justify-center py-12">
        <Spinner />
      </div>
    );
  }

  if (!configuration) return null;

  return (
    <AiAgentForm
      key={configuration.id}
      workspaceId={workspaceId}
      configuration={configuration}
    />
  );
}

type AiAgentFormProps = {
  workspaceId: string;
  configuration: AiAgentConfiguration;
};

function AiAgentForm({ workspaceId, configuration }: AiAgentFormProps) {
  const updateConfiguration = useUpdateAiAgentConfiguration(workspaceId);
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

    updateConfiguration.mutate(
      {
        name: agentName.trim() || "AI Agent",
        enabled,
        autonomyCeiling,
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
    <div className="mx-auto max-w-2xl px-6 py-8 sm:px-8">
      <div className="mb-8">
        <h1 className="text-xl font-bold text-primary">AI Agent</h1>
        <p className="mt-1.5 text-sm text-neutral-500">
          Configure an AI agent to handle routine messages autonomously.
        </p>
      </div>

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
            When enabled, the agent handles inbound messages that are not owned
            by a workflow.
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
                  <span className="text-xs text-neutral-500">
                    {description}
                  </span>
                </span>
              </label>
            ))}
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
            Tell the agent about your business, tone, what it can handle, and
            when to escalate to a human.
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
                    updateWorkflowMapping(
                      i,
                      "triggerDescription",
                      e.target.value
                    )
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
            workflow it triggers, as <code>agent.data.&lt;key&gt;</code>{" "}
            workflow variables. Use case varies per workflow — leave empty if
            none apply.
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
        <div className="flex justify-end border-t border-neutral-200 pt-6">
          <button
            type="submit"
            disabled={isUnchanged || updateConfiguration.isPending}
            className="rounded-lg bg-secondary px-5 py-2 text-sm font-semibold text-neutral-100 transition-colors hover:bg-secondary-dark disabled:cursor-not-allowed disabled:opacity-60"
          >
            {updateConfiguration.isPending ? "Saving…" : "Save"}
          </button>
        </div>
      </form>
    </div>
  );
}
