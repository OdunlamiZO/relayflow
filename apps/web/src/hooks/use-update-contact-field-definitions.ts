import { useMutation, useQueryClient } from "@tanstack/react-query";

import type { ContactFieldDefinition } from "@/lib/messaging-api";
import { messagingApi } from "@/lib/messaging-api";

export function useUpdateContactFieldDefinitions(workspaceId: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (contactFieldDefinitions: ContactFieldDefinition[]) =>
      messagingApi.updateContactFieldDefinitions(workspaceId, {
        contactFieldDefinitions,
      }),

    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ["workspaces"] });
    },
  });
}
