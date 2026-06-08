import { useQuery } from "@tanstack/react-query";

import { type Subscription, messagingApi } from "@/lib/messaging-api";

export function useSubscription(workspaceId: string) {
  return useQuery<Subscription>({
    queryKey: ["subscription", workspaceId],
    queryFn: () => messagingApi.getSubscription(workspaceId),
    enabled: !!workspaceId,
  });
}
