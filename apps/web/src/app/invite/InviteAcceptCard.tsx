"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";

import { LoadingButton } from "@/components/common/LoadingButton";
import { InviteStatusMessage } from "@/components/invite/InviteStatusMessage";
import { useAcceptInvite } from "@/hooks/use-accept-invite";
import { errorMessage } from "@/lib/error-message";
import {
  type InvitePreview,
  type WorkspacePermission,
} from "@/lib/messaging-api";

const PERMISSION_LABEL: Record<WorkspacePermission, string> = {
  INBOX: "Inbox — view conversations and send messages",
  CONTACTS_DELETE: "Contacts — delete and merge contacts",
  CONTACT_FIELDS_WRITE: "Contacts — define and edit contact fields",
  WORKFLOWS_WRITE: "Workflows — create, edit and toggle workflows",
  WORKFLOWS_DELETE: "Workflows — delete workflows",
  CHANNELS_WRITE: "Channels — view and connect channels",
  CHANNELS_DELETE: "Channels — disconnect channels",
  API_KEYS_WRITE: "Integrations — manage API keys",
  WEBHOOKS_WRITE: "Integrations — configure webhooks",
  AI_AGENT_WRITE: "AI Agent — configure the AI agent",
};

const PERMISSION_ICON: Record<WorkspacePermission, string> = {
  INBOX: "inbox",
  CONTACTS_DELETE: "contacts",
  CONTACT_FIELDS_WRITE: "contacts",
  WORKFLOWS_WRITE: "account_tree",
  WORKFLOWS_DELETE: "account_tree",
  CHANNELS_WRITE: "hub",
  CHANNELS_DELETE: "hub",
  API_KEYS_WRITE: "api",
  WEBHOOKS_WRITE: "api",
  AI_AGENT_WRITE: "smart_toy",
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

  if (preview.status !== "PENDING") {
    return <InviteStatusMessage status={preview.status} />;
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
          <LoadingButton
            type="button"
            onClick={handleAccept}
            isLoading={isPending}
            className="flex w-full items-center justify-center rounded-lg bg-secondary px-5 py-3 text-sm font-semibold text-neutral-100 transition-colors hover:bg-secondary-dark disabled:cursor-not-allowed disabled:opacity-60"
          >
            Join {preview.workspaceName}
          </LoadingButton>
        ) : preview.accountExists ? (
          <Link
            href={`/login?returnUrl=${encodeURIComponent(returnUrl)}`}
            className="flex w-full items-center justify-center rounded-lg bg-secondary px-5 py-3 text-sm font-semibold text-neutral-100 transition-colors hover:bg-secondary-dark"
          >
            Log in to accept
          </Link>
        ) : (
          <Link
            href={`/signup?token=${encodeURIComponent(token)}`}
            className="flex w-full items-center justify-center rounded-lg bg-secondary px-5 py-3 text-sm font-semibold text-neutral-100 transition-colors hover:bg-secondary-dark"
          >
            Create account to accept
          </Link>
        )}
      </div>
    </div>
  );
}
