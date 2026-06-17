"use client";

import { useEffect, useRef, useState } from "react";

import { Spinner } from "@/components/common/Spinner";
import { useSendMessage } from "@/hooks/use-send-message";

type Props = {
  workspaceId: string;
  conversationId: string;
  lockedByWorkflow?: boolean;
  prefillText?: string;
  onPrefillConsumed?: () => void;
};

export function MessageComposer({
  workspaceId,
  conversationId,
  lockedByWorkflow = false,
  prefillText,
  onPrefillConsumed,
}: Props) {
  const [text, setText] = useState("");
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  useEffect(() => {
    if (!prefillText) return;
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setText(prefillText);
    onPrefillConsumed?.();
    textareaRef.current?.focus();
  }, [prefillText, onPrefillConsumed]);

  const { mutate: sendMessage, isPending } = useSendMessage(
    workspaceId,
    conversationId
  );

  function resize() {
    const el = textareaRef.current;
    if (!el) return;
    el.style.height = "auto";
    el.style.height = `${el.scrollHeight}px`;
  }

  function submit() {
    const trimmed = text.trim();
    if (!trimmed || isPending) return;

    sendMessage(
      { direction: "OUTBOUND", senderType: "AGENT", text: trimmed },
      {
        onSuccess: () => {
          setText("");
          if (textareaRef.current) {
            textareaRef.current.style.height = "auto";
          }
        },
      }
    );
  }

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    submit();
  }

  function handleKeyDown(e: React.KeyboardEvent<HTMLTextAreaElement>) {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault();
      submit();
    }
  }

  function handleChange(e: React.ChangeEvent<HTMLTextAreaElement>) {
    setText(e.target.value);
    resize();
  }

  if (lockedByWorkflow) {
    return (
      <div className="flex items-center gap-2.5 border-t border-neutral-300 bg-neutral-100 px-4 py-3.5">
        <span
          className="material-symbols-rounded flex-shrink-0 text-[16px] leading-none text-purple-text"
          aria-hidden="true"
        >
          smart_toy
        </span>
        <p className="text-xs text-neutral-500">
          A workflow is handling this conversation. You can reply once it
          finishes.
        </p>
      </div>
    );
  }

  return (
    <form
      onSubmit={handleSubmit}
      className="border-t border-neutral-300 bg-neutral-100 p-4"
    >
      <div className="flex items-end gap-3 rounded-xl border border-neutral-300 bg-neutral-200 px-4 py-3 transition-colors focus-within:border-secondary focus-within:ring-1 focus-within:ring-secondary">
        <textarea
          ref={textareaRef}
          value={text}
          onChange={handleChange}
          onKeyDown={handleKeyDown}
          placeholder="Type a message…"
          rows={1}
          disabled={isPending}
          className="flex-1 resize-none bg-transparent text-sm text-neutral-800 placeholder-neutral-500 focus:outline-none disabled:opacity-50"
          style={{ minHeight: "24px", maxHeight: "160px", overflowY: "auto" }}
          aria-label="Message text"
        />

        <button
          type="submit"
          disabled={!text.trim() || isPending}
          className="flex h-8 w-8 flex-shrink-0 items-center justify-center rounded-lg bg-secondary text-neutral-100 transition-colors hover:bg-secondary-dark disabled:cursor-not-allowed disabled:opacity-40"
          aria-label="Send message"
        >
          {isPending ? (
            <Spinner
              size="sm"
              className="border-neutral-100/40 border-t-neutral-100"
            />
          ) : (
            <span
              className="material-symbols-rounded"
              style={{ fontSize: 16 }}
              aria-hidden="true"
            >
              send
            </span>
          )}
        </button>
      </div>
    </form>
  );
}
