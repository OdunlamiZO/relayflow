import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import { Suspense } from "react";

import { Spinner } from "@/components/common/Spinner";
import { InboxShell } from "@/components/inbox/InboxShell";
import { CreateWorkspaceForm } from "@/components/workspace/CreateWorkspaceForm";
import { serverApiBaseUrl } from "@/lib/api-base-url";

export const metadata = {
  title: "Inbox — RelayFlow",
};

type WorkspaceItem = { id: string; name: string };

async function fetchWorkspaces(): Promise<WorkspaceItem[]> {
  const cookieStore = await cookies();
  const cookieHeader = cookieStore
    .getAll()
    .map((c) => `${c.name}=${c.value}`)
    .join("; ");

  try {
    const res = await fetch(`${serverApiBaseUrl()}/workspaces`, {
      headers: { Cookie: cookieHeader },
      cache: "no-store",
    });

    if (!res.ok) {
      return [];
    }

    return (await res.json()) as WorkspaceItem[];
  } catch {
    return [];
  }
}

type SearchParams = Promise<{ workspaceId?: string }>;

type Props = {
  searchParams: SearchParams;
};

export default async function InboxPage({ searchParams }: Props) {
  const { workspaceId } = await searchParams;

  // workspaceId already in URL — render the inbox directly.
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

  // No workspaceId — check if the user has any workspaces.
  const workspaces = await fetchWorkspaces();

  if (workspaces.length > 0) {
    // Auto-select the first (oldest) workspace.
    redirect(`/inbox?workspaceId=${workspaces[0].id}`);
  }

  // No workspaces yet — prompt to create one.
  return <CreateWorkspaceForm />;
}
