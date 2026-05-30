import { useMutation, useQueryClient } from "@tanstack/react-query";

import { messagingApi } from "@/lib/messaging-api";

export function useRevokeApiKey(workspaceId: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (keyId: string) =>
      messagingApi.revokeApiKey(workspaceId, keyId),

    onSuccess: () => {
      void queryClient.invalidateQueries({
        queryKey: ["api-keys", workspaceId],
      });
    },
  });
}
