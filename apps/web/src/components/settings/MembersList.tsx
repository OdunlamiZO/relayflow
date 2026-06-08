"use client";

import { useState } from "react";

import { ConfirmModal } from "@/components/common/ConfirmModal";
import { Spinner } from "@/components/common/Spinner";
import { useCreateInvite } from "@/hooks/use-create-invite";
import { useRemoveMember } from "@/hooks/use-remove-member";
import { useRevokeInvite } from "@/hooks/use-revoke-invite";
import { useUpdateMember } from "@/hooks/use-update-member";
import { useWorkspaceInvites } from "@/hooks/use-workspace-invites";
import { useWorkspaceMembers } from "@/hooks/use-workspace-members";
import { errorMessage } from "@/lib/error-message";
import {
  type WorkspaceInvite,
  type WorkspaceMember,
  type WorkspacePermission,
} from "@/lib/messaging-api";

type Props = {
  workspaceId: string;
  currentUserId?: string;
};

type PermissionRow = {
  key: WorkspacePermission;
  label: string;
};

type PermissionGroup = {
  section: string;
  icon: string;
  rows: PermissionRow[];
};

const PERMISSION_GROUPS: PermissionGroup[] = [
  {
    section: "Inbox",
    icon: "inbox",
    rows: [{ key: "INBOX", label: "View conversations and send messages" }],
  },
  {
    section: "Contacts",
    icon: "contacts",
    rows: [{ key: "CONTACTS_DELETE", label: "Delete and merge contacts" }],
  },
  {
    section: "Workflows",
    icon: "account_tree",
    rows: [
      { key: "WORKFLOWS_WRITE", label: "Create, edit and toggle workflows" },
      { key: "WORKFLOWS_DELETE", label: "Delete workflows" },
    ],
  },
  {
    section: "Channels",
    icon: "hub",
    rows: [
      { key: "CHANNELS_WRITE", label: "View and connect channels" },
      { key: "CHANNELS_DELETE", label: "Disconnect channels" },
    ],
  },
  {
    section: "Integrations",
    icon: "api",
    rows: [
      { key: "API_KEYS_WRITE", label: "Manage API keys" },
      { key: "WEBHOOKS_WRITE", label: "Configure webhooks" },
    ],
  },
];

// Permissions that are prerequisites for another permission.
// key = dependent, value = the prerequisite it requires.
const PREREQUISITES: Partial<Record<WorkspacePermission, WorkspacePermission>> =
  {
    WORKFLOWS_DELETE: "WORKFLOWS_WRITE",
    CHANNELS_DELETE: "CHANNELS_WRITE",
  };

/**
 * Returns a new Set after toggling `perm`, cascading dependency rules:
 * - Toggling ON a dependent → also adds its prerequisite.
 * - Toggling OFF a prerequisite → also removes its dependent.
 */
function applyPermissionToggle(
  prev: Set<WorkspacePermission>,
  perm: WorkspacePermission
): Set<WorkspacePermission> {
  const next = new Set(prev);

  if (next.has(perm)) {
    next.delete(perm);
    // If this is a prerequisite, remove any dependent that relied on it.
    for (const [dep, prereq] of Object.entries(PREREQUISITES) as [
      WorkspacePermission,
      WorkspacePermission,
    ][]) {
      if (prereq === perm) next.delete(dep);
    }
  } else {
    next.add(perm);
    // If this is a dependent, also add its prerequisite.
    const prereq = PREREQUISITES[perm];
    if (prereq) next.add(prereq);
  }

  return next;
}

// Flat map used for the collapsed pill display
const ALL_PERMISSION_LABELS: Record<WorkspacePermission, string> = {
  INBOX: "Inbox",
  CONTACTS_DELETE: "Delete contacts",
  WORKFLOWS_WRITE: "Edit workflows",
  WORKFLOWS_DELETE: "Delete workflows",
  CHANNELS_WRITE: "Connect channels",
  CHANNELS_DELETE: "Disconnect channels",
  API_KEYS_WRITE: "Manage API keys",
  WEBHOOKS_WRITE: "Configure webhooks",
};

function getInitials(displayName: string | null, email: string | null): string {
  const source = displayName ?? email ?? "?";
  const parts = source.split(/[\s@.]+/).filter(Boolean);

  if (parts.length >= 2) {
    return (parts[0][0] + parts[1][0]).toUpperCase();
  }

  return source.slice(0, 2).toUpperCase();
}

export function MembersList({ workspaceId, currentUserId }: Props) {
  const {
    data: members,
    isLoading: membersLoading,
    isError: membersError,
  } = useWorkspaceMembers(workspaceId);

  const currentUserIsOwner =
    members?.find((m) => m.userId === currentUserId)?.role === "OWNER";

  const { data: invites, isLoading: invitesLoading } = useWorkspaceInvites(
    workspaceId,
    currentUserIsOwner
  );

  const [email, setEmail] = useState("");
  const [invitePerms, setInvitePerms] = useState<Set<WorkspacePermission>>(
    new Set(["INBOX"])
  );
  const [inviteError, setInviteError] = useState<string | null>(null);

  const { mutate: createInvite, isPending: isInviting } =
    useCreateInvite(workspaceId);

  function toggleInvitePerm(perm: WorkspacePermission) {
    setInvitePerms((prev) => applyPermissionToggle(prev, perm));
  }

  function handleInvite(e: React.FormEvent) {
    e.preventDefault();

    const trimmed = email.trim();

    if (!trimmed) {
      return;
    }

    setInviteError(null);

    createInvite(
      { email: trimmed, permissions: Array.from(invitePerms) },
      {
        onSuccess: () => {
          setEmail("");
          setInvitePerms(new Set(["INBOX"]));
        },
        onError: (err) => setInviteError(errorMessage(err)),
      }
    );
  }

  return (
    <div className="mx-auto max-w-2xl px-6 py-8 sm:px-8">
      {/* Page header */}
      <div className="mb-8">
        <h1 className="text-xl font-bold text-primary">Members</h1>
        <p className="mt-1.5 text-sm text-neutral-500">
          {currentUserIsOwner
            ? "Invite team members and control what each person can do in this workspace."
            : "View everyone who has access to this workspace."}
        </p>
      </div>

      {/* Invite form — owner only */}
      {currentUserIsOwner && (
        <section className="mb-8">
          <h2 className="mb-3 text-xs font-semibold uppercase tracking-wide text-neutral-400">
            Invite member
          </h2>

          <form
            onSubmit={handleInvite}
            className="rounded-xl border border-neutral-200 bg-neutral-100 p-5"
          >
            <div className="mb-4">
              <label
                htmlFor="invite-email"
                className="mb-1.5 block text-xs font-medium text-neutral-600"
              >
                Email address
              </label>
              <input
                id="invite-email"
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="colleague@example.com"
                required
                disabled={isInviting}
                className="w-full rounded-lg border border-neutral-200 bg-neutral-50 px-3 py-2 text-sm text-neutral-800 placeholder-neutral-400 focus:border-secondary focus:outline-none focus:ring-2 focus:ring-secondary/20 disabled:opacity-50"
              />
            </div>

            <div className="mb-5">
              <p className="mb-2 text-xs font-medium text-neutral-600">
                Permissions
              </p>

              <PermissionEditor
                checked={invitePerms}
                onChange={toggleInvitePerm}
              />
            </div>

            {inviteError && (
              <p className="mb-3 text-sm text-red-text">{inviteError}</p>
            )}

            <button
              type="submit"
              disabled={isInviting || !email.trim()}
              className="flex w-full items-center justify-center gap-2 rounded-lg bg-secondary px-4 py-2.5 text-sm font-semibold text-neutral-100 transition-colors hover:bg-secondary-dark disabled:cursor-not-allowed disabled:opacity-50 sm:w-auto"
            >
              {isInviting && (
                <Spinner
                  size="sm"
                  className="border-neutral-100/40 border-t-neutral-100"
                />
              )}
              {isInviting ? "Sending…" : "Send invite"}
            </button>
          </form>
        </section>
      )}

      {/* Pending invites — owner only */}
      {currentUserIsOwner &&
        (invitesLoading || (invites && invites.length > 0)) && (
          <section className="mb-8">
            <h2 className="mb-3 text-xs font-semibold uppercase tracking-wide text-neutral-400">
              Pending invites
            </h2>

            {invitesLoading ? (
              <div className="flex justify-center py-4">
                <Spinner size="sm" />
              </div>
            ) : (
              <ul className="flex flex-col gap-2">
                {invites!.map((invite) => (
                  <InviteRow
                    key={invite.id}
                    invite={invite}
                    workspaceId={workspaceId}
                  />
                ))}
              </ul>
            )}
          </section>
        )}

      {/* Current members */}
      <section>
        <h2 className="mb-3 text-xs font-semibold uppercase tracking-wide text-neutral-400">
          Current members
        </h2>

        {membersLoading && (
          <div className="flex justify-center py-8">
            <Spinner />
          </div>
        )}

        {membersError && (
          <p className="text-sm text-red-text">Failed to load members.</p>
        )}

        {!membersLoading && !membersError && members && (
          <ul className="flex flex-col gap-2">
            {members.map((member) => (
              <MemberRow
                key={member.id}
                member={member}
                workspaceId={workspaceId}
                isCurrentUser={member.userId === currentUserId}
                ownerCount={members.filter((m) => m.role === "OWNER").length}
                viewerIsOwner={currentUserIsOwner}
              />
            ))}
          </ul>
        )}
      </section>
    </div>
  );
}

// ---------------------------------------------------------------------------
// Invite row
// ---------------------------------------------------------------------------

function InviteRow({
  invite,
  workspaceId,
}: {
  invite: WorkspaceInvite;
  workspaceId: string;
}) {
  const [confirmRevoke, setConfirmRevoke] = useState(false);
  const { mutate: revoke, isPending: isRevoking } =
    useRevokeInvite(workspaceId);

  return (
    <>
      <li className="flex items-center gap-3 rounded-xl border border-dashed border-neutral-300 bg-neutral-50 px-4 py-3">
        {/* Envelope icon */}
        <div className="flex h-8 w-8 flex-shrink-0 items-center justify-center rounded-full bg-neutral-200 text-neutral-500">
          <span
            className="material-symbols-rounded text-[16px] leading-none"
            aria-hidden="true"
          >
            mail
          </span>
        </div>

        <div className="min-w-0 flex-1">
          <p className="truncate text-sm font-medium text-neutral-800">
            {invite.email}
          </p>
          <p className="text-xs text-neutral-500">
            Invited by {invite.inviterName} · pending acceptance
          </p>
        </div>

        <button
          type="button"
          onClick={() => setConfirmRevoke(true)}
          disabled={isRevoking}
          className="flex-shrink-0 rounded-lg p-1.5 text-neutral-400 transition-colors hover:bg-red-bg hover:text-red-text disabled:opacity-40"
          aria-label="Revoke invite"
        >
          <span
            className="material-symbols-rounded text-[17px] leading-none"
            aria-hidden="true"
          >
            close
          </span>
        </button>
      </li>

      {confirmRevoke && (
        <ConfirmModal
          title="Revoke invite?"
          description={`${invite.email} will no longer be able to use this invite link.`}
          confirmLabel="Revoke"
          destructive
          isPending={isRevoking}
          onConfirm={() =>
            revoke(invite.id, { onSettled: () => setConfirmRevoke(false) })
          }
          onCancel={() => setConfirmRevoke(false)}
        />
      )}
    </>
  );
}

// ---------------------------------------------------------------------------
// Member row
// ---------------------------------------------------------------------------

function MemberRow({
  member,
  workspaceId,
  isCurrentUser,
  ownerCount,
  viewerIsOwner,
}: {
  member: WorkspaceMember;
  workspaceId: string;
  isCurrentUser: boolean;
  ownerCount: number;
  viewerIsOwner: boolean;
}) {
  const [expanded, setExpanded] = useState(false);
  const [pendingPerms, setPendingPerms] = useState<Set<WorkspacePermission>>(
    new Set(member.permissions)
  );
  const [confirmRemove, setConfirmRemove] = useState(false);

  const { mutate: updateMember, isPending: isSaving } =
    useUpdateMember(workspaceId);
  const { mutate: removeMember, isPending: isRemoving } =
    useRemoveMember(workspaceId);

  const isLastOwner = member.role === "OWNER" && ownerCount <= 1;
  const isOwner = member.role === "OWNER";

  function togglePerm(perm: WorkspacePermission) {
    setPendingPerms((prev) => applyPermissionToggle(prev, perm));
  }

  function handleSave() {
    updateMember(
      {
        memberId: member.id,
        request: { permissions: Array.from(pendingPerms) },
      },
      { onSuccess: () => setExpanded(false) }
    );
  }

  function handleCancel() {
    setPendingPerms(new Set(member.permissions));
    setExpanded(false);
  }

  return (
    <>
      <li className="overflow-hidden rounded-xl border border-neutral-200 bg-neutral-100">
        {/* Member row */}
        <div className="flex items-center gap-3 p-4">
          {/* Avatar */}
          <div className="flex h-9 w-9 flex-shrink-0 items-center justify-center rounded-full bg-secondary/10 text-sm font-semibold text-secondary">
            {member.avatarUrl ? (
              // eslint-disable-next-line @next/next/no-img-element
              <img
                src={member.avatarUrl}
                alt=""
                className="h-9 w-9 rounded-full object-cover"
              />
            ) : (
              getInitials(member.displayName, member.email)
            )}
          </div>

          {/* Name / email */}
          <div className="min-w-0 flex-1">
            <p className="truncate text-sm font-semibold text-primary">
              {member.displayName ?? member.email ?? "Unknown"}
              {isCurrentUser && (
                <span className="ml-1.5 text-xs font-normal text-neutral-400">
                  (you)
                </span>
              )}
            </p>
            {member.displayName && (
              <p className="truncate text-xs text-neutral-500">
                {member.email}
              </p>
            )}
          </div>

          {/* Role badge */}
          <span
            className={`flex-shrink-0 rounded-full px-2.5 py-0.5 text-xs font-medium ${
              isOwner
                ? "bg-purple-bg text-purple-text"
                : "bg-neutral-100 text-neutral-500"
            }`}
          >
            {isOwner ? "Owner" : "Member"}
          </span>

          {/* Edit permissions — viewer must be owner; displayed member must not be owner */}
          {viewerIsOwner && !isOwner && (
            <button
              type="button"
              onClick={() => {
                setPendingPerms(new Set(member.permissions));
                setExpanded((v) => !v);
              }}
              className="flex-shrink-0 rounded-lg p-1.5 text-neutral-400 transition-colors hover:bg-neutral-100 hover:text-neutral-700"
              aria-label="Edit permissions"
            >
              <span
                className="material-symbols-rounded text-[18px] leading-none"
                aria-hidden="true"
              >
                {expanded ? "expand_less" : "tune"}
              </span>
            </button>
          )}

          {/* Remove — viewer must be owner; cannot remove the last owner */}
          {viewerIsOwner && !isLastOwner && (
            <button
              type="button"
              onClick={() => setConfirmRemove(true)}
              className="flex-shrink-0 rounded-lg p-1.5 text-neutral-400 transition-colors hover:bg-red-bg hover:text-red-text"
              aria-label="Remove member"
            >
              <span
                className="material-symbols-rounded text-[18px] leading-none"
                aria-hidden="true"
              >
                person_remove
              </span>
            </button>
          )}
        </div>

        {/* Permission pills (collapsed view) — only shown to owners */}
        {viewerIsOwner &&
          !isOwner &&
          !expanded &&
          member.permissions.length > 0 && (
            <div className="flex flex-wrap gap-1.5 border-t border-neutral-100 px-4 py-2.5">
              {member.permissions.map((p) => (
                <span
                  key={p}
                  className="rounded-full bg-neutral-100 px-2 py-0.5 text-[11px] font-medium text-neutral-600"
                >
                  {ALL_PERMISSION_LABELS[p] ?? p}
                </span>
              ))}
            </div>
          )}

        {/* Expanded permission editor — only owners can reach this state */}
        {viewerIsOwner && !isOwner && expanded && (
          <div className="border-t border-neutral-100 px-4 pb-4 pt-3">
            <p className="mb-2.5 text-xs font-medium text-neutral-600">
              Edit permissions
            </p>

            <div className="mb-4">
              <PermissionEditor checked={pendingPerms} onChange={togglePerm} />
            </div>

            <div className="flex items-center gap-2">
              <button
                type="button"
                onClick={handleSave}
                disabled={isSaving}
                className="flex items-center gap-1.5 rounded-lg bg-secondary px-3 py-1.5 text-sm font-semibold text-neutral-100 transition-colors hover:bg-secondary-dark disabled:opacity-50"
              >
                {isSaving && (
                  <Spinner
                    size="sm"
                    className="border-neutral-100/40 border-t-neutral-100"
                  />
                )}
                {isSaving ? "Saving…" : "Save"}
              </button>

              <button
                type="button"
                onClick={handleCancel}
                disabled={isSaving}
                className="rounded-lg px-3 py-1.5 text-sm font-medium text-neutral-500 transition-colors hover:bg-neutral-100 hover:text-neutral-700 disabled:opacity-50"
              >
                Cancel
              </button>
            </div>
          </div>
        )}
      </li>

      {confirmRemove && (
        <ConfirmModal
          title="Remove member?"
          description={`${member.displayName ?? member.email ?? "This member"} will lose access to the workspace immediately.`}
          confirmLabel="Remove"
          destructive
          isPending={isRemoving}
          onConfirm={() =>
            removeMember(member.id, {
              onSettled: () => setConfirmRemove(false),
            })
          }
          onCancel={() => setConfirmRemove(false)}
        />
      )}
    </>
  );
}

// ---------------------------------------------------------------------------
// PermissionEditor — grouped, sectioned permission selector
// ---------------------------------------------------------------------------

function PermissionEditor({
  checked,
  onChange,
}: {
  checked: Set<WorkspacePermission>;
  onChange: (perm: WorkspacePermission) => void;
}) {
  return (
    <div className="flex flex-col gap-2">
      {PERMISSION_GROUPS.map((group) => (
        <div
          key={group.section}
          className="overflow-hidden rounded-xl border border-neutral-200 bg-neutral-100"
        >
          {/* Section header */}
          <div className="flex items-center gap-2 border-b border-neutral-100 bg-neutral-50 px-3.5 py-2">
            <span
              className="material-symbols-rounded text-[15px] text-neutral-400"
              aria-hidden="true"
            >
              {group.icon}
            </span>
            <span className="text-[11px] font-semibold uppercase tracking-wide text-neutral-400">
              {group.section}
            </span>
          </div>

          {/* Permission rows */}
          {group.rows.map((row, idx) => {
            const isChecked = checked.has(row.key);
            const isLast = idx === group.rows.length - 1;

            // A prerequisite is locked on when its dependent is currently checked.
            // e.g. CHANNELS_WRITE is locked when CHANNELS_DELETE is on.
            const isLocked = Object.entries(PREREQUISITES).some(
              ([dep, prereq]) =>
                prereq === row.key && checked.has(dep as WorkspacePermission)
            );

            return (
              <button
                key={row.key}
                type="button"
                role="checkbox"
                aria-checked={isChecked}
                disabled={isLocked}
                onClick={() => !isLocked && onChange(row.key)}
                className={`flex w-full items-center gap-3 px-3.5 py-3 text-left transition-colors ${
                  !isLast ? "border-b border-neutral-100" : ""
                } ${
                  isLocked
                    ? "cursor-not-allowed bg-secondary/5"
                    : isChecked
                      ? "bg-secondary/5 hover:bg-secondary/10"
                      : "hover:bg-neutral-200"
                }`}
              >
                {/* Label */}
                <span
                  className={`flex-1 text-sm transition-colors ${
                    isChecked || isLocked
                      ? "font-medium text-secondary"
                      : "text-neutral-700"
                  }`}
                >
                  {row.label}
                </span>

                {/* Locked indicator — lock icon replaces toggle pill */}
                {isLocked ? (
                  <span
                    className="material-symbols-rounded flex-shrink-0 text-[16px] text-secondary/60"
                    aria-label="Required by another permission"
                  >
                    lock
                  </span>
                ) : (
                  /* Toggle pill */
                  <div
                    className={`relative flex h-[22px] w-[38px] flex-shrink-0 items-center rounded-full transition-colors ${
                      isChecked ? "bg-secondary" : "bg-neutral-200"
                    }`}
                    aria-hidden="true"
                  >
                    <div
                      className={`absolute h-[16px] w-[16px] rounded-full bg-neutral-100 shadow-sm transition-transform ${
                        isChecked ? "translate-x-[18px]" : "translate-x-[3px]"
                      }`}
                    />
                  </div>
                )}
              </button>
            );
          })}
        </div>
      ))}
    </div>
  );
}
