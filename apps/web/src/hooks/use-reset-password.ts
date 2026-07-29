import { useMutation } from "@tanstack/react-query";

import {
  type ResetPasswordPayload,
  resetPassword as resetPasswordApi,
} from "@/lib/authentication-api";

export function useResetPassword() {
  return useMutation({
    mutationFn: (payload: ResetPasswordPayload) => resetPasswordApi(payload),
  });
}
