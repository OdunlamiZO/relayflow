import { useMutation } from "@tanstack/react-query";

import { type Plan, messagingApi } from "@/lib/messaging-api";

/**
 * Initiates a plan upgrade checkout session.
 *
 * On success, redirects the browser to the Paystack authorization URL to complete payment.
 * The user is sent back to `/settings?tab=billing&workspace={workspaceId}` after payment.
 */
export function useStartCheckout(workspaceId: string) {
  return useMutation({
    mutationFn: (plan: Plan) => messagingApi.startCheckout(workspaceId, plan),
    onSuccess: ({ authorizationUrl }) => {
      window.location.href = authorizationUrl;
    },
  });
}
