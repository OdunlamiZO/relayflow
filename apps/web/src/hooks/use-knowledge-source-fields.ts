// Docs: docs/codebase-components.md#knowledge-source-hooks
import { useQuery } from "@tanstack/react-query";

import { type KnowledgeSourceFields, messagingApi } from "@/lib/messaging-api";

/** Fetches the source's live response and lists its value paths; idle while sourceId is null. */
export function useKnowledgeSourceFields(
  workspaceId: string,
  sourceId: string | null
) {
  return useQuery<KnowledgeSourceFields>({
    queryKey: ["knowledge-sources", workspaceId, sourceId, "fields"],
    queryFn: () =>
      messagingApi.listKnowledgeSourceFields(workspaceId, sourceId as string),
    enabled: !!workspaceId && !!sourceId,
  });
}
