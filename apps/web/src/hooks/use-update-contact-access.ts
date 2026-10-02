import { useMutation, useQueryClient } from "@tanstack/react-query";

import {
  type UpdateContactAccessRequest,
  messagingApi,
} from "@/lib/messaging-api";

export function useUpdateContactAccess(workspaceId: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (request: UpdateContactAccessRequest) =>
      messagingApi.updateContactAccess(workspaceId, request),

    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ["workspaces"] });
    },
  });
}
