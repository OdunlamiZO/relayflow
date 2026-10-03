export type FormattedNode =
  | string
  | { type: "bold" | "italic" | "strike"; children: FormattedNode[] }
  | { type: "code" | "codeBlock"; text: string };

export type FormattingMarker = "*" | "_" | "~" | "`" | "```";

const CODE_BLOCK = /```([\s\S]+?)```/g;

const INLINE_CODE = /`([^`\n]+)`/g;

const BULLET = /^([ \t]*)[-*] (?=\S)/gm;

const STYLES: { type: "bold" | "italic" | "strike"; pattern: RegExp }[] = [
  { type: "bold", pattern: stylePattern("\\*") },
  { type: "italic", pattern: stylePattern("_") },
  { type: "strike", pattern: stylePattern("~") },
];

function stylePattern(marker: string): RegExp {
  return new RegExp(
    `(?<![\\p{L}\\p{N}])${marker}(\\S(?:[^\\n]*?\\S)?)${marker}(?![\\p{L}\\p{N}])`,
    "u"
  );
}

export function parseFormattedText(text: string): FormattedNode[] {
  return splitByPattern(text, CODE_BLOCK, "codeBlock", (part) =>
    splitByPattern(
      part.replace(BULLET, "$1• "),
      INLINE_CODE,
      "code",
      parseStyles
    )
  );
}

function splitByPattern(
  text: string,
  pattern: RegExp,
  type: "code" | "codeBlock",
  parseRest: (text: string) => FormattedNode[]
): FormattedNode[] {
  const nodes: FormattedNode[] = [];
  let position = 0;

  for (const match of text.matchAll(pattern)) {
    nodes.push(...parseRest(text.slice(position, match.index)));
    nodes.push({ type, text: match[1] });
    position = match.index + match[0].length;
  }

  nodes.push(...parseRest(text.slice(position)));

  return nodes;
}

function parseStyles(text: string): FormattedNode[] {
  const nodes: FormattedNode[] = [];
  let rest = text;

  while (rest) {
    let earliest: {
      type: "bold" | "italic" | "strike";
      match: RegExpExecArray;
    } | null = null;

    for (const style of STYLES) {
      const match = style.pattern.exec(rest);

      if (match && (!earliest || match.index < earliest.match.index)) {
        earliest = { type: style.type, match };
      }
    }

    if (!earliest) {
      nodes.push(rest);

      break;
    }

    if (earliest.match.index > 0) {
      nodes.push(rest.slice(0, earliest.match.index));
    }

    nodes.push({
      type: earliest.type,
      children: parseStyles(earliest.match[1]),
    });
    rest = rest.slice(earliest.match.index + earliest.match[0].length);
  }

  return nodes;
}

export function bulletLines(
  value: string,
  start: number,
  end: number
): { value: string; selectionStart: number; selectionEnd: number } {
  const lineStart = value.lastIndexOf("\n", start - 1) + 1;
  const nextBreak = value.indexOf("\n", end);
  const lineEnd = nextBreak === -1 ? value.length : nextBreak;
  const lines = value
    .slice(lineStart, lineEnd)
    .split("\n")
    .map((line) => (/^[ \t]*[-*] /.test(line) ? line : `- ${line}`))
    .join("\n");

  return {
    value: value.slice(0, lineStart) + lines + value.slice(lineEnd),
    selectionStart: lineStart,
    selectionEnd: lineStart + lines.length,
  };
}

export function wrapSelection(
  value: string,
  start: number,
  end: number,
  marker: FormattingMarker
): { value: string; selectionStart: number; selectionEnd: number } {
  const selected = value.slice(start, end);
  const isBlock = marker === "```";
  const opening = isBlock ? "```\n" : marker;
  const closing = isBlock ? "\n```" : marker;

  return {
    value:
      value.slice(0, start) + opening + selected + closing + value.slice(end),
    selectionStart: start + opening.length,
    selectionEnd: start + opening.length + selected.length,
  };
}
