import { type Node, type NodeProps } from "@xyflow/react";

import { WorkflowNode } from "./WorkflowNode";

export type EndConversationNodeData = {
  label?: string;
  message?: string;
};

type EndConversationNodeType = Node<EndConversationNodeData, "endConversation">;

export function EndConversationNode({
  id,
  data,
  selected,
  isConnectable,
}: NodeProps<EndConversationNodeType>) {
  return (
    <WorkflowNode
      id={id}
      icon="mark_chat_read"
      label={data.label ?? "End Conversation"}
      headerColor="bg-red-bg text-red-text"
      hasSource={false}
      isConnectable={isConnectable}
      selected={selected}
    >
      <span className="line-clamp-2 text-neutral-400">
        {data.message ?? "No closing message"}
      </span>
    </WorkflowNode>
  );
}
