import type {
  ContactTagColor,
  ContactTagDefinition,
} from "@/lib/messaging-api";

export const CONTACT_TAG_COLORS: ContactTagColor[] = [
  "neutral",
  "blue",
  "green",
  "yellow",
  "orange",
  "red",
  "purple",
  "teal",
];

const COLOR_CLASS: Record<ContactTagColor, string> = {
  neutral: "bg-neutral-200 text-neutral-700",
  blue: "bg-blue-bg text-blue-text",
  green: "bg-green-bg text-green-text",
  yellow: "bg-yellow-bg text-yellow-text",
  orange: "bg-orange-bg text-orange-text",
  red: "bg-red-bg text-red-text",
  purple: "bg-purple-bg text-purple-text",
  teal: "bg-teal-bg text-teal-text",
};

export const CONTACT_TAG_SWATCH_CLASS: Record<ContactTagColor, string> = {
  neutral: "bg-neutral-400",
  blue: "bg-blue-border",
  green: "bg-green-border",
  yellow: "bg-yellow-border",
  orange: "bg-orange-border",
  red: "bg-red-border",
  purple: "bg-purple-border",
  teal: "bg-teal-border",
};

export function tagColor(
  definition: ContactTagDefinition,
  value: string
): ContactTagColor {
  return definition.colors?.[value] ?? "neutral";
}

type PillSize = "default" | "small";

const SIZE_CLASS: Record<PillSize, string> = {
  default: "px-2 py-0.5 text-xs",
  small: "px-1.5 py-px text-[11px] leading-4",
};

type PillProps = {
  label?: string;
  value: string;
  color: ContactTagColor;
  size?: PillSize;
  onClick?: () => void;
};

export function ContactTagPill({
  label,
  value,
  color,
  size = "default",
  onClick,
}: PillProps) {
  const className = `inline-flex max-w-full flex-shrink-0 items-center gap-1 rounded-full font-medium ${SIZE_CLASS[size]} ${COLOR_CLASS[color]}`;
  const content = (
    <>
      {label && <span className="opacity-70">{label}:</span>}
      <span className="truncate">{value}</span>
    </>
  );

  return onClick ? (
    <button
      type="button"
      onClick={onClick}
      className={`${className} transition-opacity hover:opacity-80`}
    >
      {content}
    </button>
  ) : (
    <span className={className}>{content}</span>
  );
}

type PillsProps = {
  tags: Record<string, string>;
  definitions: ContactTagDefinition[];
  size?: PillSize;
};

export function ContactTagPills({
  tags,
  definitions,
  size = "default",
}: PillsProps) {
  const setTags = definitions.filter((definition) => tags[definition.key]);

  if (setTags.length === 0) {
    return null;
  }

  return (
    <div className="flex flex-wrap gap-1">
      {setTags.map((definition) => (
        <ContactTagPill
          key={definition.key}
          label={definition.label || definition.key}
          value={tags[definition.key]}
          color={tagColor(definition, tags[definition.key])}
          size={size}
        />
      ))}
    </div>
  );
}
