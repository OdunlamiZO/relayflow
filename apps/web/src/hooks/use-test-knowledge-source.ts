// Docs: docs/codebase-components.md#knowledge-source-hooks
import { useMutation } from "@tanstack/react-query";

import {
  type TestKnowledgeSourceRequest,
  type TestKnowledgeSourceResponse,
  messagingApi,
} from "@/lib/messaging-api";

export function useTestKnowledgeSource(workspaceId: string) {
  return useMutation<
    TestKnowledgeSourceResponse,
    Error,
    TestKnowledgeSourceRequest
  >({
    mutationFn: (request) =>
      messagingApi.testKnowledgeSource(workspaceId, request),
  });
}
