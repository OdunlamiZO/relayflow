import { useMutation } from "@tanstack/react-query";

import { useToast } from "@/components/providers/ToastProvider";
import { errorMessage } from "@/lib/error-message";
import { messagingApi } from "@/lib/messaging-api";

export function useGenerateMemberPasswordReset(workspaceId: string) {
  const { showToast } = useToast();

  return useMutation({
    mutationFn: (memberId: string) =>
      messagingApi.generateMemberPasswordReset(workspaceId, memberId),
    onError: (error) => {
      showToast({ kind: "error", message: errorMessage(error) });
    },
    onSuccess: () => {
      showToast({ kind: "success", message: "Password reset link sent." });
    },
  });
}
