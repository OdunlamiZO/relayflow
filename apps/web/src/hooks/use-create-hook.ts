import { useMutation, useQueryClient } from "@tanstack/react-query";

import {
  type Hook,
  type SaveHookRequest,
  messagingApi,
} from "@/lib/messaging-api";

export function useCreateHook(workspaceId: string) {
  const queryClient = useQueryClient();

  return useMutation<Hook, Error, SaveHookRequest>({
    mutationFn: (request) => messagingApi.createHook(workspaceId, request),

    onSuccess: () => {
      void queryClient.invalidateQueries({
        queryKey: ["hooks", workspaceId],
      });
    },
  });
}
