"use client";

import { useRouter, useSearchParams } from "next/navigation";

import { EmptyState } from "@/components/common/EmptyState";
import { WorkspaceNav } from "@/components/workspace/WorkspaceNav";
import { useWorkspaceEvents } from "@/hooks/use-workspace-events";

import { ConversationList } from "./ConversationList";
import { MessageThread } from "./MessageThread";

type Props = {
  workspaceId: string;
};

export function InboxShell({ workspaceId }: Props) {
  const router = useRouter();
  const searchParameters = useSearchParams();
  const conversationId = searchParameters.get("conversationId") ?? undefined;
  const contactId = searchParameters.get("contactId") ?? undefined;

  useWorkspaceEvents(workspaceId);

  function selectConversation(id: string) {
    const queryParameters = new URLSearchParams(searchParameters.toString());
    queryParameters.set("conversationId", id);

    router.push(`?${queryParameters.toString()}`);
  }

  function handleBack() {
    const queryParameters = new URLSearchParams(searchParameters.toString());
    queryParameters.delete("conversationId");

    router.push(`?${queryParameters.toString()}`);
  }

  function clearContactFilter() {
    const queryParameters = new URLSearchParams(searchParameters.toString());
    queryParameters.delete("contactId");
    queryParameters.delete("conversationId");

    router.push(`?${queryParameters.toString()}`);
  }

  return (
    <div className="flex h-full flex-col overflow-hidden">
      <div className="flex flex-1 overflow-hidden">
        <WorkspaceNav workspaceId={workspaceId} />

        <aside
          className={`flex-shrink-0 flex-col border-r border-neutral-300 bg-neutral-100 md:flex md:w-72 lg:w-80 ${
            conversationId ? "hidden md:flex" : "flex w-full"
          }`}
        >
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
