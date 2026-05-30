import { useQuery } from "@tanstack/react-query";

import type { ContactDetail } from "@/lib/messaging-api";
import { messagingApi } from "@/lib/messaging-api";

export function useContact(id: string | null, workspaceId: string) {
  return useQuery<ContactDetail>({
    queryKey: ["contact", id, workspaceId],
    queryFn: () => messagingApi.getContact(id!, workspaceId),
    enabled: !!id && !!workspaceId,
  });
}
