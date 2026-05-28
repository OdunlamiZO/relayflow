export type ChannelProvider =
  | "TELEGRAM"
  | "WHATSAPP"
  | "INSTAGRAM"
  | "MESSENGER"
  | "WEBCHAT"
  | "EMAIL"
  | "SMS";

export type ChannelAccountStatus = "ACTIVE" | "DISABLED";
export type ConversationStatus = "OPEN" | "CLOSED" | "PENDING";
export type MessageDirection = "INBOUND" | "OUTBOUND";
export type MessageSenderType = "CONTACT" | "AGENT" | "SYSTEM" | "WORKFLOW";
export type JsonObject = Record<string, unknown>;

export type Workspace = {
  id: string;
  name: string;
  createdAt: string;
};

export type ChannelAccount = {
  id: string;
  workspaceId: string;
  provider: ChannelProvider;
  name: string;
  status: ChannelAccountStatus;
  metadata: JsonObject;
  createdAt: string;
};

export type Contact = {
  id: string;
  workspaceId: string;
  displayName: string | null;
  createdAt: string;
};

export type ExternalIdentity = {
  id: string;
  workspaceId: string;
  contactId: string;
  provider: ChannelProvider;
  externalUserId: string;
  externalConversationId: string | null;
  username: string | null;
  rawProfile: JsonObject;
  createdAt: string;
};

export type Conversation = {
  id: string;
  workspaceId: string;
  contactId: string;
  contactDisplayName: string | null;
  channelAccountId: string;
  channelProvider: ChannelProvider;
  channelAccountName: string;
  status: ConversationStatus;
  assignedUserId: string | null;
  lastMessageAt: string | null;
  createdAt: string;
};

export type Message = {
  id: string;
  workspaceId: string;
  conversationId: string;
  direction: MessageDirection;
  senderType: MessageSenderType;
  text: string | null;
  providerMessageId: string | null;
  rawPayload: JsonObject;
  createdAt: string;
};

export type CreateWorkspaceRequest = {
  name: string;
};

export type CreateChannelAccountRequest = {
  workspaceId: string;
  provider: ChannelProvider;
  name: string;
  status?: ChannelAccountStatus;
  encryptedCredentials?: string;
  metadata?: JsonObject;
};

export type CreateContactRequest = {
  workspaceId: string;
  displayName?: string;
};

export type CreateExternalIdentityRequest = {
  workspaceId: string;
  contactId: string;
  provider: ChannelProvider;
  externalUserId: string;
  externalConversationId?: string;
  username?: string;
  rawProfile?: JsonObject;
};

export type CreateConversationRequest = {
  workspaceId: string;
  contactId: string;
  channelAccountId: string;
  status?: ConversationStatus;
  assignedUserId?: string;
};

export type CreateMessageRequest = {
  direction: MessageDirection;
  senderType: MessageSenderType;
  text?: string;
  providerMessageId?: string;
  rawPayload?: JsonObject;
};

export type PageResponse<T> = {
  items: T[];
  hasMore: boolean;
  /** Opaque cursor string. Present when {@code hasMore} is true and the endpoint is cursor-based. */
  nextCursor: string | null;
};

export class ApiError extends Error {
  readonly status: number;
  readonly details: unknown;

  constructor(status: number, message: string, details: unknown) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.details = details;
  }
}

type RequestOptions = {
  method?: "GET" | "POST" | "DELETE";
  body?: unknown;
};

const defaultBaseUrl =
  process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";

export class MessagingApiClient {
  private readonly baseUrl: string;

  constructor(baseUrl = defaultBaseUrl) {
    this.baseUrl = baseUrl.replace(/\/$/, "");
  }

  listWorkspaces() {
    return this.request<Workspace[]>("/api/workspaces");
  }

  createWorkspace(request: CreateWorkspaceRequest) {
    return this.request<Workspace>("/api/workspaces", {
      method: "POST",
      body: request,
    });
  }

  listChannelAccounts(workspaceId: string) {
    return this.request<ChannelAccount[]>(
      `/api/channel-accounts?workspaceId=${encodeURIComponent(workspaceId)}`
    );
  }

  createChannelAccount(request: CreateChannelAccountRequest) {
    return this.request<ChannelAccount>("/api/channel-accounts", {
      method: "POST",
      body: request,
    });
  }

  deleteChannelAccount(id: string, workspaceId: string) {
    return this.request<void>(
      `/api/channel-accounts/${id}?workspaceId=${encodeURIComponent(workspaceId)}`,
      { method: "DELETE" }
    );
  }

  reconnectChannelAccount(id: string, workspaceId: string) {
    return this.request<ChannelAccount>(
      `/api/channel-accounts/${id}/reconnect?workspaceId=${encodeURIComponent(workspaceId)}`,
      { method: "POST" }
    );
  }

  createContact(request: CreateContactRequest) {
    return this.request<Contact>("/api/contacts", {
      method: "POST",
      body: request,
    });
  }

  createExternalIdentity(request: CreateExternalIdentityRequest) {
    return this.request<ExternalIdentity>("/api/external-identities", {
      method: "POST",
      body: request,
    });
  }

  createConversation(request: CreateConversationRequest) {
    return this.request<Conversation>("/api/conversations", {
      method: "POST",
      body: request,
    });
  }

  listConversations(workspaceId: string, page = 0, size = 30) {
    return this.request<PageResponse<Conversation>>(
      `/api/conversations?workspaceId=${encodeURIComponent(workspaceId)}&page=${page}&size=${size}`
    );
  }

  getConversation(workspaceId: string, conversationId: string) {
    return this.request<Conversation>(
      `/api/conversations/${conversationId}?workspaceId=${encodeURIComponent(workspaceId)}`
    );
  }

  listMessages(
    workspaceId: string,
    conversationId: string,
    params?: { before?: string; limit?: number }
  ) {
    let url = `/api/conversations/${conversationId}/messages?workspaceId=${encodeURIComponent(workspaceId)}`;

    if (params?.before) {
      url += `&before=${encodeURIComponent(params.before)}`;
    }

    if (params?.limit !== undefined) {
      url += `&limit=${params.limit}`;
    }

    return this.request<PageResponse<Message>>(url);
  }

  createMessage(
    workspaceId: string,
    conversationId: string,
    request: CreateMessageRequest
  ) {
    return this.request<Message>(
      `/api/conversations/${conversationId}/messages?workspaceId=${encodeURIComponent(
        workspaceId
      )}`,
      {
        method: "POST",
        body: request,
      }
    );
  }

  private async request<T>(path: string, options: RequestOptions = {}) {
    const response = await fetch(`${this.baseUrl}${path}`, {
      method: options.method ?? "GET",
      credentials: "include",
      headers:
        options.body === undefined
          ? undefined
          : {
              "Content-Type": "application/json",
            },
      body:
        options.body === undefined ? undefined : JSON.stringify(options.body),
    });

    if (!response.ok) {
      if (response.status === 401 && typeof window !== "undefined") {
        window.location.href = "/login";
      }

      const details = await readResponseBody(response);

      throw new ApiError(
        response.status,
        errorMessage(response.status, details),
        details
      );
    }

    if (
      response.status === 204 ||
      response.headers.get("content-length") === "0"
    ) {
      return undefined as T;
    }

    return (await response.json()) as T;
  }
}

export const messagingApi = new MessagingApiClient();

async function readResponseBody(response: Response) {
  const contentType = response.headers.get("content-type") ?? "";
  if (contentType.includes("application/json")) {
    return response.json();
  }
  return response.text();
}

function errorMessage(status: number, details: unknown) {
  if (
    details &&
    typeof details === "object" &&
    "message" in details &&
    typeof details.message === "string"
  ) {
    return details.message;
  }
  return `Request failed with status ${status}`;
}
