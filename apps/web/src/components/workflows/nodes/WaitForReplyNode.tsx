import { Handle, type Node, type NodeProps, Position } from "@xyflow/react";

import { WorkflowNode } from "./WorkflowNode";

export type WaitForReplyOption = {
  id: string;
  text: string;
};

export type WaitForReplyNodeData = {
  label?: string;
  question?: string;
  responseType?: "generic" | "defined";
  responseVariable?: string;
  options?: WaitForReplyOption[];
  /** Minutes to wait for a reply before failing the run. Defaults to 1440 (24 hours). */
  timeoutMinutes?: number;
};

export const DEFAULT_TIMEOUT_MINUTES = 60 * 24;

/** Formats a duration in minutes as a human-readable string (e.g. "24h", "90m", "2d"). */
function formatTimeout(minutes: number): string {
  if (minutes % (60 * 24) === 0) {
    return `${minutes / (60 * 24)}d`;
  }

  if (minutes % 60 === 0) {
    return `${minutes / 60}h`;
  }

  return `${minutes}m`;
}

type WaitForReplyNodeType = Node<WaitForReplyNodeData, "waitForReply">;

export function WaitForReplyNode({
  id,
  data,
  selected,
  isConnectable,
}: NodeProps<WaitForReplyNodeType>) {
  const isGeneric = !data.responseType || data.responseType === "generic";
  const options = data.options ?? [];

  // For defined mode: N option handles + 1 "Other" default handle
  const totalHandles = options.length + 1;

  const definedFooter = !isGeneric ? (
    <div className="relative pb-5 pt-1">
      {/* Option numbers — full text is listed in the body above instead, so
          long or numerous options don't overlap here. */}
      {options.map((opt, i) => {
        const pct = ((i + 1) / (totalHandles + 1)) * 100;

        return (
          <span
            key={opt.id}
            className="absolute -translate-x-1/2 text-[10px] font-semibold leading-none text-blue-text"
            style={{ left: `${pct}%`, bottom: 8 }}
          >
            {i + 1}
          </span>
        );
      })}

      {/* "Other" label — fallback branch when no option matches the reply */}
      <span
        className="absolute -translate-x-1/2 whitespace-nowrap text-[10px] leading-none text-neutral-400"
        style={{
          left: `${(totalHandles / (totalHandles + 1)) * 100}%`,
          bottom: 8,
        }}
        title="No option matched"
      >
        Other
      </span>

      {/* Option handles — blue tint so they're distinct from the default */}
      {options.map((opt, i) => (
        <Handle
          key={opt.id}
          type="source"
          position={Position.Bottom}
          id={opt.id}
          isConnectable={isConnectable}
          style={{ left: `${((i + 1) / (totalHandles + 1)) * 100}%` }}
          className="!border-2 !border-neutral-100 !bg-blue-border"
        />
      ))}

      {/* Default "Other" handle — neutral, always present */}
      <Handle
        type="source"
        position={Position.Bottom}
        id="default"
        isConnectable={isConnectable}
        style={{ left: `${(totalHandles / (totalHandles + 1)) * 100}%` }}
        className="!border-2 !border-neutral-100 !bg-neutral-500"
      />
    </div>
  ) : undefined;

  return (
    <WorkflowNode
      id={id}
      icon="mark_unread_chat_alt"
      label={data.label ?? "Ask Question"}
      headerColor="bg-blue-bg text-blue-text"
      hasSource={isGeneric}
      isConnectable={isConnectable}
      footer={definedFooter}
      selected={selected}
    >
      <span className="line-clamp-2 text-neutral-400">
        {data.question ?? "No question configured"}
      </span>

      <p className="m-0 mt-1.5 text-[10px] text-neutral-400">
        Times out after{" "}
        {formatTimeout(data.timeoutMinutes ?? DEFAULT_TIMEOUT_MINUTES)}
      </p>

      {!isGeneric && options.length > 0 && (
        <ul className="mt-2 space-y-1">
          {options.map((opt, i) => (
            <li key={opt.id} className="flex gap-1.5 text-blue-text">
              <span className="flex-shrink-0 font-semibold">{i + 1}.</span>
              <span className="line-clamp-2 break-words">
                {opt.text || `Option ${i + 1}`}
              </span>
            </li>
          ))}

          <li className="flex gap-1.5 text-neutral-400">
            <span className="flex-shrink-0 font-semibold">•</span>
            <span>Other — no option matched</span>
          </li>
        </ul>
      )}
    </WorkflowNode>
  );
}
