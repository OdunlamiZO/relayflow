import { redirect } from "next/navigation";
import { Suspense } from "react";

import { Spinner } from "@/components/common/Spinner";
import { WorkflowEditor } from "@/components/workflows/WorkflowEditor";

export const metadata = {
  title: "Workflow Editor — RelayFlow",
};

type Props = {
  params: Promise<{ id: string }>;
  searchParams: Promise<{ workspaceId?: string }>;
};

export default async function WorkflowEditorPage({
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
        <div className="flex h-screen items-center justify-center">
          <Spinner size="lg" />
        </div>
      }
    >
      <WorkflowEditor workflowId={id} workspaceId={workspaceId} />
    </Suspense>
  );
}
