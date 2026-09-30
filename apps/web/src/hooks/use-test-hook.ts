import { useMutation } from "@tanstack/react-query";

import {
  type TestHookRequest,
  type TestHookResponse,
  messagingApi,
} from "@/lib/messaging-api";

export function useTestHook(workspaceId: string) {
  return useMutation<TestHookResponse, Error, TestHookRequest>({
    mutationFn: (request) => messagingApi.testHook(workspaceId, request),
  });
}
