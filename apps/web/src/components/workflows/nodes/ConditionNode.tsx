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

export type ConditionRule = {
  id: string;
  label?: string;
  variable?: string;
  operator?: ConditionOperator;
  value?: string;
};

export type ConditionBranch = {
  id: string;
  label: string;
  /** How the branch's conditions are combined. Defaults to "and". Ignored with a single condition. */
  combinator?: "and" | "or";
  conditions?: ConditionRule[];
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
      {/* Branch numbers — full labels are listed in the body above instead, so
          long or numerous branches don't overlap here. */}
      {branches.map((branch, i) => {
        const pct = ((i + 1) / (branchCount + 1)) * 100;

        return (
          <span
            key={branch.id}
            className="absolute -translate-x-1/2 text-[10px] font-semibold leading-none text-yellow-text"
            style={{ left: `${pct}%`, bottom: 8 }}
          >
            {i + 1}
          </span>
        );
      })}

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
            className="!border-2 !border-neutral-100 !bg-neutral-500"
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
    >
      <ul className="space-y-1">
        {branches.map((branch, i) => (
          <li key={branch.id} className="flex gap-1.5 text-neutral-500">
            <span className="flex-shrink-0 font-semibold text-yellow-text">
              {i + 1}.
            </span>
            <span className="line-clamp-2 break-words">
              {branch.label || `Branch ${i + 1}`}
            </span>
          </li>
        ))}
      </ul>
    </WorkflowNode>
  );
}
