import { useQuery } from "@tanstack/react-query";

import { messagingApi } from "@/lib/messaging-api";

export function useWebhooks(workspaceId: string) {
  return useQuery({
    queryKey: ["webhooks", workspaceId],
    queryFn: () => messagingApi.listWebhooks(workspaceId),
    enabled: !!workspaceId,
  });
}
