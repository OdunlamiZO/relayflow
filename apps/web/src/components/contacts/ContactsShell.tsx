"use client";

import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { useRef, useState } from "react";

import { ConfirmModal } from "@/components/common/ConfirmModal";
import { EmptyState } from "@/components/common/EmptyState";
import { Spinner } from "@/components/common/Spinner";
import { ContactDetailPanel } from "@/components/contacts/ContactDetailPanel";
import { MergeContactModal } from "@/components/contacts/MergeContactModal";
import { WorkspaceNav } from "@/components/workspace/WorkspaceNav";
import { useContacts } from "@/hooks/use-contacts";
import { useDeleteContact } from "@/hooks/use-delete-contact";
import type { Contact } from "@/lib/messaging-api";

type Props = {
  workspaceId: string;
};

// ── channel meta ──────────────────────────────────────────────────────────────

const CHANNEL_LABEL: Record<string, string> = {
  TELEGRAM: "Telegram",
  WHATSAPP: "WhatsApp",
  INSTAGRAM: "Instagram",
  MESSENGER: "Messenger",
  WEBCHAT: "Web Chat",
  EMAIL: "Email",
  SMS: "SMS",
};

const CHANNEL_COLOR: Record<string, string> = {
  TELEGRAM: "bg-blue-bg text-blue-text",
  WHATSAPP: "bg-green-bg text-green-text",
  INSTAGRAM: "bg-purple-bg text-purple-text",
  MESSENGER: "bg-blue-bg text-blue-text",
  WEBCHAT: "bg-teal-bg text-teal-text",
  EMAIL: "bg-yellow-bg text-yellow-text",
  SMS: "bg-orange-bg text-orange-text",
};

// ── helpers ───────────────────────────────────────────────────────────────────

const LOCALE = "en-US";

function formatDate(isoString: string): string {
  const date = new Date(isoString);
  const now = new Date();
  const diffDays = Math.floor(
    (now.getTime() - date.getTime()) / (1000 * 60 * 60 * 24)
  );

  if (diffDays === 0) return "Today";
  if (diffDays === 1) return "Yesterday";
  if (diffDays < 7)
    return date.toLocaleDateString(LOCALE, { weekday: "short" });

  return date.toLocaleDateString(LOCALE, {
    month: "short",
    day: "numeric",
    year: "numeric",
  });
}

function contactInitial(contact: Contact): string {
  return (contact.displayName ?? "?").trim().charAt(0).toUpperCase();
}

// ── action dropdown ───────────────────────────────────────────────────────────

type ActionMenuProps = {
  contact: Contact;
  workspaceId: string;
  onDeleteRequest: (contact: Contact) => void;
  onMergeRequest: (contact: Contact) => void;
};

function ActionMenu({
  contact,
  workspaceId,
  onDeleteRequest,
  onMergeRequest,
}: ActionMenuProps) {
  const [open, setOpen] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);

  function handleBlur(e: React.FocusEvent) {
    if (!containerRef.current?.contains(e.relatedTarget as Node)) {
      setOpen(false);
    }
  }

  return (
    <div
      ref={containerRef}
      className="relative inline-block"
      onBlur={handleBlur}
    >
      <button
        type="button"
        onClick={(e) => {
          e.stopPropagation();
          setOpen((v) => !v);
        }}
        aria-label="Actions"
        aria-expanded={open}
        className="flex h-8 w-8 items-center justify-center rounded-lg text-neutral-400 transition-colors hover:bg-neutral-200 hover:text-neutral-700 focus:outline-none focus-visible:ring-2 focus-visible:ring-secondary"
      >
        <span
          className="material-symbols-rounded text-[18px] leading-none"
          aria-hidden="true"
        >
          more_vert
        </span>
      </button>

      {open && (
        <div
          className="absolute right-0 top-full z-40 mt-1 w-48 overflow-hidden rounded-xl border border-neutral-200 bg-white shadow-xl"
          onClick={(e) => e.stopPropagation()}
        >
          <div className="p-1">
            <Link
              href={`/inbox?workspaceId=${workspaceId}&contactId=${contact.id}`}
              onClick={() => setOpen(false)}
              className="flex w-full items-center gap-2.5 rounded-lg px-3 py-2 text-sm text-neutral-600 transition-colors hover:bg-neutral-100 hover:text-neutral-900"
            >
              <span
                className="material-symbols-rounded text-[16px] leading-none"
                aria-hidden="true"
              >
                inbox
              </span>
              View inbox
            </Link>

            <button
              type="button"
              onClick={() => {
                setOpen(false);
                onMergeRequest(contact);
              }}
              className="flex w-full items-center gap-2.5 rounded-lg px-3 py-2 text-sm text-neutral-600 transition-colors hover:bg-neutral-100 hover:text-neutral-900"
            >
              <span
                className="material-symbols-rounded text-[16px] leading-none"
                aria-hidden="true"
              >
                merge
              </span>
              Merge with…
            </button>

            <button
              type="button"
              onClick={() => {
                setOpen(false);
                onDeleteRequest(contact);
              }}
              className="flex w-full items-center gap-2.5 rounded-lg px-3 py-2 text-sm text-neutral-600 transition-colors hover:bg-red-bg hover:text-red-text"
            >
              <span
                className="material-symbols-rounded text-[16px] leading-none"
                aria-hidden="true"
              >
                delete
              </span>
              Delete contact
            </button>
          </div>
        </div>
      )}
    </div>
  );
}

// ── contact row ───────────────────────────────────────────────────────────────

type RowProps = {
  contact: Contact;
  workspaceId: string;
  selected: boolean;
  onSelect: (id: string) => void;
  onDeleteRequest: (contact: Contact) => void;
  onMergeRequest: (contact: Contact) => void;
};

function ContactRow({
  contact,
  workspaceId,
  selected,
  onSelect,
  onDeleteRequest,
  onMergeRequest,
}: RowProps) {
  const initial = contactInitial(contact);

  return (
    <tr
      onClick={() => onSelect(contact.id)}
      className={`group cursor-pointer border-b border-neutral-200 transition-colors hover:bg-neutral-50 ${
        selected ? "bg-blue-bg hover:bg-blue-bg" : ""
      }`}
    >
      <td className="py-3 pl-4 pr-3 sm:pl-6">
        <div className="flex items-center gap-3">
          <div
            className="flex h-8 w-8 flex-shrink-0 items-center justify-center rounded-full bg-secondary text-xs font-semibold text-white"
            aria-hidden="true"
          >
            {initial}
          </div>

          <span
            className={`text-sm font-medium ${selected ? "text-secondary" : "text-neutral-800"}`}
          >
            {contact.displayName ?? (
              <span className="italic text-neutral-400">Unknown</span>
            )}
          </span>
        </div>
      </td>

      {/* Channels */}
      <td className="px-3 py-3">
        <div className="flex flex-wrap gap-1">
          {contact.identities.length === 0 ? (
            <span className="text-xs text-neutral-400">—</span>
          ) : (
            contact.identities.map((identity) => (
              <span
                key={identity.id}
                className={`inline-block rounded-full px-2 py-0.5 text-xs font-medium ${CHANNEL_COLOR[identity.provider] ?? "bg-neutral-100 text-neutral-600"}`}
              >
                {CHANNEL_LABEL[identity.provider] ?? identity.provider}
              </span>
            ))
          )}
        </div>
      </td>

      {/* Created — hidden on small screens */}
      <td
        suppressHydrationWarning
        className="hidden whitespace-nowrap px-3 py-3 text-sm text-neutral-500 sm:table-cell"
      >
        {formatDate(contact.createdAt)}
      </td>

      <td className="py-3 pl-3 pr-4 text-right sm:pr-6">
        <ActionMenu
          contact={contact}
          workspaceId={workspaceId}
          onDeleteRequest={onDeleteRequest}
          onMergeRequest={onMergeRequest}
        />
      </td>
    </tr>
  );
}

// ── shell ─────────────────────────────────────────────────────────────────────

export function ContactsShell({ workspaceId }: Props) {
  const router = useRouter();
  const searchParams = useSearchParams();
  const selectedContactId = searchParams.get("contactId");

  const {
    data,
    isPending,
    isError,
    fetchNextPage,
    hasNextPage,
    isFetchingNextPage,
  } = useContacts(workspaceId);

  const { mutate: deleteContact, isPending: isDeleting } =
    useDeleteContact(workspaceId);

  const [pendingDelete, setPendingDelete] = useState<Contact | null>(null);
  const [pendingMerge, setPendingMerge] = useState<Contact | null>(null);

  const contacts = data?.pages.flatMap((p) => p.items) ?? [];
  const totalLoaded = contacts.length;

  function selectContact(id: string) {
    const params = new URLSearchParams(searchParams.toString());

    if (params.get("contactId") === id) {
      params.delete("contactId");
    } else {
      params.set("contactId", id);
    }

    router.replace(`?${params.toString()}`, { scroll: false });
  }

  function closeDetail() {
    const params = new URLSearchParams(searchParams.toString());
    params.delete("contactId");
    router.replace(`?${params.toString()}`, { scroll: false });
  }

  const showDetail = !!selectedContactId;

  return (
    <div className="flex h-full overflow-hidden">
      <WorkspaceNav workspaceId={workspaceId} />

      {/* Contact list — hidden on mobile when detail panel is open */}
      <div
        className={`flex-col overflow-hidden ${
          showDetail ? "hidden md:flex md:w-80 lg:w-96" : "flex flex-1"
        }`}
      >
        {/* Top bar */}
        <div className="flex flex-shrink-0 items-center justify-between border-b border-neutral-300 bg-neutral-100 px-4 py-3 sm:px-6">
          <div className="flex items-center gap-2">
            <span
              className="material-symbols-rounded text-[18px] leading-none text-neutral-500"
              aria-hidden="true"
            >
              contacts
            </span>
            <h1 className="text-sm font-semibold text-neutral-800">Contacts</h1>
          </div>

          {!isPending && !isError && (
            <span className="text-xs text-neutral-400">
              {totalLoaded}
              {hasNextPage ? "+" : ""}{" "}
              {totalLoaded === 1 ? "contact" : "contacts"}
            </span>
          )}
        </div>

        {/* Body */}
        <div className="flex-1 overflow-y-auto pb-14 md:pb-0">
          {isPending && (
            <div className="flex justify-center py-16">
              <Spinner />
            </div>
          )}

          {isError && (
            <div className="flex justify-center py-16">
              <EmptyState
                icon="error"
                title="Could not load contacts"
                description="Check your connection and try again."
              />
            </div>
          )}

          {!isPending && !isError && contacts.length === 0 && (
            <div className="flex justify-center py-16">
              <EmptyState
                icon="contacts"
                title="No contacts yet"
                description="Contacts are created automatically when someone messages through a connected channel."
              />
            </div>
          )}

          {!isPending && !isError && contacts.length > 0 && (
            <table className="min-w-full">
              <thead className="border-b border-neutral-200 bg-neutral-50">
                <tr>
                  <th
                    scope="col"
                    className="py-2.5 pl-4 pr-3 text-left text-xs font-semibold uppercase tracking-wide text-neutral-400 sm:pl-6"
                  >
                    Name
                  </th>
                  <th
                    scope="col"
                    className="px-3 py-2.5 text-left text-xs font-semibold uppercase tracking-wide text-neutral-400"
                  >
                    Channels
                  </th>

                  <th
                    scope="col"
                    className="hidden px-3 py-2.5 text-left text-xs font-semibold uppercase tracking-wide text-neutral-400 sm:table-cell"
                  >
                    Added
                  </th>
                  <th scope="col" className="relative py-2.5 pl-3 pr-4 sm:pr-6">
                    <span className="sr-only">Actions</span>
                  </th>
                </tr>
              </thead>

              <tbody className="bg-white">
                {contacts.map((contact) => (
                  <ContactRow
                    key={contact.id}
                    contact={contact}
                    workspaceId={workspaceId}
                    selected={contact.id === selectedContactId}
                    onSelect={selectContact}
                    onDeleteRequest={setPendingDelete}
                    onMergeRequest={setPendingMerge}
                  />
                ))}
              </tbody>
            </table>
          )}

          {/* Load more */}
          {hasNextPage && (
            <div className="flex justify-center py-4">
              <button
                type="button"
                onClick={() => void fetchNextPage()}
                disabled={isFetchingNextPage}
                className="flex items-center gap-1.5 rounded-lg border border-neutral-300 bg-white px-4 py-2 text-sm font-medium text-neutral-600 transition-colors hover:bg-neutral-100 disabled:opacity-60"
              >
                {isFetchingNextPage ? (
                  <Spinner size="sm" />
                ) : (
                  <>
                    <span
                      className="material-symbols-rounded text-[14px] leading-none"
                      aria-hidden="true"
                    >
                      expand_more
                    </span>
                    Load more
                  </>
                )}
              </button>
            </div>
          )}
        </div>
      </div>

      {/* Detail panel */}
      {showDetail && (
        <div className="flex flex-1 flex-col overflow-hidden">
          <ContactDetailPanel
            contactId={selectedContactId}
            workspaceId={workspaceId}
            onClose={closeDetail}
          />
        </div>
      )}

      {/* Merge modal */}
      {pendingMerge && (
        <MergeContactModal
          source={pendingMerge}
          workspaceId={workspaceId}
          onSuccess={() => {
            setPendingMerge(null);
            if (pendingMerge.id === selectedContactId) closeDetail();
          }}
          onCancel={() => setPendingMerge(null)}
        />
      )}

      {/* Delete confirmation modal */}
      {pendingDelete && (
        <ConfirmModal
          title="Delete contact?"
          description={`This will permanently delete ${
            pendingDelete.displayName ?? "this contact"
          } and cannot be undone.`}
          confirmLabel="Delete"
          destructive
          isPending={isDeleting}
          onConfirm={() => {
            deleteContact(pendingDelete.id, {
              onSuccess: () => {
                setPendingDelete(null);
                if (pendingDelete.id === selectedContactId) {
                  closeDetail();
                }
              },
            });
          }}
          onCancel={() => setPendingDelete(null)}
        />
      )}
    </div>
  );
}
