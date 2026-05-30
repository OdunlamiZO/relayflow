"use client";

import { useEffect, useRef, useState } from "react";

export type WorkflowVariable = {
  name: string;
  label: string;
  group: "built-in" | "workflow";
};

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
    name: "customer.intent",
    label: "Customer's opening message",
    group: "built-in",
  },
];

type Props = {
  variables: WorkflowVariable[];
  /** Called with the raw variable name (no braces). The caller decides how to format it. */
  onSelect: (name: string) => void;
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
        <div className="absolute right-0 top-full z-50 mt-1 w-52 overflow-hidden rounded-lg border border-neutral-200 bg-white shadow-lg">
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
        <button
          key={v.name}
          type="button"
          onClick={() => onSelect(v.name)}
          className="flex w-full flex-col gap-0.5 px-3 py-1.5 text-left transition-colors hover:bg-neutral-50"
        >
          <span className="font-mono text-[11px] text-secondary">
            {"{{"}
            {v.name}
            {"}}"}
          </span>

          {v.label !== v.name && (
            <span className="text-[11px] text-neutral-400">{v.label}</span>
          )}
        </button>
      ))}
    </>
  );
}
