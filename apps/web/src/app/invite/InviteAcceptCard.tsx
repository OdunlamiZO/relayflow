"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";

import { Spinner } from "@/components/common/Spinner";
import { useAcceptInvite } from "@/hooks/use-accept-invite";
import { errorMessage } from "@/lib/error-message";
import {
  type InvitePreview,
  type WorkspacePermission,
} from "@/lib/messaging-api";

const PERMISSION_LABEL: Record<WorkspacePermission, string> = {
  INBOX: "Inbox — view conversations and send messages",
  CONTACTS_DELETE: "Contacts — delete and merge contacts",
  WORKFLOWS_WRITE: "Workflows — create, edit and toggle workflows",
  WORKFLOWS_DELETE: "Workflows — delete workflows",
  CHANNELS_WRITE: "Channels — view and connect channels",
  CHANNELS_DELETE: "Channels — disconnect channels",
  API_KEYS_WRITE: "Integrations — manage API keys",
  WEBHOOKS_WRITE: "Integrations — configure webhooks",
};

const PERMISSION_ICON: Record<WorkspacePermission, string> = {
  INBOX: "inbox",
  CONTACTS_DELETE: "contacts",
  WORKFLOWS_WRITE: "account_tree",
  WORKFLOWS_DELETE: "account_tree",
  CHANNELS_WRITE: "hub",
  CHANNELS_DELETE: "hub",
  API_KEYS_WRITE: "api",
  WEBHOOKS_WRITE: "api",
};

type Props = {
  token: string;
  preview: InvitePreview;
  isAuthenticated: boolean;
};

export function InviteAcceptCard({ token, preview, isAuthenticated }: Props) {
  const router = useRouter();
  const { mutate: accept, isPending, error } = useAcceptInvite();

  const returnUrl = `/invite?token=${encodeURIComponent(token)}`;

  function handleAccept() {
    accept(token, {
      onSuccess: ({ workspaceId }) => {
        router.push(`/inbox?workspaceId=${workspaceId}`);
      },
    });
  }

  // Already accepted — just redirect.
  if (preview.status === "ACCEPTED") {
    return (
      <div className="rounded-2xl border border-neutral-300 bg-neutral-100 p-8 text-center shadow-sm">
        <span
          className="material-symbols-rounded mb-3 text-[40px] text-green-text"
          aria-hidden="true"
        >
          check_circle
        </span>
        <h1 className="text-lg font-semibold text-primary">Already accepted</h1>
        <p className="mt-2 text-sm text-neutral-500">
          This invite has already been accepted.
        </p>
        <Link
          href="/inbox"
          className="mt-5 inline-flex items-center gap-2 rounded-lg bg-secondary px-5 py-2.5 text-sm font-semibold text-neutral-100 transition-colors hover:bg-secondary-dark"
        >
          Go to inbox
        </Link>
      </div>
    );
  }

  if (preview.status === "REVOKED" || preview.status === "EXPIRED") {
    return (
      <div className="rounded-2xl border border-neutral-300 bg-neutral-100 p-8 text-center shadow-sm">
        <span
          className="material-symbols-rounded mb-3 text-[40px] text-neutral-400"
          aria-hidden="true"
        >
          timer_off
        </span>
        <h1 className="text-lg font-semibold text-primary">
          {preview.status === "EXPIRED" ? "Invite expired" : "Invite revoked"}
        </h1>
        <p className="mt-2 text-sm text-neutral-500">
          {preview.status === "EXPIRED"
            ? "This invite link has expired. Ask the workspace owner to send a new one."
            : "This invite has been revoked by the workspace owner."}
        </p>
      </div>
    );
  }

  // PENDING — show the invite card.
  return (
    <div className="overflow-hidden rounded-2xl border border-neutral-300 bg-neutral-100 shadow-sm">
      {/* Header */}
      <div className="bg-neutral-100 px-7 py-6">
        <p className="mb-1 text-xs font-semibold uppercase tracking-wide text-neutral-400">
          Workspace invite
        </p>
        <h1 className="text-xl font-bold text-primary">
          {preview.workspaceName}
        </h1>
        <p className="mt-1.5 text-sm text-neutral-500">
          <span className="font-medium text-neutral-700">
            {preview.inviterName}
          </span>{" "}
          has invited you to collaborate in this workspace.
        </p>
      </div>

      {/* Permissions */}
      {preview.permissions.length > 0 && (
        <div className="border-t border-neutral-200 bg-neutral-50 px-7 py-5">
          <p className="mb-3 text-xs font-semibold uppercase tracking-wide text-neutral-400">
            Your permissions
          </p>
          <ul className="flex flex-col gap-2">
            {preview.permissions.map((p) => (
              <li key={p} className="flex items-center gap-2.5">
                <span
                  className="material-symbols-rounded text-[17px] text-secondary"
                  aria-hidden="true"
                >
                  {PERMISSION_ICON[p]}
                </span>
                <span className="text-sm text-neutral-700">
                  {PERMISSION_LABEL[p]}
                </span>
              </li>
            ))}
          </ul>
        </div>
      )}

      {/* Action */}
      <div className="border-t border-neutral-200 px-7 py-6">
        {error && (
          <p className="mb-4 rounded-lg bg-red-bg px-3 py-2 text-sm text-red-text">
            {errorMessage(error)}
          </p>
        )}

        {isAuthenticated ? (
          <button
            type="button"
            onClick={handleAccept}
            disabled={isPending}
            className="flex w-full items-center justify-center gap-2 rounded-lg bg-secondary px-5 py-3 text-sm font-semibold text-neutral-100 transition-colors hover:bg-secondary-dark disabled:cursor-not-allowed disabled:opacity-60"
          >
            {isPending && (
              <Spinner
                size="sm"
                className="border-neutral-100/40 border-t-neutral-100"
              />
            )}
            {isPending ? "Accepting…" : `Join ${preview.workspaceName}`}
          </button>
        ) : (
          <div className="flex flex-col gap-3">
            <p className="text-sm text-neutral-600">
              Log in or create an account to accept this invite.
            </p>

            <Link
              href={`/login?returnUrl=${encodeURIComponent(returnUrl)}`}
              className="flex w-full items-center justify-center rounded-lg bg-secondary px-5 py-3 text-sm font-semibold text-neutral-100 transition-colors hover:bg-secondary-dark"
            >
              Log in to accept
            </Link>

            <Link
              href={`/signup?returnUrl=${encodeURIComponent(returnUrl)}`}
              className="flex w-full items-center justify-center rounded-lg border border-neutral-300 bg-neutral-100 px-5 py-3 text-sm font-semibold text-neutral-700 transition-colors hover:bg-neutral-200"
            >
              Create account
            </Link>
          </div>
        )}
      </div>
    </div>
  );
}
