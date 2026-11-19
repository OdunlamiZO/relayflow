// Docs: docs/codebase-components.md#provider-settings-hooks
import { useQuery } from "@tanstack/react-query";

import { type ProviderSettings, messagingApi } from "@/lib/messaging-api";

export function useProviderSettings(workspaceId: string) {
  return useQuery<ProviderSettings>({
    queryKey: ["provider-settings", workspaceId],
    queryFn: () => messagingApi.getProviderSettings(workspaceId),
    enabled: !!workspaceId,
  });
}
