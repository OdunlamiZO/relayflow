"use client";

// Docs: docs/codebase-components.md#variablepicker
import { useEffect, useRef, useState } from "react";

export type WorkflowVariable = {
  name: string;
  label: string;
  group: "contact" | "ai" | "conversation" | "workflow" | "secrets";
};

/** Mirrors VariableInterpolator's filters on the backend. */
export const VARIABLE_FILTERS = {
  UPPER: "upper",
  LOWER: "lower",
  TITLE: "title",
} as const;

export type VariableFilter =
  (typeof VARIABLE_FILTERS)[keyof typeof VARIABLE_FILTERS];

const KNOWN_FILTERS: ReadonlySet<string> = new Set(
  Object.values(VARIABLE_FILTERS)
);

export function findUnknownFilters(text: string | undefined): string[] {
  if (!text) return [];

  const unknown = new Set<string>();

  for (const match of text.matchAll(/\{\{([^}]+)}}/g)) {
    for (const part of match[1].split("|").slice(1)) {
      const filter = part.trim().toLowerCase();

      if (filter && !KNOWN_FILTERS.has(filter)) {
        unknown.add(filter);
      }
    }
  }

  return Array.from(unknown);
}

export function FilterWarning({ text }: { text: string | undefined }) {
  const unknown = findUnknownFilters(text);

  if (unknown.length === 0) return null;

  return (
    <p className="text-xs text-red-text">
      Unknown filter{unknown.length > 1 ? "s" : ""}:{" "}
      {unknown.map((field) => `"${field}"`).join(", ")}. Supported:{" "}
      {Object.values(VARIABLE_FILTERS).join(", ")}.
    </p>
  );
}

export const BUILT_IN_VARIABLES: WorkflowVariable[] = [
  { name: "contact.id", label: "Contact ID", group: "contact" },
  { name: "contact.name", label: "Contact name", group: "contact" },
  { name: "contact.username", label: "Contact username", group: "contact" },
  {
    name: "contact.message",
    label: "Contact's opening message",
    group: "contact",
  },
  {
    name: "conversation.channel",
    label: "Conversation channel",
    group: "conversation",
  },
  {
    name: "agent.reply",
    label: "AI agent's reply (when triggered by the AI agent)",
    group: "ai",
  },
  {
    name: "agent.confidence",
    label: "AI agent's confidence (when triggered by the AI agent)",
    group: "ai",
  },
];

type Props = {
  variables: WorkflowVariable[];
  /** Receives the expression without braces, such as name or name | upper. */
  onSelect: (expression: string) => void;
};

export function VariablePicker({ variables, onSelect }: Props) {
  const [open, setOpen] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!open) return;

    function onMouseDown(event: MouseEvent) {
      if (
        containerRef.current &&
        !containerRef.current.contains(event.target as globalThis.Node)
      ) {
        setOpen(false);
      }
    }

    document.addEventListener("mousedown", onMouseDown);

    return () => document.removeEventListener("mousedown", onMouseDown);
  }, [open]);

  const sections: {
    title: string;
    items: WorkflowVariable[];
    allowFilters?: boolean;
  }[] = [
    {
      title: "Contact",
      items: variables.filter((variable) => variable.group === "contact"),
    },
    {
      title: "AI agent",
      items: variables.filter((variable) => variable.group === "ai"),
    },
    {
      title: "Conversation",
      items: variables.filter((variable) => variable.group === "conversation"),
    },
    {
      title: "From workflow",
      items: variables.filter((variable) => variable.group === "workflow"),
    },
    {
      title: "Secrets",
      items: variables.filter((variable) => variable.group === "secrets"),
      allowFilters: false,
    },
  ].filter((section) => section.items.length > 0);

  return (
    <div ref={containerRef} className="relative flex-shrink-0">
      <button
        type="button"
        onClick={() => setOpen((previous) => !previous)}
        title="Insert variable"
        className="rounded px-1.5 py-0.5 font-mono text-[10px] text-neutral-400 transition-colors hover:bg-secondary/10 hover:text-secondary"
      >
        {"{{…}}"}
      </button>

      {open && (
        <div className="absolute right-0 top-full z-50 mt-1 w-72 overflow-hidden rounded-lg border border-neutral-200 bg-neutral-100 shadow-lg">
          {sections.length === 0 && (
            <p className="px-3 py-2.5 text-[11px] text-neutral-400">
              No variables available yet.
            </p>
          )}

          {sections.map((section, index) => (
            <VariableGroup
              key={section.title}
              title={section.title}
              items={section.items}
              bordered={index > 0}
              allowFilters={section.allowFilters ?? true}
              onSelect={(name) => {
                onSelect(name);
                setOpen(false);
              }}
            />
          ))}
        </div>
      )}
    </div>
  );
}

function VariableGroup({
  title,
  items,
  bordered = false,
  allowFilters = true,
  onSelect,
}: {
  title: string;
  items: WorkflowVariable[];
  bordered?: boolean;
  allowFilters?: boolean;
  onSelect: (name: string) => void;
}) {
  return (
    <>
      <div
        className={`px-3 py-1.5 ${bordered ? "border-t border-neutral-100" : ""}`}
      >
        <span className="text-[10px] font-semibold uppercase tracking-wide text-neutral-400">
          {title}
        </span>
      </div>

      {items.map((variable) => (
        <div
          key={variable.name}
          className="flex items-center justify-between gap-1 px-3 py-1.5 transition-colors hover:bg-neutral-200"
        >
          <button
            type="button"
            onClick={() => onSelect(variable.name)}
            className="flex min-w-0 flex-1 flex-col gap-0.5 text-left"
          >
            <span className="truncate font-mono text-[11px] text-secondary">
              {"{{"}
              {variable.name}
              {"}}"}
            </span>

            {variable.label !== variable.name && (
              <span className="truncate text-[11px] text-neutral-400">
                {variable.label}
              </span>
            )}
          </button>

          {allowFilters && (
            <div className="flex flex-shrink-0 gap-0.5">
              <button
                type="button"
                title="Insert as UPPERCASE"
                onClick={() =>
                  onSelect(`${variable.name} | ${VARIABLE_FILTERS.UPPER}`)
                }
                className="rounded px-1 py-0.5 text-[10px] font-semibold text-neutral-400 transition-colors hover:bg-neutral-300 hover:text-neutral-700"
              >
                AA
              </button>

              <button
                type="button"
                title="Insert as lowercase"
                onClick={() =>
                  onSelect(`${variable.name} | ${VARIABLE_FILTERS.LOWER}`)
                }
                className="rounded px-1 py-0.5 text-[10px] font-semibold text-neutral-400 transition-colors hover:bg-neutral-300 hover:text-neutral-700"
              >
                aa
              </button>

              <button
                type="button"
                title="Insert as Title Case"
                onClick={() =>
                  onSelect(`${variable.name} | ${VARIABLE_FILTERS.TITLE}`)
                }
                className="rounded px-1 py-0.5 text-[10px] font-semibold text-neutral-400 transition-colors hover:bg-neutral-300 hover:text-neutral-700"
              >
                Aa
              </button>
            </div>
          )}
        </div>
      ))}
    </>
  );
}
