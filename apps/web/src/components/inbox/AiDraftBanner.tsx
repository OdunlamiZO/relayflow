"use client";

import { LoadingButton } from "@/components/common/LoadingButton";
import { useDiscardAiDraft } from "@/hooks/use-discard-ai-draft";
import { useSendAiDraft } from "@/hooks/use-send-ai-draft";
import { useTriggerWorkflowFromDraft } from "@/hooks/use-trigger-workflow-from-draft";
import type { ConversationAiDraft } from "@/lib/messaging-api";

type Props = {
  workspaceId: string;
  conversationId: string;
  draft: ConversationAiDraft;
  onEdit: (text: string) => void;
};

function parseWorkflowActions(suggestedActions: string[]): string[] {
  return suggestedActions
    .filter((action) => action.startsWith("trigger_workflow:"))
    .map((action) => action.slice("trigger_workflow:".length));
}

export function AiDraftBanner({
  workspaceId,
  conversationId,
  draft,
  onEdit,
}: Props) {
  const sendDraft = useSendAiDraft(workspaceId, conversationId);
  const discardDraft = useDiscardAiDraft(workspaceId, conversationId);
  const triggerWorkflow = useTriggerWorkflowFromDraft(
    workspaceId,
    conversationId
  );

  const workflowIds = parseWorkflowActions(draft.suggestedActions);
  const isWorkflowDraft = workflowIds.length > 0;
  const isBusy =
    sendDraft.isPending || discardDraft.isPending || triggerWorkflow.isPending;

  function handleEdit() {
    onEdit(draft.proposedReply);
    discardDraft.mutate();
  }

  return (
    <div className="mx-4 mb-2 rounded-xl border border-blue-border bg-blue-bg px-4 py-3">
      <div className="mb-2 flex items-center gap-1.5">
        <span
          className="material-symbols-rounded text-[14px] leading-none text-blue-text"
          aria-hidden="true"
        >
          smart_toy
        </span>
        <span className="text-xs font-semibold text-blue-text">
          {isWorkflowDraft ? "AI workflow suggestion" : "AI draft"}
        </span>
      </div>

      {!isWorkflowDraft && (
        <p className="mb-3 text-sm text-neutral-700">{draft.proposedReply}</p>
      )}

      {isWorkflowDraft && (
        <p className="mb-3 text-sm text-neutral-500">
          The AI suggests running a workflow to handle this conversation.
        </p>
      )}

      <div className="flex flex-wrap items-center gap-2">
        {isWorkflowDraft ? (
          workflowIds.map((workflowId) => (
            <LoadingButton
              key={workflowId}
              type="button"
              disabled={isBusy}
              isLoading={triggerWorkflow.isPending}
              onClick={() => triggerWorkflow.mutate(workflowId)}
              className="rounded-lg bg-secondary px-3 py-1.5 text-xs font-semibold text-neutral-100 transition-colors hover:bg-secondary-dark disabled:cursor-not-allowed disabled:opacity-60"
            >
              Run Workflow
            </LoadingButton>
          ))
        ) : (
          <>
            <LoadingButton
              type="button"
              disabled={isBusy}
              isLoading={sendDraft.isPending}
              onClick={() => sendDraft.mutate()}
              className="rounded-lg bg-secondary px-3 py-1.5 text-xs font-semibold text-neutral-100 transition-colors hover:bg-secondary-dark disabled:cursor-not-allowed disabled:opacity-60"
            >
              Send
            </LoadingButton>

            <button
              type="button"
              disabled={isBusy}
              onClick={handleEdit}
              className="rounded-lg border border-neutral-300 bg-neutral-100 px-3 py-1.5 text-xs font-medium text-neutral-700 transition-colors hover:bg-neutral-200 disabled:cursor-not-allowed disabled:opacity-60"
            >
              Edit
            </button>
          </>
        )}

        <LoadingButton
          type="button"
          disabled={isBusy}
          isLoading={discardDraft.isPending}
          onClick={() => discardDraft.mutate()}
          className="rounded-lg px-3 py-1.5 text-xs font-medium text-neutral-500 transition-colors hover:text-red-500 disabled:cursor-not-allowed disabled:opacity-60"
        >
          Discard
        </LoadingButton>
      </div>
    </div>
  );
}
