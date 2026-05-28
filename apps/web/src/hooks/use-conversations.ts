import { useInfiniteQuery } from "@tanstack/react-query";

import { messagingApi } from "@/lib/messaging-api";

const PAGE_SIZE = 30;

export function useConversations(workspaceId: string | undefined) {
  return useInfiniteQuery({
    queryKey: ["conversations", workspaceId],
    queryFn: ({ pageParam }) =>
      messagingApi.listConversations(workspaceId!, pageParam, PAGE_SIZE),
    initialPageParam: 0,
    getNextPageParam: (lastPage, allPages) =>
      lastPage.hasMore ? allPages.length : undefined,
    enabled: Boolean(workspaceId),
    // SSE is the primary delivery mechanism; polling is the fallback for missed events.
    staleTime: 30 * 1000,
    refetchInterval: 60 * 1000,
  });
}
