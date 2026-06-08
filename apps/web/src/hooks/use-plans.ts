import { useQuery } from "@tanstack/react-query";

import { type PlanInfo, messagingApi } from "@/lib/messaging-api";

/**
 * Fetches the public plan catalogue — limits, pricing, and availability for every plan.
 * No authentication required; safe to call from the landing page or upgrade modal.
 */
export function usePlans() {
  return useQuery<PlanInfo[]>({
    queryKey: ["plans"],
    queryFn: () => messagingApi.getPlans(),
    staleTime: 5 * 60 * 1000, // plan config rarely changes; cache for 5 minutes
  });
}
