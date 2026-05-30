import { useMutation } from "@tanstack/react-query";

import {
  type RotateWebhookSecretResponse,
  messagingApi,
} from "@/lib/messaging-api";

export function useRotateWebhookSecret(workspaceId: string) {
  return useMutation<RotateWebhookSecretResponse, Error, void>({
    mutationFn: () => messagingApi.rotateWebhookSecret(workspaceId),
  });
}
