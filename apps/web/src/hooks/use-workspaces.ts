// Docs: docs/codebase-components.md#workspace-hooks
import { useQuery } from "@tanstack/react-query";

import type { Workspace } from "@/lib/messaging-api";
import { messagingApi } from "@/lib/messaging-api";

export function useWorkspaces() {
  return useQuery<Workspace[]>({
    queryKey: ["workspaces"],
    queryFn: () => messagingApi.listWorkspaces(),
    staleTime: 5 * 60 * 1000,
  });
}

export function useWorkspace(id: string): Workspace | null {
  const { data } = useWorkspaces();

  return data?.find((workspace) => workspace.id === id) ?? null;
}
