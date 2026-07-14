"use client";

import { useRouter, useSearchParams } from "next/navigation";

import { EmptyState } from "@/components/common/EmptyState";
import { WorkspaceNav } from "@/components/workspace/WorkspaceNav";
import { WorkspaceSwitcher } from "@/components/workspace/WorkspaceSwitcher";
import { useWorkspaceEvents } from "@/hooks/use-workspace-events";

import { ConversationList } from "./ConversationList";
import { MessageThread } from "./MessageThread";

type Props = {
  workspaceId: string;
};

export function InboxShell({ workspaceId }: Props) {
  const router = useRouter();
  const searchParams = useSearchParams();
  const conversationId = searchParams.get("conversationId") ?? undefined;
  const contactId = searchParams.get("contactId") ?? undefined;

  useWorkspaceEvents(workspaceId);

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

  function clearContactFilter() {
    const params = new URLSearchParams(searchParams.toString());
    params.delete("contactId");
    params.delete("conversationId");

    router.push(`?${params.toString()}`);
  }

  return (
    <div className="flex h-full flex-col overflow-hidden">
      <div className="flex flex-1 overflow-hidden">
        <WorkspaceNav workspaceId={workspaceId} />

        {/* Sidebar — full-width on mobile when no conversation is open */}
        <aside
          className={`flex-shrink-0 flex-col border-r border-neutral-300 bg-neutral-100 md:flex md:w-72 lg:w-80 ${
            conversationId ? "hidden md:flex" : "flex w-full"
          }`}
        >
          <div className="border-b border-neutral-300 px-4 py-3">
            <WorkspaceSwitcher workspaceId={workspaceId} />
          </div>

          {/* Contact filter banner */}
          {contactId && (
            <div className="flex items-center justify-between border-b border-blue-border/20 bg-blue-bg px-3 py-2">
              <div className="flex items-center gap-1.5 text-xs font-medium text-blue-text">
                <span
                  className="material-symbols-rounded text-[14px] leading-none"
                  aria-hidden="true"
                >
                  filter_alt
                </span>
                Contact&apos;s conversations
              </div>

              <button
                type="button"
                onClick={clearContactFilter}
                title="Clear filter"
                className="rounded p-0.5 text-blue-text transition-colors hover:bg-blue-bg-hover"
              >
                <span
                  className="material-symbols-rounded text-[14px] leading-none"
                  aria-hidden="true"
                >
                  close
                </span>
              </button>
            </div>
          )}

          <ConversationList
            workspaceId={workspaceId}
            selectedConversationId={conversationId}
            onSelect={selectConversation}
            contactId={contactId}
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
