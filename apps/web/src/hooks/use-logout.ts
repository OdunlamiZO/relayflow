import { useMutation, useQueryClient } from "@tanstack/react-query";

import { useToast } from "@/components/providers/ToastProvider";
import { authenticationApi } from "@/lib/authentication-api";
import { errorMessage } from "@/lib/error-message";

export function useLogout() {
  const queryClient = useQueryClient();
  const { showToast } = useToast();

  return useMutation({
    mutationFn: () => authenticationApi.logout(),
    onError: (error) => {
      showToast({ kind: "error", message: errorMessage(error) });
    },
    onSuccess: () => {
      // Drop the guest recovery token so the login page doesn't silently
      // re-establish the anonymous session and bounce the user back to /inbox.
      localStorage.removeItem("guestRecoveryToken");

      // Clear all cached queries so no stale authenticated data is shown
      // during the navigation away from the inbox.
      queryClient.clear();
    },
  });
}
