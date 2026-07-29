export type SignupPayload = {
  name: string;
  email: string;
  password: string;
  inviteToken: string;
};

export type LoginPayload = {
  email: string;
  password: string;
};

export type AuthenticatedUserResponse = {
  authenticated: boolean;
  userId: string | null;
  email: string | null;
  displayName: string | null;
  avatarUrl: string | null;
  twoFactorRequired: boolean;
  challengeToken: string | null;
};

export type SignupResponse = {
  authenticated: boolean;
  userId: string;
  email: string;
  displayName: string | null;
  workspaceId: string;
};

export type BootstrapPayload = {
  name: string;
  email: string;
  password: string;
  workspaceName: string;
};

export type BootstrapResponse = {
  authenticated: boolean;
  userId: string;
  email: string;
  displayName: string | null;
  workspaceId: string;
};

export type InstanceStatusResponse = {
  bootstrapped: boolean;
};

export type ProfileResponse = {
  userId: string;
  email: string;
  displayName: string | null;
  avatarUrl: string | null;
  twoFactorEnabled: boolean;
  providers: string[];
};

export type UpdateProfilePayload = {
  displayName: string;
};

export type ResetPasswordPayload = {
  token: string;
  newPassword: string;
};

export type DeleteAccountPayload = {
  password: string | null;
};

export type Setup2FAResponse = {
  otpauthUri: string;
};

export type OtpPayload = {
  otp: string;
};

export type Login2FAPayload = {
  challengeToken: string;
  otp: string;
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

  if (response.status === 204) {
    return undefined as T;
  }

  return response.json() as Promise<T>;
}

async function apiPatch<T>(path: string, body: unknown): Promise<T> {
  const response = await fetch(`${apiBaseUrl}${path}`, {
    method: "PATCH",
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

async function apiDelete(path: string, body?: unknown): Promise<void> {
  const response = await fetch(`${apiBaseUrl}${path}`, {
    method: "DELETE",
    credentials: "include",
    headers: { "Content-Type": "application/json" },
    body: body !== undefined ? JSON.stringify(body) : undefined,
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
}

// ── Auth ────────────────────────────────────────────────────────────────────

export function signup(payload: SignupPayload): Promise<SignupResponse> {
  return apiPost<SignupResponse>("/auth/signup", payload);
}

export function bootstrap(
  payload: BootstrapPayload
): Promise<BootstrapResponse> {
  return apiPost<BootstrapResponse>("/auth/bootstrap", payload);
}

export async function getInstanceStatus(): Promise<InstanceStatusResponse> {
  const response = await fetch(`${apiBaseUrl}/auth/bootstrap-status`, {
    cache: "no-store",
  });

  if (!response.ok) {
    throw new Error(`Request failed with status ${response.status}`);
  }

  return response.json() as Promise<InstanceStatusResponse>;
}

export function login(
  payload: LoginPayload
): Promise<AuthenticatedUserResponse> {
  return apiPost<AuthenticatedUserResponse>("/auth/login", payload);
}

export function login2FA(
  payload: Login2FAPayload
): Promise<AuthenticatedUserResponse> {
  return apiPost<AuthenticatedUserResponse>("/auth/login/2fa", payload);
}

export async function logout(): Promise<void> {
  await fetch(`${apiBaseUrl}/auth/logout`, {
    method: "POST",
    credentials: "include",
  });
}

export function resetPassword(payload: ResetPasswordPayload): Promise<void> {
  return apiPost<void>(`/auth/reset-password/${payload.token}`, {
    newPassword: payload.newPassword,
  });
}

// ── Profile ─────────────────────────────────────────────────────────────────

export async function getProfile(): Promise<ProfileResponse> {
  const response = await fetch(`${apiBaseUrl}/profile`, {
    credentials: "include",
  });

  if (!response.ok) {
    throw new Error(`Request failed with status ${response.status}`);
  }

  return response.json() as Promise<ProfileResponse>;
}

export function updateProfile(
  payload: UpdateProfilePayload
): Promise<ProfileResponse> {
  return apiPatch<ProfileResponse>("/profile", payload);
}

export function requestPasswordReset(): Promise<void> {
  return apiPost<void>("/profile/request-password-reset", {});
}

export function deleteAccount(payload: DeleteAccountPayload): Promise<void> {
  return apiDelete("/profile", payload);
}

export function setup2FA(): Promise<Setup2FAResponse> {
  return apiPost<Setup2FAResponse>("/profile/2fa/setup", {});
}

export function enable2FA(payload: OtpPayload): Promise<void> {
  return apiPost<void>("/profile/2fa/enable", payload);
}

export function disable2FA(payload: OtpPayload): Promise<void> {
  return apiPost<void>("/profile/2fa/disable", payload);
}
