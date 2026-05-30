import { useQuery } from "@tanstack/react-query";

import type { WorkflowDefinition } from "@/lib/messaging-api";
import { messagingApi } from "@/lib/messaging-api";

export function useWorkflow(id: string, workspaceId: string) {
  return useQuery<WorkflowDefinition>({
    queryKey: ["workflow", id, workspaceId],
    queryFn: () => messagingApi.getWorkflow(id, workspaceId),
    enabled: !!id && !!workspaceId,
  });
}
