import { useQuery } from "@tanstack/react-query";

import { authenticationApi } from "@/lib/authentication-api";

export function useProfile() {
  return useQuery({
    queryKey: ["profile"],
    queryFn: () => authenticationApi.getProfile(),
    staleTime: 5 * 60 * 1000,
    retry: false,
  });
}
