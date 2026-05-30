import { type Node, type NodeProps } from "@xyflow/react";

import { WorkflowNode } from "./WorkflowNode";

export type TriggerNodeData = {
  label?: string;
  event?: string;
};

type TriggerNodeType = Node<TriggerNodeData, "trigger">;

const EVENT_LABELS: Record<string, string> = {
  conversation_opened: "Conversation Opened",
};

export function TriggerNode({
  id,
  data,
  selected,
  isConnectable,
}: NodeProps<TriggerNodeType>) {
  const eventLabel = data.event
    ? (EVENT_LABELS[data.event] ?? data.event)
    : "No event configured";

  return (
    <WorkflowNode
      id={id}
      icon="bolt"
      label={data.label ?? "Trigger"}
      headerColor="bg-blue-bg text-blue-text"
      hasTarget={false}
      isConnectable={isConnectable}
      selected={selected}
    >
      <span className={data.event ? "text-neutral-400" : "text-red-border"}>
        {eventLabel}
      </span>
    </WorkflowNode>
  );
}
