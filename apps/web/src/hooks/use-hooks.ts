import { useQuery } from "@tanstack/react-query";

import { type Hook, messagingApi } from "@/lib/messaging-api";

export function useHooks(workspaceId: string) {
  return useQuery<Hook[]>({
    queryKey: ["hooks", workspaceId],
    queryFn: () => messagingApi.listHooks(workspaceId),
    enabled: !!workspaceId,
  });
}
