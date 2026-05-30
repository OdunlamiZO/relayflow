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
};

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
      {/* Option labels */}
      {options.map((opt, i) => {
        const pct = ((i + 1) / (totalHandles + 1)) * 100;

        return (
          <span
            key={opt.id}
            className="absolute -translate-x-1/2 truncate text-[10px] text-blue-text"
            style={{ left: `${pct}%`, bottom: 8 }}
          >
            {opt.text || `Option ${i + 1}`}
          </span>
        );
      })}

      {/* "Other" label */}
      <span
        className="absolute -translate-x-1/2 truncate text-[10px] text-neutral-400"
        style={{
          left: `${(totalHandles / (totalHandles + 1)) * 100}%`,
          bottom: 8,
        }}
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
          className="!border-2 !border-white !bg-blue-border"
        />
      ))}

      {/* Default "Other" handle — neutral, always present */}
      <Handle
        type="source"
        position={Position.Bottom}
        id="default"
        isConnectable={isConnectable}
        style={{ left: `${(totalHandles / (totalHandles + 1)) * 100}%` }}
        className="!border-2 !border-white !bg-neutral-500"
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
    </WorkflowNode>
  );
}
