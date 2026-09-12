"use client";

import { useEffect, useState } from "react";

type Props = {
  secret: string;
  onClose: () => void;
};

export function WebhookSecretModal({ secret, onClose }: Props) {
  const [copied, setCopied] = useState(false);

  useEffect(() => {
    function onKeyDown(e: KeyboardEvent) {
      if (e.key === "Escape") onClose();
    }

    document.addEventListener("keydown", onKeyDown);

    return () => document.removeEventListener("keydown", onKeyDown);
  }, [onClose]);

  async function copySecret() {
    await navigator.clipboard.writeText(secret);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  }

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center p-4"
      aria-modal="true"
      role="dialog"
    >
      {/* Backdrop */}
      <div
        className="absolute inset-0 bg-neutral-900/40"
        onClick={onClose}
        aria-hidden="true"
      />

      {/* Dialog */}
      <div className="relative w-full max-w-md rounded-2xl bg-neutral-100 p-6 shadow-xl">
        <button
          type="button"
          onClick={onClose}
          className="absolute right-4 top-4 rounded-lg p-1 text-neutral-400 hover:bg-neutral-100 hover:text-neutral-700"
          aria-label="Close"
        >
          <span className="material-symbols-rounded text-[20px] leading-none">
            close
          </span>
        </button>

        <div className="mb-1 flex items-center gap-2">
          <span className="material-symbols-rounded text-[22px] leading-none text-green-text">
            check_circle
          </span>
          <h2 className="text-base font-semibold text-primary">
            New signing secret
          </h2>
        </div>

        <p className="mb-4 text-sm text-neutral-500">
          Copy this secret now — it will not be shown again.
        </p>

        <div className="mb-4 flex items-stretch gap-2 overflow-hidden rounded-xl border border-neutral-200 bg-neutral-100">
          <code className="flex-1 overflow-x-auto p-3 text-xs text-neutral-700 select-all">
            {secret}
          </code>

          <button
            type="button"
            onClick={copySecret}
            className="flex flex-shrink-0 items-center gap-1.5 border-l border-neutral-200 px-3 text-sm font-medium text-secondary transition-colors hover:bg-neutral-100"
            title="Copy to clipboard"
          >
            <span className="material-symbols-rounded text-[16px] leading-none">
              {copied ? "check" : "content_copy"}
            </span>
            {copied ? "Copied" : "Copy"}
          </button>
        </div>

        <p className="mb-4 text-xs text-neutral-500">
          Use it to verify the{" "}
          <code className="rounded bg-neutral-200 px-1 text-[11px]">
            X-RelayFlow-Signature
          </code>{" "}
          header on each delivery: it&apos;s{" "}
          <code className="rounded bg-neutral-200 px-1 text-[11px]">
            sha256=&lt;hex digest&gt;
          </code>
          , an HMAC-SHA256 of the raw request body keyed with this secret.
        </p>

        <div className="flex justify-end">
          <button
            type="button"
            onClick={onClose}
            className="rounded-lg bg-secondary px-4 py-2 text-sm font-semibold text-neutral-100 transition-colors hover:opacity-90"
          >
            Done
          </button>
        </div>
      </div>
    </div>
  );
}
