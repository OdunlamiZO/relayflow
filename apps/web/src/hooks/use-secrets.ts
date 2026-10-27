import { useQuery } from "@tanstack/react-query";

import { type Secret, messagingApi } from "@/lib/messaging-api";

export function useSecrets(workspaceId: string) {
  return useQuery<Secret[]>({
    queryKey: ["secrets", workspaceId],
    queryFn: () => messagingApi.listSecrets(workspaceId),
    enabled: !!workspaceId,
  });
}
