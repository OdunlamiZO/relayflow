"use client";

import Link from "next/link";
import { useEffect, useRef } from "react";

import { EmptyState } from "@/components/common/EmptyState";
import { Spinner } from "@/components/common/Spinner";
import { useAuthentication } from "@/hooks/use-authentication";
import { useConversations } from "@/hooks/use-conversations";

import { ConversationItem } from "./ConversationItem";

const sharedBotUsername = process.env.NEXT_PUBLIC_SHARED_BOT_USERNAME ?? "";

type Props = {
  workspaceId: string;
  selectedConversationId: string | undefined;
  onSelect: (conversationId: string) => void;
  telegramLinked?: boolean;
};

export function ConversationList({
  workspaceId,
  selectedConversationId,
  onSelect,
  telegramLinked = false,
}: Props) {
  const scrollRef = useRef<HTMLDivElement>(null);

  const {
    data,
    isLoading,
    isError,
    fetchNextPage,
    hasNextPage,
    isFetchingNextPage,
  } = useConversations(workspaceId);

  const { isAnonymous } = useAuthentication();

  const conversations = data?.pages.flatMap((page) => page.items) ?? [];
  const isEmpty = !isLoading && !isError && conversations.length === 0;

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
        className="flex-1 divide-y divide-neutral-300 overflow-y-auto"
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

        {isEmpty && (
          <div className="flex flex-col items-center gap-4 px-4 py-10 text-center">
            {isAnonymous && sharedBotUsername ? (
              <>
                {telegramLinked ? (
                  <EmptyState
                    icon="check_circle"
                    title="Telegram connected!"
                    description="Send a message to the bot and it will appear here."
                  />
                ) : (
                  <>
                    <EmptyState
                      icon="send"
                      title="Connect Telegram to get started"
                      description={`Send a message to @${sharedBotUsername} on Telegram and it will appear here.`}
                    />

                    <a
                      href={`https://t.me/${sharedBotUsername}?start=${workspaceId}`}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="flex items-center gap-1.5 rounded-lg border border-neutral-300 bg-white px-3 py-2 text-xs font-semibold text-neutral-700 transition-colors hover:border-neutral-400 hover:bg-neutral-100"
                    >
                      <span
                        className="material-symbols-rounded text-[13px] text-[#229ED9]"
                        aria-hidden="true"
                      >
                        send
                      </span>
                      Open @{sharedBotUsername}
                    </a>
                  </>
                )}
              </>
            ) : (
              <>
                <EmptyState
                  icon="forum"
                  title="No conversations yet"
                  description="Connect a channel to start receiving messages."
                />

                <Link
                  href={`/settings?workspaceId=${workspaceId}`}
                  className="flex items-center gap-1.5 rounded-lg border border-neutral-300 bg-white px-3 py-2 text-xs font-semibold text-neutral-700 transition-colors hover:border-neutral-400 hover:bg-neutral-100"
                >
                  <span
                    className="material-symbols-rounded text-[13px]"
                    aria-hidden="true"
                  >
                    settings
                  </span>
                  Connect a channel
                </Link>
              </>
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
