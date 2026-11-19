// Docs: docs/codebase-components.md#messagebubbletesttsx
import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import type { Message, MessageAudio } from "@/lib/messaging-api";

import { MessageBubble } from "./MessageBubble";

function voiceMessage(audio: MessageAudio, text: string | null): Message {
  return {
    id: "message-1",
    workspaceId: "workspace-1",
    conversationId: "conversation-1",
    direction: "INBOUND",
    senderType: "CONTACT",
    text,
    providerMessageId: null,
    rawPayload: {},
    createdAt: "2026-11-18T10:00:00Z",
    audio,
  };
}

describe("MessageBubble", () => {
  it("plays a stored voice message and shows its transcript", () => {
    const { container } = render(
      <MessageBubble
        message={voiceMessage(
          {
            mimeType: "audio/ogg",
            durationSeconds: 3,
            stored: true,
            transcriptionStatus: "TRANSCRIBED",
          },
          "I want to borrow 50,000"
        )}
      />
    );

    expect(screen.getByText("🎤 Voice message")).toBeInTheDocument();
    expect(screen.getByText("I want to borrow 50,000")).toBeInTheDocument();
    expect(container.querySelector("audio")?.getAttribute("src")).toContain(
      "/conversations/conversation-1/messages/message-1/audio?workspaceId=workspace-1"
    );
  });

  it("explains a voice message that has no transcript or recording", () => {
    const { container } = render(
      <MessageBubble
        message={voiceMessage(
          {
            mimeType: "audio/ogg",
            durationSeconds: null,
            stored: false,
            transcriptionStatus: "NO_PROVIDER",
          },
          null
        )}
      />
    );

    expect(
      screen.getByText("Not transcribed: no transcription plugin is installed")
    ).toBeInTheDocument();
    expect(container.querySelector("audio")).toBeNull();
  });
});
