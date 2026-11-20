// Docs: docs/codebase-components.md#insert-at-cursorts

/** Replaces the selection with text, or appends it when there is no element, and puts the cursor after it. */
export function insertAtCursor(
  element: HTMLInputElement | HTMLTextAreaElement | null,
  currentValue: string,
  text: string,
  onChange: (value: string) => void
) {
  if (!element) {
    onChange(currentValue + text);

    return;
  }

  const start = element.selectionStart ?? currentValue.length;
  const end = element.selectionEnd ?? currentValue.length;

  onChange(currentValue.slice(0, start) + text + currentValue.slice(end));

  requestAnimationFrame(() => {
    element.focus();
    element.setSelectionRange(start + text.length, start + text.length);
  });
}
