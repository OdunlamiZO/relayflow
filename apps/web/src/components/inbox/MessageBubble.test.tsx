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

    expect(
      screen.getByRole("button", { name: "Play voice message" })
    ).toBeInTheDocument();
    expect(screen.getByText("0:00 / 0:03")).toBeInTheDocument();
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

    expect(screen.getByText("Recording not kept")).toBeInTheDocument();
    expect(
      screen.getByRole("link", { name: "Choose a transcription provider" })
    ).toHaveAttribute("href", "/settings?workspaceId=workspace-1#providers");
    expect(container.querySelector("audio")).toBeNull();
  });

  it("shows a voice message that's still being transcribed", () => {
    render(
      <MessageBubble
        message={voiceMessage(
          {
            mimeType: "audio/ogg",
            durationSeconds: 3,
            stored: true,
            transcriptionStatus: "PENDING",
          },
          null
        )}
      />
    );

    expect(screen.getByText("Transcribing…")).toBeInTheDocument();
  });
});
