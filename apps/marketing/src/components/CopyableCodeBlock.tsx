"use client";

import { useState } from "react";

type Props = {
  filename: string;
  content: string;
};

export function CopyableCodeBlock({ filename, content }: Props) {
  const [copied, setCopied] = useState(false);

  function handleCopy() {
    navigator.clipboard.writeText(content);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  }

  return (
    <div className="overflow-hidden rounded-xl border border-neutral-300 bg-neutral-100">
      <div className="flex items-center justify-between border-b border-neutral-300 bg-neutral-200 px-4 py-2.5">
        <span className="font-mono text-xs font-semibold text-neutral-700">
          {filename}
        </span>

        <button
          type="button"
          onClick={handleCopy}
          className="rounded-md border border-neutral-300 bg-neutral-100 px-2.5 py-1 text-xs font-semibold text-neutral-700 transition-colors hover:border-neutral-400"
        >
          {copied ? "Copied" : "Copy"}
        </button>
      </div>

      <pre className="max-h-96 overflow-auto p-4 text-xs leading-5 text-neutral-700">
        <code>{content}</code>
      </pre>
    </div>
  );
}
