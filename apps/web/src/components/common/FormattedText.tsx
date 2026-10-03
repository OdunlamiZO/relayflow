import {
  type FormattedNode,
  parseFormattedText,
} from "@/lib/message-formatting";

type Props = {
  text: string;
};

export function FormattedText({ text }: Props) {
  return <>{parseFormattedText(text).map(renderNode)}</>;
}

function renderNode(node: FormattedNode, index: number): React.ReactNode {
  if (typeof node === "string") {
    return node;
  }

  switch (node.type) {
    case "bold":
      return <strong key={index}>{node.children.map(renderNode)}</strong>;
    case "italic":
      return <em key={index}>{node.children.map(renderNode)}</em>;
    case "strike":
      return <s key={index}>{node.children.map(renderNode)}</s>;
    case "code":
      return (
        <code key={index} className="font-mono text-[0.9em]">
          {node.text}
        </code>
      );
    case "codeBlock":
      return (
        <code
          key={index}
          className="block whitespace-pre-wrap font-mono text-[0.9em]"
        >
          {node.text.replace(/^\n|\n$/g, "")}
        </code>
      );
  }
}
