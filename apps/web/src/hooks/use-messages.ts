import { useInfiniteQuery } from "@tanstack/react-query";

import { messagingApi } from "@/lib/messaging-api";

const LIMIT = 50;

export function useMessages(
  workspaceId: string | undefined,
  conversationId: string | undefined
) {
  return useInfiniteQuery({
    queryKey: ["messages", workspaceId, conversationId],
    // pageParam is the cursor (createdAt ISO string of the oldest message on the current page).
    // undefined = first page = most recent messages.
    queryFn: ({ pageParam }) =>
      messagingApi.listMessages(workspaceId!, conversationId!, {
        before: pageParam ?? undefined,
        limit: LIMIT,
      }),
    initialPageParam: undefined as string | undefined,
    // nextCursor points at older messages (load-more scrolling upward).
    getNextPageParam: (firstPage) =>
      firstPage.hasMore ? (firstPage.nextCursor ?? undefined) : undefined,
    enabled: Boolean(workspaceId) && Boolean(conversationId),
    // SSE is the primary delivery mechanism; polling is the fallback for missed events.
    staleTime: 30 * 1000,
    refetchInterval: 60 * 1000,
  });
}
