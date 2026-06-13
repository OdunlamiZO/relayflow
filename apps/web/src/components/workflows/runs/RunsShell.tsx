"use client";

import Link from "next/link";
import { useState } from "react";

import { Spinner } from "@/components/common/Spinner";
import { useWorkflow } from "@/hooks/use-workflow";

import { RunDetail } from "./RunDetail";
import { RunsList } from "./RunsList";

type Props = {
  workflowId: string;
  workspaceId: string;
};

export function RunsShell({ workflowId, workspaceId }: Props) {
  const [selectedRunId, setSelectedRunId] = useState<string | undefined>(
    undefined
  );

  const { data: workflow, isLoading } = useWorkflow(workflowId, workspaceId);

  return (
    <div className="flex h-full flex-col overflow-hidden">
      <div className="flex flex-shrink-0 items-center gap-3 border-b border-neutral-300 bg-neutral-100 px-4 py-2.5">
        <Link
          href={`/workflows/${workflowId}?workspaceId=${workspaceId}`}
          className="flex items-center rounded-md p-1 text-neutral-400 transition-colors hover:bg-neutral-200 hover:text-neutral-700"
          title="Back to workflow"
        >
          <span
            className="material-symbols-rounded text-[18px] leading-none"
            aria-hidden="true"
          >
            arrow_back
          </span>
        </Link>

        <div className="min-w-0 flex-1">
          <div className="truncate text-sm font-semibold text-neutral-800">
            {isLoading ? (
              <Spinner size="sm" />
            ) : (
              (workflow?.name ?? "Untitled workflow")
            )}
          </div>
          <p className="m-0 truncate text-xs text-neutral-500">Run history</p>
        </div>
      </div>

      <div className="flex flex-1 overflow-hidden">
        <div
          className={`flex-shrink-0 flex-col overflow-hidden border-r border-neutral-300 md:flex md:max-w-sm lg:max-w-md ${
            selectedRunId ? "hidden md:flex" : "flex w-full"
          }`}
        >
          <RunsList
            workflowId={workflowId}
            workspaceId={workspaceId}
            selectedRunId={selectedRunId}
            onSelect={setSelectedRunId}
          />
        </div>

        <div
          className={`w-0 flex-1 overflow-hidden ${
            selectedRunId ? "flex" : "hidden md:flex"
          }`}
        >
          <RunDetail
            workflowId={workflowId}
            workspaceId={workspaceId}
            runId={selectedRunId}
            onBack={() => setSelectedRunId(undefined)}
          />
        </div>
      </div>
    </div>
  );
}
