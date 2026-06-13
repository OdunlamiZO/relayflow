"use client";

import { EmptyState } from "@/components/common/EmptyState";
import { Spinner } from "@/components/common/Spinner";
import { useWorkflowRun } from "@/hooks/use-workflow-run";
import type { JsonObject } from "@/lib/messaging-api";

const RUN_STATUS_CHIP: Record<string, string> = {
  COMPLETED: "bg-green-bg text-green-text",
  RUNNING: "bg-yellow-bg text-yellow-text",
  WAITING: "bg-blue-bg text-blue-text",
  FAILED: "bg-red-bg text-red-text",
};

const STEP_STATUS_CHIP: Record<string, string> = {
  COMPLETED: "bg-green-bg text-green-text",
  FAILED: "bg-red-bg text-red-text",
  SKIPPED: "bg-neutral-300 text-neutral-600",
};

const LOCALE = "en-US";

function formatTimestamp(isoString: string | null): string {
  if (!isoString) {
    return "";
  }

  return new Date(isoString).toLocaleString(LOCALE, {
    month: "short",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
  });
}

/** Returns the entries of `current` that are new or changed compared to `previous`. */
function diffVariables(
  previous: JsonObject | null,
  current: JsonObject
): JsonObject {
  if (!previous) {
    return current;
  }

  const changed: JsonObject = {};

  for (const [key, value] of Object.entries(current)) {
    if (JSON.stringify(previous[key]) !== JSON.stringify(value)) {
      changed[key] = value;
    }
  }

  return changed;
}

function formatDuration(durationMs: number | null): string {
  if (durationMs === null) {
    return "";
  }

  if (durationMs < 1000) {
    return `${durationMs} ms`;
  }

  return `${(durationMs / 1000).toFixed(2)} s`;
}

type Props = {
  workflowId: string;
  workspaceId: string;
  runId: string | undefined;
  onBack?: () => void;
};

export function RunDetail({ workflowId, workspaceId, runId, onBack }: Props) {
  const {
    data: run,
    isLoading,
    isError,
  } = useWorkflowRun(workflowId, runId, workspaceId);

  if (!runId) {
    return (
      <div className="flex h-full w-full items-center justify-center">
        <EmptyState
          icon="visibility"
          title="Select a run"
          description="Choose a run from the list to see its step-by-step breakdown."
        />
      </div>
    );
  }

  if (isLoading) {
    return (
      <div className="flex h-full w-full items-center justify-center">
        <Spinner />
      </div>
    );
  }

  if (isError || !run) {
    return (
      <div className="flex h-full w-full items-center justify-center">
        <EmptyState
          icon="error"
          title="Could not load run"
          description="Check your connection and try again."
        />
      </div>
    );
  }

  const runChipClass =
    RUN_STATUS_CHIP[run.status] ?? "bg-neutral-300 text-neutral-600";

  return (
    <div className="flex h-full w-full min-w-0 flex-col overflow-hidden">
      <div className="flex flex-shrink-0 items-center gap-3 border-b border-neutral-300 px-4 py-3">
        {onBack && (
          <button
            type="button"
            onClick={onBack}
            className="flex-shrink-0 rounded-lg p-1 text-neutral-500 transition-colors hover:bg-neutral-200 hover:text-neutral-700 md:hidden"
            title="Back to runs"
          >
            <span
              className="material-symbols-rounded text-[18px] leading-none"
              aria-hidden="true"
            >
              arrow_back
            </span>
          </button>
        )}

        <span
          className={`flex-shrink-0 rounded-full px-2.5 py-0.5 text-xs font-medium ${runChipClass}`}
        >
          {run.status}
        </span>

        <div className="min-w-0 flex-1">
          <p className="m-0 text-xs text-neutral-500">
            Started {formatTimestamp(run.startedAt)}
            {run.finishedAt && ` · Finished ${formatTimestamp(run.finishedAt)}`}
          </p>
        </div>
      </div>

      <div className="grid w-full flex-1 grid-cols-[minmax(0,1fr)] gap-3 overflow-y-auto p-4 [scrollbar-gutter:stable]">
        {run.errorMessage && (
          <div className="rounded-lg border border-red-border bg-red-bg px-3 py-2 text-xs text-red-text">
            {run.errorMessage}
          </div>
        )}

        {run.steps.length === 0 && (
          <EmptyState
            icon="timeline"
            title="No steps recorded"
            description="This run does not have any recorded steps."
          />
        )}

        {run.steps.map((step, index) => {
          const stepChipClass =
            STEP_STATUS_CHIP[step.status] ?? "bg-neutral-300 text-neutral-600";

          const previousSnapshot =
            index > 0 ? run.steps[index - 1].inputSnapshot : null;
          const changedVariables = diffVariables(
            previousSnapshot,
            step.inputSnapshot
          );
          const hasChangedVariables = Object.keys(changedVariables).length > 0;

          return (
            <div
              key={step.id}
              className="w-full min-w-0 rounded-lg border border-neutral-300 bg-neutral-100 p-3"
            >
              <div className="flex items-center justify-between gap-2">
                <div className="min-w-0">
                  <p className="m-0 truncate text-sm font-semibold text-primary">
                    {step.nodeId}
                  </p>
                  <p className="m-0 truncate text-xs text-neutral-500">
                    {step.nodeType}
                    {step.durationMs !== null &&
                      ` · ${formatDuration(step.durationMs)}`}
                  </p>
                </div>

                <span
                  className={`flex-shrink-0 rounded-full px-2.5 py-0.5 text-xs font-medium ${stepChipClass}`}
                >
                  {step.status}
                </span>
              </div>

              {step.errorMessage && (
                <p className="m-0 mt-2 text-xs text-red-text">
                  {step.errorMessage}
                </p>
              )}

              <div className="mt-2 grid w-full grid-cols-[minmax(0,1fr)] gap-2">
                {hasChangedVariables ? (
                  <div className="max-w-full overflow-hidden rounded-md border border-neutral-300 bg-neutral-200 p-2">
                    <p className="m-0 text-xs font-medium text-neutral-700">
                      {index === 0
                        ? "Variables at the start of this run"
                        : "New or updated variables"}
                    </p>
                    <pre className="mt-2 w-full max-w-full overflow-x-hidden whitespace-pre-wrap break-all text-[11px] text-neutral-700">
                      {JSON.stringify(changedVariables, null, 2)}
                    </pre>
                  </div>
                ) : (
                  <p className="m-0 rounded-md border border-neutral-300 bg-neutral-200 p-2 text-xs text-neutral-500">
                    No new or updated variables before this step.
                  </p>
                )}

                <details className="max-w-full overflow-hidden rounded-md border border-neutral-300 bg-neutral-200 p-2">
                  <summary className="cursor-pointer text-xs font-medium text-neutral-700">
                    All variables available to this step
                  </summary>
                  <pre className="mt-2 w-full max-w-full overflow-x-hidden whitespace-pre-wrap break-all text-[11px] text-neutral-700">
                    {JSON.stringify(step.inputSnapshot, null, 2)}
                  </pre>
                </details>

                <details className="max-w-full overflow-hidden rounded-md border border-neutral-300 bg-neutral-200 p-2">
                  <summary className="cursor-pointer text-xs font-medium text-neutral-700">
                    Output
                  </summary>
                  <pre className="mt-2 w-full max-w-full overflow-x-hidden whitespace-pre-wrap break-all text-[11px] text-neutral-700">
                    {JSON.stringify(step.outputSnapshot, null, 2)}
                  </pre>
                </details>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}
