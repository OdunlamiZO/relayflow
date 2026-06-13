import { type Node, type NodeProps } from "@xyflow/react";

import { WorkflowNode } from "./WorkflowNode";

export type JumpToNodeData = {
  label?: string;
  /** Id of the node execution should jump to. */
  targetNodeId?: string;
  /** Denormalised label kept for display so the node card stays readable without traversing the graph. */
  targetNodeLabel?: string;
  /** How many times this jump is allowed before the walk terminates. Defaults to 1. */
  maxJumps?: number;
};

type JumpToNodeType = Node<JumpToNodeData, "jumpTo">;

export function JumpToNode({
  id,
  data,
  selected,
  isConnectable,
}: NodeProps<JumpToNodeType>) {
  const targetLabel = data.targetNodeLabel ?? data.targetNodeId ?? null;
  const maxJumps = data.maxJumps ?? 1;

  return (
    <WorkflowNode
      id={id}
      icon="redo"
      label={data.label ?? "Jump To"}
      headerColor="bg-orange-bg text-orange-text"
      isConnectable={isConnectable}
      selected={selected}
    >
      <div className="flex flex-col gap-1">
        <span className={targetLabel ? "text-neutral-400" : "text-red-border"}>
          {targetLabel ? `→ ${targetLabel}` : "No target configured"}
        </span>

        <span className="text-neutral-400">
          Max {maxJumps} jump{maxJumps !== 1 ? "s" : ""}
        </span>

        <span className="text-neutral-400">
          After the limit, continue to the node connected below
        </span>
      </div>
    </WorkflowNode>
  );
}
