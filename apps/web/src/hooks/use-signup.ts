import { useMutation, useQueryClient } from "@tanstack/react-query";

import { useToast } from "@/components/providers/ToastProvider";
import {
  type SignupPayload,
  signup as signupApi,
} from "@/lib/authentication-api";
import { errorMessage } from "@/lib/error-message";

export function useSignup() {
  const queryClient = useQueryClient();
  const { showToast } = useToast();

  return useMutation({
    mutationFn: (payload: SignupPayload) => signupApi(payload),
    onError: (error) => {
      showToast({ kind: "error", message: errorMessage(error) });
    },
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ["auth", "me"] });
    },
  });
}
