"use client";

import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";

import { EmptyState } from "@/components/common/EmptyState";
import { useAuthentication } from "@/hooks/use-authentication";
import { useWorkspaceEvents } from "@/hooks/use-workspace-events";
import { useWorkspace } from "@/hooks/use-workspaces";

import { ConversationList } from "./ConversationList";
import { MessageThread } from "./MessageThread";

type Props = {
  workspaceId: string;
};

export function InboxShell({ workspaceId }: Props) {
  const router = useRouter();
  const searchParams = useSearchParams();
  const workspace = useWorkspace(workspaceId);
  const { isAnonymous } = useAuthentication();

  const conversationId = searchParams.get("conversationId") ?? undefined;

  const { telegramLinked } = useWorkspaceEvents(workspaceId);

  function selectConversation(id: string) {
    const params = new URLSearchParams(searchParams.toString());
    params.set("conversationId", id);

    router.push(`?${params.toString()}`);
  }

  function handleBack() {
    const params = new URLSearchParams(searchParams.toString());
    params.delete("conversationId");

    router.push(`?${params.toString()}`);
  }

  return (
    <div className="flex h-full flex-col overflow-hidden">
      {isAnonymous && (
        <div className="flex flex-shrink-0 items-center justify-between border-b border-amber-200 bg-amber-50 px-4 py-2.5">
          <p className="text-xs text-amber-700">
            <span className="font-semibold">Guest mode</span> — your data is
            temporary and will be removed after 24 hours.
          </p>

          <Link
            href="/signup"
            className="ml-4 flex-shrink-0 rounded-md bg-amber-600 px-3 py-1 text-xs font-semibold text-white transition-colors hover:bg-amber-700"
          >
            Create account →
          </Link>
        </div>
      )}

      <div className="flex flex-1 overflow-hidden">
        {/* Sidebar — full-width on mobile when no conversation is open */}
        <aside
          className={`flex-shrink-0 flex-col border-r border-neutral-300 bg-neutral-100 md:flex md:w-72 lg:w-80 ${
            conversationId ? "hidden md:flex" : "flex w-full"
          }`}
        >
          <div className="flex items-center gap-2 border-b border-neutral-300 px-4 py-3">
            <span
              className="material-symbols-rounded leading-none text-[16px] text-secondary"
              aria-hidden="true"
            >
              workspaces
            </span>
            <span className="truncate text-sm font-semibold text-primary">
              {workspace?.name ?? "Inbox"}
            </span>

            <Link
              href={`/inbox/channels?workspaceId=${workspaceId}`}
              title="Channels"
              className="ml-auto flex flex-shrink-0 items-center rounded-md p-1 text-neutral-400 transition-colors hover:bg-neutral-200 hover:text-neutral-700"
            >
              <span
                className="material-symbols-rounded leading-none text-[16px]"
                aria-hidden="true"
              >
                settings
              </span>
            </Link>
          </div>

          <ConversationList
            workspaceId={workspaceId}
            selectedConversationId={conversationId}
            onSelect={selectConversation}
            telegramLinked={telegramLinked}
          />
        </aside>

        {/* Main thread — full-width on mobile when a conversation is open */}
        <main
          className={`flex-col overflow-hidden ${
            conversationId ? "flex flex-1" : "hidden md:flex md:flex-1"
          }`}
        >
          {conversationId ? (
            <MessageThread
              workspaceId={workspaceId}
              conversationId={conversationId}
              onBack={handleBack}
            />
          ) : (
            <div className="flex h-full items-center justify-center">
              <EmptyState
                icon="chat"
                title="Select a conversation"
                description="Choose a conversation from the list to start reading."
              />
            </div>
          )}
        </main>
      </div>
    </div>
  );
}
