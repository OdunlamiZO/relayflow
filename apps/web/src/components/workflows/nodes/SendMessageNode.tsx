import { type Node, type NodeProps } from "@xyflow/react";

import { WorkflowNode } from "./WorkflowNode";

export type SendMessageNodeData = {
  label?: string;
  message?: string;
};

type SendMessageNodeType = Node<SendMessageNodeData, "sendMessage">;

export function SendMessageNode({
  id,
  data,
  selected,
  isConnectable,
}: NodeProps<SendMessageNodeType>) {
  return (
    <WorkflowNode
      id={id}
      icon="send"
      label={data.label ?? "Send Message"}
      headerColor="bg-green-bg text-green-text"
      isConnectable={isConnectable}
      selected={selected}
    >
      <span className="line-clamp-2 text-neutral-400">
        {data.message ?? "No message configured"}
      </span>
    </WorkflowNode>
  );
}
