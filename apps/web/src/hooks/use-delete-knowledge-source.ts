// Docs: docs/codebase-components.md#knowledge-source-hooks
import { useMutation, useQueryClient } from "@tanstack/react-query";

import { messagingApi } from "@/lib/messaging-api";

export function useDeleteKnowledgeSource(workspaceId: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (sourceId: string) =>
      messagingApi.deleteKnowledgeSource(workspaceId, sourceId),

    onSuccess: () => {
      void queryClient.invalidateQueries({
        queryKey: ["knowledge-sources", workspaceId],
      });
    },
  });
}
