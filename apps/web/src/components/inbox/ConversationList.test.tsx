import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { ConversationList } from "./ConversationList";

vi.mock("next/navigation", () => ({
  useRouter: vi.fn(() => ({ push: vi.fn() })),
  useSearchParams: vi.fn(() => new URLSearchParams()),
  usePathname: vi.fn(() => "/inbox"),
}));

vi.mock("@/hooks/use-authentication", () => ({
  useAuthentication: vi.fn(() => ({
    isAnonymous: false,
    isAuthenticated: true,
  })),
}));

const mockConversation = {
  id: "conv-1",
  workspaceId: "ws-1",
  contactId: "contact-1",
  contactDisplayName: "Ada Lovelace",
  channelAccountId: "ch-1",
  channelProvider: "TELEGRAM" as const,
  channelAccountName: "My Bot",
  status: "OPEN" as const,
  assignedUserId: null,
  lastMessageAt: "2026-05-26T10:00:00Z",
  createdAt: "2026-05-26T09:00:00Z",
};

// Because vi.doMock doesn't work well across describe blocks we use vi.mock
// at the module level and override the return value per test instead.
const mockUseConversations = vi.fn();

vi.mock("@/hooks/use-conversations", () => ({
  useConversations: (...args: unknown[]) => mockUseConversations(...args),
}));

function baseHookReturn(overrides = {}) {
  return {
    data: undefined,
    isLoading: false,
    isError: false,
    fetchNextPage: vi.fn(),
    hasNextPage: false,
    isFetchingNextPage: false,
    ...overrides,
  };
}

function renderList(props?: { selectedId?: string }) {
  return render(
    <ConversationList
      workspaceId="ws-1"
      selectedConversationId={props?.selectedId}
      onSelect={vi.fn()}
    />
  );
}

describe("ConversationList", () => {
  it("shows a spinner while loading", () => {
    mockUseConversations.mockReturnValue(baseHookReturn({ isLoading: true }));

    renderList();

    expect(screen.getByRole("status")).toBeInTheDocument();
  });

  it("shows an error message when the request fails", () => {
    mockUseConversations.mockReturnValue(baseHookReturn({ isError: true }));

    renderList();

    expect(
      screen.getByText(/could not load conversations/i)
    ).toBeInTheDocument();
  });

  it("shows the connect-channel prompt when empty and not anonymous", () => {
    mockUseConversations.mockReturnValue(
      baseHookReturn({ data: { pages: [{ items: [], hasMore: false }] } })
    );

    renderList();

    expect(screen.getByText(/no conversations yet/i)).toBeInTheDocument();
    expect(
      screen.getByRole("link", { name: /connect a channel/i })
    ).toBeInTheDocument();
  });

  it("renders conversation items", () => {
    mockUseConversations.mockReturnValue(
      baseHookReturn({
        data: { pages: [{ items: [mockConversation], hasMore: false }] },
      })
    );

    renderList();

    expect(screen.getByText("Ada Lovelace")).toBeInTheDocument();
  });

  it("renders the Conversations heading", () => {
    mockUseConversations.mockReturnValue(baseHookReturn());

    renderList();

    expect(
      screen.getByRole("heading", { name: /conversations/i })
    ).toBeInTheDocument();
  });
});
