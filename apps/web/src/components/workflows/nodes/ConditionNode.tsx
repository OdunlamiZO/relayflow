import { Handle, type Node, type NodeProps, Position } from "@xyflow/react";

import { WorkflowNode } from "./WorkflowNode";

export type ConditionOperator =
  | "eq"
  | "neq"
  | "gt"
  | "lt"
  | "gte"
  | "lte"
  | "contains"
  | "not_contains"
  | "starts_with"
  | "ends_with"
  | "is_set"
  | "is_not_set";

export type ConditionBranch = {
  id: string;
  label: string;
  variable?: string;
  operator?: ConditionOperator;
  value?: string;
};

export type ConditionNodeData = {
  label?: string;
  branches?: ConditionBranch[];
};

export const DEFAULT_CONDITION_BRANCHES: ConditionBranch[] = [
  { id: "true", label: "True" },
  { id: "false", label: "False" },
];

type ConditionNodeType = Node<ConditionNodeData, "condition">;

export function ConditionNode({
  id,
  data,
  selected,
  isConnectable,
}: NodeProps<ConditionNodeType>) {
  const branches = data.branches ?? DEFAULT_CONDITION_BRANCHES;
  const branchCount = branches.length;

  const branchFooter = (
    <div className="relative pb-5 pt-1">
      {branches.map((branch, i) => {
        const pct = ((i + 1) / (branchCount + 1)) * 100;

        return (
          <span
            key={branch.id}
            className="absolute -translate-x-1/2 truncate text-[10px] text-neutral-400"
            style={{ left: `${pct}%`, bottom: 8 }}
          >
            {branch.label}
          </span>
        );
      })}

      {/* Dynamic source handles — same percentage origin as labels */}
      {branches.map((branch, i) => {
        const pct = ((i + 1) / (branchCount + 1)) * 100;

        return (
          <Handle
            key={branch.id}
            type="source"
            position={Position.Bottom}
            id={branch.id}
            isConnectable={isConnectable}
            style={{ left: `${pct}%` }}
            className="!border-2 !border-white !bg-neutral-500"
          />
        );
      })}
    </div>
  );

  return (
    <WorkflowNode
      id={id}
      icon="call_split"
      label={data.label ?? "Condition"}
      headerColor="bg-yellow-bg text-yellow-text"
      hasSource={false}
      isConnectable={isConnectable}
      selected={selected}
      footer={branchFooter}
    />
  );
}
