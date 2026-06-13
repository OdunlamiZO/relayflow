import { useMutation, useQueryClient } from "@tanstack/react-query";

import { messagingApi } from "@/lib/messaging-api";

export function useRenameWorkflow(workspaceId: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, name }: { id: string; name: string }) =>
      messagingApi.updateWorkflow(id, workspaceId, { name }),

    onSuccess: (updated, variables) => {
      queryClient.setQueryData(
        ["workflow", variables.id, workspaceId],
        updated
      );

      void queryClient.invalidateQueries({
        queryKey: ["workflows", workspaceId],
      });
    },
  });
}
