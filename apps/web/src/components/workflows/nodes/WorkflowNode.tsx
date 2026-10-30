// Docs: docs/codebase-components.md#workflownode
import { type ReactNode } from "react";

import { Handle, Position, useReactFlow } from "@xyflow/react";

type Props = {
  id: string;
  icon: string;
  label: string;
  headerColor: string;
  hasTarget?: boolean;
  hasSource?: boolean;
  /** Must be forwarded so handles respect React Flow's global nodesConnectable setting. */
  isConnectable?: boolean;
  children?: ReactNode;
  /** Rendered with no horizontal padding, for elements that align with full-width handles. */
  footer?: ReactNode;
  selected?: boolean;
};

export function WorkflowNode({
  id,
  icon,
  label,
  headerColor,
  hasTarget = true,
  hasSource = true,
  isConnectable = true,
  children,
  footer,
  selected,
}: Props) {
  const { deleteElements } = useReactFlow();

  function handleDelete(event: React.MouseEvent) {
    event.stopPropagation();
    void deleteElements({ nodes: [{ id }] });
  }

  return (
    <div
      className={`w-52 rounded-xl border bg-neutral-100 shadow-sm transition-shadow ${
        selected ? "border-accent shadow-md" : "border-neutral-200"
      }`}
    >
      {hasTarget && (
        <Handle
          type="target"
          position={Position.Top}
          isConnectable={isConnectable}
          className="!border-2 !border-neutral-100 !bg-neutral-500"
        />
      )}

      <div
        className={`flex items-center gap-2 rounded-t-xl px-3 py-2 ${headerColor}`}
      >
        <span
          className="material-symbols-rounded flex-shrink-0 text-xs leading-none"
          aria-hidden="true"
        >
          {icon}
        </span>

        <span className="flex-1 truncate text-xs font-semibold leading-none">
          {label}
        </span>

        {selected && (
          <button
            onClick={handleDelete}
            title="Delete node"
            className="-mr-0.5 flex h-4 w-4 items-center justify-center rounded opacity-60 transition-opacity hover:opacity-100"
          >
            <span
              className="material-symbols-rounded text-[13px] leading-none"
              aria-hidden="true"
            >
              delete
            </span>
          </button>
        )}
      </div>

      {children && (
        <div className="px-3 py-2 text-xs text-neutral-500">{children}</div>
      )}

      {footer}

      {hasSource && (
        <Handle
          type="source"
          position={Position.Bottom}
          isConnectable={isConnectable}
          className="!border-2 !border-neutral-100 !bg-neutral-500"
        />
      )}
    </div>
  );
}
