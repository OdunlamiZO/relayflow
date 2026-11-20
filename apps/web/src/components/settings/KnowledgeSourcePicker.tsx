"use client";

// Docs: docs/codebase-components.md#knowledgesourcepicker
import { useEffect, useRef, useState } from "react";

import { Spinner } from "@/components/common/Spinner";
import { useKnowledgeSourceFields } from "@/hooks/use-knowledge-source-fields";
import { useKnowledgeSources } from "@/hooks/use-knowledge-sources";
import { errorMessage } from "@/lib/error-message";
import { type KnowledgeSource } from "@/lib/messaging-api";

type Props = {
  workspaceId: string;
  /** Receives the expression without braces, such as sources.rates.usd. */
  onSelect: (expression: string) => void;
};

const ROW_CLASS =
  "flex w-full flex-col gap-0.5 px-3 py-1.5 text-left transition-colors hover:bg-neutral-200";

export function KnowledgeSourcePicker({ workspaceId, onSelect }: Props) {
  const { data: sources = [], isLoading } = useKnowledgeSources(workspaceId);
  const [open, setOpen] = useState(false);
  const [source, setSource] = useState<KnowledgeSource | null>(null);
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

  function toggle() {
    setSource(null);
    setOpen((previous) => !previous);
  }

  function select(expression: string) {
    onSelect(expression);
    setOpen(false);
  }

  return (
    <div ref={containerRef} className="relative flex-shrink-0">
      <button
        type="button"
        onClick={toggle}
        title="Insert a knowledge source value"
        className="rounded px-1.5 py-0.5 font-mono text-[10px] text-neutral-400 transition-colors hover:bg-secondary/10 hover:text-secondary"
      >
        {"{{…}}"}
      </button>

      {open && (
        <div className="absolute right-0 top-full z-50 mt-1 max-h-72 w-72 overflow-y-auto rounded-lg border border-neutral-200 bg-neutral-100 shadow-lg">
          {source ? (
            <SourceFields
              workspaceId={workspaceId}
              source={source}
              onBack={() => setSource(null)}
              onSelect={select}
            />
          ) : (
            <>
              <Heading>Knowledge sources</Heading>

              {isLoading && (
                <div className="flex justify-center py-3">
                  <Spinner />
                </div>
              )}

              {!isLoading && sources.length === 0 && (
                <p className="px-3 py-2.5 text-[11px] text-neutral-400">
                  No knowledge sources yet. Add one in Settings → Knowledge
                  sources.
                </p>
              )}

              {sources.map((item) => (
                <button
                  key={item.id}
                  type="button"
                  onClick={() => setSource(item)}
                  className={ROW_CLASS}
                >
                  <span className="flex items-center justify-between gap-1">
                    <span className="truncate font-mono text-[11px] text-secondary">
                      {item.name}
                    </span>
                    <span
                      className="material-symbols-rounded text-[14px] leading-none text-neutral-400"
                      aria-hidden="true"
                    >
                      chevron_right
                    </span>
                  </span>
                  <span className="truncate text-[11px] text-neutral-400">
                    {item.url}
                  </span>
                </button>
              ))}
            </>
          )}
        </div>
      )}
    </div>
  );
}

function SourceFields({
  workspaceId,
  source,
  onBack,
  onSelect,
}: {
  workspaceId: string;
  source: KnowledgeSource;
  onBack: () => void;
  onSelect: (expression: string) => void;
}) {
  const { data, isLoading, error } = useKnowledgeSourceFields(
    workspaceId,
    source.id
  );
  const base = `sources.${source.name}`;
  const fetchError = error ? errorMessage(error) : data?.error;

  return (
    <>
      <button
        type="button"
        onClick={onBack}
        className="flex w-full items-center gap-1 px-3 py-1.5 text-left transition-colors hover:bg-neutral-200"
      >
        <span
          className="material-symbols-rounded text-[14px] leading-none text-neutral-400"
          aria-hidden="true"
        >
          chevron_left
        </span>
        <span className="truncate font-mono text-[11px] font-semibold text-neutral-600">
          {source.name}
        </span>
      </button>

      <button
        type="button"
        onClick={() => onSelect(base)}
        className={`${ROW_CLASS} border-t border-neutral-200`}
      >
        <span className="truncate font-mono text-[11px] text-secondary">
          {`{{${base}}}`}
        </span>
        <span className="truncate text-[11px] text-neutral-400">
          The whole response
        </span>
      </button>

      <Heading bordered>Fields</Heading>

      {isLoading && (
        <div className="flex justify-center py-3">
          <Spinner />
        </div>
      )}

      {fetchError && (
        <p className="px-3 py-2.5 text-[11px] text-red-text" role="alert">
          {fetchError}
        </p>
      )}

      {data && !data.error && data.fields.length === 0 && (
        <p className="px-3 py-2.5 text-[11px] text-neutral-400">
          The response has no fields to pick.
        </p>
      )}

      {data?.fields.map((field) => {
        const expression = field.path.startsWith("[")
          ? `${base}${field.path}`
          : `${base}.${field.path}`;

        return (
          <button
            key={field.path}
            type="button"
            onClick={() => onSelect(expression)}
            className={ROW_CLASS}
          >
            <span className="truncate font-mono text-[11px] text-secondary">
              {field.path}
            </span>
            <span className="truncate text-[11px] text-neutral-400">
              {field.sample}
            </span>
          </button>
        );
      })}
    </>
  );
}

function Heading({
  children,
  bordered = false,
}: {
  children: React.ReactNode;
  bordered?: boolean;
}) {
  return (
    <div
      className={`px-3 py-1.5 ${bordered ? "border-t border-neutral-200" : ""}`}
    >
      <span className="text-[10px] font-semibold uppercase tracking-wide text-neutral-400">
        {children}
      </span>
    </div>
  );
}
