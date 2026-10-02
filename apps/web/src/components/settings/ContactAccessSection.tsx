"use client";

import { useMemo, useState } from "react";

import { ConfirmModal } from "@/components/common/ConfirmModal";
import { IconButton } from "@/components/common/IconButton";
import { LoadingButton } from "@/components/common/LoadingButton";
import { Select } from "@/components/common/Select";
import { Spinner } from "@/components/common/Spinner";
import { useToast } from "@/components/providers/ToastProvider";
import { useAddToWhitelist } from "@/hooks/use-add-to-whitelist";
import { useRemoveFromWhitelist } from "@/hooks/use-remove-from-whitelist";
import { useUpdateContactAccess } from "@/hooks/use-update-contact-access";
import { useWhitelist } from "@/hooks/use-whitelist";
import { useWorkspace } from "@/hooks/use-workspaces";
import { countryOptions } from "@/lib/countries";
import { errorMessage } from "@/lib/error-message";
import {
  type ContactAccess,
  type UpdateContactAccessRequest,
  type WhitelistedPhoneNumber,
} from "@/lib/messaging-api";

type Props = {
  workspaceId: string;
};

const ACCESS_OPTIONS: { value: ContactAccess; label: string }[] = [
  { value: "ALL", label: "Everyone" },
  { value: "WHITELIST", label: "Whitelisted numbers only" },
];

const ACCESS_DESCRIPTIONS: Record<ContactAccess, string> = {
  ALL: "Anyone who messages a connected channel reaches the inbox.",
  WHITELIST:
    "Only whitelisted numbers reach the inbox; others are ignored. Telegram users are asked to share their number first.",
};

const SEARCH_THRESHOLD = 10;

const NO_DEFAULT_COUNTRY = "";

export function ContactAccessSection({ workspaceId }: Props) {
  const workspace = useWorkspace(workspaceId);
  const { data: whitelist, isLoading } = useWhitelist(workspaceId);
  const updateContactAccess = useUpdateContactAccess(workspaceId);
  const { showToast } = useToast();

  const [confirmingWhitelist, setConfirmingWhitelist] = useState(false);

  const contactAccess = workspace?.contactAccess ?? "ALL";
  const phoneRegion = workspace?.phoneRegion ?? null;

  function save(request: UpdateContactAccessRequest, onSuccess?: () => void) {
    updateContactAccess.mutate(request, {
      onSuccess,
      onError: (error) => {
        showToast({ kind: "error", message: errorMessage(error) });
      },
    });
  }

  function handleAccessChange(next: ContactAccess) {
    if (next === contactAccess) return;

    if (next === "WHITELIST" && (whitelist?.length ?? 0) === 0) {
      setConfirmingWhitelist(true);

      return;
    }

    save({ contactAccess: next, phoneRegion });
  }

  return (
    <section className="mt-8">
      <h2 className="mb-1 text-xs font-semibold uppercase tracking-wide text-neutral-400">
        Contact access
      </h2>
      <p className="mb-3 text-xs text-neutral-500">
        Choose who can message this workspace.
      </p>

      <div
        role="radiogroup"
        aria-label="Who can message"
        className="inline-flex rounded-lg border border-neutral-300 bg-neutral-200 p-0.5"
      >
        {ACCESS_OPTIONS.map((option) => (
          <button
            key={option.value}
            type="button"
            role="radio"
            aria-checked={contactAccess === option.value}
            disabled={updateContactAccess.isPending}
            onClick={() => handleAccessChange(option.value)}
            className={`rounded-md px-3 py-1.5 text-sm font-medium transition-colors disabled:cursor-wait ${
              contactAccess === option.value
                ? "bg-neutral-100 text-neutral-900 shadow-sm"
                : "text-neutral-500 hover:text-neutral-800"
            }`}
          >
            {option.label}
          </button>
        ))}
      </div>

      <p className="mt-2 text-xs text-neutral-500">
        {ACCESS_DESCRIPTIONS[contactAccess]}
      </p>

      {contactAccess === "WHITELIST" && (
        <WhitelistSection
          workspaceId={workspaceId}
          whitelist={whitelist ?? []}
          isLoading={isLoading}
          phoneRegion={phoneRegion}
          onPhoneRegionChange={(next) =>
            save({ contactAccess, phoneRegion: next })
          }
        />
      )}

      {confirmingWhitelist && (
        <ConfirmModal
          title="Turn on the whitelist?"
          description="No numbers are whitelisted yet, so every new message will be ignored until you add some."
          confirmLabel="Turn on"
          isPending={updateContactAccess.isPending}
          onConfirm={() =>
            save({ contactAccess: "WHITELIST", phoneRegion }, () =>
              setConfirmingWhitelist(false)
            )
          }
          onCancel={() => setConfirmingWhitelist(false)}
        />
      )}
    </section>
  );
}

type WhitelistSectionProps = {
  workspaceId: string;
  whitelist: WhitelistedPhoneNumber[];
  isLoading: boolean;
  phoneRegion: string | null;
  onPhoneRegionChange: (phoneRegion: string | null) => void;
};

function WhitelistSection({
  workspaceId,
  whitelist,
  isLoading,
  phoneRegion,
  onPhoneRegionChange,
}: WhitelistSectionProps) {
  const addToWhitelist = useAddToWhitelist(workspaceId);
  const removeFromWhitelist = useRemoveFromWhitelist(workspaceId);
  const { showToast } = useToast();

  const [input, setInput] = useState("");
  const [search, setSearch] = useState("");
  const [removeTarget, setRemoveTarget] =
    useState<WhitelistedPhoneNumber | null>(null);

  const countries = useMemo(
    () => [{ value: NO_DEFAULT_COUNTRY, label: "None" }, ...countryOptions()],
    []
  );

  const phoneNumbers = input
    .split(/[\n,;]+/)
    .map((value) => value.trim())
    .filter(Boolean);

  const searchDigits = search.replace(/\D/g, "");
  const visibleEntries = searchDigits
    ? whitelist.filter((entry) => entry.phoneNumber.includes(searchDigits))
    : whitelist;

  function handleAdd(e: React.FormEvent) {
    e.preventDefault();

    if (phoneNumbers.length === 0) return;

    addToWhitelist.mutate(phoneNumbers, {
      onSuccess: (added) => {
        setInput("");
        showToast({
          kind: "success",
          message:
            added.length === 0
              ? "Already whitelisted"
              : `Added ${added.length} number${added.length === 1 ? "" : "s"}`,
        });
      },
      onError: (error) => {
        showToast({ kind: "error", message: errorMessage(error) });
      },
    });
  }

  function handleRemove() {
    if (!removeTarget) return;

    removeFromWhitelist.mutate(removeTarget.id, {
      onSuccess: () => setRemoveTarget(null),
      onError: (error) => {
        showToast({ kind: "error", message: errorMessage(error) });
      },
    });
  }

  return (
    <div className="mt-5">
      <h3 className="mb-2 text-sm font-medium text-neutral-800">
        Whitelisted numbers ({whitelist.length})
      </h3>

      <form onSubmit={handleAdd} className="flex items-center gap-2">
        <input
          type="text"
          value={input}
          onChange={(e) => setInput(e.target.value)}
          placeholder="+14155550123, +447700900123"
          aria-label="Phone numbers to whitelist"
          className="w-full flex-1 rounded-lg border border-neutral-300 bg-neutral-100 px-3 py-2 font-mono text-sm text-neutral-800 outline-none placeholder:font-sans placeholder:text-neutral-400 focus:border-secondary focus:ring-2 focus:ring-secondary/20"
        />
        <LoadingButton
          type="submit"
          isLoading={addToWhitelist.isPending}
          disabled={phoneNumbers.length === 0}
          className="flex-shrink-0 rounded-lg bg-secondary px-4 py-2 text-sm font-semibold text-neutral-100 transition-colors hover:bg-secondary-dark disabled:cursor-not-allowed disabled:opacity-60"
        >
          Add
        </LoadingButton>
      </form>

      <div className="mt-2 flex items-center gap-2 text-xs text-neutral-500">
        <span>Default country</span>
        <div className="w-48">
          <Select
            value={phoneRegion ?? NO_DEFAULT_COUNTRY}
            onChange={(value) => onPhoneRegionChange(value || null)}
            options={countries}
          />
        </div>
        <span>for numbers without a country code</span>
      </div>

      <div className="mt-5">
        {whitelist.length > SEARCH_THRESHOLD && (
          <input
            type="search"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Search numbers"
            className="mb-2 w-full rounded-lg border border-neutral-300 bg-neutral-100 px-3 py-1.5 text-sm text-neutral-800 outline-none placeholder:text-neutral-400 focus:border-secondary focus:ring-2 focus:ring-secondary/20"
          />
        )}

        {isLoading ? (
          <div className="flex justify-center py-6">
            <Spinner />
          </div>
        ) : whitelist.length === 0 ? (
          <div className="rounded-xl border border-dashed border-neutral-300 px-4 py-6 text-center">
            <p className="text-sm text-neutral-400">
              No whitelisted numbers yet.
            </p>
          </div>
        ) : visibleEntries.length === 0 ? (
          <p className="py-4 text-center text-sm text-neutral-400">
            No matching numbers.
          </p>
        ) : (
          <ul className="divide-y divide-neutral-300 rounded-lg border border-neutral-300 bg-neutral-100">
            {visibleEntries.map((entry) => (
              <li
                key={entry.id}
                className="flex items-center justify-between gap-2 px-3 py-1.5"
              >
                <span className="font-mono text-sm text-neutral-800">
                  {entry.phoneNumber}
                </span>
                <IconButton
                  icon="delete"
                  destructive
                  label={`Remove ${entry.phoneNumber}`}
                  onClick={() => setRemoveTarget(entry)}
                />
              </li>
            ))}
          </ul>
        )}
      </div>

      {removeTarget && (
        <ConfirmModal
          title={`Remove ${removeTarget.phoneNumber}?`}
          description="Messages from this number will be ignored."
          confirmLabel="Remove"
          destructive
          isPending={removeFromWhitelist.isPending}
          onConfirm={handleRemove}
          onCancel={() => setRemoveTarget(null)}
        />
      )}
    </div>
  );
}
