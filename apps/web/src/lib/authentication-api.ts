export type SignupPayload = {
  name: string;
  email: string;
  password: string;
};

export type LoginPayload = {
  email: string;
  password: string;
};

export type AuthenticatedUserResponse = {
  authenticated: boolean;
  anonymous: boolean;
  email: string | null;
  displayName: string | null;
  avatarUrl: string | null;
};

export type GuestSessionResponse = {
  workspaceId: string;
};

const apiBaseUrl =
  process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";

async function apiPost<T>(path: string, body: unknown): Promise<T> {
  const response = await fetch(`${apiBaseUrl}${path}`, {
    method: "POST",
    credentials: "include",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
  });

  if (!response.ok) {
    const data: unknown = await response.json().catch(() => ({}));

    const message =
      data !== null &&
      typeof data === "object" &&
      "message" in data &&
      typeof (data as { message: unknown }).message === "string"
        ? (data as { message: string }).message
        : `Request failed with status ${response.status}`;

    throw new Error(message);
  }

  return response.json() as Promise<T>;
}

export function signup(
  payload: SignupPayload
): Promise<AuthenticatedUserResponse> {
  return apiPost<AuthenticatedUserResponse>("/api/auth/signup", payload);
}

export function login(
  payload: LoginPayload
): Promise<AuthenticatedUserResponse> {
  return apiPost<AuthenticatedUserResponse>("/api/auth/login", payload);
}

export async function logout(): Promise<void> {
  await fetch(`${apiBaseUrl}/api/auth/logout`, {
    method: "POST",
    credentials: "include",
  });
}

export function createGuestSession(): Promise<GuestSessionResponse> {
  return apiPost<GuestSessionResponse>("/api/auth/guest", {});
}
