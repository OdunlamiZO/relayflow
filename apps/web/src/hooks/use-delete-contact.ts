import { useMutation, useQueryClient } from "@tanstack/react-query";

import { messagingApi } from "@/lib/messaging-api";

export function useDeleteContact(workspaceId: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (contactId: string) =>
      messagingApi.deleteContact(contactId, workspaceId),
    onSuccess: () => {
      void queryClient.invalidateQueries({
        queryKey: ["contacts", workspaceId],
      });
    },
  });
}
