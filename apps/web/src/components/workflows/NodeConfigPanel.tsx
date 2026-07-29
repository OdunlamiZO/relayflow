"use client";

import { useRef } from "react";
import { useState } from "react";

import { type Node, useReactFlow } from "@xyflow/react";

import { Select } from "@/components/common/Select";
import { type SelectOption } from "@/components/common/Select";
import { useAiAgentConfiguration } from "@/hooks/use-ai-agent-configuration";
import { useWorkspace } from "@/hooks/use-workspaces";
import {
  RESERVED_CONTACT_FIELDS,
  RESERVED_CONTACT_FIELD_KEYS,
} from "@/lib/messaging-api";

import {
  BUILT_IN_VARIABLES,
  FilterWarning,
  VariablePicker,
  type WorkflowVariable,
} from "./VariablePicker";
import {
  type ConditionBranch,
  type ConditionOperator,
  type ConditionRule,
  DEFAULT_CONDITION_BRANCHES,
} from "./nodes/ConditionNode";
import {
  DEFAULT_TIMEOUT_MINUTES,
  type WaitForReplyOption,
} from "./nodes/WaitForReplyNode";

type Props = {
  node: Node;
  nodes: Node[];
  workspaceId: string;
  onClose: () => void;
  onDirty: () => void;
};

// ─── variable extraction ──────────────────────────────────────────────────────

function extractWorkflowVariables(nodes: Node[]): WorkflowVariable[] {
  const seen = new Set<string>();
  const vars: WorkflowVariable[] = [];

  for (const node of nodes) {
    const data = node.data as Record<string, unknown>;

    if (node.type === "setVariable") {
      const name = (data.variableName as string | undefined)?.trim();

      if (name && !seen.has(name)) {
        seen.add(name);
        vars.push({ name, label: name, group: "workflow" });
      }
    }

    if (node.type === "httpRequest") {
      const statusVar = (
        data.responseStatusVariable as string | undefined
      )?.trim();

      if (statusVar && !seen.has(statusVar)) {
        seen.add(statusVar);
        vars.push({ name: statusVar, label: statusVar, group: "workflow" });
      }

      const mappings =
        (data.responseMappings as Array<{ variable?: string }>) ?? [];

      for (const m of mappings) {
        const v = m.variable?.trim();

        if (v && !seen.has(v)) {
          seen.add(v);
          vars.push({ name: v, label: v, group: "workflow" });
        }
      }
    }

    if (node.type === "waitForReply") {
      const responseVar = (data.responseVariable as string | undefined)?.trim();

      if (responseVar && !seen.has(responseVar)) {
        seen.add(responseVar);
        vars.push({ name: responseVar, label: responseVar, group: "workflow" });
      }
    }
  }

  return vars;
}

// ─── cursor insertion helper ──────────────────────────────────────────────────

/**
 * Inserts {@code text} at the cursor position of a controlled input/textarea.
 * Falls back to appending if the element ref is unavailable.
 */
function insertAtCursor(
  el: HTMLInputElement | HTMLTextAreaElement | null,
  currentValue: string,
  text: string,
  onChange: (v: string) => void
) {
  if (el) {
    const start = el.selectionStart ?? currentValue.length;
    const end = el.selectionEnd ?? currentValue.length;
    const newValue =
      currentValue.slice(0, start) + text + currentValue.slice(end);

    onChange(newValue);

    requestAnimationFrame(() => {
      el.focus();
      el.setSelectionRange(start + text.length, start + text.length);
    });
  } else {
    onChange(currentValue + text);
  }
}

// ─── field helpers ────────────────────────────────────────────────────────────

function Field({
  label,
  action,
  children,
}: {
  label: string;
  action?: React.ReactNode;
  children: React.ReactNode;
}) {
  return (
    <div className="flex flex-col gap-1.5">
      <div className="flex items-center justify-between">
        <label className="text-xs font-medium text-neutral-500">{label}</label>
        {action}
      </div>

      {children}
    </div>
  );
}

const inputCls =
  "w-full rounded-lg border border-neutral-200 bg-neutral-100 px-3 py-2 text-sm text-neutral-800 outline-none placeholder:text-neutral-400 focus:border-secondary focus:ring-2 focus:ring-secondary/20";

// ─── type-specific forms ──────────────────────────────────────────────────────

const TRIGGER_EVENTS = [
  { value: "conversation_opened", label: "Conversation Opened" },
  { value: "manual", label: "Manual" },
];

function TriggerForm({
  data,
  onChange,
}: {
  data: Record<string, unknown>;
  onChange: (u: Record<string, unknown>) => void;
}) {
  return (
    <Field label="Trigger event">
      <Select
        value={(data.event as string) ?? "conversation_opened"}
        onChange={(v) => onChange({ event: v })}
        options={TRIGGER_EVENTS}
      />
    </Field>
  );
}

function SendMessageForm({
  data,
  onChange,
  variables,
}: {
  data: Record<string, unknown>;
  onChange: (u: Record<string, unknown>) => void;
  variables: WorkflowVariable[];
}) {
  const textareaRef = useRef<HTMLTextAreaElement>(null);
  const message = (data.message as string) ?? "";

  return (
    <Field
      label="Message"
      action={
        <VariablePicker
          variables={variables}
          onSelect={(name) =>
            insertAtCursor(textareaRef.current, message, `{{${name}}}`, (v) =>
              onChange({ message: v })
            )
          }
        />
      }
    >
      <textarea
        ref={textareaRef}
        value={message}
        onChange={(e) => onChange({ message: e.target.value })}
        placeholder="Type your message… use {{variable}} to insert values"
        rows={4}
        className={`${inputCls} resize-none`}
      />

      <FilterWarning text={message} />
    </Field>
  );
}

const CONDITION_OPERATORS: { value: ConditionOperator; label: string }[] = [
  { value: "eq", label: "Is equal to" },
  { value: "neq", label: "Is not equal to" },
  { value: "gt", label: "Is greater than" },
  { value: "lt", label: "Is less than" },
  { value: "gte", label: "Is greater than or equal to" },
  { value: "lte", label: "Is less than or equal to" },
  { value: "contains", label: "Contains" },
  { value: "not_contains", label: "Does not contain" },
  { value: "starts_with", label: "Starts with" },
  { value: "ends_with", label: "Ends with" },
  { value: "is_set", label: "Is set" },
  { value: "is_not_set", label: "Is not set" },
];

/** Operators that compare against a value — the value field is hidden for the rest. */
const NO_VALUE_OPERATORS = new Set<ConditionOperator>(["is_set", "is_not_set"]);

/** Combined "Wait for Reply" timeout budget across an entire workflow. */
const MAX_TOTAL_TIMEOUT_MINUTES = 7 * 24 * 60;

/** WhatsApp truncates interactive button titles beyond this length. */
const WHATSAPP_BUTTON_TITLE_LIMIT = 20;

function ConditionForm({
  nodeId,
  data,
  onChange,
  variables,
}: {
  nodeId: string;
  data: Record<string, unknown>;
  onChange: (u: Record<string, unknown>) => void;
  variables: WorkflowVariable[];
}) {
  const { setEdges } = useReactFlow();

  const branches: ConditionBranch[] =
    (data.branches as ConditionBranch[]) ?? DEFAULT_CONDITION_BRANCHES;

  function updateBranch(id: string, patch: Partial<ConditionBranch>) {
    onChange({
      branches: branches.map((b) => (b.id === id ? { ...b, ...patch } : b)),
    });
  }

  function addBranch() {
    const newId = `branch-${Date.now()}`;
    onChange({
      branches: [
        ...branches,
        { id: newId, label: `Branch ${branches.length + 1}` },
      ],
    });
  }

  function removeBranch(id: string) {
    onChange({ branches: branches.filter((b) => b.id !== id) });

    setEdges((eds) =>
      eds.filter((e) => !(e.source === nodeId && e.sourceHandle === id))
    );
  }

  function getConditions(branch: ConditionBranch): ConditionRule[] {
    return branch.conditions && branch.conditions.length > 0
      ? branch.conditions
      : [{ id: "cond-0" }];
  }

  function updateCondition(
    branch: ConditionBranch,
    conditionId: string,
    patch: Partial<ConditionRule>
  ) {
    const conditions = getConditions(branch).map((c) =>
      c.id === conditionId ? { ...c, ...patch } : c
    );

    updateBranch(branch.id, { conditions });
  }

  function addCondition(branch: ConditionBranch) {
    updateBranch(branch.id, {
      conditions: [...getConditions(branch), { id: `cond-${Date.now()}` }],
    });
  }

  function removeCondition(branch: ConditionBranch, conditionId: string) {
    updateBranch(branch.id, {
      conditions: getConditions(branch).filter((c) => c.id !== conditionId),
    });
  }

  return (
    <div className="flex flex-col gap-3">
      {branches.map((branch, idx) => {
        const conditions = getConditions(branch);
        const combinator = branch.combinator ?? "and";

        return (
          <div
            key={branch.id}
            className="flex flex-col gap-2 rounded-lg border border-neutral-200 bg-neutral-50 p-3"
          >
            {/* Branch header */}
            <div className="flex items-center justify-between gap-2">
              <span className="text-xs font-semibold text-neutral-500">
                Branch {idx + 1}
              </span>

              {branches.length > 1 && (
                <button
                  onClick={() => removeBranch(branch.id)}
                  className="text-neutral-300 transition-colors hover:text-red-border"
                  title="Remove branch"
                >
                  <span className="material-symbols-rounded text-[14px] leading-none">
                    delete
                  </span>
                </button>
              )}
            </div>

            <Field label="Label">
              <input
                type="text"
                value={branch.label}
                onChange={(e) =>
                  updateBranch(branch.id, { label: e.target.value })
                }
                placeholder="Branch label"
                className={inputCls}
              />
            </Field>

            {conditions.map((condition, ci) => {
              const hideValue = condition.operator
                ? NO_VALUE_OPERATORS.has(condition.operator)
                : false;

              return (
                <div key={condition.id} className="flex flex-col gap-2">
                  {ci > 0 && (
                    <div className="flex items-center justify-center">
                      <div className="flex rounded-full border border-neutral-200 bg-white p-0.5 text-[10px] font-semibold">
                        <button
                          type="button"
                          onClick={() =>
                            updateBranch(branch.id, { combinator: "and" })
                          }
                          className={`rounded-full px-2 py-0.5 transition-colors ${
                            combinator === "and"
                              ? "bg-secondary text-white"
                              : "text-neutral-400 hover:text-neutral-600"
                          }`}
                        >
                          AND
                        </button>
                        <button
                          type="button"
                          onClick={() =>
                            updateBranch(branch.id, { combinator: "or" })
                          }
                          className={`rounded-full px-2 py-0.5 transition-colors ${
                            combinator === "or"
                              ? "bg-secondary text-white"
                              : "text-neutral-400 hover:text-neutral-600"
                          }`}
                        >
                          OR
                        </button>
                      </div>
                    </div>
                  )}

                  <div className="flex flex-col gap-2 rounded-lg border border-neutral-200 bg-white p-2">
                    <div className="flex items-center justify-between gap-2">
                      <input
                        type="text"
                        value={condition.label ?? ""}
                        onChange={(e) =>
                          updateCondition(branch, condition.id, {
                            label: e.target.value,
                          })
                        }
                        placeholder={`Condition ${ci + 1}`}
                        className="flex-1 border-none bg-transparent p-0 text-[10px] font-semibold uppercase tracking-wide text-neutral-400 placeholder:text-neutral-400 focus:outline-none"
                      />

                      {conditions.length > 1 && (
                        <button
                          onClick={() => removeCondition(branch, condition.id)}
                          className="text-neutral-300 transition-colors hover:text-red-border"
                          title="Remove condition"
                        >
                          <span className="material-symbols-rounded text-[14px] leading-none">
                            delete
                          </span>
                        </button>
                      )}
                    </div>

                    <Field
                      label="Variable"
                      action={
                        <VariablePicker
                          variables={variables}
                          onSelect={(name) =>
                            updateCondition(branch, condition.id, {
                              variable: name,
                            })
                          }
                        />
                      }
                    >
                      <input
                        type="text"
                        value={condition.variable ?? ""}
                        onChange={(e) =>
                          updateCondition(branch, condition.id, {
                            variable: e.target.value,
                          })
                        }
                        placeholder="e.g. contact.name"
                        className={inputCls}
                      />
                    </Field>

                    <Field label="Operator">
                      <Select
                        value={condition.operator ?? "eq"}
                        onChange={(v) =>
                          updateCondition(branch, condition.id, {
                            operator: v as ConditionOperator,
                          })
                        }
                        options={CONDITION_OPERATORS}
                      />
                    </Field>

                    {!hideValue && (
                      <ConditionValueField
                        value={condition.value ?? ""}
                        onChange={(v) =>
                          updateCondition(branch, condition.id, { value: v })
                        }
                        variables={variables}
                      />
                    )}
                  </div>
                </div>
              );
            })}

            <button
              onClick={() => addCondition(branch)}
              className="flex w-full items-center justify-center gap-1.5 rounded-lg border border-dashed border-neutral-200 px-3 py-1.5 text-xs font-medium text-neutral-400 transition-colors hover:border-neutral-300 hover:text-neutral-600"
            >
              <span
                className="material-symbols-rounded text-[14px] leading-none"
                aria-hidden="true"
              >
                add
              </span>
              Add condition
            </button>
          </div>
        );
      })}

      <button
        onClick={addBranch}
        className="flex w-full items-center justify-center gap-1.5 rounded-lg border border-dashed border-neutral-300 px-3 py-2 text-xs font-medium text-neutral-500 transition-colors hover:border-neutral-400 hover:text-neutral-700"
      >
        <span
          className="material-symbols-rounded text-[14px] leading-none"
          aria-hidden="true"
        >
          add
        </span>
        Add branch
      </button>
    </div>
  );
}

/** Separate component so each branch value field has its own ref. */
function ConditionValueField({
  value,
  onChange,
  variables,
}: {
  value: string;
  onChange: (v: string) => void;
  variables: WorkflowVariable[];
}) {
  const inputRef = useRef<HTMLInputElement>(null);

  return (
    <Field
      label="Value"
      action={
        <VariablePicker
          variables={variables}
          onSelect={(name) =>
            insertAtCursor(inputRef.current, value, `{{${name}}}`, onChange)
          }
        />
      }
    >
      <input
        ref={inputRef}
        type="text"
        value={value}
        onChange={(e) => onChange(e.target.value)}
        placeholder="e.g. hello or {{variable}}"
        className={inputCls}
      />

      <FilterWarning text={value} />
    </Field>
  );
}

const HTTP_METHODS = ["GET", "POST", "PUT", "PATCH", "DELETE"].map((m) => ({
  value: m,
  label: m,
}));

const BODY_METHODS = new Set(["POST", "PUT", "PATCH"]);

const CONTENT_TYPES = [
  { value: "application/json", label: "JSON (application/json)" },
  {
    value: "application/x-www-form-urlencoded",
    label: "Form (application/x-www-form-urlencoded)",
  },
  { value: "text/plain", label: "Plain text (text/plain)" },
  { value: "application/xml", label: "XML (application/xml)" },
];

const BODY_PLACEHOLDERS: Record<string, string> = {
  "application/json": '{\n  "key": "value"\n}',
  "application/x-www-form-urlencoded": "key1=value1&key2=value2",
  "text/plain": "Your message here",
  "application/xml": "<root>\n  <key>value</key>\n</root>",
};

type HttpHeader = { id: string; key: string; value: string };

function KeyValueEditor({
  rows,
  onChange,
  keyPlaceholder = "Key",
  valuePlaceholder = "Value",
  variables,
}: {
  rows: HttpHeader[];
  onChange: (rows: HttpHeader[]) => void;
  keyPlaceholder?: string;
  valuePlaceholder?: string;
  variables: WorkflowVariable[];
}) {
  function updateRow(id: string, patch: Partial<HttpHeader>) {
    onChange(rows.map((r) => (r.id === id ? { ...r, ...patch } : r)));
  }

  function addRow() {
    onChange([...rows, { id: `row-${Date.now()}`, key: "", value: "" }]);
  }

  function removeRow(id: string) {
    onChange(rows.filter((r) => r.id !== id));
  }

  return (
    <div className="flex flex-col gap-2">
      {rows.map((row) => (
        <HeaderRow
          key={row.id}
          row={row}
          keyPlaceholder={keyPlaceholder}
          valuePlaceholder={valuePlaceholder}
          variables={variables}
          onUpdate={(patch) => updateRow(row.id, patch)}
          onRemove={() => removeRow(row.id)}
        />
      ))}

      <button
        onClick={addRow}
        className="flex w-full items-center justify-center gap-1.5 rounded-lg border border-dashed border-neutral-300 px-3 py-2 text-xs font-medium text-neutral-500 transition-colors hover:border-neutral-400 hover:text-neutral-700"
      >
        <span
          className="material-symbols-rounded text-[14px] leading-none"
          aria-hidden="true"
        >
          add
        </span>
        Add header
      </button>
    </div>
  );
}

/** Separate component so each header value field has its own ref. */
function HeaderRow({
  row,
  keyPlaceholder,
  valuePlaceholder,
  variables,
  onUpdate,
  onRemove,
}: {
  row: HttpHeader;
  keyPlaceholder: string;
  valuePlaceholder: string;
  variables: WorkflowVariable[];
  onUpdate: (patch: Partial<HttpHeader>) => void;
  onRemove: () => void;
}) {
  const valueRef = useRef<HTMLInputElement>(null);

  return (
    <div className="flex flex-col gap-1">
      <div className="flex items-center gap-1.5">
        <input
          type="text"
          value={row.key}
          onChange={(e) => onUpdate({ key: e.target.value })}
          placeholder={keyPlaceholder}
          className={`${inputCls} min-w-0 flex-1`}
        />

        <div className="relative flex min-w-0 flex-1 items-center">
          <input
            ref={valueRef}
            type="text"
            value={row.value}
            onChange={(e) => onUpdate({ value: e.target.value })}
            placeholder={valuePlaceholder}
            className={`${inputCls} pr-8`}
          />

          <div className="absolute right-1">
            <VariablePicker
              variables={variables}
              onSelect={(name) =>
                insertAtCursor(
                  valueRef.current,
                  row.value,
                  `{{${name}}}`,
                  (v) => onUpdate({ value: v })
                )
              }
            />
          </div>
        </div>

        <button
          onClick={onRemove}
          className="flex-shrink-0 text-neutral-300 transition-colors hover:text-red-border"
          title="Remove"
        >
          <span className="material-symbols-rounded text-[14px] leading-none">
            delete
          </span>
        </button>
      </div>

      <FilterWarning text={row.value} />
    </div>
  );
}

type ResponseMappingRow = { id: string; path: string; variable: string };

function ResponseMappingEditor({
  rows,
  onChange,
}: {
  rows: ResponseMappingRow[];
  onChange: (rows: ResponseMappingRow[]) => void;
}) {
  function updateRow(id: string, patch: Partial<ResponseMappingRow>) {
    onChange(rows.map((r) => (r.id === id ? { ...r, ...patch } : r)));
  }

  function addRow() {
    onChange([...rows, { id: `map-${Date.now()}`, path: "", variable: "" }]);
  }

  function removeRow(id: string) {
    onChange(rows.filter((r) => r.id !== id));
  }

  return (
    <div className="flex flex-col gap-2">
      {rows.map((row) => (
        <div key={row.id} className="flex items-center gap-1.5">
          <span className="flex-shrink-0 font-mono text-[11px] text-neutral-400">
            $resp.
          </span>

          <input
            type="text"
            value={row.path}
            onChange={(e) => updateRow(row.id, { path: e.target.value })}
            placeholder="id"
            className={`${inputCls} min-w-0 flex-1`}
          />

          <span className="flex-shrink-0 text-xs text-neutral-400">→</span>

          <input
            type="text"
            value={row.variable}
            onChange={(e) => updateRow(row.id, { variable: e.target.value })}
            placeholder="variable"
            className={`${inputCls} min-w-0 flex-1`}
          />

          <button
            onClick={() => removeRow(row.id)}
            className="flex-shrink-0 text-neutral-300 transition-colors hover:text-red-border"
            title="Remove"
          >
            <span className="material-symbols-rounded text-[14px] leading-none">
              delete
            </span>
          </button>
        </div>
      ))}

      <button
        onClick={addRow}
        className="flex w-full items-center justify-center gap-1.5 rounded-lg border border-dashed border-neutral-300 px-3 py-2 text-xs font-medium text-neutral-500 transition-colors hover:border-neutral-400 hover:text-neutral-700"
      >
        <span
          className="material-symbols-rounded text-[14px] leading-none"
          aria-hidden="true"
        >
          add
        </span>
        Add mapping
      </button>
    </div>
  );
}

function HttpRequestForm({
  data,
  onChange,
  variables,
}: {
  data: Record<string, unknown>;
  onChange: (u: Record<string, unknown>) => void;
  variables: WorkflowVariable[];
}) {
  const urlRef = useRef<HTMLInputElement>(null);
  const bodyRef = useRef<HTMLTextAreaElement>(null);

  const method = (data.method as string) ?? "GET";
  const url = (data.url as string) ?? "";
  const body = (data.body as string) ?? "";
  const headers = (data.headers as HttpHeader[]) ?? [];
  const showBody = BODY_METHODS.has(method);

  return (
    <>
      <Field label="Method">
        <Select
          value={method}
          onChange={(v) => onChange({ method: v })}
          options={HTTP_METHODS}
        />
      </Field>

      <Field
        label="URL"
        action={
          <VariablePicker
            variables={variables}
            onSelect={(name) =>
              insertAtCursor(urlRef.current, url, `{{${name}}}`, (v) =>
                onChange({ url: v })
              )
            }
          />
        }
      >
        <input
          ref={urlRef}
          type="text"
          value={url}
          onChange={(e) => onChange({ url: e.target.value })}
          placeholder="https://example.com/api"
          className={inputCls}
        />

        <FilterWarning text={url} />
      </Field>

      <Field label="Headers">
        <KeyValueEditor
          rows={headers}
          onChange={(rows) => onChange({ headers: rows })}
          keyPlaceholder="Header name"
          valuePlaceholder="Value"
          variables={variables}
        />
      </Field>

      {showBody && (
        <>
          <Field label="Content type">
            <Select
              value={(data.contentType as string) ?? "application/json"}
              onChange={(v) => onChange({ contentType: v })}
              options={CONTENT_TYPES}
            />
          </Field>

          <Field
            label="Body"
            action={
              <VariablePicker
                variables={variables}
                onSelect={(name) =>
                  insertAtCursor(bodyRef.current, body, `{{${name}}}`, (v) =>
                    onChange({ body: v })
                  )
                }
              />
            }
          >
            <textarea
              ref={bodyRef}
              value={body}
              onChange={(e) => onChange({ body: e.target.value })}
              placeholder={
                BODY_PLACEHOLDERS[
                  (data.contentType as string) ?? "application/json"
                ]
              }
              rows={5}
              className={`${inputCls} resize-none font-mono text-xs`}
            />

            <FilterWarning text={body} />
          </Field>
        </>
      )}

      <Field label="Timeout (seconds)">
        <input
          type="number"
          min={1}
          max={300}
          value={(data.timeoutSeconds as number) ?? 30}
          onChange={(e) =>
            onChange({
              timeoutSeconds: e.target.value
                ? Number(e.target.value)
                : undefined,
            })
          }
          className={inputCls}
        />
      </Field>

      {/* ── Response ─────────────────────────────────────────────────── */}
      <div className="flex items-center gap-2">
        <div className="h-px flex-1 bg-neutral-200" />
        <span className="text-[10px] font-semibold uppercase tracking-wide text-neutral-400">
          Response
        </span>
        <div className="h-px flex-1 bg-neutral-200" />
      </div>

      <Field label="Status code">
        <div className="flex items-center gap-2">
          <span className="flex-shrink-0 text-xs text-neutral-400">
            Save to
          </span>
          <input
            type="text"
            value={(data.responseStatusVariable as string) ?? ""}
            onChange={(e) =>
              onChange({ responseStatusVariable: e.target.value })
            }
            placeholder="e.g. statusCode"
            className={`${inputCls} flex-1`}
          />
        </div>
      </Field>

      <Field label="Map response fields">
        <ResponseMappingEditor
          rows={(data.responseMappings as ResponseMappingRow[]) ?? []}
          onChange={(rows) => onChange({ responseMappings: rows })}
        />
      </Field>
    </>
  );
}

function SetVariableForm({
  data,
  onChange,
  variables,
}: {
  data: Record<string, unknown>;
  onChange: (u: Record<string, unknown>) => void;
  variables: WorkflowVariable[];
}) {
  const valueRef = useRef<HTMLInputElement>(null);
  const value = (data.value as string) ?? "";

  return (
    <>
      <Field label="Variable name">
        <input
          type="text"
          value={(data.variableName as string) ?? ""}
          onChange={(e) => onChange({ variableName: e.target.value })}
          placeholder="e.g. userCity"
          className={inputCls}
        />
      </Field>

      <Field
        label="Value"
        action={
          <VariablePicker
            variables={variables}
            onSelect={(name) =>
              insertAtCursor(valueRef.current, value, `{{${name}}}`, (v) =>
                onChange({ value: v })
              )
            }
          />
        }
      >
        <input
          ref={valueRef}
          type="text"
          value={value}
          onChange={(e) => onChange({ value: e.target.value })}
          placeholder="e.g. {{contact.name}} or a static value"
          className={inputCls}
        />

        <FilterWarning text={value} />
      </Field>
    </>
  );
}

function SetContactFieldForm({
  data,
  onChange,
  variables,
  fieldOptions,
}: {
  data: Record<string, unknown>;
  onChange: (u: Record<string, unknown>) => void;
  variables: WorkflowVariable[];
  fieldOptions: SelectOption[];
}) {
  const valueRef = useRef<HTMLInputElement>(null);
  const value = (data.value as string) ?? "";

  return (
    <>
      <Field label="Contact field">
        <Select
          value={(data.fieldKey as string) ?? ""}
          onChange={(v) => onChange({ fieldKey: v })}
          options={fieldOptions}
          placeholder="Select a field…"
        />
      </Field>

      <Field
        label="Value"
        action={
          <VariablePicker
            variables={variables}
            onSelect={(name) =>
              insertAtCursor(valueRef.current, value, `{{${name}}}`, (v) =>
                onChange({ value: v })
              )
            }
          />
        }
      >
        <input
          ref={valueRef}
          type="text"
          value={value}
          onChange={(e) => onChange({ value: e.target.value })}
          placeholder="e.g. {{agent.data.city}} or a static value"
          className={inputCls}
        />

        <FilterWarning text={value} />
      </Field>
    </>
  );
}

function EndConversationForm({
  data,
  onChange,
  variables,
}: {
  data: Record<string, unknown>;
  onChange: (u: Record<string, unknown>) => void;
  variables: WorkflowVariable[];
}) {
  const textareaRef = useRef<HTMLTextAreaElement>(null);
  const message = (data.message as string) ?? "";

  return (
    <>
      <Field
        label="Closing message (optional)"
        action={
          <VariablePicker
            variables={variables}
            onSelect={(name) =>
              insertAtCursor(textareaRef.current, message, `{{${name}}}`, (v) =>
                onChange({ message: v })
              )
            }
          />
        }
      >
        <textarea
          ref={textareaRef}
          value={message}
          onChange={(e) => onChange({ message: e.target.value })}
          placeholder="Type a closing message… or leave empty to close silently"
          rows={4}
          className={`${inputCls} resize-none`}
        />

        <FilterWarning text={message} />
      </Field>

      <p className="text-xs text-neutral-400">
        Closes the conversation. No further steps will run after this node.
      </p>
    </>
  );
}

const RESPONSE_TYPE_OPTIONS = [
  { value: "generic", label: "Open-ended — save reply to variable" },
  { value: "defined", label: "Defined options" },
];

function WaitForReplyForm({
  nodeId,
  data,
  onChange,
  variables,
}: {
  nodeId: string;
  data: Record<string, unknown>;
  onChange: (u: Record<string, unknown>) => void;
  variables: WorkflowVariable[];
}) {
  const { setEdges, getNodes } = useReactFlow();
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  const question = (data.question as string) ?? "";
  const responseType = (data.responseType as string) ?? "generic";
  const options: WaitForReplyOption[] =
    (data.options as WaitForReplyOption[]) ?? [];

  // The combined timeout of every "Wait for Reply" node in the workflow may not
  // exceed MAX_TOTAL_TIMEOUT_MINUTES — this node's max is whatever's left over.
  const otherWaitTimeoutTotal = getNodes()
    .filter((n) => n.type === "waitForReply" && n.id !== nodeId)
    .reduce(
      (sum, n) =>
        sum +
        (((n.data as Record<string, unknown>).timeoutMinutes as number) ??
          DEFAULT_TIMEOUT_MINUTES),
      0
    );
  const maxTimeoutMinutes = Math.max(
    1,
    MAX_TOTAL_TIMEOUT_MINUTES - otherWaitTimeoutTotal
  );

  function updateOption(id: string, text: string) {
    onChange({
      options: options.map((o) => (o.id === id ? { ...o, text } : o)),
    });
  }

  function addOption() {
    const newId = `opt-${Date.now()}`;
    onChange({ options: [...options, { id: newId, text: "" }] });
  }

  function removeOption(id: string) {
    onChange({ options: options.filter((o) => o.id !== id) });
    setEdges((eds) =>
      eds.filter((e) => !(e.source === nodeId && e.sourceHandle === id))
    );
  }

  return (
    <>
      <Field
        label="Question"
        action={
          <VariablePicker
            variables={variables}
            onSelect={(name) =>
              insertAtCursor(
                textareaRef.current,
                question,
                `{{${name}}}`,
                (v) => onChange({ question: v })
              )
            }
          />
        }
      >
        <textarea
          ref={textareaRef}
          value={question}
          onChange={(e) => onChange({ question: e.target.value })}
          placeholder="e.g. What can I help you with today?"
          rows={3}
          className={`${inputCls} resize-none`}
        />

        <FilterWarning text={question} />
      </Field>

      <Field label="Expected response">
        <Select
          value={responseType}
          onChange={(v) => onChange({ responseType: v })}
          options={RESPONSE_TYPE_OPTIONS}
        />
      </Field>

      {responseType === "generic" && (
        <Field label="Save reply to variable">
          <input
            type="text"
            value={(data.responseVariable as string) ?? ""}
            onChange={(e) => onChange({ responseVariable: e.target.value })}
            placeholder="e.g. customerReply"
            className={inputCls}
          />
        </Field>
      )}

      {responseType === "defined" && (
        <>
          {options.map((opt, i) => (
            <div key={opt.id}>
              <div className="flex items-center gap-2">
                <span className="flex-shrink-0 text-xs text-neutral-400">
                  {i + 1}.
                </span>

                <input
                  type="text"
                  value={opt.text}
                  onChange={(e) => updateOption(opt.id, e.target.value)}
                  placeholder={`Option ${i + 1} text`}
                  className={`${inputCls} flex-1`}
                />

                <button
                  onClick={() => removeOption(opt.id)}
                  className="flex-shrink-0 text-neutral-300 transition-colors hover:text-red-border"
                  title="Remove option"
                >
                  <span className="material-symbols-rounded text-[14px] leading-none">
                    delete
                  </span>
                </button>
              </div>

              {options.length <= 3 &&
                opt.text.length > WHATSAPP_BUTTON_TITLE_LIMIT && (
                  <p className="ml-5 text-xs text-red-text">
                    WhatsApp buttons are limited to{" "}
                    {WHATSAPP_BUTTON_TITLE_LIMIT} characters — this will be
                    truncated.
                  </p>
                )}
            </div>
          ))}

          <button
            onClick={addOption}
            className="flex w-full items-center justify-center gap-1.5 rounded-lg border border-dashed border-neutral-300 px-3 py-2 text-xs font-medium text-neutral-500 transition-colors hover:border-neutral-400 hover:text-neutral-700"
          >
            <span
              className="material-symbols-rounded text-[14px] leading-none"
              aria-hidden="true"
            >
              add
            </span>
            Add option
          </button>

          <Field label="Save selection to variable">
            <input
              type="text"
              value={(data.responseVariable as string) ?? ""}
              onChange={(e) => onChange({ responseVariable: e.target.value })}
              placeholder="e.g. selectedOption"
              className={inputCls}
            />
          </Field>

          <p className="text-xs text-neutral-400">
            Replies that don&apos;t match any option follow the{" "}
            <span className="font-medium text-neutral-500">Other</span> route.
            Typing a number (1, 2…) also matches the corresponding option.
          </p>
        </>
      )}

      <Field label="Reply timeout (minutes)">
        <input
          type="number"
          min={1}
          max={maxTimeoutMinutes}
          value={(data.timeoutMinutes as number | undefined) ?? ""}
          onChange={(e) =>
            onChange({
              timeoutMinutes: e.target.value
                ? Math.min(Number(e.target.value), maxTimeoutMinutes)
                : undefined,
            })
          }
          className={inputCls}
        />
        <p className="mt-1 text-xs text-neutral-400">
          If the contact doesn&apos;t reply within this time, the run fails and
          the conversation is released. The combined timeout across all
          &quot;Wait for Reply&quot; nodes in this workflow can&apos;t exceed 7
          days — up to {maxTimeoutMinutes} minutes left for this node.
        </p>
      </Field>
    </>
  );
}

function JumpToForm({
  nodeId,
  data,
  onChange,
  nodes,
}: {
  nodeId: string;
  data: Record<string, unknown>;
  onChange: (u: Record<string, unknown>) => void;
  nodes: Node[];
}) {
  const nodeOptions: SelectOption[] = nodes
    .filter((n) => n.id !== nodeId)
    .map((n) => ({
      value: n.id,
      label:
        ((n.data as Record<string, unknown>).label as string) ?? n.type ?? n.id,
    }));

  return (
    <>
      <Field label="Target node">
        <Select
          value={(data.targetNodeId as string) ?? ""}
          placeholder="Select a node…"
          onChange={(v) => {
            const targetNode = nodes.find((n) => n.id === v);
            const targetLabel =
              ((targetNode?.data as Record<string, unknown>)
                ?.label as string) ??
              targetNode?.type ??
              v;
            onChange({ targetNodeId: v, targetNodeLabel: targetLabel });
          }}
          options={nodeOptions}
        />
      </Field>

      <Field label="Max jumps">
        <input
          type="number"
          min={1}
          max={20}
          value={(data.maxJumps as number) ?? 1}
          onChange={(e) =>
            onChange({ maxJumps: e.target.value ? Number(e.target.value) : 1 })
          }
          className={inputCls}
        />
      </Field>

      <p className="text-xs text-neutral-400">
        Once the jump limit is reached the workflow terminates instead of
        looping.
      </p>
    </>
  );
}

// ─── panel labels ─────────────────────────────────────────────────────────────

const TYPE_LABEL: Record<string, string> = {
  trigger: "Trigger",
  sendMessage: "Send Message",
  condition: "Condition",
  httpRequest: "HTTP Request",
  setVariable: "Set Variable",
  setContactField: "Set Contact Field",
  endConversation: "End Conversation",
  waitForReply: "Ask Question",
  jumpTo: "Jump To",
};

// ─── panel ────────────────────────────────────────────────────────────────────

export function NodeConfigPanel({
  node,
  nodes,
  workspaceId,
  onClose,
  onDirty,
}: Props) {
  const { setNodes } = useReactFlow();
  // key={node.id} is set by the parent (WorkflowEditor) so this component
  // remounts whenever the selected node changes — no sync effect needed.
  const [data, setData] = useState<Record<string, unknown>>(
    node.data as Record<string, unknown>
  );

  const { data: aiAgentConfiguration } = useAiAgentConfiguration(workspaceId);
  const extractionFieldVariables: WorkflowVariable[] = (
    aiAgentConfiguration?.extractionFields ?? []
  ).map((field) => ({
    name: `agent.data.${field.key}`,
    label: field.description || field.key,
    group: "built-in",
  }));

  const workspace = useWorkspace(workspaceId);
  const contactFieldVariables: WorkflowVariable[] = (
    workspace?.contactFieldDefinitions ?? []
  ).map((field) => ({
    name: `contact.data.${field.key}`,
    label: field.label || field.key,
    group: "built-in",
  }));

  const writableContactFieldOptions: SelectOption[] = [
    ...RESERVED_CONTACT_FIELD_KEYS.map((key) => ({
      value: key,
      label: RESERVED_CONTACT_FIELDS[key].label,
    })),
    ...(workspace?.contactFieldDefinitions ?? []).map((field) => ({
      value: field.key,
      label: field.label || field.key,
    })),
  ];

  const variables: WorkflowVariable[] = [
    ...BUILT_IN_VARIABLES,
    ...extractionFieldVariables,
    ...contactFieldVariables,
    ...extractWorkflowVariables(nodes),
  ];

  function update(updates: Record<string, unknown>) {
    const next = { ...data, ...updates };
    setData(next);
    setNodes((nds) =>
      nds.map((n) => (n.id === node.id ? { ...n, data: next } : n))
    );
    onDirty();
  }

  return (
    <aside className="flex w-96 flex-shrink-0 flex-col border-l border-neutral-300 bg-neutral-50">
      {/* Header */}
      <div className="flex items-center justify-between border-b border-neutral-300 px-4 py-3">
        <span className="text-sm font-semibold text-neutral-800">
          {TYPE_LABEL[node.type ?? ""] ?? "Node"}
        </span>

        <button
          onClick={onClose}
          className="flex items-center rounded-md p-1 text-neutral-400 transition-colors hover:bg-neutral-200 hover:text-neutral-700"
        >
          <span
            className="material-symbols-rounded text-[16px] leading-none"
            aria-hidden="true"
          >
            close
          </span>
        </button>
      </div>

      {/* Form */}
      <div className="flex flex-1 flex-col gap-4 overflow-y-auto p-4">
        {/* Label — common to all nodes */}
        <Field label="Label">
          <input
            type="text"
            value={(data.label as string) ?? ""}
            onChange={(e) => update({ label: e.target.value })}
            placeholder="Node label"
            className={inputCls}
          />
        </Field>

        {/* Type-specific fields */}
        {node.type === "trigger" && (
          <TriggerForm data={data} onChange={update} />
        )}
        {node.type === "sendMessage" && (
          <SendMessageForm
            data={data}
            onChange={update}
            variables={variables}
          />
        )}
        {node.type === "condition" && (
          <ConditionForm
            nodeId={node.id}
            data={data}
            onChange={update}
            variables={variables}
          />
        )}
        {node.type === "httpRequest" && (
          <HttpRequestForm
            data={data}
            onChange={update}
            variables={variables}
          />
        )}
        {node.type === "setVariable" && (
          <SetVariableForm
            data={data}
            onChange={update}
            variables={variables}
          />
        )}
        {node.type === "setContactField" && (
          <SetContactFieldForm
            data={data}
            onChange={update}
            variables={variables}
            fieldOptions={writableContactFieldOptions}
          />
        )}
        {node.type === "endConversation" && (
          <EndConversationForm
            data={data}
            onChange={update}
            variables={variables}
          />
        )}
        {node.type === "waitForReply" && (
          <WaitForReplyForm
            nodeId={node.id}
            data={data}
            onChange={update}
            variables={variables}
          />
        )}
        {node.type === "jumpTo" && (
          <JumpToForm
            nodeId={node.id}
            data={data}
            onChange={update}
            nodes={nodes}
          />
        )}
      </div>
    </aside>
  );
}
