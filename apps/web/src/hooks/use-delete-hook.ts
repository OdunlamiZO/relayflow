import { useMutation, useQueryClient } from "@tanstack/react-query";

import { messagingApi } from "@/lib/messaging-api";

export function useDeleteHook(workspaceId: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (hookId: string) =>
      messagingApi.deleteHook(workspaceId, hookId),

    onSuccess: () => {
      void queryClient.invalidateQueries({
        queryKey: ["hooks", workspaceId],
      });
    },
  });
}
