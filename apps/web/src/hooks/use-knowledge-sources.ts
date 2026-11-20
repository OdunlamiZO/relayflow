// Docs: docs/codebase-components.md#knowledge-source-hooks
import { useQuery } from "@tanstack/react-query";

import { type KnowledgeSource, messagingApi } from "@/lib/messaging-api";

export function useKnowledgeSources(workspaceId: string) {
  return useQuery<KnowledgeSource[]>({
    queryKey: ["knowledge-sources", workspaceId],
    queryFn: () => messagingApi.listKnowledgeSources(workspaceId),
    enabled: !!workspaceId,
  });
}
