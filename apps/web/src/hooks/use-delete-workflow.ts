import { useMutation, useQueryClient } from "@tanstack/react-query";

import { messagingApi } from "@/lib/messaging-api";

export function useDeleteWorkflow(workspaceId: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: string) => messagingApi.deleteWorkflow(id, workspaceId),

    onSuccess: () => {
      void queryClient.invalidateQueries({
        queryKey: ["workflows", workspaceId],
      });
    },
  });
}
