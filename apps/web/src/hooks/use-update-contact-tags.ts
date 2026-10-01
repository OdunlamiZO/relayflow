import { useMutation, useQueryClient } from "@tanstack/react-query";

import { messagingApi } from "@/lib/messaging-api";

export function useUpdateContactTags(id: string, workspaceId: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (tags: Record<string, string | null>) =>
      messagingApi.updateContactTags(id, workspaceId, { tags }),

    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ["contact", id] });
      void queryClient.invalidateQueries({
        queryKey: ["contacts", workspaceId],
      });
    },
  });
}
