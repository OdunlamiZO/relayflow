import { useQuery } from "@tanstack/react-query";

import { getProfile } from "@/lib/authentication-api";

export function useProfile() {
  return useQuery({
    queryKey: ["profile"],
    queryFn: getProfile,
    staleTime: 5 * 60 * 1000,
    retry: false,
  });
}
