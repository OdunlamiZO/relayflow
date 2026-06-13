"use client";

import { useEffect, useRef, useState } from "react";

export type WorkflowVariable = {
  name: string;
  label: string;
  group: "built-in" | "workflow";
};

/** Transforms supported by the `{{variable | filter}}` pipe syntax — mirrors VariableInterpolator on the backend. */
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

/** Returns the distinct, unrecognised `{{variable | filter}}` filter names used in `text`. */
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

/** Inline warning shown below a field when it contains an unrecognised `{{variable | filter}}`. */
export function FilterWarning({ text }: { text: string | undefined }) {
  const unknown = findUnknownFilters(text);

  if (unknown.length === 0) return null;

  return (
    <p className="text-xs text-red-text">
      Unknown filter{unknown.length > 1 ? "s" : ""}:{" "}
      {unknown.map((f) => `"${f}"`).join(", ")}. Supported:{" "}
      {Object.values(VARIABLE_FILTERS).join(", ")}.
    </p>
  );
}

/** Variables seeded into every workflow run — contact.id and internal IDs are intentionally excluded. */
export const BUILT_IN_VARIABLES: WorkflowVariable[] = [
  { name: "contact.name", label: "Contact name", group: "built-in" },
  { name: "contact.username", label: "Contact username", group: "built-in" },
  {
    name: "conversation.channel",
    label: "Conversation channel",
    group: "built-in",
  },
  {
    name: "contact.message",
    label: "Contact's opening message",
    group: "built-in",
  },
];

type Props = {
  variables: WorkflowVariable[];
  /**
   * Called with the placeholder expression (no braces) — either the raw variable name, or
   * `name | upper` / `name | lower` if a transform was picked. The caller decides how to format
   * it (e.g. wrapping in `{{...}}`).
   */
  onSelect: (expression: string) => void;
};

export function VariablePicker({ variables, onSelect }: Props) {
  const [open, setOpen] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!open) return;

    function onMouseDown(e: MouseEvent) {
      if (
        containerRef.current &&
        !containerRef.current.contains(e.target as globalThis.Node)
      ) {
        setOpen(false);
      }
    }

    document.addEventListener("mousedown", onMouseDown);

    return () => document.removeEventListener("mousedown", onMouseDown);
  }, [open]);

  const builtIn = variables.filter((v) => v.group === "built-in");
  const workflow = variables.filter((v) => v.group === "workflow");

  return (
    <div ref={containerRef} className="relative flex-shrink-0">
      <button
        type="button"
        onClick={() => setOpen((o) => !o)}
        title="Insert variable"
        className="rounded px-1.5 py-0.5 font-mono text-[10px] text-neutral-400 transition-colors hover:bg-secondary/10 hover:text-secondary"
      >
        {"{{…}}"}
      </button>

      {open && (
        <div className="absolute right-0 top-full z-50 mt-1 w-72 overflow-hidden rounded-lg border border-neutral-200 bg-neutral-100 shadow-lg">
          {variables.length === 0 && (
            <p className="px-3 py-2.5 text-[11px] text-neutral-400">
              No variables available yet.
            </p>
          )}

          {builtIn.length > 0 && (
            <VariableGroup
              title="Built-in"
              items={builtIn}
              onSelect={(name) => {
                onSelect(name);
                setOpen(false);
              }}
            />
          )}

          {workflow.length > 0 && (
            <VariableGroup
              title="From workflow"
              items={workflow}
              bordered={builtIn.length > 0}
              onSelect={(name) => {
                onSelect(name);
                setOpen(false);
              }}
            />
          )}
        </div>
      )}
    </div>
  );
}

function VariableGroup({
  title,
  items,
  bordered = false,
  onSelect,
}: {
  title: string;
  items: WorkflowVariable[];
  bordered?: boolean;
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

      {items.map((v) => (
        <div
          key={v.name}
          className="flex items-center justify-between gap-1 px-3 py-1.5 transition-colors hover:bg-neutral-200"
        >
          <button
            type="button"
            onClick={() => onSelect(v.name)}
            className="flex min-w-0 flex-1 flex-col gap-0.5 text-left"
          >
            <span className="truncate font-mono text-[11px] text-secondary">
              {"{{"}
              {v.name}
              {"}}"}
            </span>

            {v.label !== v.name && (
              <span className="truncate text-[11px] text-neutral-400">
                {v.label}
              </span>
            )}
          </button>

          <div className="flex flex-shrink-0 gap-0.5">
            <button
              type="button"
              title="Insert as UPPERCASE"
              onClick={() => onSelect(`${v.name} | ${VARIABLE_FILTERS.UPPER}`)}
              className="rounded px-1 py-0.5 text-[10px] font-semibold text-neutral-400 transition-colors hover:bg-neutral-300 hover:text-neutral-700"
            >
              AA
            </button>

            <button
              type="button"
              title="Insert as lowercase"
              onClick={() => onSelect(`${v.name} | ${VARIABLE_FILTERS.LOWER}`)}
              className="rounded px-1 py-0.5 text-[10px] font-semibold text-neutral-400 transition-colors hover:bg-neutral-300 hover:text-neutral-700"
            >
              aa
            </button>

            <button
              type="button"
              title="Insert as Title Case"
              onClick={() => onSelect(`${v.name} | ${VARIABLE_FILTERS.TITLE}`)}
              className="rounded px-1 py-0.5 text-[10px] font-semibold text-neutral-400 transition-colors hover:bg-neutral-300 hover:text-neutral-700"
            >
              Aa
            </button>
          </div>
        </div>
      ))}
    </>
  );
}
