"use client";

// Docs: docs/codebase-components.md#knowledgeanswerpreview
import { LoadingButton } from "@/components/common/LoadingButton";
import { usePreviewKnowledge } from "@/hooks/use-preview-knowledge";
import { errorMessage } from "@/lib/error-message";

type Props = {
  workspaceId: string;
  answer: string;
};

export function KnowledgeAnswerPreview({ workspaceId, answer }: Props) {
  const preview = usePreviewKnowledge(workspaceId);
  const result = preview.data;
  const isStale =
    preview.variables !== undefined && preview.variables !== answer;

  return (
    <div className="mt-2">
      <LoadingButton
        type="button"
        isLoading={preview.isPending}
        onClick={() => preview.mutate(answer)}
        className="text-xs font-medium text-secondary hover:text-secondary-dark"
      >
        Preview with live values
      </LoadingButton>

      {preview.error && (
        <p className="mt-1.5 text-xs text-red-text" role="alert">
          {errorMessage(preview.error)}
        </p>
      )}

      {result && !isStale && (
        <div className="mt-1.5 rounded-lg border border-neutral-300 bg-neutral-200 px-3 py-2 text-xs">
          <p className="whitespace-pre-wrap text-neutral-700">{result.text}</p>
          {result.failures.length > 0 && (
            <ul className="mt-1.5 list-disc pl-4 text-red-text">
              {result.failures.map((failure) => (
                <li key={failure}>{failure}</li>
              ))}
            </ul>
          )}
        </div>
      )}
    </div>
  );
}
