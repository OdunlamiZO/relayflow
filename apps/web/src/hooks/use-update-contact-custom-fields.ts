import { useMutation, useQueryClient } from "@tanstack/react-query";

import { messagingApi } from "@/lib/messaging-api";

export function useUpdateContactCustomFields(id: string, workspaceId: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (customFields: Record<string, string>) =>
      messagingApi.updateContactCustomFields(id, workspaceId, {
        customFields,
      }),

    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ["contact", id] });
      void queryClient.invalidateQueries({
        queryKey: ["contacts", workspaceId],
      });
    },
  });
}
