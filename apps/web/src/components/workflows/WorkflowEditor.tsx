"use client";

import Link from "next/link";
import {
  type ComponentType,
  useCallback,
  useEffect,
  useRef,
  useState,
} from "react";

import {
  Background,
  type Connection,
  Controls,
  type Edge,
  MiniMap,
  type Node,
  type NodeProps,
  ReactFlow,
  addEdge,
  useEdgesState,
  useNodesState,
  useReactFlow,
} from "@xyflow/react";
// ReactFlowProvider must wrap any component that uses useReactFlow.
import { ReactFlowProvider } from "@xyflow/react";
import "@xyflow/react/dist/style.css";

import { Spinner } from "@/components/common/Spinner";
import { WorkspaceNav } from "@/components/workspace/WorkspaceNav";
import { useSaveWorkflow } from "@/hooks/use-save-workflow";
import { useWorkflow } from "@/hooks/use-workflow";

import { NodeConfigPanel } from "./NodeConfigPanel";
import { DeletableEdge } from "./edges/DeletableEdge";
import {
  ConditionNode,
  EndConversationNode,
  HttpRequestNode,
  JumpToNode,
  SendMessageNode,
  SetVariableNode,
  TriggerNode,
  WaitForReplyNode,
} from "./nodes";

const edgeTypes = {
  default: DeletableEdge,
};

// @xyflow/react requires ComponentType<NodeProps> in the map; we cast because
// each node is typed with its specific Node<Data, Type> variant.
const nodeTypes: Record<string, ComponentType<NodeProps>> = {
  trigger: TriggerNode as ComponentType<NodeProps>,
  sendMessage: SendMessageNode as ComponentType<NodeProps>,
  condition: ConditionNode as ComponentType<NodeProps>,
  httpRequest: HttpRequestNode as ComponentType<NodeProps>,
  setVariable: SetVariableNode as ComponentType<NodeProps>,
  endConversation: EndConversationNode as ComponentType<NodeProps>,
  waitForReply: WaitForReplyNode as ComponentType<NodeProps>,
  jumpTo: JumpToNode as ComponentType<NodeProps>,
};

const palette = [
  {
    type: "trigger",
    label: "Trigger",
    icon: "bolt",
    color: "bg-blue-bg text-blue-text border-blue-border",
  },
  {
    type: "sendMessage",
    label: "Send Message",
    icon: "send",
    color: "bg-green-bg text-green-text border-green-border",
  },
  {
    type: "condition",
    label: "Condition",
    icon: "call_split",
    color: "bg-yellow-bg text-yellow-text border-yellow-border",
  },
  {
    type: "httpRequest",
    label: "HTTP Request",
    icon: "http",
    color: "bg-teal-bg text-teal-text border-teal-border",
  },
  {
    type: "setVariable",
    label: "Set Variable",
    icon: "variable_insert",
    color: "bg-purple-bg text-purple-text border-purple-border",
  },
  {
    type: "waitForReply",
    label: "Ask Question",
    icon: "mark_unread_chat_alt",
    color: "bg-blue-bg text-blue-text border-blue-border",
  },
  {
    type: "jumpTo",
    label: "Jump To",
    icon: "redo",
    color: "bg-orange-bg text-orange-text border-orange-border",
  },
  {
    type: "endConversation",
    label: "End Conversation",
    icon: "mark_chat_read",
    color: "bg-red-bg text-red-text border-red-border",
  },
] as const;

type Props = {
  workflowId: string;
  workspaceId: string;
};

function EditorCanvas({ workflowId, workspaceId }: Props) {
  const {
    data: workflow,
    isPending,
    isError,
  } = useWorkflow(workflowId, workspaceId);
  const { mutate: save, isPending: isSaving } = useSaveWorkflow(
    workflowId,
    workspaceId
  );
  const { screenToFlowPosition } = useReactFlow();

  const [nodes, setNodes, onNodesChange] = useNodesState<Node>([]);
  const [edges, setEdges, onEdgesChange] = useEdgesState<Edge>([]);
  const [name, setName] = useState("");
  const [isDirty, setIsDirty] = useState(false);
  const [publishError, setPublishError] = useState<string | null>(null);

  // Track which workflow we've initialised local state for.
  const [seenWorkflowId, setSeenWorkflowId] = useState<string | undefined>();

  const nodeIdRef = useRef(0);

  // Derived state — reset name and dirty flag synchronously during render when
  // a different workflow is loaded. This is the React-idiomatic replacement for
  // calling setState inside a useEffect body (which triggers a double render).
  if (workflow && workflow.id !== seenWorkflowId) {
    setSeenWorkflowId(workflow.id);
    setName(workflow.name);
    setIsDirty(false);
  }

  // Sync ReactFlow's external node/edge state whenever the workflow data changes.
  useEffect(() => {
    if (!workflow) return;

    const graph = workflow.draftGraph as { nodes?: Node[]; edges?: Edge[] };

    setNodes(graph.nodes ?? []);
    setEdges(graph.edges ?? []);
  }, [workflow, setNodes, setEdges]);

  const isLocked = workflow?.enabled ?? false;

  const onConnect = useCallback(
    (connection: Connection) => {
      if (isLocked) return;
      setEdges((eds) => addEdge(connection, eds));
      setIsDirty(true);
    },
    [setEdges, isLocked]
  );

  const onDragOver = useCallback((event: React.DragEvent) => {
    event.preventDefault();
    event.dataTransfer.dropEffect = "move";
  }, []);

  const onDrop = useCallback(
    (event: React.DragEvent) => {
      event.preventDefault();

      if (isLocked) return;

      const type = event.dataTransfer.getData("application/reactflow");

      if (!type) return;

      const position = screenToFlowPosition({
        x: event.clientX,
        y: event.clientY,
      });

      const id = `${type}-${++nodeIdRef.current}`;

      const defaultData: Record<string, unknown> = {
        label: palette.find((p) => p.type === type)?.label ?? type,
      };

      // Seed mandatory fields so nodes are valid on first drop.
      if (type === "trigger") defaultData.event = "conversation_opened";
      if (type === "jumpTo") defaultData.maxJumps = 1;

      const newNode: Node = { id, type, position, data: defaultData };

      setNodes((nds) => [...nds, newNode]);
      setIsDirty(true);
    },
    [screenToFlowPosition, setNodes, isLocked]
  );

  function handleSave() {
    setPublishError(null);
    save(
      { name, draftGraph: { nodes, edges } as Record<string, unknown> },
      { onSuccess: () => setIsDirty(false) }
    );
  }

  function handlePublishToggle() {
    if (!workflow) return;

    setPublishError(null);

    if (workflow.enabled) {
      // Deactivate — no validation needed.
      save({ enabled: false });
    } else {
      // Activate — always include the current graph so the backend validates
      // what the user sees, even if there are unsaved changes.
      save(
        {
          name,
          draftGraph: { nodes, edges } as Record<string, unknown>,
          enabled: true,
        },
        {
          onSuccess: () => setIsDirty(false),
          onError: (err) => {
            const message =
              err instanceof Error ? err.message : "Failed to publish workflow";
            setPublishError(message);
          },
        }
      );
    }
  }

  if (isPending) {
    return (
      <div className="flex h-screen items-center justify-center">
        <Spinner size="lg" />
      </div>
    );
  }

  if (isError || !workflow) {
    return (
      <div className="flex h-screen items-center justify-center">
        <p className="text-sm text-red-text">Failed to load workflow.</p>
      </div>
    );
  }

  const selectedNode = (!isLocked && nodes.find((n) => n.selected)) ?? null;

  function closeConfigPanel() {
    setNodes((nds) => nds.map((n) => ({ ...n, selected: false })));
  }

  return (
    <div className="flex h-full overflow-hidden">
      <WorkspaceNav workspaceId={workspaceId} />

      <div className="flex flex-1 flex-col overflow-hidden pb-14 md:pb-0">
        {/* Editor top bar */}
        <div className="flex flex-shrink-0 flex-col border-b border-neutral-300 bg-neutral-100">
          <div className="flex items-center gap-3 px-4 py-2.5">
            <Link
              href={`/workflows?workspaceId=${workspaceId}`}
              className="flex items-center rounded-md p-1 text-neutral-400 transition-colors hover:bg-neutral-200 hover:text-neutral-700"
              title="Back to workflows"
            >
              <span
                className="material-symbols-rounded text-[18px] leading-none"
                aria-hidden="true"
              >
                arrow_back
              </span>
            </Link>

            <input
              type="text"
              value={name}
              readOnly={isLocked}
              onChange={(e) => {
                if (isLocked) return;
                setName(e.target.value);
                setIsDirty(true);
              }}
              className={`flex-1 bg-transparent text-sm font-semibold text-neutral-800 outline-none placeholder:text-neutral-400 focus:text-neutral-900 ${
                isLocked ? "cursor-default select-none" : ""
              }`}
              aria-label="Workflow name"
              placeholder="Untitled workflow"
            />

            {!isLocked && (
              <button
                onClick={handleSave}
                disabled={!isDirty || isSaving}
                className="flex h-7 items-center gap-1.5 rounded-md bg-accent px-3 text-xs font-semibold text-neutral-100 transition-opacity disabled:opacity-40"
              >
                Save
                {isSaving && (
                  <Spinner
                    size="sm"
                    className="border-neutral-100/40 border-t-neutral-100"
                  />
                )}
              </button>
            )}

            <button
              onClick={handlePublishToggle}
              disabled={isSaving}
              className={`flex h-7 items-center gap-1.5 rounded-md border px-3 text-xs font-semibold transition-colors disabled:opacity-40 ${
                workflow.enabled
                  ? "border-green-border bg-green-bg text-green-text hover:bg-green-bg-hover"
                  : "border-neutral-300 bg-neutral-100 text-neutral-600 hover:bg-neutral-200"
              }`}
            >
              <span
                className="material-symbols-rounded text-[12px] leading-none"
                aria-hidden="true"
              >
                {workflow.enabled ? "stop_circle" : "play_arrow"}
              </span>
              {workflow.enabled ? "Unpublish" : "Publish"}
            </button>
          </div>

          {isLocked && (
            <div className="flex items-center gap-2 border-t border-yellow-border/30 bg-yellow-bg px-4 py-1.5">
              <span
                className="material-symbols-rounded text-[14px] leading-none text-yellow-text"
                aria-hidden="true"
              >
                lock
              </span>
              <p className="text-xs font-medium text-yellow-text">
                This workflow is published. Unpublish it to make changes.
              </p>
            </div>
          )}

          {publishError && (
            <div className="flex items-center gap-2 border-t border-red-border/30 bg-red-bg px-4 py-2">
              <span
                className="material-symbols-rounded mt-px flex-shrink-0 text-[14px] leading-none text-red-text"
                aria-hidden="true"
              >
                error
              </span>
              <p className="text-xs text-red-text">{publishError}</p>
            </div>
          )}
        </div>

        {/* Node palette — horizontal scroll on mobile */}
        {!isLocked && (
          <div className="flex flex-shrink-0 gap-2 overflow-x-auto border-b border-neutral-300 bg-neutral-100 p-2 md:hidden">
            {palette.map(({ type, label, icon, color }) => (
              <div
                key={type}
                draggable
                onDragStart={(e) => {
                  e.dataTransfer.setData("application/reactflow", type);
                  e.dataTransfer.effectAllowed = "move";
                }}
                className={`flex flex-shrink-0 cursor-grab items-center gap-1.5 rounded-lg border px-2.5 py-1.5 text-xs font-medium select-none active:cursor-grabbing ${color}`}
              >
                <span
                  className="material-symbols-rounded text-[13px] leading-none"
                  aria-hidden="true"
                >
                  {icon}
                </span>
                {label}
              </div>
            ))}
          </div>
        )}

        {/* Canvas area */}
        <div className="flex flex-1 overflow-hidden">
          {/* Node palette — vertical sidebar on desktop */}
          <aside className="hidden w-48 flex-shrink-0 flex-col gap-2 border-r border-neutral-300 bg-neutral-100 p-3 md:flex">
            <p className="mb-1 text-xs font-semibold uppercase tracking-wide text-neutral-400">
              Nodes
            </p>

            {palette.map(({ type, label, icon, color }) => (
              <div
                key={type}
                draggable={!isLocked}
                onDragStart={(e) => {
                  if (isLocked) return;
                  e.dataTransfer.setData("application/reactflow", type);
                  e.dataTransfer.effectAllowed = "move";
                }}
                className={`flex items-center gap-2 rounded-lg border px-3 py-2 text-xs font-medium select-none ${color} ${
                  isLocked
                    ? "cursor-not-allowed opacity-40"
                    : "cursor-grab active:cursor-grabbing"
                }`}
              >
                <span
                  className="material-symbols-rounded text-[14px] leading-none"
                  aria-hidden="true"
                >
                  {icon}
                </span>
                {label}
              </div>
            ))}
          </aside>

          {/* React Flow canvas */}
          <ReactFlow
            nodes={nodes}
            edges={edges}
            onNodesChange={(changes) => {
              onNodesChange(changes);
              // "select" and "dimensions" are internal React Flow bookkeeping —
              // not user edits. Only position, add, remove, and replace changes
              // mean the graph actually changed.
              if (
                !isLocked &&
                changes.some(
                  (c) => c.type !== "select" && c.type !== "dimensions"
                )
              ) {
                setIsDirty(true);
              }
            }}
            onEdgesChange={(changes) => {
              onEdgesChange(changes);
              if (!isLocked && changes.some((c) => c.type !== "select")) {
                setIsDirty(true);
              }
            }}
            onConnect={onConnect}
            onDrop={onDrop}
            onDragOver={onDragOver}
            nodeTypes={nodeTypes}
            edgeTypes={edgeTypes}
            nodesDraggable={!isLocked}
            nodesConnectable={!isLocked}
            elementsSelectable={!isLocked}
            deleteKeyCode={isLocked ? null : "Backspace"}
            fitView
            proOptions={{ hideAttribution: true }}
            className="bg-neutral-200"
          >
            <Background gap={16} size={1} color="#d4d4d4" />
            <Controls />
            <MiniMap zoomable pannable className="!bg-neutral-100" />
          </ReactFlow>

          {/* Config panel — slides in when a node is selected (locked canvas never has a selection) */}
          {selectedNode && (
            <NodeConfigPanel
              key={selectedNode.id}
              node={selectedNode}
              nodes={nodes}
              onClose={closeConfigPanel}
              onDirty={() => setIsDirty(true)}
            />
          )}
        </div>
      </div>
    </div>
  );
}

export function WorkflowEditor(props: Props) {
  return (
    <ReactFlowProvider>
      <EditorCanvas {...props} />
    </ReactFlowProvider>
  );
}
