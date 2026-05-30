import { useMutation, useQueryClient } from "@tanstack/react-query";

import {
  type CreateApiKeyRequest,
  type CreateApiKeyResponse,
  messagingApi,
} from "@/lib/messaging-api";

export function useCreateApiKey(workspaceId: string) {
  const queryClient = useQueryClient();

  return useMutation<CreateApiKeyResponse, Error, CreateApiKeyRequest>({
    mutationFn: (request) => messagingApi.createApiKey(workspaceId, request),

    onSuccess: () => {
      void queryClient.invalidateQueries({
        queryKey: ["api-keys", workspaceId],
      });
    },
  });
}
