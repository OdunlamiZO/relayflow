"use client";

import { useEffect, useRef } from "react";

import { EmptyState } from "@/components/common/EmptyState";
import { Spinner } from "@/components/common/Spinner";
import { useWorkflowRuns } from "@/hooks/use-workflow-runs";

const RUN_STATUS_CHIP: Record<string, string> = {
  COMPLETED: "bg-green-bg text-green-text",
  RUNNING: "bg-yellow-bg text-yellow-text",
  WAITING: "bg-blue-bg text-blue-text",
  FAILED: "bg-red-bg text-red-text",
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
  });
}

type Props = {
  workflowId: string;
  workspaceId: string;
  selectedRunId: string | undefined;
  onSelect: (runId: string) => void;
};

export function RunsList({
  workflowId,
  workspaceId,
  selectedRunId,
  onSelect,
}: Props) {
  const scrollRef = useRef<HTMLDivElement>(null);

  const {
    data,
    isLoading,
    isError,
    fetchNextPage,
    hasNextPage,
    isFetchingNextPage,
  } = useWorkflowRuns(workflowId, workspaceId);

  const runs = data?.pages.flatMap((page) => page.items) ?? [];
  const isEmpty = !isLoading && !isError && runs.length === 0;

  function handleScroll() {
    const el = scrollRef.current;

    if (!el || !hasNextPage || isFetchingNextPage) {
      return;
    }

    if (el.scrollHeight - el.scrollTop - el.clientHeight < 100) {
      void fetchNextPage();
    }
  }

  useEffect(() => {
    if (scrollRef.current) {
      scrollRef.current.scrollTop = 0;
    }
  }, [workflowId, workspaceId]);

  return (
    <div className="flex flex-1 flex-col overflow-hidden">
      <div className="flex items-center justify-between border-b border-neutral-300 px-4 py-3">
        <h2 className="text-sm font-semibold text-primary">Runs</h2>
      </div>

      <div
        ref={scrollRef}
        onScroll={handleScroll}
        className="flex-1 divide-y divide-neutral-300 overflow-y-auto"
      >
        {isLoading && (
          <div className="flex justify-center py-8">
            <Spinner />
          </div>
        )}

        {isError && (
          <EmptyState
            icon="error"
            title="Could not load runs"
            description="Check your connection and try again."
          />
        )}

        {isEmpty && (
          <EmptyState
            icon="history"
            title="No runs yet"
            description="Runs will appear here once this workflow is triggered."
          />
        )}

        {runs.map((run) => {
          const chipClass =
            RUN_STATUS_CHIP[run.status] ?? "bg-neutral-300 text-neutral-600";

          return (
            <button
              key={run.id}
              type="button"
              onClick={() => onSelect(run.id)}
              className={`w-full px-4 py-3 text-left transition-colors hover:bg-neutral-200 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-secondary ${
                run.id === selectedRunId
                  ? "border-r-2 border-r-secondary bg-blue-bg"
                  : ""
              }`}
            >
              <div className="flex items-center justify-between gap-2">
                <span
                  className={`flex-shrink-0 rounded-full px-2.5 py-0.5 text-xs font-medium ${chipClass}`}
                >
                  {run.status}
                </span>

                <span className="flex-shrink-0 text-xs text-neutral-500">
                  {formatTimestamp(run.startedAt)}
                </span>
              </div>

              <p className="m-0 mt-1.5 truncate text-xs text-neutral-400">
                #{run.id.slice(0, 8)}
              </p>

              {run.errorMessage && (
                <p className="m-0 mt-1.5 truncate text-xs text-red-text">
                  {run.errorMessage}
                </p>
              )}
            </button>
          );
        })}

        {isFetchingNextPage && (
          <div className="flex justify-center py-4">
            <Spinner size="sm" />
          </div>
        )}
      </div>
    </div>
  );
}
