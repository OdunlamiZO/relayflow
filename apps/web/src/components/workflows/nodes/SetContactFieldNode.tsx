import { type Node, type NodeProps } from "@xyflow/react";

import { WorkflowNode } from "./WorkflowNode";

export type SetContactFieldNodeData = {
  label?: string;
  fieldKey?: string;
  value?: string;
};

type SetContactFieldNodeType = Node<SetContactFieldNodeData, "setContactField">;

export function SetContactFieldNode({
  id,
  data,
  selected,
  isConnectable,
}: NodeProps<SetContactFieldNodeType>) {
  return (
    <WorkflowNode
      id={id}
      icon="contact_page"
      label={data.label ?? "Set Contact Field"}
      headerColor="bg-purple-bg text-purple-text"
      isConnectable={isConnectable}
      selected={selected}
    >
      {data.fieldKey ? (
        <span className="font-mono text-neutral-500">
          {data.fieldKey} = {data.value ?? "…"}
        </span>
      ) : (
        <span className="text-neutral-400">Not configured</span>
      )}
    </WorkflowNode>
  );
}
