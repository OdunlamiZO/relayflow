import { useMutation, useQueryClient } from "@tanstack/react-query";

import { useToast } from "@/components/providers/ToastProvider";
import { logout as logoutApi } from "@/lib/authentication-api";
import { errorMessage } from "@/lib/error-message";

export function useLogout() {
  const queryClient = useQueryClient();
  const { showToast } = useToast();

  return useMutation({
    mutationFn: () => logoutApi(),
    onError: (error) => {
      showToast({ kind: "error", message: errorMessage(error) });
    },
    onSuccess: () => {
      // Clear all cached queries so no stale authenticated data is shown
      // during the navigation away from the inbox.
      queryClient.clear();
    },
  });
}
