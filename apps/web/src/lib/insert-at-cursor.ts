// Docs: docs/codebase-components.md#insert-at-cursorts

/** Inserts text at the cursor, or appends it when there is no element, then places the cursor cursorOffset characters into the inserted text. */
export function insertAtCursor(
  element: HTMLInputElement | HTMLTextAreaElement | null,
  currentValue: string,
  text: string,
  onChange: (value: string) => void,
  cursorOffset: number = text.length
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
    element.setSelectionRange(start + cursorOffset, start + cursorOffset);
  });
}
