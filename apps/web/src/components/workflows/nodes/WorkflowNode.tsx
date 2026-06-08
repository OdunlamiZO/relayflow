import { type ReactNode } from "react";

import { Handle, Position, useReactFlow } from "@xyflow/react";

type Props = {
  /** The node's id — needed for the delete action. */
  id: string;
  /** Icon name (Material Symbols). */
  icon: string;
  /** Node label. */
  label: string;
  /** Tailwind colour classes for the header stripe. */
  headerColor: string;
  /** Whether this node has an incoming handle. */
  hasTarget?: boolean;
  /** Whether this node has an outgoing handle. */
  hasSource?: boolean;
  /**
   * Forwarded from ReactFlow's NodeProps — must be passed so handles
   * respect the global nodesConnectable setting.
   */
  isConnectable?: boolean;
  /** Extra content rendered below the header, with px-3 py-2 padding. */
  children?: ReactNode;
  /**
   * Content rendered below children with NO horizontal padding — use this
   * for elements (e.g. branch labels) that must align with full-width handles.
   */
  footer?: ReactNode;
  /** Whether the node is currently selected. */
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

  function handleDelete(e: React.MouseEvent) {
    e.stopPropagation();
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
