// Docs: docs/codebase-components.md#knowledge-source-hooks
import { useMutation } from "@tanstack/react-query";

import {
  type PreviewKnowledgeResponse,
  messagingApi,
} from "@/lib/messaging-api";

export function usePreviewKnowledge(workspaceId: string) {
  return useMutation<PreviewKnowledgeResponse, Error, string>({
    mutationFn: (text) => messagingApi.previewKnowledge(workspaceId, text),
  });
}
