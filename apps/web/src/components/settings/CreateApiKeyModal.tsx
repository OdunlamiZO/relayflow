"use client";

import { useEffect, useRef, useState } from "react";

import { type CreateApiKeyResponse } from "@/lib/messaging-api";

type ExpiryOption = {
  label: string;
  days: number | null;
};

const EXPIRY_OPTIONS: ExpiryOption[] = [
  { label: "No expiry", days: null },
  { label: "30 days", days: 30 },
  { label: "90 days", days: 90 },
  { label: "1 year", days: 365 },
];

type Props = {
  isCreating: boolean;
  createdKey: CreateApiKeyResponse | null;
  onSubmit: (name: string, expiresAt: string | null) => void;
  onClose: () => void;
};

export function CreateApiKeyModal({
  isCreating,
  createdKey,
  onSubmit,
  onClose,
}: Props) {
  const [name, setName] = useState("");
  const [expiryDays, setExpiryDays] = useState<number | null>(null);
  const [copied, setCopied] = useState(false);
  const nameRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    nameRef.current?.focus();
  }, []);

  useEffect(() => {
    function onKeyDown(e: KeyboardEvent) {
      if (e.key === "Escape") onClose();
    }

    document.addEventListener("keydown", onKeyDown);

    return () => document.removeEventListener("keydown", onKeyDown);
  }, [onClose]);

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();

    if (!name.trim()) return;

    let expiresAt: string | null = null;

    if (expiryDays !== null) {
      const d = new Date();
      d.setDate(d.getDate() + expiryDays);
      expiresAt = d.toISOString();
    }

    onSubmit(name.trim(), expiresAt);
  }

  async function copyKey() {
    if (!createdKey) return;

    await navigator.clipboard.writeText(createdKey.key);
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

        {createdKey ? (
          /* ── Step 2: Key reveal ── */
          <div>
            <div className="mb-1 flex items-center gap-2">
              <span className="material-symbols-rounded text-[22px] leading-none text-green-text">
                check_circle
              </span>
              <h2 className="text-base font-semibold text-primary">
                API key created
              </h2>
            </div>

            <p className="mb-4 text-sm text-neutral-500">
              Copy this key now — it will not be shown again.
            </p>

            <div className="mb-4 flex items-stretch gap-2 overflow-hidden rounded-xl border border-neutral-200 bg-neutral-50">
              <code className="flex-1 overflow-x-auto p-3 text-xs text-neutral-700 select-all">
                {createdKey.key}
              </code>

              <button
                type="button"
                onClick={copyKey}
                className="flex flex-shrink-0 items-center gap-1.5 border-l border-neutral-200 px-3 text-sm font-medium text-secondary transition-colors hover:bg-neutral-100"
                title="Copy to clipboard"
              >
                <span className="material-symbols-rounded text-[16px] leading-none">
                  {copied ? "check" : "content_copy"}
                </span>
                {copied ? "Copied" : "Copy"}
              </button>
            </div>

            {createdKey.expiresAt && (
              <p className="mb-4 text-xs text-neutral-400">
                Expires{" "}
                {new Date(createdKey.expiresAt).toLocaleDateString(undefined, {
                  year: "numeric",
                  month: "long",
                  day: "numeric",
                })}
              </p>
            )}

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
        ) : (
          /* ── Step 1: Creation form ── */
          <form onSubmit={handleSubmit}>
            <h2 className="mb-4 text-base font-semibold text-primary">
              Create API key
            </h2>

            <div className="mb-4">
              <label
                htmlFor="key-name"
                className="mb-1.5 block text-sm font-medium text-neutral-700"
              >
                Name
              </label>
              <input
                id="key-name"
                ref={nameRef}
                type="text"
                value={name}
                onChange={(e) => setName(e.target.value)}
                placeholder="e.g. Production integration"
                maxLength={100}
                required
                className="w-full rounded-xl border border-neutral-200 bg-neutral-50 px-3 py-2 text-sm text-primary placeholder-neutral-400 outline-none focus:border-secondary focus:ring-1 focus:ring-secondary"
              />
            </div>

            <div className="mb-6">
              <label className="mb-1.5 block text-sm font-medium text-neutral-700">
                Expiry
              </label>

              <div className="flex flex-wrap gap-2">
                {EXPIRY_OPTIONS.map((opt) => (
                  <button
                    key={opt.label}
                    type="button"
                    onClick={() => setExpiryDays(opt.days)}
                    className={`rounded-full border px-3 py-1 text-sm font-medium transition-colors ${
                      expiryDays === opt.days
                        ? "border-secondary bg-secondary text-neutral-100"
                        : "border-neutral-200 text-neutral-600 hover:border-secondary hover:text-secondary"
                    }`}
                  >
                    {opt.label}
                  </button>
                ))}
              </div>
            </div>

            <div className="flex justify-end gap-2">
              <button
                type="button"
                onClick={onClose}
                disabled={isCreating}
                className="rounded-lg px-4 py-2 text-sm font-medium text-neutral-600 transition-colors hover:bg-neutral-100 disabled:opacity-60"
              >
                Cancel
              </button>

              <button
                type="submit"
                disabled={isCreating || !name.trim()}
                className="rounded-lg bg-secondary px-4 py-2 text-sm font-semibold text-neutral-100 transition-colors hover:opacity-90 disabled:opacity-60"
              >
                {isCreating ? "Creating…" : "Create"}
              </button>
            </div>
          </form>
        )}
      </div>
    </div>
  );
}
