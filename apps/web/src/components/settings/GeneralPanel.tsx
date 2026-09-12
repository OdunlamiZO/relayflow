"use client";

import { useState } from "react";

import { LoadingButton } from "@/components/common/LoadingButton";
import { useToast } from "@/components/providers/ToastProvider";
import { useAuthentication } from "@/hooks/use-authentication";
import { useCurrentMember } from "@/hooks/use-current-member";
import { useUpdateContactFieldDefinitions } from "@/hooks/use-update-contact-field-definitions";
import { useUpdateWorkspace } from "@/hooks/use-update-workspace";
import { useWorkspace } from "@/hooks/use-workspaces";
import { errorMessage } from "@/lib/error-message";
import {
  type ContactFieldDefinition,
  RESERVED_CONTACT_FIELDS,
  RESERVED_CONTACT_FIELD_KEYS,
  isReservedContactFieldKey,
} from "@/lib/messaging-api";

type Props = {
  workspaceId: string;
};

export function GeneralPanel({ workspaceId }: Props) {
  const workspace = useWorkspace(workspaceId);
  const { user } = useAuthentication();
  const currentMember = useCurrentMember(workspaceId, user?.userId);
  const canEditContactFields =
    currentMember?.role === "OWNER" ||
    currentMember?.permissions.includes("CONTACT_FIELDS_WRITE") === true;
  const updateWorkspace = useUpdateWorkspace(workspaceId);
  const updateContactFieldDefinitions =
    useUpdateContactFieldDefinitions(workspaceId);
  const { showToast } = useToast();

  const [edited, setEdited] = useState<string | null>(null);
  const name = edited ?? workspace?.name ?? "";

  const trimmed = name.trim();
  const isUnchanged = trimmed === (workspace?.name ?? "");

  const [editedFields, setEditedFields] = useState<
    ContactFieldDefinition[] | null
  >(null);
  const contactFieldDefinitions =
    editedFields ?? workspace?.contactFieldDefinitions ?? [];
  const fieldsUnchanged =
    JSON.stringify(contactFieldDefinitions) ===
    JSON.stringify(workspace?.contactFieldDefinitions ?? []);
  const hasReservedKeyConflict = contactFieldDefinitions.some((field) =>
    isReservedContactFieldKey(field.key)
  );

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();

    if (!trimmed || isUnchanged) return;

    updateWorkspace.mutate(trimmed, {
      onSuccess: () => {
        showToast({ kind: "success", message: "Workspace renamed" });
      },
      onError: (error) => {
        showToast({ kind: "error", message: errorMessage(error) });
      },
    });
  }

  function patchFields(next: ContactFieldDefinition[]) {
    setEditedFields(next);
  }

  function addContactField() {
    patchFields([
      ...contactFieldDefinitions,
      { key: "", label: "", description: "" },
    ]);
  }

  function updateContactField(
    index: number,
    field: keyof ContactFieldDefinition,
    value: string
  ) {
    patchFields(
      contactFieldDefinitions.map((entry, i) =>
        i === index ? { ...entry, [field]: value } : entry
      )
    );
  }

  function removeContactField(index: number) {
    patchFields(contactFieldDefinitions.filter((_, i) => i !== index));
  }

  function handleSaveContactFields() {
    updateContactFieldDefinitions.mutate(contactFieldDefinitions, {
      onSuccess: () => {
        showToast({ kind: "success", message: "Contact fields saved" });
      },
      onError: (error) => {
        showToast({ kind: "error", message: errorMessage(error) });
      },
    });
  }

  return (
    <div className="mx-auto max-w-2xl px-6 py-8 sm:px-8">
      <div className="mb-8">
        <h1 className="text-xl font-bold text-primary">General</h1>
        <p className="mt-1.5 text-sm text-neutral-500">
          Manage your workspace&apos;s basic settings.
        </p>
      </div>

      <section>
        <h2 className="mb-3 text-xs font-semibold uppercase tracking-wide text-neutral-400">
          Workspace name
        </h2>

        <form onSubmit={handleSubmit} className="flex items-center gap-2">
          <input
            type="text"
            value={name}
            onChange={(e) => {
              setEdited(e.target.value);
            }}
            maxLength={160}
            required
            className="w-full flex-1 rounded-lg border border-neutral-300 bg-neutral-100 px-3 py-2 text-sm text-neutral-800 outline-none placeholder:text-neutral-400 focus:border-secondary focus:ring-2 focus:ring-secondary/20"
          />

          <LoadingButton
            type="submit"
            isLoading={updateWorkspace.isPending}
            disabled={!trimmed || isUnchanged}
            className="flex-shrink-0 rounded-lg bg-secondary px-4 py-2 text-sm font-semibold text-neutral-100 transition-colors hover:bg-secondary-dark disabled:cursor-not-allowed disabled:opacity-60"
          >
            Save
          </LoadingButton>
        </form>
      </section>

      <section className="mt-8">
        <h2 className="mb-1 text-xs font-semibold uppercase tracking-wide text-neutral-400">
          Contact fields
        </h2>
        <p className="mb-3 text-xs text-neutral-500">
          Fields tracked on every contact — editable from the contact detail
          panel, and filled in automatically by the AI agent when it extracts a
          matching key (see AI Agent → Data extraction).
        </p>

        {/* Built-in fields — always present, never editable here */}
        <div className="mb-4 space-y-2">
          {RESERVED_CONTACT_FIELD_KEYS.map((key) => (
            <div
              key={key}
              className="rounded-lg border border-neutral-200 bg-neutral-50 px-3 py-2.5"
            >
              <div className="flex items-center justify-between gap-2">
                <span className="text-sm font-medium text-neutral-800">
                  {RESERVED_CONTACT_FIELDS[key].label}
                </span>
                <code className="rounded bg-neutral-200 px-1 text-[11px] text-neutral-500">
                  {key}
                </code>
              </div>
              <p className="mt-0.5 text-xs text-neutral-500">
                {RESERVED_CONTACT_FIELDS[key].description}
              </p>
            </div>
          ))}
        </div>

        {/* Custom fields — editable only with CONTACT_FIELDS_WRITE */}
        <div className="space-y-3">
          {contactFieldDefinitions.map((field, i) => {
            const isReserved = isReservedContactFieldKey(field.key);

            return canEditContactFields ? (
              <div
                key={i}
                className="rounded-lg border border-neutral-200 bg-neutral-100 p-3"
              >
                <div className="mb-2 flex items-center justify-between">
                  <span className="text-xs font-medium text-neutral-500">
                    Field {i + 1}
                  </span>
                  <button
                    type="button"
                    onClick={() => removeContactField(i)}
                    className="text-xs text-red-500 hover:text-red-700"
                  >
                    Remove
                  </button>
                </div>
                <input
                  type="text"
                  placeholder="Key (e.g. orderNumber)"
                  value={field.key}
                  onChange={(e) => updateContactField(i, "key", e.target.value)}
                  className={`mb-2 w-full rounded-lg border bg-neutral-100 px-3 py-2 font-mono text-sm text-neutral-800 outline-none transition-colors focus:ring-2 ${
                    isReserved
                      ? "border-red-400 focus:border-red-500 focus:ring-red-500/20"
                      : "border-neutral-300 hover:border-neutral-400 focus:border-secondary focus:ring-secondary/20"
                  }`}
                />
                {isReserved && (
                  <p className="mb-2 text-xs text-red-500">
                    &quot;{field.key.trim()}&quot; is already a built-in field.
                  </p>
                )}
                <input
                  type="text"
                  placeholder="Label (e.g. Order Number)"
                  value={field.label}
                  onChange={(e) =>
                    updateContactField(i, "label", e.target.value)
                  }
                  className="mb-2 w-full rounded-lg border border-neutral-300 bg-neutral-100 px-3 py-2 text-sm text-neutral-800 outline-none transition-colors hover:border-neutral-400 focus:border-secondary focus:ring-2 focus:ring-secondary/20"
                />
                <input
                  type="text"
                  placeholder="Description (optional)"
                  value={field.description}
                  onChange={(e) =>
                    updateContactField(i, "description", e.target.value)
                  }
                  className="w-full rounded-lg border border-neutral-300 bg-neutral-100 px-3 py-2 text-sm text-neutral-800 outline-none transition-colors hover:border-neutral-400 focus:border-secondary focus:ring-2 focus:ring-secondary/20"
                />
              </div>
            ) : (
              <div
                key={i}
                className="rounded-lg border border-neutral-200 bg-neutral-50 px-3 py-2.5"
              >
                <span className="text-sm font-medium text-neutral-800">
                  {field.label || field.key}
                </span>
                {field.description && (
                  <p className="mt-0.5 text-xs text-neutral-500">
                    {field.description}
                  </p>
                )}
              </div>
            );
          })}

          {contactFieldDefinitions.length === 0 && !canEditContactFields && (
            <p className="text-xs text-neutral-400">
              No custom fields defined yet.
            </p>
          )}
        </div>

        {canEditContactFields && (
          <div className="mt-3 flex items-center gap-4">
            <button
              type="button"
              onClick={addContactField}
              className="text-xs font-medium text-secondary hover:text-secondary-dark"
            >
              + Add field
            </button>

            {!fieldsUnchanged && (
              <LoadingButton
                type="button"
                onClick={handleSaveContactFields}
                isLoading={updateContactFieldDefinitions.isPending}
                disabled={hasReservedKeyConflict}
                className="rounded-lg bg-secondary px-3 py-1.5 text-xs font-semibold text-neutral-100 transition-colors hover:bg-secondary-dark disabled:cursor-not-allowed disabled:opacity-60"
              >
                Save
              </LoadingButton>
            )}
          </div>
        )}
      </section>
    </div>
  );
}
