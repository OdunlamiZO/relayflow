import {
  ApiError,
  type RequestOptions,
  apiErrorMessage,
  readResponseBody,
} from "@/lib/api-client";

export { ApiError };

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

const defaultBaseUrl =
  process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";

export class AuthenticationApiClient {
  private readonly baseUrl: string;

  constructor(baseUrl = defaultBaseUrl) {
    this.baseUrl = baseUrl.replace(/\/$/, "");
  }

  // ── Auth ──────────────────────────────────────────────────────────────────

  signup(payload: SignupPayload) {
    return this.request<SignupResponse>("/auth/signup", {
      method: "POST",
      body: payload,
    });
  }

  bootstrap(payload: BootstrapPayload) {
    return this.request<BootstrapResponse>("/auth/bootstrap", {
      method: "POST",
      body: payload,
    });
  }

  getInstanceStatus() {
    return this.request<InstanceStatusResponse>("/auth/bootstrap-status");
  }

  getCurrentUser() {
    return this.request<AuthenticatedUserResponse>("/auth/me");
  }

  login(payload: LoginPayload) {
    return this.request<AuthenticatedUserResponse>("/auth/login", {
      method: "POST",
      body: payload,
    });
  }

  login2FA(payload: Login2FAPayload) {
    return this.request<AuthenticatedUserResponse>("/auth/login/2fa", {
      method: "POST",
      body: payload,
    });
  }

  async logout(): Promise<void> {
    await this.request<void>("/auth/logout", { method: "POST" });
  }

  resetPassword(payload: ResetPasswordPayload) {
    return this.request<void>(`/auth/reset-password/${payload.token}`, {
      method: "POST",
      body: { newPassword: payload.newPassword },
    });
  }

  // ── Profile ───────────────────────────────────────────────────────────────

  getProfile() {
    return this.request<ProfileResponse>("/profile");
  }

  updateProfile(payload: UpdateProfilePayload) {
    return this.request<ProfileResponse>("/profile", {
      method: "PATCH",
      body: payload,
    });
  }

  requestPasswordReset() {
    return this.request<void>("/profile/request-password-reset", {
      method: "POST",
    });
  }

  deleteAccount(payload: DeleteAccountPayload) {
    return this.request<void>("/profile", {
      method: "DELETE",
      body: payload,
    });
  }

  setup2FA() {
    return this.request<Setup2FAResponse>("/profile/2fa/setup", {
      method: "POST",
    });
  }

  enable2FA(payload: OtpPayload) {
    return this.request<void>("/profile/2fa/enable", {
      method: "POST",
      body: payload,
    });
  }

  disable2FA(payload: OtpPayload) {
    return this.request<void>("/profile/2fa/disable", {
      method: "POST",
      body: payload,
    });
  }

  private async request<T>(
    path: string,
    options: RequestOptions = {}
  ): Promise<T> {
    const response = await fetch(`${this.baseUrl}${path}`, {
      method: options.method ?? "GET",
      credentials: "include",
      headers:
        options.body === undefined
          ? undefined
          : { "Content-Type": "application/json" },
      body:
        options.body === undefined ? undefined : JSON.stringify(options.body),
    });

    if (!response.ok) {
      const details = await readResponseBody(response);

      throw new ApiError(
        response.status,
        apiErrorMessage(response.status, details),
        details
      );
    }

    if (
      response.status === 204 ||
      response.headers.get("content-length") === "0"
    ) {
      return undefined as T;
    }

    return (await response.json()) as T;
  }
}

export const authenticationApi = new AuthenticationApiClient();
