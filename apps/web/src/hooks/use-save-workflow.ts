import { useMutation, useQueryClient } from "@tanstack/react-query";

import type { UpdateWorkflowRequest } from "@/lib/messaging-api";
import { messagingApi } from "@/lib/messaging-api";

export function useSaveWorkflow(id: string, workspaceId: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (request: UpdateWorkflowRequest) =>
      messagingApi.updateWorkflow(id, workspaceId, request),

    onSuccess: (updated) => {
      queryClient.setQueryData(["workflow", id, workspaceId], updated);

      void queryClient.invalidateQueries({
        queryKey: ["workflows", workspaceId],
      });
    },
  });
}
