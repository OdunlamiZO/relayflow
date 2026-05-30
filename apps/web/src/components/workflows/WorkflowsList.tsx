"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";

import { Spinner } from "@/components/common/Spinner";
import { useWorkflows } from "@/hooks/use-workflows";

type Props = {
  workspaceId: string;
};

export function WorkflowsList({ workspaceId }: Props) {
  const pathname = usePathname();
  const { data: workflows, isPending, isError } = useWorkflows(workspaceId);

  if (isPending) {
    return (
      <div className="flex flex-1 items-center justify-center py-8">
        <Spinner />
      </div>
    );
  }

  if (isError) {
    return (
      <div className="flex flex-1 items-center justify-center px-4 py-8">
        <p className="text-center text-sm text-red-text">
          Failed to load workflows.
        </p>
      </div>
    );
  }

  if (workflows.length === 0) {
    return (
      <div className="flex flex-1 items-center justify-center px-4 py-8">
        <p className="text-center text-sm text-neutral-400">
          No workflows yet. Hit + to create one.
        </p>
      </div>
    );
  }

  return (
    <ul className="flex-1 overflow-y-auto pb-14 md:pb-0">
      {workflows.map((workflow) => {
        const href = `/workflows/${workflow.id}?workspaceId=${workspaceId}`;
        const isActive = pathname === `/workflows/${workflow.id}`;

        return (
          <li key={workflow.id}>
            <Link
              href={href}
              className={`flex items-center gap-3 px-4 py-3 text-sm transition-colors hover:bg-neutral-200 ${
                isActive ? "bg-neutral-200" : ""
              }`}
            >
              <span
                className={`h-2 w-2 flex-shrink-0 rounded-full ${
                  workflow.enabled ? "bg-green-border" : "bg-neutral-300"
                }`}
              />

              <span className="flex-1 truncate font-medium text-neutral-800">
                {workflow.name}
              </span>
            </Link>
          </li>
        );
      })}
    </ul>
  );
}
