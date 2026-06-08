import { Handle, type Node, type NodeProps, Position } from "@xyflow/react";

import { WorkflowNode } from "./WorkflowNode";

export type HttpHeader = { id: string; key: string; value: string };

export type ResponseMapping = {
  id: string;
  /** Path into the response body, e.g. "id" or "user.email". Accessed as $resp.<path>. */
  path: string;
  /** Variable name to store the extracted value in. */
  variable: string;
};

export type HttpRequestNodeData = {
  label?: string;
  method?: "GET" | "POST" | "PUT" | "PATCH" | "DELETE";
  url?: string;
  headers?: HttpHeader[];
  contentType?: string;
  body?: string;
  timeoutSeconds?: number;
  responseMappings?: ResponseMapping[];
  responseStatusVariable?: string;
};

type HttpRequestNodeType = Node<HttpRequestNodeData, "httpRequest">;

export function HttpRequestNode({
  id,
  data,
  selected,
  isConnectable,
}: NodeProps<HttpRequestNodeType>) {
  const footer = (
    <div className="relative pb-5 pt-1">
      <span
        className="absolute -translate-x-1/2 text-[10px] text-green-text"
        style={{ left: "25%", bottom: 8 }}
      >
        Success
      </span>
      <span
        className="absolute -translate-x-1/2 text-[10px] text-red-border"
        style={{ left: "75%", bottom: 8 }}
      >
        Error
      </span>

      <Handle
        type="source"
        position={Position.Bottom}
        id="success"
        isConnectable={isConnectable}
        style={{ left: "25%" }}
        className="!border-2 !border-neutral-100 !bg-green-border"
      />
      <Handle
        type="source"
        position={Position.Bottom}
        id="error"
        isConnectable={isConnectable}
        style={{ left: "75%" }}
        className="!border-2 !border-neutral-100 !bg-red-border"
      />
    </div>
  );

  return (
    <WorkflowNode
      id={id}
      icon="http"
      label={data.label ?? "HTTP Request"}
      headerColor="bg-teal-bg text-teal-text"
      hasSource={false}
      isConnectable={isConnectable}
      selected={selected}
      footer={footer}
    >
      {data.url ? (
        <div className="flex flex-col gap-1">
          <span className="flex items-center gap-1.5 truncate">
            <span className="flex-shrink-0 rounded bg-teal-bg px-1 py-0.5 font-mono text-[10px] font-semibold text-teal-text">
              {data.method ?? "GET"}
            </span>
            <span className="truncate text-neutral-400">{data.url}</span>
          </span>

          {((data.headers?.length ?? 0) > 0 ||
            data.body ||
            data.timeoutSeconds) && (
            <span className="flex items-center gap-2 text-[10px] text-neutral-400">
              {(data.headers?.length ?? 0) > 0 && (
                <span>
                  {data.headers!.length} header
                  {data.headers!.length !== 1 ? "s" : ""}
                </span>
              )}
              {data.body && <span>body</span>}
              {data.timeoutSeconds && data.timeoutSeconds !== 30 && (
                <span>{data.timeoutSeconds}s</span>
              )}
            </span>
          )}

          {((data.responseMappings?.length ?? 0) > 0 ||
            data.responseStatusVariable) && (
            <span className="flex items-center gap-1 text-[10px] text-neutral-400">
              <span
                className="material-symbols-rounded text-[11px] leading-none"
                aria-hidden="true"
              >
                arrow_downward
              </span>
              {[
                (data.responseMappings?.length ?? 0) > 0 &&
                  `${data.responseMappings!.length} mapping${data.responseMappings!.length !== 1 ? "s" : ""}`,
                data.responseStatusVariable &&
                  `status → ${data.responseStatusVariable}`,
              ]
                .filter(Boolean)
                .join(" · ")}
            </span>
          )}
        </div>
      ) : (
        <span className="text-neutral-400">Not configured</span>
      )}
    </WorkflowNode>
  );
}
