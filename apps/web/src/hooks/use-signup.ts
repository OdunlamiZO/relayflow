import { useMutation } from "@tanstack/react-query";

import { useToast } from "@/components/providers/ToastProvider";
import {
  type SignupPayload,
  signup as signupApi,
} from "@/lib/authentication-api";
import { errorMessage } from "@/lib/error-message";

export function useSignup() {
  const { showToast } = useToast();

  return useMutation({
    mutationFn: (payload: SignupPayload) => signupApi(payload),
    onSuccess: () => {
      // A guest account that signs up is converted into a real account —
      // its recovery token is no longer valid, so drop it to avoid stale
      // "guest session no longer exists" recovery attempts later.
      localStorage.removeItem("guestRecoveryToken");
    },
    onError: (error) => {
      showToast({ kind: "error", message: errorMessage(error) });
    },
  });
}
