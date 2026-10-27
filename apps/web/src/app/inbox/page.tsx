import { redirect } from "next/navigation";
import { Suspense } from "react";

import { Spinner } from "@/components/common/Spinner";
import { InboxShell } from "@/components/inbox/InboxShell";
import { CreateWorkspaceForm } from "@/components/workspace/CreateWorkspaceForm";
import { serverMessagingApi } from "@/lib/server-messaging-api";

export const metadata = {
  title: "Inbox — RelayFlow",
};

type SearchParams = Promise<{ workspaceId?: string }>;

type Props = {
  searchParams: SearchParams;
};

export default async function InboxPage({ searchParams }: Props) {
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
        <InboxShell workspaceId={workspaceId} />
      </Suspense>
    );
  }

  const workspaces = await serverMessagingApi.listWorkspaces();

  if (workspaces.length > 0) {
    redirect(`/inbox?workspaceId=${workspaces[0].id}`);
  }

  return <CreateWorkspaceForm />;
}
