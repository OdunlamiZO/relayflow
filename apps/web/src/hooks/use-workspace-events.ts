"use client";

import { useEffect, useRef, useState } from "react";

import { useQueryClient } from "@tanstack/react-query";

const API_BASE_URL =
  process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";

type MessageCreatedEvent = {
  workspaceId: string;
  conversationId: string;
};

type WorkspaceUpdatedEvent = {
  workspaceId: string;
};

type AiDraftCreatedEvent = {
  conversationId: string;
};

/**
 * Opens a Server-Sent Events connection for the given workspace and invalidates
 * the relevant React Query caches whenever a {@code message.created} or
 * {@code workspace.updated} event arrives.
 *
 * Falls back gracefully if the browser does not support {@code EventSource} or
 * if the connection fails — polling in the query hooks acts as the backstop.
 */
export function useWorkspaceEvents(workspaceId: string | undefined) {
  const queryClient = useQueryClient();
  const esRef = useRef<EventSource | null>(null);
  const [telegramLinked, setTelegramLinked] = useState(false);

  useEffect(() => {
    if (!workspaceId || typeof window === "undefined" || !window.EventSource) {
      return;
    }

    const url = `${API_BASE_URL}/sse/workspace/${workspaceId}`;
    const es = new EventSource(url, { withCredentials: true });

    esRef.current = es;

    es.addEventListener("message.created", (event) => {
      try {
        const data = JSON.parse(event.data) as MessageCreatedEvent;

        void queryClient.invalidateQueries({
          queryKey: ["messages", data.workspaceId, data.conversationId],
        });

        void queryClient.invalidateQueries({
          queryKey: ["conversations", data.workspaceId],
        });
      } catch {
        // Malformed event data — ignore; polling will catch up.
      }
    });

    // Fired when workspace state changes without a new message (e.g. Telegram /start link).
    es.addEventListener("workspace.updated", (event) => {
      try {
        const data = JSON.parse(event.data) as WorkspaceUpdatedEvent;

        setTelegramLinked(true);

        void queryClient.invalidateQueries({
          queryKey: ["conversations", data.workspaceId],
        });
      } catch {
        // Malformed event data — ignore.
      }
    });

    es.addEventListener("ai.draft.created", (event) => {
      try {
        const data = JSON.parse(event.data) as AiDraftCreatedEvent;

        void queryClient.invalidateQueries({
          queryKey: ["ai-draft", workspaceId, data.conversationId],
        });
      } catch {
        // Malformed event data — ignore.
      }
    });

    es.onerror = () => {
      // Browser will automatically reconnect on transient failures.
      // Log nothing to avoid noise from expected reconnects.
    };

    return () => {
      es.close();
      esRef.current = null;
    };
  }, [workspaceId, queryClient]);

  return { telegramLinked };
}
