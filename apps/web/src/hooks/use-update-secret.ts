import { useMutation, useQueryClient } from "@tanstack/react-query";

import {
  type SaveSecretRequest,
  type Secret,
  messagingApi,
} from "@/lib/messaging-api";

export function useUpdateSecret(workspaceId: string, secretId: string) {
  const queryClient = useQueryClient();

  return useMutation<Secret, Error, SaveSecretRequest>({
    mutationFn: (request) =>
      messagingApi.updateSecret(workspaceId, secretId, request),

    onSuccess: () => {
      void queryClient.invalidateQueries({
        queryKey: ["secrets", workspaceId],
      });
    },
  });
}
