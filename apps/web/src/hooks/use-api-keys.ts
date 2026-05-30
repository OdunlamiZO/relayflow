import { useQuery } from "@tanstack/react-query";

import { type ApiKey, messagingApi } from "@/lib/messaging-api";

export function useApiKeys(workspaceId: string) {
  return useQuery<ApiKey[]>({
    queryKey: ["api-keys", workspaceId],
    queryFn: () => messagingApi.listApiKeys(workspaceId),
    enabled: !!workspaceId,
  });
}
