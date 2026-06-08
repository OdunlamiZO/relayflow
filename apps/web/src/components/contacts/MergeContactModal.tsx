"use client";

import { useState } from "react";

import { Spinner } from "@/components/common/Spinner";
import { useContacts } from "@/hooks/use-contacts";
import { useMergeContact } from "@/hooks/use-merge-contact";
import type { Contact } from "@/lib/messaging-api";
import type { ApiError } from "@/lib/messaging-api";

// ── helpers ───────────────────────────────────────────────────────────────────

function contactInitial(contact: Contact): string {
  return (contact.displayName ?? "?").trim().charAt(0).toUpperCase();
}

// ── props ─────────────────────────────────────────────────────────────────────

type Props = {
  /** The contact being merged FROM (will be deleted after merge). */
  source: Contact;
  workspaceId: string;
  onSuccess: () => void;
  onCancel: () => void;
};

// ── component ─────────────────────────────────────────────────────────────────

export function MergeContactModal({
  source,
  workspaceId,
  onSuccess,
  onCancel,
}: Props) {
  const [query, setQuery] = useState("");
  const [selected, setSelected] = useState<Contact | null>(null);

  const { data, isPending: isLoadingContacts } = useContacts(workspaceId);
  const {
    mutate: merge,
    isPending: isMerging,
    error,
  } = useMergeContact(workspaceId);

  const allContacts = data?.pages.flatMap((p) => p.items) ?? [];

  const candidates = allContacts.filter((c) => {
    if (c.id === source.id) return false;

    if (!query.trim()) return true;

    const q = query.toLowerCase();
    const name = (c.displayName ?? "").toLowerCase();

    return name.includes(q);
  });

  function handleConfirm() {
    if (!selected) return;

    merge({ targetId: selected.id, sourceId: source.id }, { onSuccess });
  }

  const errorMessage =
    error && typeof error === "object" && "message" in error
      ? (error as ApiError).message
      : error
        ? "Merge failed. Please try again."
        : null;

  return (
    <div className="fixed inset-0 z-50 flex items-end justify-center bg-neutral-900/40 backdrop-blur-sm sm:items-center sm:p-4">
      <div className="flex max-h-[90dvh] w-full max-w-md flex-col overflow-hidden rounded-t-2xl bg-neutral-100 shadow-2xl sm:rounded-2xl">
        {/* Header */}
        <div className="flex items-start justify-between border-b border-neutral-300 px-5 py-4">
          <div>
            <h2 className="text-sm font-semibold text-neutral-900">
              Merge contact
            </h2>
            <p className="mt-0.5 text-xs text-neutral-500">
              Choose the contact to merge{" "}
              <span className="font-medium text-neutral-700">
                {source.displayName ?? "Unknown"}
              </span>{" "}
              into. The selected contact will keep all conversations and channel
              identities from both.
            </p>
          </div>

          <button
            type="button"
            onClick={onCancel}
            className="ml-3 flex h-8 w-8 flex-shrink-0 items-center justify-center rounded-lg text-neutral-400 hover:bg-neutral-100 hover:text-neutral-700"
          >
            <span
              className="material-symbols-rounded text-[18px] leading-none"
              aria-hidden="true"
            >
              close
            </span>
          </button>
        </div>

        {/* Search */}
        <div className="border-b border-neutral-300 px-4 py-3">
          <div className="flex items-center gap-2 rounded-lg border border-neutral-300 bg-neutral-100 px-3 py-2 focus-within:border-secondary focus-within:ring-1 focus-within:ring-secondary">
            <span
              className="material-symbols-rounded text-[16px] leading-none text-neutral-400"
              aria-hidden="true"
            >
              search
            </span>
            <input
              type="text"
              placeholder="Search contacts…"
              value={query}
              onChange={(e) => {
                setQuery(e.target.value);
                setSelected(null);
              }}
              className="flex-1 bg-transparent text-sm text-neutral-800 outline-none placeholder:text-neutral-400"
            />
          </div>
        </div>

        {/* Contact list */}
        <div className="flex-1 overflow-y-auto">
          {isLoadingContacts && (
            <div className="flex justify-center py-8">
              <Spinner />
            </div>
          )}

          {!isLoadingContacts && candidates.length === 0 && (
            <p className="py-8 text-center text-sm text-neutral-400">
              No contacts found.
            </p>
          )}

          {!isLoadingContacts && candidates.length > 0 && (
            <ul className="divide-y divide-neutral-200">
              {candidates.map((contact) => {
                const isSelected = selected?.id === contact.id;

                return (
                  <li key={contact.id}>
                    <button
                      type="button"
                      onClick={() => setSelected(isSelected ? null : contact)}
                      className={`flex w-full items-center gap-3 px-4 py-3 text-left transition-colors hover:bg-neutral-200 ${
                        isSelected ? "bg-blue-bg" : ""
                      }`}
                    >
                      <div
                        className="flex h-8 w-8 flex-shrink-0 items-center justify-center rounded-full bg-secondary text-xs font-semibold text-neutral-100"
                        aria-hidden="true"
                      >
                        {contactInitial(contact)}
                      </div>

                      <span
                        className={`flex-1 text-sm font-medium ${
                          isSelected ? "text-secondary" : "text-neutral-800"
                        }`}
                      >
                        {contact.displayName ?? (
                          <span className="italic text-neutral-400">
                            Unknown
                          </span>
                        )}
                      </span>

                      {isSelected && (
                        <span
                          className="material-symbols-rounded text-[18px] leading-none text-secondary"
                          aria-hidden="true"
                        >
                          check_circle
                        </span>
                      )}
                    </button>
                  </li>
                );
              })}
            </ul>
          )}
        </div>

        {/* Error */}
        {errorMessage && (
          <p className="border-t border-neutral-200 px-5 py-2.5 text-xs text-red-text">
            {errorMessage}
          </p>
        )}

        {/* Footer */}
        <div className="flex items-center justify-end gap-2 border-t border-neutral-300 px-5 py-3">
          <button
            type="button"
            onClick={onCancel}
            disabled={isMerging}
            className="rounded-lg border border-neutral-300 bg-neutral-100 px-4 py-2 text-sm font-medium text-neutral-700 transition-colors hover:bg-neutral-100 disabled:opacity-60"
          >
            Cancel
          </button>

          <button
            type="button"
            onClick={handleConfirm}
            disabled={!selected || isMerging}
            className="flex items-center gap-1.5 rounded-lg bg-secondary px-4 py-2 text-sm font-medium text-neutral-100 transition-colors hover:bg-secondary-dark disabled:opacity-50"
          >
            {isMerging && <Spinner size="sm" />}
            <span className="truncate">
              {selected
                ? `Merge into ${selected.displayName ?? "Unknown"}`
                : "Select a contact"}
            </span>
          </button>
        </div>
      </div>
    </div>
  );
}
