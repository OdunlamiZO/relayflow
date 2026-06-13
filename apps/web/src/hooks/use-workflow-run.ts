import { useQuery } from "@tanstack/react-query";

import type { WorkflowRunDetail } from "@/lib/messaging-api";
import { messagingApi } from "@/lib/messaging-api";

export function useWorkflowRun(
  workflowId: string,
  runId: string | undefined,
  workspaceId: string
) {
  return useQuery<WorkflowRunDetail>({
    queryKey: ["workflow-run", workflowId, runId, workspaceId],
    queryFn: () => messagingApi.getWorkflowRun(workflowId, runId!, workspaceId),
    enabled: !!workflowId && !!runId && !!workspaceId,
  });
}
