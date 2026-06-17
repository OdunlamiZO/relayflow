"use client";

import { useEffect, useRef, useState } from "react";

import { EmptyState } from "@/components/common/EmptyState";
import { Select } from "@/components/common/Select";
import { Spinner } from "@/components/common/Spinner";
import { useConversationAiDraft } from "@/hooks/use-conversation-ai-draft";
import { useConversations } from "@/hooks/use-conversations";
import { useMessages } from "@/hooks/use-messages";
import { useUpdateConversation } from "@/hooks/use-update-conversation";
import { useUpdateConversationAssignee } from "@/hooks/use-update-conversation-assignee";
import { useWorkspaceMembers } from "@/hooks/use-workspace-members";

import { AiDraftBanner } from "./AiDraftBanner";
import { MessageBubble } from "./MessageBubble";
import { MessageComposer } from "./MessageComposer";

type Props = {
  workspaceId: string;
  conversationId: string;
  onBack?: () => void;
};

const STATUS_CHIP: Record<string, string> = {
  OPEN: "bg-green-bg text-green-text",
  PENDING: "bg-yellow-bg text-yellow-text",
  CLOSED: "bg-neutral-300 text-neutral-600",
};

export function MessageThread({ workspaceId, conversationId, onBack }: Props) {
  const scrollRef = useRef<HTMLDivElement>(null);
  const [composerPrefill, setComposerPrefill] = useState("");

  const { data: aiDraft } = useConversationAiDraft(workspaceId, conversationId);

  // Track the previous scrollHeight so we can restore position after prepending older messages.
  const prevScrollHeightRef = useRef<number>(0);

  const { data: conversationsData } = useConversations(workspaceId);
  const { mutate: updateConversation, isPending: isUpdating } =
    useUpdateConversation(workspaceId);
  const { mutate: updateAssignee } = useUpdateConversationAssignee(workspaceId);
  const { data: members = [] } = useWorkspaceMembers(workspaceId);
  const {
    data,
    isLoading,
    isError,
    fetchNextPage,
    hasNextPage,
    isFetchingNextPage,
  } = useMessages(workspaceId, conversationId);

  // Flatten pages: older pages (loaded later) come first — reverse page order, keep item order.
  const messages =
    data?.pages
      .slice()
      .reverse()
      .flatMap((page) => page.items) ?? [];

  const conversations =
    conversationsData?.pages.flatMap((page) => page.items) ?? [];
  const conversation = conversations.find((c) => c.id === conversationId);

  const totalMessages = messages.length;

  // On initial load and when new messages arrive at the bottom: scroll to bottom.
  useEffect(() => {
    const el = scrollRef.current;

    if (!el || isFetchingNextPage) {
      return;
    }

    el.scrollTop = el.scrollHeight;
  }, [totalMessages, conversationId, isFetchingNextPage]);

  // After loading older messages (fetchNextPage), restore scroll so the user stays at the same
  // visual position rather than jumping to the top.
  useEffect(() => {
    if (!isFetchingNextPage) {
      const el = scrollRef.current;

      if (!el) {
        return;
      }

      const newScrollHeight = el.scrollHeight;
      el.scrollTop = newScrollHeight - prevScrollHeightRef.current;
    }
  }, [isFetchingNextPage]);

  function handleScroll() {
    const el = scrollRef.current;

    if (!el || !hasNextPage || isFetchingNextPage) {
      return;
    }

    // When the user scrolls within 120 px of the top, load older messages.
    if (el.scrollTop < 120) {
      prevScrollHeightRef.current = el.scrollHeight;
      void fetchNextPage();
    }
  }

  const chipClass =
    STATUS_CHIP[conversation?.status ?? ""] ??
    "bg-neutral-300 text-neutral-600";

  return (
    <div className="flex h-full flex-col overflow-hidden">
      <div className="flex flex-shrink-0 flex-wrap items-center gap-2 border-b border-neutral-300 bg-neutral-100 px-4 py-3 sm:flex-nowrap sm:gap-3 sm:px-6 sm:py-4">
        {/* Back button — mobile only, only when a handler is wired */}
        {onBack && (
          <button
            type="button"
            onClick={onBack}
            className="flex-shrink-0 rounded-lg p-1 text-neutral-500 transition-colors hover:bg-neutral-200 hover:text-neutral-700 md:hidden"
            aria-label="Back to conversations"
          >
            <span
              className="material-symbols-rounded text-[20px]"
              aria-hidden="true"
            >
              arrow_back
            </span>
          </button>
        )}

        <div className="flex h-9 w-9 flex-shrink-0 items-center justify-center rounded-full bg-blue-bg text-blue-text">
          <span
            className="material-symbols-rounded"
            style={{ fontSize: 18 }}
            aria-hidden="true"
          >
            person
          </span>
        </div>

        <div className="min-w-0 flex-1">
          <p className="m-0 truncate text-sm font-semibold text-primary">
            {conversation?.contactDisplayName ?? "Unknown contact"}
          </p>
          <p className="m-0 truncate text-xs text-neutral-500">
            {conversation
              ? `${conversation.channelAccountName} · ${conversation.channelProvider}`
              : conversationId}
          </p>
        </div>

        {conversation && (
          <div className="flex w-full flex-shrink-0 items-center justify-end gap-2 sm:w-auto">
            <span
              className={`flex-shrink-0 rounded-full px-2.5 py-0.5 text-xs font-medium ${chipClass}`}
            >
              {conversation.status}
            </span>

            {conversation.lockedByWorkflow ? (
              <div
                className="flex w-28 min-w-0 flex-shrink items-center gap-1.5 rounded-lg border border-purple-border bg-purple-bg px-3 py-2 text-xs font-medium text-purple-text sm:w-36"
                title="A workflow is currently running and owns this conversation"
              >
                <span
                  className="material-symbols-rounded text-[14px] leading-none"
                  aria-hidden="true"
                >
                  bolt
                </span>
                <span className="truncate">Workflow</span>
              </div>
            ) : (
              <div className="w-28 min-w-0 flex-shrink sm:w-36">
                <Select
                  value={conversation.assigneeId ?? ""}
                  onChange={(value) =>
                    updateAssignee({
                      conversationId,
                      assigneeId: value || null,
                    })
                  }
                  options={[
                    { value: "", label: "Unassigned" },
                    ...members.map((member) => ({
                      value: member.userId,
                      label:
                        member.displayName ??
                        member.email?.split("@")[0] ??
                        member.userId,
                    })),
                  ]}
                />
              </div>
            )}

            <button
              type="button"
              disabled={isUpdating}
              onClick={() =>
                updateConversation({
                  conversationId: conversationId,
                  status: conversation.status === "CLOSED" ? "OPEN" : "CLOSED",
                })
              }
              className={`flex flex-shrink-0 items-center gap-1.5 rounded-lg px-2.5 py-1.5 text-xs font-medium transition-colors disabled:cursor-not-allowed disabled:opacity-50 ${
                conversation.status === "CLOSED"
                  ? "bg-neutral-200 text-neutral-700 hover:bg-neutral-300"
                  : "bg-green-bg text-green-text hover:bg-green-bg/70"
              }`}
            >
              {isUpdating ? (
                <Spinner size="sm" />
              ) : (
                <span
                  className="material-symbols-rounded text-[14px] leading-none"
                  aria-hidden="true"
                >
                  {conversation.status === "CLOSED" ? "refresh" : "done_all"}
                </span>
              )}
              {conversation.status === "CLOSED" ? "Reopen" : "Close"}
            </button>
          </div>
        )}
      </div>

      <div
        ref={scrollRef}
        onScroll={handleScroll}
        className="flex-1 space-y-3 overflow-y-auto px-4 pt-4 pb-14 sm:px-6 md:pb-4"
      >
        {/* Load-older spinner */}
        {isFetchingNextPage && (
          <div className="flex justify-center pb-2">
            <Spinner size="sm" />
          </div>
        )}

        {isLoading && (
          <div className="flex justify-center py-8">
            <Spinner />
          </div>
        )}

        {isError && (
          <EmptyState
            icon="error"
            title="Could not load messages"
            description="Check your connection and try again."
          />
        )}

        {!isLoading && !isError && messages.length === 0 && (
          <EmptyState
            icon="chat_bubble_outline"
            title="No messages yet"
            description="Messages will appear here once the conversation starts."
          />
        )}

        {messages.map((message) => (
          <MessageBubble key={message.id} message={message} />
        ))}
      </div>

      {aiDraft && (
        <AiDraftBanner
          workspaceId={workspaceId}
          conversationId={conversationId}
          draft={aiDraft}
          onEdit={(text) => setComposerPrefill(text)}
        />
      )}

      <MessageComposer
        workspaceId={workspaceId}
        conversationId={conversationId}
        lockedByWorkflow={conversation?.lockedByWorkflow ?? false}
        prefillText={composerPrefill}
        onPrefillConsumed={() => setComposerPrefill("")}
      />
    </div>
  );
}
