import {
  BaseEdge,
  EdgeLabelRenderer,
  type EdgeProps,
  getBezierPath,
  useReactFlow,
} from "@xyflow/react";

export function DeletableEdge({
  id,
  sourceX,
  sourceY,
  targetX,
  targetY,
  sourcePosition,
  targetPosition,
  selected,
  markerEnd,
  style,
}: EdgeProps) {
  const { deleteElements } = useReactFlow();

  const [edgePath, labelX, labelY] = getBezierPath({
    sourceX,
    sourceY,
    sourcePosition,
    targetX,
    targetY,
    targetPosition,
  });

  return (
    <>
      <BaseEdge id={id} path={edgePath} markerEnd={markerEnd} style={style} />

      {selected && (
        <EdgeLabelRenderer>
          <button
            style={{
              transform: `translate(-50%, -50%) translate(${labelX}px, ${labelY}px)`,
            }}
            className="pointer-events-auto absolute flex h-5 w-5 items-center justify-center rounded-full border border-neutral-200 bg-white shadow-sm transition-colors hover:border-red-bg-hover hover:text-red-border"
            title="Remove connection"
            onClick={() => void deleteElements({ edges: [{ id }] })}
          >
            <span
              className="material-symbols-rounded text-[11px] leading-none text-neutral-400 hover:text-red-border"
              aria-hidden="true"
            >
              close
            </span>
          </button>
        </EdgeLabelRenderer>
      )}
    </>
  );
}
