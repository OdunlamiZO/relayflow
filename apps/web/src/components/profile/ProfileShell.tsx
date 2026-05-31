"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";

import { QRCodeSVG } from "qrcode.react";

import { Spinner } from "@/components/common/Spinner";
import { useChangePassword } from "@/hooks/use-change-password";
import { useDeleteAccount } from "@/hooks/use-delete-account";
import { useDisable2FA } from "@/hooks/use-disable-2fa";
import { useEnable2FA } from "@/hooks/use-enable-2fa";
import { useProfile } from "@/hooks/use-profile";
import { useSetup2FA } from "@/hooks/use-setup-2fa";
import { useUpdateProfile } from "@/hooks/use-update-profile";

export function ProfileShell() {
  const router = useRouter();
  const { data: profile, isLoading, isError } = useProfile();

  if (isLoading) {
    return (
      <div className="flex h-full items-center justify-center">
        <Spinner size="lg" />
      </div>
    );
  }

  if (isError || !profile) {
    return (
      <div className="flex h-full items-center justify-center">
        <p className="text-sm text-neutral-500">Failed to load profile.</p>
      </div>
    );
  }

  const hasEmailLogin = profile.providers.includes("EMAIL");

  return (
    <div className="mx-auto max-w-2xl px-4 py-8 sm:px-6">
      <Link
        href="/inbox"
        className="mb-6 inline-flex items-center gap-1.5 text-sm text-neutral-500 transition-colors hover:text-neutral-800"
      >
        <span
          className="material-symbols-rounded text-[16px]"
          aria-hidden="true"
        >
          arrow_back
        </span>
        Back to inbox
      </Link>

      <div className="mb-8">
        <h1 className="text-2xl font-bold text-primary">Profile</h1>

        <p className="mt-1 text-sm text-neutral-500">
          Manage your account details and security settings.
        </p>
      </div>

      <div className="flex flex-col gap-6">
        <ProfileInfoSection
          displayName={profile.displayName ?? ""}
          email={profile.email}
          receiveEmailUpdates={profile.receiveEmailUpdates}
        />

        {hasEmailLogin && <ChangePasswordSection />}

        {hasEmailLogin && (
          <TwoFactorSection twoFactorEnabled={profile.twoFactorEnabled} />
        )}

        <DangerZoneSection
          isEmailProvider={hasEmailLogin}
          onDeleted={() => {
            router.push("/login");
          }}
        />
      </div>
    </div>
  );
}

// ── Profile info ──────────────────────────────────────────────────────────────

type ProfileInfoProps = {
  displayName: string;
  email: string;
  receiveEmailUpdates: boolean;
};

function ProfileInfoSection({
  displayName,
  email,
  receiveEmailUpdates,
}: ProfileInfoProps) {
  const { mutate: updateProfile, isPending } = useUpdateProfile();

  const [name, setName] = useState(displayName);
  const [emailUpdates, setEmailUpdates] = useState(receiveEmailUpdates);

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    updateProfile({ displayName: name, receiveEmailUpdates: emailUpdates });
  }

  return (
    <section className="rounded-2xl border border-neutral-300 bg-neutral-100 p-4 sm:p-6">
      <h2 className="mb-5 text-base font-semibold text-primary">
        Personal information
      </h2>

      <form onSubmit={handleSubmit} className="flex flex-col gap-4">
        <div className="flex flex-col gap-1.5">
          <label
            htmlFor="displayName"
            className="text-xs font-semibold uppercase tracking-wide text-neutral-600"
          >
            Display name
          </label>

          <input
            id="displayName"
            type="text"
            required
            maxLength={200}
            value={name}
            onChange={(e) => {
              setName(e.target.value);
            }}
            className="w-full rounded-lg border border-neutral-300 bg-white px-3 py-2.5 text-sm text-neutral-800 outline-none placeholder:text-neutral-400 focus:border-secondary focus:ring-2 focus:ring-secondary/20"
          />
        </div>

        <div className="flex flex-col gap-1.5">
          <label className="text-xs font-semibold uppercase tracking-wide text-neutral-600">
            Email
          </label>

          <input
            type="email"
            readOnly
            value={email}
            className="w-full rounded-lg border border-neutral-200 bg-neutral-200 px-3 py-2.5 text-sm text-neutral-500 outline-none"
          />
        </div>

        <label className="flex cursor-pointer items-center gap-3">
          <input
            type="checkbox"
            checked={emailUpdates}
            onChange={(e) => {
              setEmailUpdates(e.target.checked);
            }}
            className="h-4 w-4 rounded border-neutral-300 accent-secondary"
          />

          <span className="text-sm text-neutral-700">
            Receive email updates and product news
          </span>
        </label>

        <div className="flex justify-end">
          <button
            type="submit"
            disabled={isPending}
            className="flex items-center gap-2 rounded-lg bg-secondary px-4 py-2 text-sm font-semibold text-white transition-colors hover:bg-secondary-dark disabled:opacity-60"
          >
            {isPending && (
              <span
                className="material-symbols-rounded animate-spin text-[16px]"
                aria-hidden="true"
              >
                progress_activity
              </span>
            )}
            Save changes
          </button>
        </div>
      </form>
    </section>
  );
}

// ── Change password ───────────────────────────────────────────────────────────

function ChangePasswordSection() {
  const { mutate: changePassword, isPending } = useChangePassword();

  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [mismatch, setMismatch] = useState(false);

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();

    if (newPassword !== confirmPassword) {
      setMismatch(true);

      return;
    }

    setMismatch(false);
    changePassword(
      { currentPassword, newPassword },
      {
        onSuccess: () => {
          setCurrentPassword("");
          setNewPassword("");
          setConfirmPassword("");
        },
      }
    );
  }

  return (
    <section className="rounded-2xl border border-neutral-300 bg-neutral-100 p-4 sm:p-6">
      <h2 className="mb-5 text-base font-semibold text-primary">
        Change password
      </h2>

      <form onSubmit={handleSubmit} className="flex flex-col gap-4">
        <div className="flex flex-col gap-1.5">
          <label
            htmlFor="currentPassword"
            className="text-xs font-semibold uppercase tracking-wide text-neutral-600"
          >
            Current password
          </label>

          <input
            id="currentPassword"
            type="password"
            autoComplete="current-password"
            required
            value={currentPassword}
            onChange={(e) => {
              setCurrentPassword(e.target.value);
            }}
            className="w-full rounded-lg border border-neutral-300 bg-white px-3 py-2.5 text-sm text-neutral-800 outline-none placeholder:text-neutral-400 focus:border-secondary focus:ring-2 focus:ring-secondary/20"
          />
        </div>

        <div className="flex flex-col gap-1.5">
          <label
            htmlFor="newPassword"
            className="text-xs font-semibold uppercase tracking-wide text-neutral-600"
          >
            New password
          </label>

          <input
            id="newPassword"
            type="password"
            autoComplete="new-password"
            required
            minLength={8}
            value={newPassword}
            onChange={(e) => {
              setNewPassword(e.target.value);
              setMismatch(false);
            }}
            className="w-full rounded-lg border border-neutral-300 bg-white px-3 py-2.5 text-sm text-neutral-800 outline-none placeholder:text-neutral-400 focus:border-secondary focus:ring-2 focus:ring-secondary/20"
          />
        </div>

        <div className="flex flex-col gap-1.5">
          <label
            htmlFor="confirmPassword"
            className="text-xs font-semibold uppercase tracking-wide text-neutral-600"
          >
            Confirm new password
          </label>

          <input
            id="confirmPassword"
            type="password"
            autoComplete="new-password"
            required
            value={confirmPassword}
            onChange={(e) => {
              setConfirmPassword(e.target.value);
              setMismatch(false);
            }}
            className={`w-full rounded-lg border px-3 py-2.5 text-sm text-neutral-800 outline-none placeholder:text-neutral-400 focus:ring-2 ${
              mismatch
                ? "border-red-text bg-red-bg focus:border-red-text focus:ring-red-text/20"
                : "border-neutral-300 bg-white focus:border-secondary focus:ring-secondary/20"
            }`}
          />

          {mismatch && (
            <p className="text-xs text-red-text">Passwords do not match.</p>
          )}
        </div>

        <div className="flex justify-end">
          <button
            type="submit"
            disabled={isPending}
            className="flex items-center gap-2 rounded-lg bg-secondary px-4 py-2 text-sm font-semibold text-white transition-colors hover:bg-secondary-dark disabled:opacity-60"
          >
            {isPending && (
              <span
                className="material-symbols-rounded animate-spin text-[16px]"
                aria-hidden="true"
              >
                progress_activity
              </span>
            )}
            Update password
          </button>
        </div>
      </form>
    </section>
  );
}

// ── Two-factor authentication ─────────────────────────────────────────────────

type TwoFactorProps = {
  twoFactorEnabled: boolean;
};

function TwoFactorSection({ twoFactorEnabled }: TwoFactorProps) {
  // "setup" = showing QR + OTP confirm; "disable" = showing OTP for disable.
  const [mode, setMode] = useState<"idle" | "setup" | "disable">("idle");
  const [otpauthUri, setOtpauthUri] = useState<string | null>(null);
  const [otp, setOtp] = useState("");

  const { mutate: setup2FA, isPending: isSettingUp } = useSetup2FA();
  const { mutate: enable2FA, isPending: isEnabling } = useEnable2FA();
  const { mutate: disable2FA, isPending: isDisabling } = useDisable2FA();

  function handleStartSetup() {
    setup2FA(undefined, {
      onSuccess: (data) => {
        setOtpauthUri(data.otpauthUri);
        setMode("setup");
        setOtp("");
      },
    });
  }

  function handleEnable(e: React.FormEvent) {
    e.preventDefault();

    enable2FA(
      { otp },
      {
        onSuccess: () => {
          setMode("idle");
          setOtp("");
          setOtpauthUri(null);
        },
      }
    );
  }

  function handleDisable(e: React.FormEvent) {
    e.preventDefault();

    disable2FA(
      { otp },
      {
        onSuccess: () => {
          setMode("idle");
          setOtp("");
        },
      }
    );
  }

  return (
    <section className="rounded-2xl border border-neutral-300 bg-neutral-100 p-4 sm:p-6">
      <div className="mb-5 flex flex-wrap items-start justify-between gap-3">
        <div>
          <h2 className="text-base font-semibold text-primary">
            Two-factor authentication
          </h2>

          <p className="mt-0.5 text-sm text-neutral-500">
            {twoFactorEnabled
              ? "Your account is protected with an authenticator app."
              : "Add an extra layer of security to your account."}
          </p>
        </div>

        <span
          className={`shrink-0 rounded-full px-2.5 py-1 text-xs font-semibold ${
            twoFactorEnabled
              ? "bg-green-100 text-green-700"
              : "bg-neutral-200 text-neutral-500"
          }`}
        >
          {twoFactorEnabled ? "Enabled" : "Disabled"}
        </span>
      </div>

      {mode === "idle" && (
        <div>
          {twoFactorEnabled ? (
            <button
              type="button"
              onClick={() => {
                setMode("disable");
                setOtp("");
              }}
              className="rounded-lg border border-red-text px-4 py-2 text-sm font-semibold text-red-text transition-colors hover:bg-red-bg"
            >
              Disable 2FA
            </button>
          ) : (
            <button
              type="button"
              onClick={handleStartSetup}
              disabled={isSettingUp}
              className="flex items-center gap-2 rounded-lg bg-secondary px-4 py-2 text-sm font-semibold text-white transition-colors hover:bg-secondary-dark disabled:opacity-60"
            >
              {isSettingUp && (
                <span
                  className="material-symbols-rounded animate-spin text-[16px]"
                  aria-hidden="true"
                >
                  progress_activity
                </span>
              )}
              Set up 2FA
            </button>
          )}
        </div>
      )}

      {mode === "setup" && otpauthUri && (
        <div className="flex flex-col gap-5">
          <div className="flex flex-col gap-2">
            <p className="text-sm font-medium text-neutral-700">
              1. Scan this QR code with your authenticator app
            </p>

            <div className="flex justify-center rounded-xl border border-neutral-200 bg-white p-4">
              <QRCodeSVG value={otpauthUri} size={180} />
            </div>
          </div>

          <form onSubmit={handleEnable} className="flex flex-col gap-3">
            <div className="flex flex-col gap-1.5">
              <label
                htmlFor="enable-otp"
                className="text-sm font-medium text-neutral-700"
              >
                2. Enter the 6-digit code to confirm
              </label>

              <input
                id="enable-otp"
                type="text"
                inputMode="numeric"
                autoComplete="one-time-code"
                maxLength={6}
                required
                value={otp}
                onChange={(e) => {
                  setOtp(e.target.value.replace(/\D/g, ""));
                }}
                placeholder="000000"
                className="w-full rounded-lg border border-neutral-300 bg-white px-3 py-2.5 text-center font-mono text-lg tracking-widest text-neutral-800 outline-none placeholder:text-neutral-400 focus:border-secondary focus:ring-2 focus:ring-secondary/20"
              />
            </div>

            <div className="flex flex-col gap-2 sm:flex-row">
              <button
                type="submit"
                disabled={isEnabling || otp.length !== 6}
                className="flex items-center justify-center gap-2 rounded-lg bg-secondary px-4 py-2.5 text-sm font-semibold text-white transition-colors hover:bg-secondary-dark disabled:opacity-60 sm:py-2"
              >
                {isEnabling && (
                  <span
                    className="material-symbols-rounded animate-spin text-[16px]"
                    aria-hidden="true"
                  >
                    progress_activity
                  </span>
                )}
                Enable 2FA
              </button>

              <button
                type="button"
                onClick={() => {
                  setMode("idle");
                  setOtp("");
                  setOtpauthUri(null);
                }}
                className="rounded-lg px-4 py-2.5 text-sm font-medium text-neutral-500 transition-colors hover:text-neutral-700 sm:py-2"
              >
                Cancel
              </button>
            </div>
          </form>
        </div>
      )}

      {mode === "disable" && (
        <form onSubmit={handleDisable} className="flex flex-col gap-3">
          <div className="flex flex-col gap-1.5">
            <label
              htmlFor="disable-otp"
              className="text-sm font-medium text-neutral-700"
            >
              Enter the 6-digit code from your authenticator app to confirm
            </label>

            <input
              id="disable-otp"
              type="text"
              inputMode="numeric"
              autoComplete="one-time-code"
              maxLength={6}
              required
              value={otp}
              onChange={(e) => {
                setOtp(e.target.value.replace(/\D/g, ""));
              }}
              placeholder="000000"
              className="w-full rounded-lg border border-neutral-300 bg-white px-3 py-2.5 text-center font-mono text-lg tracking-widest text-neutral-800 outline-none placeholder:text-neutral-400 focus:border-secondary focus:ring-2 focus:ring-secondary/20"
            />
          </div>

          <div className="flex flex-col gap-2 sm:flex-row">
            <button
              type="submit"
              disabled={isDisabling || otp.length !== 6}
              className="flex items-center justify-center gap-2 rounded-lg border border-red-text px-4 py-2.5 text-sm font-semibold text-red-text transition-colors hover:bg-red-bg disabled:opacity-60 sm:py-2"
            >
              {isDisabling && (
                <span
                  className="material-symbols-rounded animate-spin text-[16px]"
                  aria-hidden="true"
                >
                  progress_activity
                </span>
              )}
              Disable 2FA
            </button>

            <button
              type="button"
              onClick={() => {
                setMode("idle");
                setOtp("");
              }}
              className="rounded-lg px-4 py-2.5 text-sm font-medium text-neutral-500 transition-colors hover:text-neutral-700 sm:py-2"
            >
              Cancel
            </button>
          </div>
        </form>
      )}
    </section>
  );
}

// ── Danger zone ───────────────────────────────────────────────────────────────

type DangerZoneProps = {
  isEmailProvider: boolean;
  onDeleted: () => void;
};

function DangerZoneSection({ isEmailProvider, onDeleted }: DangerZoneProps) {
  const { mutate: deleteAccount, isPending } = useDeleteAccount();

  const [showConfirm, setShowConfirm] = useState(false);
  const [password, setPassword] = useState("");
  const [passwordError, setPasswordError] = useState<string | null>(null);

  function handleDelete() {
    if (isEmailProvider && !password.trim()) {
      setPasswordError("Password is required to delete your account.");

      return;
    }

    setPasswordError(null);
    deleteAccount(
      { password: isEmailProvider ? password : null },
      {
        onSuccess: () => {
          onDeleted();
        },
      }
    );
  }

  return (
    <section className="rounded-2xl border border-red-text/30 bg-red-bg/30 p-4 sm:p-6">
      <h2 className="mb-1 text-base font-semibold text-red-text">
        Danger zone
      </h2>

      <p className="mb-5 text-sm text-neutral-500">
        Permanently delete your account and all associated data. This cannot be
        undone.
      </p>

      <button
        type="button"
        onClick={() => {
          setShowConfirm(true);
          setPassword("");
          setPasswordError(null);
        }}
        className="rounded-lg border border-red-text px-4 py-2 text-sm font-semibold text-red-text transition-colors hover:bg-red-bg"
      >
        Delete account
      </button>

      {showConfirm && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center p-4"
          aria-modal="true"
          role="dialog"
        >
          {/* Backdrop */}
          <div
            className="absolute inset-0 bg-black/40"
            onClick={() => {
              setShowConfirm(false);
            }}
            aria-hidden="true"
          />

          {/* Dialog */}
          <div className="relative w-full max-w-sm rounded-2xl bg-white p-6 shadow-xl">
            <h2 className="text-base font-semibold text-primary">
              Delete account
            </h2>

            <p className="mt-1.5 text-sm text-neutral-500">
              This will permanently delete your account and all your data. This
              action cannot be reversed.
            </p>

            {isEmailProvider && (
              <div className="mt-4 flex flex-col gap-1.5">
                <label
                  htmlFor="delete-password"
                  className="text-xs font-semibold uppercase tracking-wide text-neutral-600"
                >
                  Confirm with your password
                </label>

                <input
                  id="delete-password"
                  type="password"
                  autoComplete="current-password"
                  value={password}
                  onChange={(e) => {
                    setPassword(e.target.value);
                    setPasswordError(null);
                  }}
                  placeholder="••••••••"
                  className={`w-full rounded-lg border px-3 py-2.5 text-sm text-neutral-800 outline-none placeholder:text-neutral-400 focus:ring-2 ${
                    passwordError
                      ? "border-red-text focus:border-red-text focus:ring-red-text/20"
                      : "border-neutral-300 focus:border-secondary focus:ring-secondary/20"
                  }`}
                />

                {passwordError && (
                  <p className="text-xs text-red-text">{passwordError}</p>
                )}
              </div>
            )}

            <div className="mt-5 flex justify-end gap-2">
              <button
                type="button"
                onClick={() => {
                  setShowConfirm(false);
                }}
                disabled={isPending}
                className="rounded-lg px-4 py-2 text-sm font-medium text-neutral-600 transition-colors hover:bg-neutral-100 disabled:opacity-60"
              >
                Cancel
              </button>

              <button
                type="button"
                onClick={handleDelete}
                disabled={isPending}
                className="rounded-lg bg-red-text px-4 py-2 text-sm font-semibold text-white transition-colors hover:opacity-90 disabled:opacity-60"
              >
                {isPending ? "Deleting…" : "Delete account"}
              </button>
            </div>
          </div>
        </div>
      )}
    </section>
  );
}
