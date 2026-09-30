import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import { Suspense } from "react";

import { Spinner } from "@/components/common/Spinner";
import { WorkflowsShell } from "@/components/workflows/WorkflowsShell";
import { serverApiBaseUrl } from "@/lib/api-base-url";

export const metadata = {
  title: "Workflows — RelayFlow",
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

  const workspaces = await fetchWorkspaces();

  if (workspaces.length > 0) {
    redirect(`/workflows?workspaceId=${workspaces[0].id}`);
  }

  redirect("/inbox");
}
