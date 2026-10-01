"use client";

import { PopoverMenu } from "@/components/common/PopoverMenu";
import { useToast } from "@/components/providers/ToastProvider";
import { useAuthentication } from "@/hooks/use-authentication";
import { useCurrentMember } from "@/hooks/use-current-member";
import { useUpdateContactTags } from "@/hooks/use-update-contact-tags";
import { useWorkspace } from "@/hooks/use-workspaces";
import { errorMessage } from "@/lib/error-message";

import { ContactTagPill, ContactTagPills, tagColor } from "./ContactTagPill";

type Props = {
  contactId: string;
  workspaceId: string;
  tags: Record<string, string>;
};

export function ContactTagEditor({ contactId, workspaceId, tags }: Props) {
  const workspace = useWorkspace(workspaceId);
  const { user } = useAuthentication();
  const currentMember = useCurrentMember(workspaceId, user?.userId);
  const updateTags = useUpdateContactTags(contactId, workspaceId);
  const { showToast } = useToast();

  const canEdit =
    currentMember?.role === "OWNER" ||
    currentMember?.permissions.includes("CONTACT_FIELDS_WRITE") === true;

  const definitions = workspace?.contactTagDefinitions ?? [];
  const unsetDefinitions = definitions.filter(
    (definition) => !tags[definition.key]
  );

  function save(key: string, value: string | null) {
    updateTags.mutate(
      { [key]: value },
      {
        onError: (error) => {
          showToast({ kind: "error", message: errorMessage(error) });
        },
      }
    );
  }

  if (definitions.length === 0) {
    return null;
  }

  if (!canEdit) {
    return <ContactTagPills tags={tags} definitions={definitions} />;
  }

  return (
    <div className="flex flex-wrap items-center gap-1.5">
      {definitions
        .filter((definition) => tags[definition.key])
        .map((definition) => (
          <PopoverMenu
            key={definition.key}
            trigger={(toggle) => (
              <ContactTagPill
                label={definition.label || definition.key}
                value={tags[definition.key]}
                color={tagColor(definition, tags[definition.key])}
                onClick={toggle}
              />
            )}
            items={[
              ...definition.values.map((value) => ({
                key: value,
                content: (
                  <ContactTagPill
                    value={value}
                    color={tagColor(definition, value)}
                  />
                ),
                selected: tags[definition.key] === value,
                onSelect: () => save(definition.key, value),
              })),
              {
                key: "clear",
                content: (
                  <span className="text-xs text-neutral-500">Clear</span>
                ),
                onSelect: () => save(definition.key, null),
              },
            ]}
          />
        ))}

      {unsetDefinitions.length > 0 && (
        <PopoverMenu
          trigger={(toggle) => (
            <button
              type="button"
              onClick={toggle}
              className="inline-flex items-center gap-0.5 rounded-full border border-dashed border-neutral-300 px-2 py-px text-xs font-medium text-neutral-500 transition-colors hover:border-neutral-400 hover:text-neutral-700"
            >
              <span
                className="material-symbols-rounded text-[14px] leading-none"
                aria-hidden="true"
              >
                add
              </span>
              Add tag
            </button>
          )}
          items={unsetDefinitions.flatMap((definition) =>
            definition.values.map((value) => ({
              key: `${definition.key}:${value}`,
              content: (
                <ContactTagPill
                  label={definition.label || definition.key}
                  value={value}
                  color={tagColor(definition, value)}
                />
              ),
              onSelect: () => save(definition.key, value),
            }))
          )}
        />
      )}
    </div>
  );
}
