import { Handle, type Node, type NodeProps, Position } from "@xyflow/react";

import { OutputLabel } from "./OutputLabel";
import { WorkflowNode } from "./WorkflowNode";

export type WaitForReplyOption = {
  id: string;
  text: string;
};

export type WaitForReplyNodeData = {
  label?: string;
  question?: string;
  responseType?: "generic" | "defined";
  responseVariable?: string;
  options?: WaitForReplyOption[];
  /** Minutes to wait for a reply before following the "No reply" output. Defaults to 1440 (24 hours). */
  timeoutMinutes?: number;
  validationHook?: string;
  validationErrorMessage?: string;
  maxAttempts?: number;
};

export const DEFAULT_TIMEOUT_MINUTES = 60 * 24;

/** Formats a duration in minutes as a human-readable string (e.g. "24h", "90m", "2d"). */
function formatTimeout(minutes: number): string {
  if (minutes % (60 * 24) === 0) {
    return `${minutes / (60 * 24)}d`;
  }

  if (minutes % 60 === 0) {
    return `${minutes / 60}h`;
  }

  return `${minutes}m`;
}

type WaitForReplyNodeType = Node<WaitForReplyNodeData, "waitForReply">;

export function WaitForReplyNode({
  id,
  data,
  selected,
  isConnectable,
}: NodeProps<WaitForReplyNodeType>) {
  const isGeneric = !data.responseType || data.responseType === "generic";
  const isValidated = isGeneric && !!data.validationHook;
  const options = data.options ?? [];

  // For defined mode: N option handles + "Other" + "No reply"
  const totalHandles = options.length + 2;

  const definedFooter = !isGeneric ? (
    <div className="relative pb-5 pt-1">
      {/* Option numbers — full text is listed in the body above instead, so
          long or numerous options don't overlap here. */}
      {options.map((opt, i) => {
        const pct = ((i + 1) / (totalHandles + 1)) * 100;

        return (
          <OutputLabel
            key={opt.id}
            left={`${pct}%`}
            className="font-semibold text-blue-text"
          >
            {i + 1}
          </OutputLabel>
        );
      })}

      {/* "Other" label — fallback branch when no option matches the reply */}
      <OutputLabel
        left={`${((totalHandles - 1) / (totalHandles + 1)) * 100}%`}
        className="text-neutral-400"
        title="No option matched"
      >
        Other
      </OutputLabel>

      <NoReplyLabel left={`${(totalHandles / (totalHandles + 1)) * 100}%`} />

      {/* Option handles — blue tint so they're distinct from the default */}
      {options.map((opt, i) => (
        <Handle
          key={opt.id}
          type="source"
          position={Position.Bottom}
          id={opt.id}
          isConnectable={isConnectable}
          style={{ left: `${((i + 1) / (totalHandles + 1)) * 100}%` }}
          className="!border-2 !border-neutral-100 !bg-blue-border"
        />
      ))}

      {/* Default "Other" handle — neutral, always present */}
      <Handle
        type="source"
        position={Position.Bottom}
        id="default"
        isConnectable={isConnectable}
        style={{ left: `${((totalHandles - 1) / (totalHandles + 1)) * 100}%` }}
        className="!border-2 !border-neutral-100 !bg-neutral-500"
      />

      <NoReplyHandle
        left={`${(totalHandles / (totalHandles + 1)) * 100}%`}
        isConnectable={isConnectable}
      />
    </div>
  ) : undefined;

  const validatedFooter = isValidated ? (
    <div className="relative pb-5 pt-1">
      <OutputLabel left="20%" className="text-green-text">
        Valid
      </OutputLabel>
      <OutputLabel left="50%" className="text-red-border">
        Invalid
      </OutputLabel>
      <NoReplyLabel left="80%" />

      <Handle
        type="source"
        position={Position.Bottom}
        id="valid"
        isConnectable={isConnectable}
        style={{ left: "20%" }}
        className="!border-2 !border-neutral-100 !bg-green-border"
      />
      <Handle
        type="source"
        position={Position.Bottom}
        id="invalid"
        isConnectable={isConnectable}
        style={{ left: "50%" }}
        className="!border-2 !border-neutral-100 !bg-red-border"
      />
      <NoReplyHandle left="80%" isConnectable={isConnectable} />
    </div>
  ) : undefined;

  const genericFooter = (
    <div className="relative pb-5 pt-1">
      <OutputLabel left="33%" className="text-neutral-500">
        Reply
      </OutputLabel>
      <NoReplyLabel left="67%" />

      <Handle
        type="source"
        position={Position.Bottom}
        isConnectable={isConnectable}
        style={{ left: "33%" }}
        className="!border-2 !border-neutral-100 !bg-neutral-500"
      />
      <NoReplyHandle left="67%" isConnectable={isConnectable} />
    </div>
  );

  return (
    <WorkflowNode
      id={id}
      icon="mark_unread_chat_alt"
      label={data.label ?? "Ask Question"}
      headerColor="bg-blue-bg text-blue-text"
      hasSource={false}
      isConnectable={isConnectable}
      footer={definedFooter ?? validatedFooter ?? genericFooter}
      selected={selected}
    >
      <span className="line-clamp-2 text-neutral-400">
        {data.question ?? "No question configured"}
      </span>

      <p className="m-0 mt-1.5 text-[10px] text-neutral-400">
        Times out after{" "}
        {formatTimeout(data.timeoutMinutes ?? DEFAULT_TIMEOUT_MINUTES)}
        {isValidated && ` · validated, ${data.maxAttempts ?? 3} attempts`}
      </p>

      {!isGeneric && options.length > 0 && (
        <ul className="mt-2 space-y-1">
          {options.map((opt, i) => (
            <li key={opt.id} className="flex gap-1.5 text-blue-text">
              <span className="flex-shrink-0 font-semibold">{i + 1}.</span>
              <span className="line-clamp-2 break-words">
                {opt.text || `Option ${i + 1}`}
              </span>
            </li>
          ))}

          <li className="flex gap-1.5 text-neutral-400">
            <span className="flex-shrink-0 font-semibold">•</span>
            <span>Other — no option matched</span>
          </li>
        </ul>
      )}
    </WorkflowNode>
  );
}

function NoReplyLabel({ left }: { left: string }) {
  return (
    <OutputLabel
      left={left}
      className="text-yellow-text"
      title="No reply before the timeout"
    >
      No reply
    </OutputLabel>
  );
}

function NoReplyHandle({
  left,
  isConnectable,
}: {
  left: string;
  isConnectable: boolean;
}) {
  return (
    <Handle
      type="source"
      position={Position.Bottom}
      id="noReply"
      isConnectable={isConnectable}
      style={{ left }}
      className="!border-2 !border-neutral-100 !bg-yellow-border"
    />
  );
}
