// Docs: docs/codebase-components.md#provider-settings-hooks
import { useMutation, useQueryClient } from "@tanstack/react-query";

import {
  type UpdateProviderSettingsRequest,
  messagingApi,
} from "@/lib/messaging-api";

export function useUpdateProviderSettings(workspaceId: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (request: UpdateProviderSettingsRequest) =>
      messagingApi.updateProviderSettings(workspaceId, request),

    onSuccess: (settings) => {
      queryClient.setQueryData(["provider-settings", workspaceId], settings);
    },
  });
}
