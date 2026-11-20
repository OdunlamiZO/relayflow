"use client";

// Docs: docs/codebase-components.md#runworkflowmenu
import { PopoverMenu } from "@/components/common/PopoverMenu";
import { Spinner } from "@/components/common/Spinner";
import { useConversationAgentWorkflows } from "@/hooks/use-conversation-agent-workflows";
import { useRunConversationAgentWorkflow } from "@/hooks/use-run-conversation-agent-workflow";

type Props = {
  workspaceId: string;
  conversationId: string;
};

export function RunWorkflowMenu({ workspaceId, conversationId }: Props) {
  const { data: workflows = [] } = useConversationAgentWorkflows(
    workspaceId,
    conversationId
  );
  const {
    mutate: runWorkflow,
    isPending,
    error,
    reset,
  } = useRunConversationAgentWorkflow(workspaceId, conversationId);

  if (workflows.length === 0) {
    return null;
  }

  return (
    <div className="flex flex-shrink-0 items-center gap-2">
      {error && (
        <span
          className="max-w-40 truncate text-xs text-red-text"
          title={error.message}
          role="alert"
        >
          {error.message}
        </span>
      )}

      <PopoverMenu
        align="end"
        trigger={(toggle) => (
          <button
            type="button"
            onClick={() => {
              reset();
              toggle();
            }}
            disabled={isPending}
            aria-haspopup="menu"
            className="flex flex-shrink-0 items-center gap-1.5 rounded-lg bg-purple-bg px-2.5 py-1.5 text-xs font-medium text-purple-text transition-colors hover:bg-purple-bg/70 disabled:cursor-not-allowed disabled:opacity-50"
          >
            {isPending ? (
              <Spinner size="sm" />
            ) : (
              <span
                className="material-symbols-rounded text-[14px] leading-none"
                aria-hidden="true"
              >
                bolt
              </span>
            )}
            Run workflow
          </button>
        )}
        items={workflows.map((workflow) => ({
          key: workflow.workflowId,
          onSelect: () => runWorkflow(workflow.workflowId),
          content: (
            <span className="flex w-60 flex-col gap-0.5">
              <span className="font-medium text-neutral-800">
                {workflow.name}
              </span>
              {workflow.triggerDescription && (
                <span className="text-xs text-neutral-500">
                  {workflow.triggerDescription}
                </span>
              )}
            </span>
          ),
        }))}
      />
    </div>
  );
}
