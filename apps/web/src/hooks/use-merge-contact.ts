import { useMutation, useQueryClient } from "@tanstack/react-query";

import { messagingApi } from "@/lib/messaging-api";

export function useMergeContact(workspaceId: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      targetId,
      sourceId,
    }: {
      targetId: string;
      sourceId: string;
    }) =>
      messagingApi.mergeContact(targetId, workspaceId, {
        sourceContactId: sourceId,
      }),
    onSuccess: () => {
      void queryClient.invalidateQueries({
        queryKey: ["contacts", workspaceId],
      });
      void queryClient.invalidateQueries({ queryKey: ["contact"] });
    },
  });
}
