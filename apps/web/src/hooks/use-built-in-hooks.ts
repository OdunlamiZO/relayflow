import { useQuery } from "@tanstack/react-query";

import { type BuiltInHook, messagingApi } from "@/lib/messaging-api";

export function useBuiltInHooks(workspaceId: string) {
  return useQuery<BuiltInHook[]>({
    queryKey: ["built-in-hooks", workspaceId],
    queryFn: () => messagingApi.listBuiltInHooks(workspaceId),
    enabled: !!workspaceId,
    staleTime: Infinity,
  });
}
