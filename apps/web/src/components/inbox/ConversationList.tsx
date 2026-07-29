"use client";

import Link from "next/link";
import { useEffect, useRef } from "react";

import { EmptyState } from "@/components/common/EmptyState";
import { Spinner } from "@/components/common/Spinner";
import { useAuthentication } from "@/hooks/use-authentication";
import { useChannelAccounts } from "@/hooks/use-channel-accounts";
import { useConversations } from "@/hooks/use-conversations";
import { useCurrentMember } from "@/hooks/use-current-member";

import { ConversationItem } from "./ConversationItem";

type Props = {
  workspaceId: string;
  selectedConversationId: string | undefined;
  onSelect: (conversationId: string) => void;
  /** When set, only conversations for this contact are shown. */
  contactId?: string;
};

export function ConversationList({
  workspaceId,
  selectedConversationId,
  onSelect,
  contactId,
}: Props) {
  const scrollRef = useRef<HTMLDivElement>(null);

  const { user } = useAuthentication();
  const currentMember = useCurrentMember(workspaceId, user?.userId);

  const {
    data,
    isLoading,
    isError,
    fetchNextPage,
    hasNextPage,
    isFetchingNextPage,
  } = useConversations(workspaceId, contactId);

  const conversations = data?.pages.flatMap((page) => page.items) ?? [];
  const isEmpty = !isLoading && !isError && conversations.length === 0;

  const { data: channelAccounts } = useChannelAccounts(workspaceId);
  const hasActiveChannel =
    channelAccounts?.some((c) => c.status === "ACTIVE") ?? false;

  // Same permission check as SettingsShell's Channels tab — no point linking
  // there if the user can't see or use it once they arrive.
  const canConnectChannel =
    currentMember?.role === "OWNER" ||
    currentMember?.permissions.includes("CHANNELS_WRITE") === true;

  function handleScroll() {
    const el = scrollRef.current;

    if (!el || !hasNextPage || isFetchingNextPage) {
      return;
    }

    // When the user scrolls within 100 px of the bottom, load more conversations.
    if (el.scrollHeight - el.scrollTop - el.clientHeight < 100) {
      void fetchNextPage();
    }
  }

  // Reset scroll to top when workspace changes.
  useEffect(() => {
    if (scrollRef.current) {
      scrollRef.current.scrollTop = 0;
    }
  }, [workspaceId]);

  return (
    <div className="flex flex-1 flex-col overflow-hidden">
      <div className="flex items-center justify-between border-b border-neutral-300 px-4 py-3">
        <h2 className="text-sm font-semibold text-primary">Conversations</h2>
      </div>

      <div
        ref={scrollRef}
        onScroll={handleScroll}
        className="flex-1 divide-y divide-neutral-300 overflow-y-auto pb-14 md:pb-0"
      >
        {isLoading && (
          <div className="flex justify-center py-8">
            <Spinner />
          </div>
        )}

        {isError && (
          <EmptyState
            icon="error"
            title="Could not load conversations"
            description="Check your connection and try again."
          />
        )}

        {isEmpty && hasActiveChannel && (
          <div className="flex flex-col items-center gap-4 px-4 py-10 text-center">
            <EmptyState
              icon="forum"
              title="No conversations yet"
              description="Messages will show up here once a contact reaches out on a connected channel."
            />
          </div>
        )}

        {isEmpty && !hasActiveChannel && (
          <div className="flex flex-col items-center gap-4 px-4 py-10 text-center">
            <EmptyState
              icon="forum"
              title="No conversations yet"
              description="Connect a channel to start receiving messages."
            />

            {canConnectChannel ? (
              <Link
                href={`/settings?workspaceId=${workspaceId}`}
                className="flex items-center gap-1.5 rounded-lg border border-neutral-300 bg-neutral-100 px-3 py-2 text-xs font-semibold text-neutral-700 transition-colors hover:border-neutral-400 hover:bg-neutral-100"
              >
                <span
                  className="material-symbols-rounded text-[13px]"
                  aria-hidden="true"
                >
                  settings
                </span>
                Connect a channel
              </Link>
            ) : (
              <button
                type="button"
                disabled
                className="flex cursor-not-allowed items-center gap-1.5 rounded-lg border border-neutral-300 bg-neutral-100 px-3 py-2 text-xs font-semibold text-neutral-400"
              >
                <span
                  className="material-symbols-rounded text-[13px]"
                  aria-hidden="true"
                >
                  settings
                </span>
                Connect a channel
              </button>
            )}
          </div>
        )}

        {conversations.map((conversation) => (
          <ConversationItem
            key={conversation.id}
            conversation={conversation}
            isSelected={conversation.id === selectedConversationId}
            onClick={() => onSelect(conversation.id)}
          />
        ))}

        {/* Load-more spinner at the bottom of the list */}
        {isFetchingNextPage && (
          <div className="flex justify-center py-4">
            <Spinner size="sm" />
          </div>
        )}
      </div>
    </div>
  );
}
