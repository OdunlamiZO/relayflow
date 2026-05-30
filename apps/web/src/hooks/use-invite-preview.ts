import { useQuery } from "@tanstack/react-query";

import { type InvitePreview, messagingApi } from "@/lib/messaging-api";

export function useInvitePreview(token: string | null) {
  return useQuery<InvitePreview>({
    queryKey: ["invite-preview", token],
    queryFn: () => messagingApi.getInvitePreview(token!),
    enabled: !!token,
    retry: false,
  });
}
