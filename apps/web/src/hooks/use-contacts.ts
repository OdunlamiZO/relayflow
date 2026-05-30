import { useInfiniteQuery } from "@tanstack/react-query";

import { messagingApi } from "@/lib/messaging-api";

const PAGE_SIZE = 50;

export function useContacts(workspaceId: string | undefined) {
  return useInfiniteQuery({
    queryKey: ["contacts", workspaceId],
    queryFn: ({ pageParam }) =>
      messagingApi.listContacts(workspaceId!, pageParam, PAGE_SIZE),
    initialPageParam: 0,
    getNextPageParam: (lastPage, allPages) =>
      lastPage.hasMore ? allPages.length : undefined,
    enabled: Boolean(workspaceId),
    staleTime: 30 * 1000,
  });
}
