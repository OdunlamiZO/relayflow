import { useMutation, useQueryClient } from "@tanstack/react-query";

import { messagingApi } from "@/lib/messaging-api";

export function useDeleteSecret(workspaceId: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (secretId: string) =>
      messagingApi.deleteSecret(workspaceId, secretId),

    onSuccess: () => {
      void queryClient.invalidateQueries({
        queryKey: ["secrets", workspaceId],
      });
    },
  });
}
