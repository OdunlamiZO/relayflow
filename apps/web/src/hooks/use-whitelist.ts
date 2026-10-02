import { useQuery } from "@tanstack/react-query";

import { type WhitelistedPhoneNumber, messagingApi } from "@/lib/messaging-api";

export function useWhitelist(workspaceId: string) {
  return useQuery<WhitelistedPhoneNumber[]>({
    queryKey: ["whitelist", workspaceId],
    queryFn: () => messagingApi.listWhitelist(workspaceId),
    enabled: !!workspaceId,
  });
}
