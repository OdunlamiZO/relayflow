import { useInfiniteQuery } from "@tanstack/react-query";

import { messagingApi } from "@/lib/messaging-api";

const LIMIT = 50;

export function useMessages(
  workspaceId: string | undefined,
  conversationId: string | undefined
) {
  return useInfiniteQuery({
    queryKey: ["messages", workspaceId, conversationId],
    // pageParam is the createdAt of the oldest loaded message; undefined loads the most recent page.
    queryFn: ({ pageParam }) =>
      messagingApi.listMessages(workspaceId!, conversationId!, {
        before: pageParam ?? undefined,
        limit: LIMIT,
      }),
    initialPageParam: undefined as string | undefined,
    getNextPageParam: (firstPage) =>
      firstPage.hasMore ? (firstPage.nextCursor ?? undefined) : undefined,
    enabled: Boolean(workspaceId) && Boolean(conversationId),
    // SSE delivers updates; polling covers missed events.
    staleTime: 30 * 1000,
    refetchInterval: 60 * 1000,
  });
}
