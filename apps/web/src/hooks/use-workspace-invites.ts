import { useQuery } from "@tanstack/react-query";

import { type WorkspaceInvite, messagingApi } from "@/lib/messaging-api";

export function useWorkspaceInvites(workspaceId: string, isOwner: boolean) {
  return useQuery<WorkspaceInvite[]>({
    queryKey: ["workspace-invites", workspaceId],
    queryFn: () => messagingApi.listInvites(workspaceId),
    enabled: !!workspaceId && isOwner,
  });
}
