import { useMutation } from "@tanstack/react-query";

import {
  type ResetPasswordPayload,
  authenticationApi,
} from "@/lib/authentication-api";

export function useResetPassword() {
  return useMutation({
    mutationFn: (payload: ResetPasswordPayload) =>
      authenticationApi.resetPassword(payload),
  });
}
