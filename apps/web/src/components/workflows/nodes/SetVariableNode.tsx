import { type Node, type NodeProps } from "@xyflow/react";

import { WorkflowNode } from "./WorkflowNode";

export type SetVariableNodeData = {
  label?: string;
  variableName?: string;
  value?: string;
};

type SetVariableNodeType = Node<SetVariableNodeData, "setVariable">;

export function SetVariableNode({
  id,
  data,
  selected,
  isConnectable,
}: NodeProps<SetVariableNodeType>) {
  return (
    <WorkflowNode
      id={id}
      icon="variable_insert"
      label={data.label ?? "Set Variable"}
      headerColor="bg-purple-bg text-purple-text"
      isConnectable={isConnectable}
      selected={selected}
    >
      {data.variableName ? (
        <span className="font-mono text-neutral-500">
          {data.variableName} = {data.value ?? "…"}
        </span>
      ) : (
        <span className="text-neutral-400">Not configured</span>
      )}
    </WorkflowNode>
  );
}
