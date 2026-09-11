import { useQuery } from "@tanstack/react-query";

import { authenticationApi } from "@/lib/authentication-api";

export function useAuthentication() {
  const { data, isLoading, isError } = useQuery({
    queryKey: ["auth", "me"],
    queryFn: () => authenticationApi.getCurrentUser(),
    staleTime: 5 * 60 * 1000,
    retry: false,
  });

  return {
    user: data ?? null,
    isLoading,
    isError,
    isAuthenticated: data?.authenticated ?? false,
  };
}
