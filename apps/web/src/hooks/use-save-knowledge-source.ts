// Docs: docs/codebase-components.md#knowledge-source-hooks
import { useMutation, useQueryClient } from "@tanstack/react-query";

import {
  type KnowledgeSource,
  type SaveKnowledgeSourceRequest,
  messagingApi,
} from "@/lib/messaging-api";

/** Creates a source, or updates it when {@link sourceId} is given. */
export function useSaveKnowledgeSource(workspaceId: string, sourceId?: string) {
  const queryClient = useQueryClient();

  return useMutation<KnowledgeSource, Error, SaveKnowledgeSourceRequest>({
    mutationFn: (request) =>
      sourceId
        ? messagingApi.updateKnowledgeSource(workspaceId, sourceId, request)
        : messagingApi.createKnowledgeSource(workspaceId, request),

    onSuccess: () => {
      void queryClient.invalidateQueries({
        queryKey: ["knowledge-sources", workspaceId],
      });
    },
  });
}
