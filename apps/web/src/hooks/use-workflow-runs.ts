import { useInfiniteQuery } from "@tanstack/react-query";

import { messagingApi } from "@/lib/messaging-api";

const PAGE_SIZE = 30;

export function useWorkflowRuns(workflowId: string, workspaceId: string) {
  return useInfiniteQuery({
    queryKey: ["workflow-runs", workflowId, workspaceId],
    queryFn: ({ pageParam }) =>
      messagingApi.listWorkflowRuns(
        workflowId,
        workspaceId,
        pageParam,
        PAGE_SIZE
      ),
    initialPageParam: 0,
    getNextPageParam: (lastPage, allPages) =>
      lastPage.hasMore ? allPages.length : undefined,
    enabled: !!workflowId && !!workspaceId,
  });
}
