// Docs: docs/codebase-components.md#workflow-pages
import { redirect } from "next/navigation";
import { Suspense } from "react";

import { Spinner } from "@/components/common/Spinner";
import { WorkflowsShell } from "@/components/workflows/WorkflowsShell";
import { serverMessagingApi } from "@/lib/server-messaging-api";

export const metadata = {
  title: "Workflows — RelayFlow",
};

type SearchParams = Promise<{ workspaceId?: string }>;

type Props = {
  searchParams: SearchParams;
};

export default async function WorkflowsPage({ searchParams }: Props) {
  const { workspaceId } = await searchParams;

  if (workspaceId) {
    return (
      <Suspense
        fallback={
          <div className="flex h-full items-center justify-center">
            <Spinner size="lg" />
          </div>
        }
      >
        <WorkflowsShell workspaceId={workspaceId} />
      </Suspense>
    );
  }

  const workspaces = await serverMessagingApi.listWorkspaces();

  if (workspaces.length > 0) {
    redirect(`/workflows?workspaceId=${workspaces[0].id}`);
  }

  redirect("/inbox");
}
