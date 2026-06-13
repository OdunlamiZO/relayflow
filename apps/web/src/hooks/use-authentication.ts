import { useQuery } from "@tanstack/react-query";

export type AuthenticatedUser = {
  authenticated: boolean;
  anonymous: boolean;
  userId: string | null;
  email: string | null;
  displayName: string | null;
  avatarUrl: string | null;
  twoFactorRequired?: boolean;
  challengeToken?: string | null;
};

const apiBaseUrl =
  process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";

async function fetchCurrentUser(): Promise<AuthenticatedUser> {
  const response = await fetch(`${apiBaseUrl}/auth/me`, {
    credentials: "include",
  });

  if (!response.ok) {
    throw new Error("Failed to fetch current user");
  }

  return response.json() as Promise<AuthenticatedUser>;
}

export function useAuthentication() {
  const { data, isLoading, isError } = useQuery({
    queryKey: ["auth", "me"],
    queryFn: fetchCurrentUser,
    staleTime: 5 * 60 * 1000,
    retry: false,
  });

  return {
    user: data ?? null,
    isLoading,
    isError,
    isAuthenticated: data?.authenticated ?? false,
    isAnonymous: data?.anonymous ?? false,
  };
}
