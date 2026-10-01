import { useMutation, useQueryClient } from "@tanstack/react-query";

import type { ContactTagDefinition } from "@/lib/messaging-api";
import { messagingApi } from "@/lib/messaging-api";

export function useUpdateContactTagDefinitions(workspaceId: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (contactTagDefinitions: ContactTagDefinition[]) =>
      messagingApi.updateContactTagDefinitions(workspaceId, {
        contactTagDefinitions,
      }),

    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ["workspaces"] });
    },
  });
}
