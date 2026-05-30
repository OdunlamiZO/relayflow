import { useQuery } from "@tanstack/react-query";

import { type WorkspaceMember, messagingApi } from "@/lib/messaging-api";

export function useWorkspaceMembers(workspaceId: string) {
  return useQuery<WorkspaceMember[]>({
    queryKey: ["workspace-members", workspaceId],
    queryFn: () => messagingApi.listMembers(workspaceId),
    enabled: !!workspaceId,
  });
}
