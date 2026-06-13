"use client";

import { useState } from "react";

type Props = {
  text: string;
  className?: string;
};

export function CopyButton({ text, className }: Props) {
  const [copied, setCopied] = useState(false);

  async function handleCopy() {
    await navigator.clipboard.writeText(text);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  }

  return (
    <button
      type="button"
      onClick={handleCopy}
      aria-label="Copy to clipboard"
      className={
        className ??
        "flex-shrink-0 text-neutral-400 transition-colors hover:text-secondary"
      }
    >
      <span
        className="material-symbols-rounded text-[14px] leading-none"
        aria-hidden="true"
      >
        {copied ? "check" : "content_copy"}
      </span>
    </button>
  );
}
