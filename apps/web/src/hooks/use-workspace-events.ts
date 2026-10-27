"use client";

import { useEffect, useRef } from "react";

import { useQueryClient } from "@tanstack/react-query";

import { BROWSER_API_BASE_URL } from "@/lib/api-base-url";

type MessageCreatedEvent = {
  workspaceId: string;
  conversationId: string;
};

type AiDraftCreatedEvent = {
  conversationId: string;
};

export function useWorkspaceEvents(workspaceId: string | undefined) {
  const queryClient = useQueryClient();
  const eventSourceRef = useRef<EventSource | null>(null);

  useEffect(() => {
    if (!workspaceId || typeof window === "undefined" || !window.EventSource) {
      return;
    }

    const url = `${BROWSER_API_BASE_URL}/sse/workspace/${workspaceId}`;
    const eventSource = new EventSource(url);

    eventSourceRef.current = eventSource;

    // Catches up on anything sent while the stream was reconnecting.
    eventSource.onopen = () => {
      void queryClient.invalidateQueries({
        queryKey: ["messages", workspaceId],
      });
      void queryClient.invalidateQueries({
        queryKey: ["conversations", workspaceId],
      });
    };

    eventSource.addEventListener("message.created", (event) => {
      try {
        const data = JSON.parse(event.data) as MessageCreatedEvent;

        void queryClient.invalidateQueries({
          queryKey: ["messages", data.workspaceId, data.conversationId],
        });

        void queryClient.invalidateQueries({
          queryKey: ["conversations", data.workspaceId],
        });
      } catch {
        // Polling catches up after malformed event data.
      }
    });

    eventSource.addEventListener("ai.draft.created", (event) => {
      try {
        const data = JSON.parse(event.data) as AiDraftCreatedEvent;

        void queryClient.invalidateQueries({
          queryKey: ["ai-draft", workspaceId, data.conversationId],
        });
      } catch {
        // Malformed event data is ignored.
      }
    });

    eventSource.addEventListener("ai.escalated", () => {
      void queryClient.invalidateQueries({
        queryKey: ["conversations", workspaceId],
      });
    });

    eventSource.addEventListener("conversation.updated", () => {
      void queryClient.invalidateQueries({
        queryKey: ["conversations", workspaceId],
      });
    });

    eventSource.onerror = () => {
      // EventSource reconnects on its own after transient failures.
    };

    return () => {
      eventSource.close();
      eventSourceRef.current = null;
    };
  }, [workspaceId, queryClient]);
}
