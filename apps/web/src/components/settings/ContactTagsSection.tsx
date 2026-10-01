"use client";

import { useState } from "react";

import { ConfirmModal } from "@/components/common/ConfirmModal";
import { IconButton } from "@/components/common/IconButton";
import { LoadingButton } from "@/components/common/LoadingButton";
import {
  CONTACT_TAG_COLORS,
  CONTACT_TAG_SWATCH_CLASS,
  ContactTagPill,
  tagColor,
} from "@/components/contacts/ContactTagPill";
import { useToast } from "@/components/providers/ToastProvider";
import { useUpdateContactTagDefinitions } from "@/hooks/use-update-contact-tag-definitions";
import { errorMessage } from "@/lib/error-message";
import type { ContactTagDefinition } from "@/lib/messaging-api";

const INPUT_CLASS =
  "w-full rounded-lg border border-neutral-300 bg-neutral-100 px-3 py-2 text-sm text-neutral-800 outline-none transition-colors hover:border-neutral-400 focus:border-secondary focus:ring-2 focus:ring-secondary/20";

const EMPTY_TAG: ContactTagDefinition = { key: "", label: "", values: [] };

type Props = {
  workspaceId: string;
  savedDefinitions: ContactTagDefinition[];
  canEdit: boolean;
};

type Editing = {
  index: number | null;
  draft: ContactTagDefinition;
};

function cleaned(definition: ContactTagDefinition): ContactTagDefinition {
  const values = definition.values.map((value) => value.trim()).filter(Boolean);

  return {
    ...definition,
    key: definition.key.trim(),
    values,
    colors: Object.fromEntries(
      Object.entries(definition.colors ?? {}).filter(([value]) =>
        values.includes(value)
      )
    ),
  };
}

export function ContactTagsSection({
  workspaceId,
  savedDefinitions,
  canEdit,
}: Props) {
  const updateDefinitions = useUpdateContactTagDefinitions(workspaceId);
  const { showToast } = useToast();

  const [editing, setEditing] = useState<Editing | null>(null);
  const [deleteIndex, setDeleteIndex] = useState<number | null>(null);

  function persist(
    next: ContactTagDefinition[],
    message: string,
    onDone: () => void
  ) {
    updateDefinitions.mutate(next, {
      onSuccess: () => {
        onDone();
        showToast({ kind: "success", message });
      },
      onError: (error) => {
        showToast({ kind: "error", message: errorMessage(error) });
      },
    });
  }

  function saveDraft() {
    if (!editing) return;

    const definition = cleaned(editing.draft);
    const next =
      editing.index === null
        ? [...savedDefinitions, definition]
        : savedDefinitions.map((saved, i) =>
            i === editing.index ? definition : saved
          );

    persist(next, "Contact tag saved", () => setEditing(null));
  }

  function confirmDelete() {
    if (deleteIndex === null) return;

    persist(
      savedDefinitions.filter((_, i) => i !== deleteIndex),
      "Contact tag deleted",
      () => setDeleteIndex(null)
    );
  }

  const form = editing && (
    <ContactTagForm
      draft={editing.draft}
      onChange={(draft) => setEditing({ ...editing, draft })}
      onCancel={() => setEditing(null)}
      onSave={saveDraft}
      isSaving={updateDefinitions.isPending}
    />
  );

  return (
    <section className="mt-8">
      <h2 className="mb-1 text-xs font-semibold uppercase tracking-wide text-neutral-400">
        Contact tags
      </h2>
      <p className="mb-3 text-xs text-neutral-500">
        Labels with a fixed set of values, such as a status. Set them on a
        contact&apos;s page, from a workflow, or through the API. Workflows can
        read them as <code>contact.tags.&lt;key&gt;</code>.
      </p>

      <div className="space-y-3">
        {savedDefinitions.map((definition, i) =>
          editing?.index === i ? (
            <div key={i}>{form}</div>
          ) : (
            <div
              key={i}
              className="rounded-lg border border-neutral-300 bg-neutral-100 px-3 py-2.5"
            >
              <div className="flex items-start justify-between gap-3">
                <div className="min-w-0">
                  <p className="text-sm font-medium text-neutral-800">
                    {definition.label || definition.key}
                  </p>
                  <p className="font-mono text-xs text-neutral-400">
                    {definition.key}
                  </p>
                </div>

                {canEdit && !editing && (
                  <div className="flex">
                    <IconButton
                      icon="edit"
                      label={`Edit ${definition.label || definition.key}`}
                      onClick={() =>
                        setEditing({ index: i, draft: definition })
                      }
                    />
                    <IconButton
                      icon="delete"
                      label={`Delete ${definition.label || definition.key}`}
                      onClick={() => setDeleteIndex(i)}
                      destructive
                    />
                  </div>
                )}
              </div>

              <div className="mt-2 flex flex-wrap gap-1">
                {definition.values.map((value) => (
                  <ContactTagPill
                    key={value}
                    value={value}
                    color={tagColor(definition, value)}
                  />
                ))}
              </div>
            </div>
          )
        )}

        {editing?.index === null && form}

        {savedDefinitions.length === 0 && !editing && (
          <p className="text-xs text-neutral-400">
            No contact tags defined yet.
          </p>
        )}
      </div>

      {canEdit && !editing && (
        <button
          type="button"
          onClick={() => setEditing({ index: null, draft: EMPTY_TAG })}
          className="mt-3 text-xs font-medium text-secondary hover:text-secondary-dark"
        >
          + Add tag
        </button>
      )}

      {deleteIndex !== null && (
        <ConfirmModal
          title={`Delete "${savedDefinitions[deleteIndex]?.label || savedDefinitions[deleteIndex]?.key}"?`}
          description="Contacts can't have this tag anymore. It can't be deleted while any contact still has one of its values."
          confirmLabel="Delete"
          destructive
          isPending={updateDefinitions.isPending}
          onConfirm={confirmDelete}
          onCancel={() => setDeleteIndex(null)}
        />
      )}
    </section>
  );
}

type FormProps = {
  draft: ContactTagDefinition;
  onChange: (draft: ContactTagDefinition) => void;
  onCancel: () => void;
  onSave: () => void;
  isSaving: boolean;
};

function ContactTagForm({
  draft,
  onChange,
  onCancel,
  onSave,
  isSaving,
}: FormProps) {
  const values = cleaned(draft).values;

  return (
    <div className="rounded-lg border border-secondary bg-neutral-100 p-3">
      <input
        type="text"
        placeholder="Key (e.g. kyc_status)"
        value={draft.key}
        onChange={(e) => onChange({ ...draft, key: e.target.value })}
        className={`mb-2 font-mono ${INPUT_CLASS}`}
      />
      <input
        type="text"
        placeholder="Label (e.g. KYC status)"
        value={draft.label}
        onChange={(e) => onChange({ ...draft, label: e.target.value })}
        className={`mb-2 ${INPUT_CLASS}`}
      />
      <input
        type="text"
        placeholder="Values, comma-separated (e.g. pending, verified, rejected)"
        value={draft.values.join(",")}
        onChange={(e) =>
          onChange({ ...draft, values: e.target.value.split(",") })
        }
        className={INPUT_CLASS}
      />

      <div className="mt-3 space-y-2">
        {values.map((value) => {
          const color = tagColor(draft, value);

          return (
            <div
              key={value}
              className="flex items-center justify-between gap-3"
            >
              <ContactTagPill value={value} color={color} />
              <div className="flex gap-1.5">
                {CONTACT_TAG_COLORS.map((option) => (
                  <button
                    key={option}
                    type="button"
                    title={option}
                    aria-label={`${value}: ${option}`}
                    onClick={() =>
                      onChange({
                        ...draft,
                        colors: { ...draft.colors, [value]: option },
                      })
                    }
                    className={`h-4 w-4 rounded-full ${CONTACT_TAG_SWATCH_CLASS[option]} ${
                      option === color
                        ? "ring-2 ring-secondary ring-offset-1"
                        : ""
                    }`}
                  />
                ))}
              </div>
            </div>
          );
        })}
      </div>

      <div className="mt-4 flex justify-end gap-2">
        <button
          type="button"
          onClick={onCancel}
          className="rounded-lg border border-neutral-300 px-3 py-1.5 text-xs font-medium text-neutral-600 transition-colors hover:text-neutral-900"
        >
          Cancel
        </button>
        <LoadingButton
          type="button"
          onClick={onSave}
          isLoading={isSaving}
          disabled={!draft.key.trim() || values.length === 0}
          className="rounded-lg bg-secondary px-3 py-1.5 text-xs font-semibold text-neutral-100 transition-colors hover:bg-secondary-dark disabled:cursor-not-allowed disabled:opacity-60"
        >
          Save
        </LoadingButton>
      </div>
    </div>
  );
}
