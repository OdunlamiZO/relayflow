// Docs: docs/codebase-components.md#messagebubble
import { type Message, messagingApi } from "@/lib/messaging-api";

export function useMessageAudioUrl(message: Message) {
  return message.audio?.stored ? messagingApi.messageAudioUrl(message) : null;
}
