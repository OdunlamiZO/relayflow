"use client";

// Docs: docs/codebase-components.md#nodeconfigpanel
import { useRef } from "react";
import { useState } from "react";

import { type Node, useReactFlow } from "@xyflow/react";

import { FormattedText } from "@/components/common/FormattedText";
import { IconButton } from "@/components/common/IconButton";
import { Select } from "@/components/common/Select";
import { type SelectOption } from "@/components/common/Select";
import { useAiAgentConfigurations } from "@/hooks/use-ai-agent-configurations";
import { useBuiltInHooks } from "@/hooks/use-built-in-hooks";
import { useHooks } from "@/hooks/use-hooks";
import { useSecrets } from "@/hooks/use-secrets";
import { useWorkspace } from "@/hooks/use-workspaces";
import {
  type FormattingMarker,
  bulletLines,
  wrapSelection,
} from "@/lib/message-formatting";
import {
  type ContactTagDefinition,
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

function extractWorkflowVariables(nodes: Node[]): WorkflowVariable[] {
  const seen = new Set<string>();
  const variables: WorkflowVariable[] = [];

  for (const node of nodes) {
    const data = node.data as Record<string, unknown>;

    if (node.type === "setVariable") {
      const name = (data.variableName as string | undefined)?.trim();

      if (name && !seen.has(name)) {
        seen.add(name);
        variables.push({ name, label: name, group: "workflow" });
      }
    }

    if (node.type === "httpRequest") {
      const statusVariable = (
        data.responseStatusVariable as string | undefined
      )?.trim();

      if (statusVariable && !seen.has(statusVariable)) {
        seen.add(statusVariable);
        variables.push({
          name: statusVariable,
          label: statusVariable,
          group: "workflow",
        });
      }

      const mappings =
        (data.responseMappings as Array<{ variable?: string }>) ?? [];

      for (const mapping of mappings) {
        const variableName = mapping.variable?.trim();

        if (variableName && !seen.has(variableName)) {
          seen.add(variableName);
          variables.push({
            name: variableName,
            label: variableName,
            group: "workflow",
          });
        }
      }
    }

    if (node.type === "waitForReply") {
      const responseVariable = (
        data.responseVariable as string | undefined
      )?.trim();

      if (responseVariable && !seen.has(responseVariable)) {
        seen.add(responseVariable);
        variables.push({
          name: responseVariable,
          label: responseVariable,
          group: "workflow",
        });
      }
    }
  }

  return variables;
}

function insertAtCursor(
  element: HTMLInputElement | HTMLTextAreaElement | null,
  currentValue: string,
  text: string,
  onChange: (value: string) => void
) {
  if (element) {
    const start = element.selectionStart ?? currentValue.length;
    const end = element.selectionEnd ?? currentValue.length;
    const newValue =
      currentValue.slice(0, start) + text + currentValue.slice(end);

    onChange(newValue);

    requestAnimationFrame(() => {
      element.focus();
      element.setSelectionRange(start + text.length, start + text.length);
    });
  } else {
    onChange(currentValue + text);
  }
}

const FORMATTING_ACTIONS: {
  marker: FormattingMarker;
  icon: string;
  label: string;
}[] = [
  { marker: "*", icon: "format_bold", label: "Bold" },
  { marker: "_", icon: "format_italic", label: "Italic" },
  { marker: "~", icon: "strikethrough_s", label: "Strikethrough" },
  { marker: "`", icon: "code", label: "Code" },
  { marker: "```", icon: "data_object", label: "Code block" },
];

function FormattingToolbar({
  textareaRef,
  value,
  onChange,
}: {
  textareaRef: React.RefObject<HTMLTextAreaElement | null>;
  value: string;
  onChange: (nextValue: string) => void;
}) {
  function applyMarker(marker: FormattingMarker) {
    const textarea = textareaRef.current;
    const start = textarea?.selectionStart ?? value.length;
    const end = textarea?.selectionEnd ?? value.length;
    const wrapped = wrapSelection(value, start, end, marker);

    onChange(wrapped.value);

    requestAnimationFrame(() => {
      textarea?.focus();
      textarea?.setSelectionRange(wrapped.selectionStart, wrapped.selectionEnd);
    });
  }

  function applyBullets() {
    const textarea = textareaRef.current;
    const start = textarea?.selectionStart ?? value.length;
    const end = textarea?.selectionEnd ?? value.length;
    const bulleted = bulletLines(value, start, end);

    onChange(bulleted.value);

    requestAnimationFrame(() => {
      textarea?.focus();
      textarea?.setSelectionRange(
        bulleted.selectionStart,
        bulleted.selectionEnd
      );
    });
  }

  return (
    <div className="mb-1 flex items-center gap-0.5">
      {FORMATTING_ACTIONS.map((action) => (
        <IconButton
          key={action.marker}
          icon={action.icon}
          label={action.label}
          onClick={() => applyMarker(action.marker)}
        />
      ))}
      <IconButton
        icon="format_list_bulleted"
        label="Bulleted list"
        onClick={applyBullets}
      />
    </div>
  );
}

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

const TRIGGER_EVENTS = [
  { value: "conversation_opened", label: "Conversation Opened" },
  { value: "manual", label: "Manual" },
];

function TriggerForm({
  data,
  onChange,
}: {
  data: Record<string, unknown>;
  onChange: (updates: Record<string, unknown>) => void;
}) {
  return (
    <Field label="Trigger event">
      <Select
        value={(data.event as string) ?? "conversation_opened"}
        onChange={(value) => onChange({ event: value })}
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
  onChange: (updates: Record<string, unknown>) => void;
  variables: WorkflowVariable[];
}) {
  return (
    <FormattedMessageField
      label="Message"
      value={(data.message as string) ?? ""}
      onChange={(message) => onChange({ message })}
      variables={variables}
      placeholder="Type your message… use {{variable}} to insert values"
      rows={4}
    />
  );
}

function FormattedMessageField({
  label,
  value,
  onChange,
  variables,
  placeholder,
  rows,
}: {
  label: string;
  value: string;
  onChange: (value: string) => void;
  variables: WorkflowVariable[];
  placeholder: string;
  rows: number;
}) {
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  return (
    <Field
      label={label}
      action={
        <VariablePicker
          variables={variables}
          onSelect={(name) =>
            insertAtCursor(textareaRef.current, value, `{{${name}}}`, onChange)
          }
        />
      }
    >
      <FormattingToolbar
        textareaRef={textareaRef}
        value={value}
        onChange={onChange}
      />
      <textarea
        ref={textareaRef}
        value={value}
        onChange={(event) => onChange(event.target.value)}
        placeholder={placeholder}
        rows={rows}
        className={`${inputCls} resize-none`}
      />

      <FilterWarning text={value} />

      {value.trim() && (
        <div className="mt-2 rounded-lg border border-neutral-300 bg-neutral-200 px-3 py-2">
          <p className="mb-1 text-[11px] font-medium uppercase tracking-wide text-neutral-500">
            Preview
          </p>
          <p className="m-0 whitespace-pre-wrap break-words text-sm text-neutral-800">
            <FormattedText text={value} />
          </p>
        </div>
      )}
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

const DEFAULT_CONDITION_OPERATOR: ConditionOperator = "eq";

/** Operators that compare against a value; the value field is hidden for the rest. */
const NO_VALUE_OPERATORS = new Set<ConditionOperator>(["is_set", "is_not_set"]);

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
  onChange: (updates: Record<string, unknown>) => void;
  variables: WorkflowVariable[];
}) {
  const { setEdges } = useReactFlow();

  const branches: ConditionBranch[] =
    (data.branches as ConditionBranch[]) ?? DEFAULT_CONDITION_BRANCHES;

  function updateBranch(id: string, patch: Partial<ConditionBranch>) {
    onChange({
      branches: branches.map((branch) =>
        branch.id === id ? { ...branch, ...patch } : branch
      ),
    });
  }

  function addBranch() {
    const newId = `branch-${Date.now()}`;
    onChange({ branches: [...branches, { id: newId, label: "" }] });
  }

  function removeBranch(id: string) {
    onChange({ branches: branches.filter((branch) => branch.id !== id) });

    setEdges((currentEdges) =>
      currentEdges.filter(
        (edge) => !(edge.source === nodeId && edge.sourceHandle === id)
      )
    );
  }

  function getConditions(branch: ConditionBranch): ConditionRule[] {
    return branch.conditions && branch.conditions.length > 0
      ? branch.conditions
      : [{ id: "cond-0", operator: DEFAULT_CONDITION_OPERATOR }];
  }

  function updateCondition(
    branch: ConditionBranch,
    conditionId: string,
    patch: Partial<ConditionRule>
  ) {
    const conditions = getConditions(branch).map((condition) =>
      condition.id === conditionId ? { ...condition, ...patch } : condition
    );

    updateBranch(branch.id, { conditions });
  }

  function addCondition(branch: ConditionBranch) {
    updateBranch(branch.id, {
      conditions: [
        ...getConditions(branch),
        { id: `cond-${Date.now()}`, operator: DEFAULT_CONDITION_OPERATOR },
      ],
    });
  }

  function removeCondition(branch: ConditionBranch, conditionId: string) {
    updateBranch(branch.id, {
      conditions: getConditions(branch).filter(
        (condition) => condition.id !== conditionId
      ),
    });
  }

  return (
    <div className="flex flex-col gap-3">
      {branches.map((branch, index) => {
        const conditions = getConditions(branch);
        const combinator = branch.combinator ?? "and";
        const isLastBranch = index === branches.length - 1;

        return (
          <div
            key={branch.id}
            className="flex flex-col gap-2 rounded-lg border border-neutral-200 bg-neutral-50 p-3"
          >
            <div className="flex items-center justify-between gap-2">
              <span className="text-xs font-semibold text-neutral-500">
                Branch {index + 1}
              </span>

              {branches.length > 1 && !isLastBranch && (
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
                onChange={(event) =>
                  updateBranch(branch.id, { label: event.target.value })
                }
                placeholder={`Branch ${index + 1}`}
                className={inputCls}
              />
            </Field>

            {isLastBranch && (
              <p className="text-[11px] text-neutral-400">
                Default branch — taken when no other branch matches.
              </p>
            )}

            {!isLastBranch &&
              conditions.map((condition, conditionIndex) => {
                const hideValue = condition.operator
                  ? NO_VALUE_OPERATORS.has(condition.operator)
                  : false;

                return (
                  <div key={condition.id} className="flex flex-col gap-2">
                    {conditionIndex > 0 && (
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
                          onChange={(event) =>
                            updateCondition(branch, condition.id, {
                              label: event.target.value,
                            })
                          }
                          placeholder={`Condition ${conditionIndex + 1}`}
                          className="flex-1 border-none bg-transparent p-0 text-[10px] font-semibold uppercase tracking-wide text-neutral-400 placeholder:text-neutral-400 focus:outline-none"
                        />

                        {conditions.length > 1 && (
                          <button
                            onClick={() =>
                              removeCondition(branch, condition.id)
                            }
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
                          onChange={(event) =>
                            updateCondition(branch, condition.id, {
                              variable: event.target.value,
                            })
                          }
                          placeholder="e.g. contact.name"
                          className={inputCls}
                        />
                      </Field>

                      <Field label="Operator">
                        <Select
                          value={
                            condition.operator ?? DEFAULT_CONDITION_OPERATOR
                          }
                          onChange={(value) =>
                            updateCondition(branch, condition.id, {
                              operator: value as ConditionOperator,
                            })
                          }
                          options={CONDITION_OPERATORS}
                        />
                      </Field>

                      {!hideValue && (
                        <ConditionValueField
                          value={condition.value ?? ""}
                          onChange={(value) =>
                            updateCondition(branch, condition.id, {
                              value: value,
                            })
                          }
                          variables={variables}
                        />
                      )}
                    </div>
                  </div>
                );
              })}

            {!isLastBranch && (
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
            )}
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
  onChange: (nextValue: string) => void;
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
        onChange={(event) => onChange(event.target.value)}
        placeholder="e.g. hello or {{variable}}"
        className={inputCls}
      />

      <FilterWarning text={value} />
    </Field>
  );
}

const HTTP_METHODS = ["GET", "POST", "PUT", "PATCH", "DELETE"].map(
  (method) => ({
    value: method,
    label: method,
  })
);

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
    onChange(rows.map((row) => (row.id === id ? { ...row, ...patch } : row)));
  }

  function addRow() {
    onChange([...rows, { id: `row-${Date.now()}`, key: "", value: "" }]);
  }

  function removeRow(id: string) {
    onChange(rows.filter((row) => row.id !== id));
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
          onChange={(event) => onUpdate({ key: event.target.value })}
          placeholder={keyPlaceholder}
          className={`${inputCls} min-w-0 flex-1`}
        />

        <div className="relative flex min-w-0 flex-1 items-center">
          <input
            ref={valueRef}
            type="text"
            value={row.value}
            onChange={(event) => onUpdate({ value: event.target.value })}
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
                  (value) => onUpdate({ value: value })
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
    onChange(rows.map((row) => (row.id === id ? { ...row, ...patch } : row)));
  }

  function addRow() {
    onChange([...rows, { id: `map-${Date.now()}`, path: "", variable: "" }]);
  }

  function removeRow(id: string) {
    onChange(rows.filter((row) => row.id !== id));
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
            onChange={(event) =>
              updateRow(row.id, { path: event.target.value })
            }
            placeholder="id"
            className={`${inputCls} min-w-0 flex-1`}
          />

          <span className="flex-shrink-0 text-xs text-neutral-400">→</span>

          <input
            type="text"
            value={row.variable}
            onChange={(event) =>
              updateRow(row.id, { variable: event.target.value })
            }
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
  onChange: (updates: Record<string, unknown>) => void;
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
          onChange={(value) => onChange({ method: value })}
          options={HTTP_METHODS}
        />
      </Field>

      <Field
        label="URL"
        action={
          <VariablePicker
            variables={variables}
            onSelect={(name) =>
              insertAtCursor(urlRef.current, url, `{{${name}}}`, (value) =>
                onChange({ url: value })
              )
            }
          />
        }
      >
        <input
          ref={urlRef}
          type="text"
          value={url}
          onChange={(event) => onChange({ url: event.target.value })}
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
              onChange={(value) => onChange({ contentType: value })}
              options={CONTENT_TYPES}
            />
          </Field>

          <Field
            label="Body"
            action={
              <VariablePicker
                variables={variables}
                onSelect={(name) =>
                  insertAtCursor(
                    bodyRef.current,
                    body,
                    `{{${name}}}`,
                    (value) => onChange({ body: value })
                  )
                }
              />
            }
          >
            <textarea
              ref={bodyRef}
              value={body}
              onChange={(event) => onChange({ body: event.target.value })}
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
          onChange={(event) =>
            onChange({
              timeoutSeconds: event.target.value
                ? Number(event.target.value)
                : undefined,
            })
          }
          className={inputCls}
        />
      </Field>

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
            onChange={(event) =>
              onChange({ responseStatusVariable: event.target.value })
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
  onChange: (updates: Record<string, unknown>) => void;
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
          onChange={(event) => onChange({ variableName: event.target.value })}
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
              insertAtCursor(
                valueRef.current,
                value,
                `{{${name}}}`,
                (nextValue) => onChange({ value: nextValue })
              )
            }
          />
        }
      >
        <input
          ref={valueRef}
          type="text"
          value={value}
          onChange={(event) => onChange({ value: event.target.value })}
          placeholder="e.g. {{contact.name}} or a static value"
          className={inputCls}
        />

        <FilterWarning text={value} />
      </Field>
    </>
  );
}

function SetContactTagForm({
  data,
  onChange,
  tagDefinitions,
}: {
  data: Record<string, unknown>;
  onChange: (updates: Record<string, unknown>) => void;
  tagDefinitions: ContactTagDefinition[];
}) {
  const tagKey = (data.tagKey as string) ?? "";
  const selectedTag = tagDefinitions.find((tag) => tag.key === tagKey);

  return (
    <>
      <Field label="Contact tag">
        <Select
          value={tagKey}
          onChange={(value) => onChange({ tagKey: value, value: "" })}
          options={tagDefinitions.map((tag) => ({
            value: tag.key,
            label: tag.label || tag.key,
          }))}
          placeholder="Select a tag…"
        />
        {tagDefinitions.length === 0 && (
          <p className="mt-1 text-xs text-neutral-400">
            Define contact tags in Settings → General first.
          </p>
        )}
      </Field>

      {selectedTag && (
        <Field label="Value">
          <Select
            value={(data.value as string) ?? ""}
            onChange={(value) => onChange({ value: value })}
            options={[
              { value: "", label: "Clear tag" },
              ...selectedTag.values.map((value) => ({ value, label: value })),
            ]}
          />
        </Field>
      )}
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
  onChange: (updates: Record<string, unknown>) => void;
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
          onChange={(nextValue) => onChange({ fieldKey: nextValue })}
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
              insertAtCursor(
                valueRef.current,
                value,
                `{{${name}}}`,
                (nextValue) => onChange({ value: nextValue })
              )
            }
          />
        }
      >
        <input
          ref={valueRef}
          type="text"
          value={value}
          onChange={(event) => onChange({ value: event.target.value })}
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
  onChange: (updates: Record<string, unknown>) => void;
  variables: WorkflowVariable[];
}) {
  return (
    <>
      <FormattedMessageField
        label="Closing message (optional)"
        value={(data.message as string) ?? ""}
        onChange={(message) => onChange({ message })}
        variables={variables}
        placeholder="Type a closing message… or leave empty to close silently"
        rows={4}
      />

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
  workspaceId,
  nodeId,
  data,
  onChange,
  variables,
}: {
  workspaceId: string;
  nodeId: string;
  data: Record<string, unknown>;
  onChange: (updates: Record<string, unknown>) => void;
  variables: WorkflowVariable[];
}) {
  const { setEdges, getNodes } = useReactFlow();

  const question = (data.question as string) ?? "";
  const responseType = (data.responseType as string) ?? "generic";
  const options: WaitForReplyOption[] =
    (data.options as WaitForReplyOption[]) ?? [];

  // Every Wait for Reply node shares MAX_TOTAL_TIMEOUT_MINUTES, so this node's max is what's left.
  const otherWaitTimeoutTotal = getNodes()
    .filter((node) => node.type === "waitForReply" && node.id !== nodeId)
    .reduce(
      (sum, node) =>
        sum +
        (((node.data as Record<string, unknown>).timeoutMinutes as number) ??
          DEFAULT_TIMEOUT_MINUTES),
      0
    );
  const maxTimeoutMinutes = Math.max(
    1,
    MAX_TOTAL_TIMEOUT_MINUTES - otherWaitTimeoutTotal
  );

  function updateOption(id: string, text: string) {
    onChange({
      options: options.map((option) =>
        option.id === id ? { ...option, text } : option
      ),
    });
  }

  function addOption() {
    const newId = `opt-${Date.now()}`;
    onChange({ options: [...options, { id: newId, text: "" }] });
  }

  function removeOption(id: string) {
    onChange({ options: options.filter((option) => option.id !== id) });
    setEdges((currentEdges) =>
      currentEdges.filter(
        (currentEdge) =>
          !(currentEdge.source === nodeId && currentEdge.sourceHandle === id)
      )
    );
  }

  return (
    <>
      <FormattedMessageField
        label="Question"
        value={question}
        onChange={(value) => onChange({ question: value })}
        variables={variables}
        placeholder="e.g. What can I help you with today?"
        rows={3}
      />

      <Field label="Expected response">
        <Select
          value={responseType}
          onChange={(value) => onChange({ responseType: value })}
          options={RESPONSE_TYPE_OPTIONS}
        />
      </Field>

      {responseType === "generic" && (
        <>
          <Field label="Save reply to variable">
            <input
              type="text"
              value={(data.responseVariable as string) ?? ""}
              onChange={(event) =>
                onChange({ responseVariable: event.target.value })
              }
              placeholder="e.g. customerReply"
              className={inputCls}
            />
          </Field>

          <ReplyValidationFields
            workspaceId={workspaceId}
            nodeId={nodeId}
            data={data}
            onChange={onChange}
          />
        </>
      )}

      {responseType === "defined" && (
        <>
          {options.map((option, index) => (
            <div key={option.id}>
              <div className="flex items-center gap-2">
                <span className="flex-shrink-0 text-xs text-neutral-400">
                  {index + 1}.
                </span>

                <input
                  type="text"
                  value={option.text}
                  onChange={(event) =>
                    updateOption(option.id, event.target.value)
                  }
                  placeholder={`Option ${index + 1} text`}
                  className={`${inputCls} flex-1`}
                />

                <button
                  onClick={() => removeOption(option.id)}
                  className="flex-shrink-0 text-neutral-300 transition-colors hover:text-red-border"
                  title="Remove option"
                >
                  <span className="material-symbols-rounded text-[14px] leading-none">
                    delete
                  </span>
                </button>
              </div>

              {options.length <= 3 &&
                option.text.length > WHATSAPP_BUTTON_TITLE_LIMIT && (
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
              onChange={(event) =>
                onChange({ responseVariable: event.target.value })
              }
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
          onChange={(event) =>
            onChange({
              timeoutMinutes: event.target.value
                ? Math.min(Number(event.target.value), maxTimeoutMinutes)
                : undefined,
            })
          }
          className={inputCls}
        />
        <p className="mt-1 text-xs text-neutral-400">
          If the contact doesn&apos;t reply within this time, the step fails and
          the run follows the <strong>No reply</strong> output. If that
          isn&apos;t connected, a validated question follows{" "}
          <strong>Invalid</strong>; otherwise the run fails and the conversation
          is released. The combined timeout across all &quot;Wait for
          Reply&quot; nodes in this workflow can&apos;t exceed 7 days — up to{" "}
          {maxTimeoutMinutes} minutes left for this node.
        </p>
      </Field>
    </>
  );
}

const DEFAULT_MAX_ATTEMPTS = 3;

const MAX_ATTEMPTS_LIMIT = 10;

function ReplyValidationFields({
  workspaceId,
  nodeId,
  data,
  onChange,
}: {
  workspaceId: string;
  nodeId: string;
  data: Record<string, unknown>;
  onChange: (updates: Record<string, unknown>) => void;
}) {
  const { setEdges } = useReactFlow();
  const { data: builtInHooks = [], isSuccess: builtInHooksLoaded } =
    useBuiltInHooks(workspaceId);
  const { data: customHooks = [], isSuccess: customHooksLoaded } =
    useHooks(workspaceId);

  const validationHook = (data.validationHook as string | undefined) ?? "";

  const hookOptions: SelectOption[] = [
    { value: "", label: "No validation" },
    ...builtInHooks.map((hook) => ({ value: hook.key, label: hook.name })),
    ...customHooks.map((hook) => ({
      value: hook.key,
      label: `${hook.name} (custom)`,
    })),
  ];

  const selectedHook =
    builtInHooks.find((hook) => hook.key === validationHook) ??
    customHooks.find((hook) => hook.key === validationHook);

  const isUnavailable =
    validationHook !== "" &&
    builtInHooksLoaded &&
    customHooksLoaded &&
    !selectedHook;

  if (isUnavailable) {
    hookOptions.push({ value: validationHook, label: "Unavailable hook" });
  }

  function selectHook(hookKey: string) {
    const wasValidated = validationHook !== "";
    const isValidated = hookKey !== "";

    onChange({ validationHook: hookKey || undefined });

    if (!wasValidated && isValidated) {
      setEdges((edges) =>
        edges.map((edge) =>
          edge.source === nodeId && !edge.sourceHandle
            ? { ...edge, sourceHandle: "valid" }
            : edge
        )
      );
    }

    if (wasValidated && !isValidated) {
      setEdges((edges) =>
        edges
          .filter(
            (edge) =>
              !(edge.source === nodeId && edge.sourceHandle === "invalid")
          )
          .map((edge) =>
            edge.source === nodeId && edge.sourceHandle === "valid"
              ? { ...edge, sourceHandle: null }
              : edge
          )
      );
    }
  }

  return (
    <>
      <Field label="Validate reply">
        <Select
          value={validationHook}
          onChange={selectHook}
          options={hookOptions}
        />
        <p className="mt-1 text-xs text-neutral-400">
          {isUnavailable
            ? "This hook no longer exists. Choose another one or turn validation off."
            : selectedHook
              ? selectedHook.description
              : "Check the reply before saving it. Manage custom hooks in Settings → Hooks."}
        </p>
      </Field>

      {validationHook !== "" && (
        <>
          <Field label="Error message">
            <input
              type="text"
              value={(data.validationErrorMessage as string) ?? ""}
              onChange={(event) =>
                onChange({ validationErrorMessage: event.target.value })
              }
              placeholder={selectedHook?.errorMessage ?? "Please try again."}
              maxLength={500}
              className={inputCls}
            />
            <p className="mt-1 text-xs text-neutral-400">
              Sent when the reply is rejected. Leave blank to use the
              hook&apos;s own message.
            </p>
          </Field>

          <Field label="Maximum attempts">
            <input
              type="number"
              min={1}
              max={MAX_ATTEMPTS_LIMIT}
              value={(data.maxAttempts as number | undefined) ?? ""}
              placeholder={String(DEFAULT_MAX_ATTEMPTS)}
              onChange={(event) =>
                onChange({
                  maxAttempts: event.target.value
                    ? Math.min(
                        Math.max(Number(event.target.value), 1),
                        MAX_ATTEMPTS_LIMIT
                      )
                    : undefined,
                })
              }
              className={inputCls}
            />
            <p className="mt-1 text-xs text-neutral-400">
              How many tries the contact gets to send a valid reply. Each
              invalid reply gets the error message and the question waits for
              another reply. If every try is invalid, the workflow continues
              from the{" "}
              <span className="font-medium text-neutral-500">Invalid</span>{" "}
              output. If nothing is connected there, the workflow ends.
            </p>
          </Field>
        </>
      )}
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
  onChange: (updates: Record<string, unknown>) => void;
  nodes: Node[];
}) {
  const nodeOptions: SelectOption[] = nodes
    .filter((node) => node.id !== nodeId)
    .map((node) => ({
      value: node.id,
      label:
        ((node.data as Record<string, unknown>).label as string) ??
        node.type ??
        node.id,
    }));

  return (
    <>
      <Field label="Target node">
        <Select
          value={(data.targetNodeId as string) ?? ""}
          placeholder="Select a node…"
          onChange={(value) => {
            const targetNode = nodes.find((node) => node.id === value);
            const targetLabel =
              ((targetNode?.data as Record<string, unknown>)
                ?.label as string) ??
              targetNode?.type ??
              value;
            onChange({ targetNodeId: value, targetNodeLabel: targetLabel });
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
          onChange={(event) =>
            onChange({
              maxJumps: event.target.value ? Number(event.target.value) : 1,
            })
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

const TYPE_LABEL: Record<string, string> = {
  trigger: "Trigger",
  sendMessage: "Send Message",
  condition: "Condition",
  httpRequest: "HTTP Request",
  setVariable: "Set Variable",
  setContactField: "Set Contact Field",
  setContactTag: "Set Contact Tag",
  endConversation: "End Conversation",
  waitForReply: "Ask Question",
  jumpTo: "Jump To",
};

export function NodeConfigPanel({
  node,
  nodes,
  workspaceId,
  onClose,
  onDirty,
}: Props) {
  const { setNodes } = useReactFlow();
  // The parent keys this component by node.id, so it remounts when the selected node changes.
  const [data, setData] = useState<Record<string, unknown>>(
    node.data as Record<string, unknown>
  );

  const workspace = useWorkspace(workspaceId);
  const contactFieldKeys = new Set<string>([
    ...RESERVED_CONTACT_FIELD_KEYS,
    ...(workspace?.contactFieldDefinitions ?? []).map((field) => field.key),
  ]);

  const { data: aiAgentConfigurations = [] } =
    useAiAgentConfigurations(workspaceId);
  const defaultAiAgentConfiguration = aiAgentConfigurations.find(
    (aiAgentConfiguration) => aiAgentConfiguration.isDefault
  );
  const extractionFieldVariables: WorkflowVariable[] = (
    defaultAiAgentConfiguration?.extractionFields ?? []
  )
    .filter((field) => !contactFieldKeys.has(field.key))
    .map((field) => ({
      name: `agent.data.${field.key}`,
      label: field.description || field.key,
      group: "ai",
    }));

  const contactFieldVariables: WorkflowVariable[] = [
    ...RESERVED_CONTACT_FIELD_KEYS.map((key) => ({
      name: `contact.data.${key}`,
      label: RESERVED_CONTACT_FIELDS[key].label,
      group: "contact" as const,
    })),
    ...(workspace?.contactFieldDefinitions ?? []).map((field) => ({
      name: `contact.data.${field.key}`,
      label: field.label || field.key,
      group: "contact" as const,
    })),
    ...(workspace?.contactTagDefinitions ?? []).map((tag) => ({
      name: `contact.tags.${tag.key}`,
      label: tag.label || tag.key,
      group: "contact" as const,
    })),
  ];

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

  // secrets.NAME is only resolved by HttpRequestNodeExecutor, so it is offered only in that node's fields.
  const { data: secrets = [] } = useSecrets(workspaceId);
  const secretVariables: WorkflowVariable[] = secrets.map((secret) => ({
    name: `secrets.${secret.name}`,
    label: secret.name,
    group: "secrets",
  }));
  const httpRequestVariables: WorkflowVariable[] = [
    ...variables,
    ...secretVariables,
  ];

  function update(updates: Record<string, unknown>) {
    const next = { ...data, ...updates };
    setData(next);
    setNodes((currentNodes) =>
      currentNodes.map((currentNode) =>
        currentNode.id === node.id
          ? { ...currentNode, data: next }
          : currentNode
      )
    );
    onDirty();
  }

  return (
    <aside className="flex w-[28rem] flex-shrink-0 flex-col border-l border-neutral-300 bg-neutral-50">
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

      <div className="flex flex-1 flex-col gap-4 overflow-y-auto p-4">
        <Field label="Label">
          <input
            type="text"
            value={(data.label as string) ?? ""}
            onChange={(event) => update({ label: event.target.value })}
            placeholder="Node label"
            className={inputCls}
          />
        </Field>

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
            variables={httpRequestVariables}
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
        {node.type === "setContactTag" && (
          <SetContactTagForm
            data={data}
            onChange={update}
            tagDefinitions={workspace?.contactTagDefinitions ?? []}
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
            workspaceId={workspaceId}
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
