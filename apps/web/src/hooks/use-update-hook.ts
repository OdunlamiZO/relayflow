import { useMutation, useQueryClient } from "@tanstack/react-query";

import {
  type Hook,
  type SaveHookRequest,
  messagingApi,
} from "@/lib/messaging-api";

export function useUpdateHook(workspaceId: string, hookId: string) {
  const queryClient = useQueryClient();

  return useMutation<Hook, Error, SaveHookRequest>({
    mutationFn: (request) =>
      messagingApi.updateHook(workspaceId, hookId, request),

    onSuccess: () => {
      void queryClient.invalidateQueries({
        queryKey: ["hooks", workspaceId],
      });
    },
  });
}
