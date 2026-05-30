import { useQuery } from "@tanstack/react-query";

import type { WorkflowDefinition } from "@/lib/messaging-api";
import { messagingApi } from "@/lib/messaging-api";

export function useWorkflows(workspaceId: string) {
  return useQuery<WorkflowDefinition[]>({
    queryKey: ["workflows", workspaceId],
    queryFn: () => messagingApi.listWorkflows(workspaceId),
    enabled: !!workspaceId,
    staleTime: 30 * 1000,
  });
}
