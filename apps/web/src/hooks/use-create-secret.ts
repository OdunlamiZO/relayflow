import { useMutation, useQueryClient } from "@tanstack/react-query";

import {
  type SaveSecretRequest,
  type Secret,
  messagingApi,
} from "@/lib/messaging-api";

export function useCreateSecret(workspaceId: string) {
  const queryClient = useQueryClient();

  return useMutation<Secret, Error, SaveSecretRequest>({
    mutationFn: (request) => messagingApi.createSecret(workspaceId, request),

    onSuccess: () => {
      void queryClient.invalidateQueries({
        queryKey: ["secrets", workspaceId],
      });
    },
  });
}
