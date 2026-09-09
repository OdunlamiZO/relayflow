"use client";

import Link from "next/link";
import { useState } from "react";

import { EmptyState } from "@/components/common/EmptyState";
import { LoadingButton } from "@/components/common/LoadingButton";
import { Spinner } from "@/components/common/Spinner";
import { useToast } from "@/components/providers/ToastProvider";
import { useAuthentication } from "@/hooks/use-authentication";
import { useContact } from "@/hooks/use-contact";
import { useCurrentMember } from "@/hooks/use-current-member";
import { useUpdateContactCustomFields } from "@/hooks/use-update-contact-custom-fields";
import { useWorkspace } from "@/hooks/use-workspaces";
import { errorMessage } from "@/lib/error-message";
import {
  type ChannelProvider,
  RESERVED_CONTACT_FIELDS,
  RESERVED_CONTACT_FIELD_KEYS,
} from "@/lib/messaging-api";

// ── channel meta ──────────────────────────────────────────────────────────────

const CHANNEL_META: Record<
  ChannelProvider,
  { label: string; icon: string; color: string }
> = {
  TELEGRAM: {
    label: "Telegram",
    icon: "send",
    color: "bg-blue-bg text-blue-text",
  },
  WHATSAPP: {
    label: "WhatsApp",
    icon: "chat",
    color: "bg-green-bg text-green-text",
  },
  INSTAGRAM: {
    label: "Instagram",
    icon: "photo_camera",
    color: "bg-purple-bg text-purple-text",
  },
  MESSENGER: {
    label: "Messenger",
    icon: "forum",
    color: "bg-blue-bg text-blue-text",
  },
  WEBCHAT: {
    label: "Web Chat",
    icon: "web",
    color: "bg-teal-bg text-teal-text",
  },
  EMAIL: {
    label: "Email",
    icon: "mail",
    color: "bg-yellow-bg text-yellow-text",
  },
  SMS: {
    label: "SMS",
    icon: "sms",
    color: "bg-orange-bg text-orange-text",
  },
};

const LOCALE = "en-US";

function formatDateFull(isoString: string): string {
  return new Date(isoString).toLocaleDateString(LOCALE, {
    month: "long",
    day: "numeric",
    year: "numeric",
  });
}

function contactInitial(displayName: string | null): string {
  return (displayName ?? "?").trim().charAt(0).toUpperCase();
}

// ── props ─────────────────────────────────────────────────────────────────────

type Props = {
  contactId: string;
  workspaceId: string;
  onClose: () => void;
};

// ── component ─────────────────────────────────────────────────────────────────

export function ContactDetailPanel({ contactId, workspaceId, onClose }: Props) {
  const {
    data: contact,
    isPending,
    isError,
  } = useContact(contactId, workspaceId);

  return (
    <div className="flex h-full flex-col overflow-hidden border-l border-neutral-300 bg-neutral-100">
      {/* Header */}
      <div className="flex flex-shrink-0 items-center justify-between border-b border-neutral-300 bg-neutral-100 px-4 py-3">
        <h2 className="text-sm font-semibold text-neutral-800">
          Contact detail
        </h2>

        <button
          type="button"
          onClick={onClose}
          aria-label="Close panel"
          className="flex h-8 w-8 items-center justify-center rounded-lg text-neutral-400 transition-colors hover:bg-neutral-200 hover:text-neutral-700"
        >
          <span
            className="material-symbols-rounded text-[18px] leading-none"
            aria-hidden="true"
          >
            close
          </span>
        </button>
      </div>

      {/* Body */}
      <div className="flex-1 overflow-y-auto pb-14 md:pb-0">
        {isPending && (
          <div className="flex justify-center py-16">
            <Spinner />
          </div>
        )}

        {isError && (
          <div className="flex justify-center py-12">
            <EmptyState
              icon="error"
              title="Could not load contact"
              description="Check your connection and try again."
            />
          </div>
        )}

        {contact && (
          <div className="space-y-6 px-4 py-5 sm:px-6">
            {/* Avatar + name */}
            <div className="flex items-center gap-4">
              <div
                className="flex h-14 w-14 flex-shrink-0 items-center justify-center rounded-full bg-secondary text-xl font-semibold text-neutral-100"
                aria-hidden="true"
              >
                {contactInitial(contact.displayName)}
              </div>

              <div>
                <p className="text-base font-semibold text-neutral-900">
                  {contact.displayName ?? (
                    <span className="italic text-neutral-400">Unknown</span>
                  )}
                </p>

                <p className="mt-0.5 text-xs text-neutral-500">
                  Added {formatDateFull(contact.createdAt)}
                </p>
              </div>
            </div>

            {/* Quick action */}
            <Link
              href={`/inbox?workspaceId=${workspaceId}&contactId=${contact.id}`}
              className="flex w-full items-center justify-center gap-2 rounded-xl border border-neutral-300 bg-neutral-100 px-4 py-2.5 text-sm font-medium text-neutral-700 transition-colors hover:bg-neutral-200"
            >
              <span
                className="material-symbols-rounded text-[16px] leading-none"
                aria-hidden="true"
              >
                inbox
              </span>
              View conversations
            </Link>

            {/* Connected channels */}
            <div>
              <p className="mb-3 text-xs font-semibold uppercase tracking-wide text-neutral-400">
                Connected channels
              </p>

              {contact.identities.length === 0 ? (
                <p className="text-sm text-neutral-400">
                  No channels linked yet.
                </p>
              ) : (
                <ul className="space-y-2">
                  {contact.identities.map((identity) => {
                    const meta = CHANNEL_META[identity.provider];

                    return (
                      <li
                        key={identity.id}
                        className="flex items-start gap-3 rounded-xl border border-neutral-200 bg-neutral-50 px-3 py-3"
                      >
                        {/* Channel badge */}
                        <span
                          className={`mt-0.5 inline-flex items-center gap-1.5 rounded-full px-2 py-0.5 text-xs font-semibold ${meta.color}`}
                        >
                          <span
                            className="material-symbols-rounded text-[12px] leading-none"
                            aria-hidden="true"
                          >
                            {meta.icon}
                          </span>
                          {meta.label}
                        </span>

                        {/* Identity details */}
                        <div className="min-w-0 flex-1">
                          {identity.username && (
                            <p className="truncate text-sm font-medium text-neutral-800">
                              @{identity.username}
                            </p>
                          )}

                          <p className="truncate text-xs text-neutral-500">
                            ID: {identity.externalUserId}
                          </p>
                        </div>
                      </li>
                    );
                  })}
                </ul>
              )}
            </div>

            {/* Custom fields */}
            <ContactCustomFieldsSection
              key={contact.id}
              contactId={contact.id}
              workspaceId={workspaceId}
              customFields={contact.customFields}
            />
          </div>
        )}
      </div>
    </div>
  );
}

// ── custom fields ─────────────────────────────────────────────────────────────

type ContactCustomFieldsSectionProps = {
  contactId: string;
  workspaceId: string;
  customFields: Record<string, string>;
};

function ContactCustomFieldsSection({
  contactId,
  workspaceId,
  customFields,
}: ContactCustomFieldsSectionProps) {
  const workspace = useWorkspace(workspaceId);
  const { user } = useAuthentication();
  const currentMember = useCurrentMember(workspaceId, user?.userId);
  const updateCustomFields = useUpdateContactCustomFields(
    contactId,
    workspaceId
  );
  const { showToast } = useToast();

  const canEdit =
    currentMember?.role === "OWNER" ||
    currentMember?.permissions.includes("CONTACT_FIELDS_WRITE") === true;

  const fields = [
    ...RESERVED_CONTACT_FIELD_KEYS.map((key) => ({
      key,
      label: RESERVED_CONTACT_FIELDS[key].label,
    })),
    ...(workspace?.contactFieldDefinitions ?? []),
  ];

  const [values, setValues] = useState<Record<string, string>>(customFields);

  const isUnchanged = JSON.stringify(values) === JSON.stringify(customFields);

  function handleSave() {
    updateCustomFields.mutate(values, {
      onSuccess: () => {
        showToast({ kind: "success", message: "Contact fields saved" });
      },
      onError: (error) => {
        showToast({ kind: "error", message: errorMessage(error) });
      },
    });
  }

  return (
    <div>
      <p className="mb-3 text-xs font-semibold uppercase tracking-wide text-neutral-400">
        Contact fields
      </p>

      <div className="space-y-3">
        {fields.map((field) =>
          canEdit ? (
            <div key={field.key}>
              <label
                htmlFor={`custom-field-${field.key}`}
                className="mb-1 block text-xs font-medium text-neutral-600"
              >
                {field.label || field.key}
              </label>
              <input
                id={`custom-field-${field.key}`}
                type="text"
                value={values[field.key] ?? ""}
                onChange={(e) =>
                  setValues((prev) => ({
                    ...prev,
                    [field.key]: e.target.value,
                  }))
                }
                className="w-full rounded-lg border border-neutral-300 bg-neutral-100 px-3 py-2 text-sm text-neutral-800 outline-none transition-colors hover:border-neutral-400 focus:border-secondary focus:ring-2 focus:ring-secondary/20"
              />
            </div>
          ) : (
            <div
              key={field.key}
              className="flex items-center justify-between gap-3 rounded-lg border border-neutral-200 bg-neutral-50 px-3 py-2"
            >
              <span className="text-xs text-neutral-500">
                {field.label || field.key}
              </span>
              <span className="truncate text-sm text-neutral-800">
                {customFields[field.key] || (
                  <span className="italic text-neutral-400">Not set</span>
                )}
              </span>
            </div>
          )
        )}
      </div>

      {canEdit && !isUnchanged && (
        <LoadingButton
          type="button"
          onClick={handleSave}
          isLoading={updateCustomFields.isPending}
          className="mt-3 rounded-lg bg-secondary px-3 py-1.5 text-xs font-semibold text-neutral-100 transition-colors hover:bg-secondary-dark disabled:cursor-not-allowed disabled:opacity-60"
        >
          Save
        </LoadingButton>
      )}
    </div>
  );
}
