"use client";

import { useState } from "react";

import { useConnectWhatsApp } from "@/hooks/use-connect-whatsapp";

type Props = {
  workspaceId: string;
  onSuccess: () => void;
  onCancel: () => void;
};

export function ConnectWhatsAppForm({
  workspaceId,
  onSuccess,
  onCancel,
}: Props) {
  const [name, setName] = useState("");
  const [accessToken, setAccessToken] = useState("");
  const [phoneNumberId, setPhoneNumberId] = useState("");
  const [verifyToken, setVerifyToken] = useState("");

  const { mutate: connect, isPending } = useConnectWhatsApp(workspaceId);

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();

    connect(
      {
        name: name.trim() || "WhatsApp",
        accessToken: accessToken.trim(),
        phoneNumberId: phoneNumberId.trim(),
        verifyToken: verifyToken.trim(),
      },
      { onSuccess }
    );
  }

  const canSubmit =
    accessToken.trim() && phoneNumberId.trim() && verifyToken.trim();

  return (
    <form onSubmit={handleSubmit} className="flex flex-col gap-4">
      <div className="flex flex-col gap-1.5">
        <label
          className="text-xs font-medium text-neutral-700"
          htmlFor="wa-channel-name"
        >
          Display name
        </label>
        <input
          id="wa-channel-name"
          type="text"
          placeholder="e.g. Support WhatsApp"
          value={name}
          onChange={(e) => setName(e.target.value)}
          className="rounded-lg border border-neutral-300 bg-white px-3 py-2 text-sm text-neutral-800 placeholder-neutral-400 focus:border-secondary focus:outline-none focus:ring-1 focus:ring-secondary"
        />
      </div>

      <div className="flex flex-col gap-1.5">
        <label
          className="text-xs font-medium text-neutral-700"
          htmlFor="wa-access-token"
        >
          Access token
        </label>
        <input
          id="wa-access-token"
          type="password"
          required
          placeholder="EAABsbCS..."
          value={accessToken}
          onChange={(e) => setAccessToken(e.target.value)}
          className="rounded-lg border border-neutral-300 bg-white px-3 py-2 font-mono text-sm text-neutral-800 placeholder-neutral-400 focus:border-secondary focus:outline-none focus:ring-1 focus:ring-secondary"
        />
        <p className="text-xs text-neutral-500">
          Permanent system-user access token from your{" "}
          <a
            href="https://developers.facebook.com/apps"
            target="_blank"
            rel="noopener noreferrer"
            className="text-secondary underline underline-offset-2"
          >
            Meta app
          </a>
          .
        </p>
      </div>

      <div className="flex flex-col gap-1.5">
        <label
          className="text-xs font-medium text-neutral-700"
          htmlFor="wa-phone-number-id"
        >
          Phone number ID
        </label>
        <input
          id="wa-phone-number-id"
          type="text"
          required
          placeholder="106540352242922"
          value={phoneNumberId}
          onChange={(e) => setPhoneNumberId(e.target.value)}
          className="rounded-lg border border-neutral-300 bg-white px-3 py-2 font-mono text-sm text-neutral-800 placeholder-neutral-400 focus:border-secondary focus:outline-none focus:ring-1 focus:ring-secondary"
        />
        <p className="text-xs text-neutral-500">
          Found under your WhatsApp Business App → Phone numbers → ID.
        </p>
      </div>

      <div className="flex flex-col gap-1.5">
        <label
          className="text-xs font-medium text-neutral-700"
          htmlFor="wa-verify-token"
        >
          Verify token
        </label>
        <input
          id="wa-verify-token"
          type="text"
          required
          placeholder="my-secret-token"
          value={verifyToken}
          onChange={(e) => setVerifyToken(e.target.value)}
          className="rounded-lg border border-neutral-300 bg-white px-3 py-2 font-mono text-sm text-neutral-800 placeholder-neutral-400 focus:border-secondary focus:outline-none focus:ring-1 focus:ring-secondary"
        />
        <p className="text-xs text-neutral-500">
          A secret string you choose. Enter the same value in the Meta Developer
          Console when registering the webhook URL.
        </p>
      </div>

      <div className="flex items-center gap-2">
        <button
          type="submit"
          disabled={isPending || !canSubmit}
          className="rounded-lg bg-secondary px-4 py-2 text-sm font-semibold text-white transition-colors hover:bg-secondary-dark disabled:cursor-not-allowed disabled:opacity-60"
        >
          {isPending ? "Connecting…" : "Connect WhatsApp"}
        </button>

        <button
          type="button"
          onClick={onCancel}
          disabled={isPending}
          className="rounded-lg border border-neutral-300 px-4 py-2 text-sm font-medium text-neutral-600 transition-colors hover:bg-neutral-100 disabled:opacity-60"
        >
          Cancel
        </button>
      </div>
    </form>
  );
}
