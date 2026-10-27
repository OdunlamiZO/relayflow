import { type Node, type NodeProps } from "@xyflow/react";

import { WorkflowNode } from "./WorkflowNode";

export type SetContactTagNodeData = {
  label?: string;
  tagKey?: string;
  value?: string;
};

type SetContactTagNodeType = Node<SetContactTagNodeData, "setContactTag">;

export function SetContactTagNode({
  id,
  data,
  selected,
  isConnectable,
}: NodeProps<SetContactTagNodeType>) {
  return (
    <WorkflowNode
      id={id}
      icon="sell"
      label={data.label ?? "Set Contact Tag"}
      headerColor="bg-purple-bg text-purple-text"
      isConnectable={isConnectable}
      selected={selected}
    >
      {data.tagKey ? (
        <span className="font-mono text-neutral-500">
          {data.tagKey} = {data.value || "cleared"}
        </span>
      ) : (
        <span className="text-neutral-400">Not configured</span>
      )}
    </WorkflowNode>
  );
}
