import { useMutation } from "@tanstack/react-query";

import { useToast } from "@/components/providers/ToastProvider";
import {
  type SignupPayload,
  authenticationApi,
} from "@/lib/authentication-api";
import { errorMessage } from "@/lib/error-message";

export function useSignup() {
  const { showToast } = useToast();

  return useMutation({
    mutationFn: (payload: SignupPayload) => authenticationApi.signup(payload),
    onError: (error) => {
      showToast({ kind: "error", message: errorMessage(error) });
    },
  });
}
