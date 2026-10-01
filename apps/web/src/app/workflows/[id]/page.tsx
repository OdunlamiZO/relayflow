import { redirect } from "next/navigation";
import { Suspense } from "react";

import { DesktopOnly } from "@/components/common/DesktopOnly";
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
      <DesktopOnly
        title="Workflow editing needs a bigger screen"
        message="Open RelayFlow on a computer to edit this workflow."
        back={{
          href: `/workflows?workspaceId=${workspaceId}`,
          label: "Back to workflows",
        }}
      >
        <WorkflowEditor workflowId={id} workspaceId={workspaceId} />
      </DesktopOnly>
    </Suspense>
  );
}
