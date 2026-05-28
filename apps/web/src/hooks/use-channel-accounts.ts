import { useQuery } from "@tanstack/react-query";

import { messagingApi } from "@/lib/messaging-api";

export function useChannelAccounts(workspaceId: string) {
  return useQuery({
    queryKey: ["channel-accounts", workspaceId],
    queryFn: () => messagingApi.listChannelAccounts(workspaceId),
    enabled: !!workspaceId,
  });
}
