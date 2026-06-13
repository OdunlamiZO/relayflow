import { redirect } from "next/navigation";
import { Suspense } from "react";

import { Spinner } from "@/components/common/Spinner";
import { RunsShell } from "@/components/workflows/runs/RunsShell";

export const metadata = {
  title: "Workflow Runs — RelayFlow",
};

type Props = {
  params: Promise<{ id: string }>;
  searchParams: Promise<{ workspaceId?: string }>;
};

export default async function WorkflowRunsPage({
  params,
  searchParams,
}: Props) {
  const { id } = await params;
  const { workspaceId } = await searchParams;

  if (!workspaceId) {
    redirect("/workflows");
  }

  return (
    <Suspense
      fallback={
        <div className="flex h-full items-center justify-center">
          <Spinner size="lg" />
        </div>
      }
    >
      <RunsShell workflowId={id} workspaceId={workspaceId} />
    </Suspense>
  );
}
