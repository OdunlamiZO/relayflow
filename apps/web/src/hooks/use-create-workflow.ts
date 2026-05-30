import { useMutation, useQueryClient } from "@tanstack/react-query";

import type { CreateWorkflowRequest } from "@/lib/messaging-api";
import { messagingApi } from "@/lib/messaging-api";

export function useCreateWorkflow() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (request: CreateWorkflowRequest) =>
      messagingApi.createWorkflow(request),

    onSuccess: (_, variables) => {
      void queryClient.invalidateQueries({
        queryKey: ["workflows", variables.workspaceId],
      });
    },
  });
}
