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
  shared: boolean;
  metadata: JsonObject;
  createdAt: string;
};

export type Contact = {
  id: string;
  workspaceId: string;
  displayName: string | null;
  createdAt: string;
  identities: ExternalIdentity[];
};

export type ExternalIdentity = {
  id: string;
  workspaceId: string;
  contactId: string;
  channelAccountId: string | null;
  provider: ChannelProvider;
  externalUserId: string;
  externalConversationId: string | null;
  username: string | null;
  rawProfile: JsonObject;
  createdAt: string;
};

/** Alias kept for components that import ContactDetail by name. */
export type ContactDetail = Contact;

export type Conversation = {
  id: string;
  workspaceId: string;
  contactId: string;
  contactDisplayName: string | null;
  channelAccountId: string;
  channelProvider: ChannelProvider;
  channelAccountName: string;
  status: ConversationStatus;
  lockedByWorkflow: boolean;
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

export type MergeContactRequest = {
  sourceContactId: string;
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

export type UpdateConversationRequest = {
  status: ConversationStatus;
};

export type CreateMessageRequest = {
  direction: MessageDirection;
  senderType: MessageSenderType;
  text?: string;
  providerMessageId?: string;
  rawPayload?: JsonObject;
};

export type WorkspacePermission =
  | "INBOX"
  | "CONTACTS_DELETE"
  | "WORKFLOWS_WRITE"
  | "WORKFLOWS_DELETE"
  | "CHANNELS_WRITE"
  | "CHANNELS_DELETE"
  | "API_KEYS_WRITE"
  | "WEBHOOKS_WRITE";
export type WorkspaceRole = "OWNER" | "MEMBER";
export type InviteStatus = "PENDING" | "ACCEPTED" | "REVOKED" | "EXPIRED";

export type WorkspaceMember = {
  id: string;
  userId: string;
  email: string | null;
  displayName: string | null;
  avatarUrl: string | null;
  role: WorkspaceRole;
  permissions: WorkspacePermission[];
  joinedAt: string;
};

export type InviteMemberRequest = {
  email: string;
  permissions: WorkspacePermission[];
};

export type UpdateMemberRequest = {
  role?: WorkspaceRole;
  permissions: WorkspacePermission[];
};

export type WorkspaceInvite = {
  id: string;
  email: string;
  inviterName: string;
  permissions: WorkspacePermission[];
  status: InviteStatus;
  createdAt: string;
  expiresAt: string;
};

export type CreateInviteRequest = {
  email: string;
  permissions: WorkspacePermission[];
};

export type InvitePreview = {
  inviteId: string;
  workspaceId: string;
  workspaceName: string;
  inviterName: string;
  email: string;
  permissions: WorkspacePermission[];
  status: InviteStatus;
  expiresAt: string;
};

export type WebhookEventType = "CONTACT_CREATED";

export type ApiKey = {
  id: string;
  name: string;
  keyPrefix: string;
  createdAt: string;
  lastUsedAt: string | null;
  revokedAt: string | null;
  expiresAt: string | null;
};

export type CreateApiKeyRequest = {
  name: string;
  /** ISO-8601 timestamp. Null means no expiry. */
  expiresAt?: string | null;
};

export type CreateApiKeyResponse = {
  id: string;
  name: string;
  keyPrefix: string;
  createdAt: string;
  expiresAt: string | null;
  /** Full plaintext key — shown once, never stored. */
  key: string;
};

export type WebhookConfig = {
  id: string;
  workspaceId: string;
  url: string;
  enabled: boolean;
  events: WebhookEventType[];
  createdAt: string;
  updatedAt: string;
};

export type SaveWebhookRequest = {
  url: string;
  /** Plaintext HMAC secret. Required on create; omit to keep existing on update. */
  secret?: string | null;
  enabled: boolean;
  events: WebhookEventType[];
};

export type RotateWebhookSecretResponse = {
  /** New plaintext HMAC secret — shown once, never stored. */
  secret: string;
};

export type WorkflowDefinition = {
  id: string;
  workspaceId: string;
  name: string;
  enabled: boolean;
  draftGraph: JsonObject;
  createdAt: string;
  updatedAt: string;
};

export type CreateWorkflowRequest = {
  workspaceId: string;
  name: string;
};

export type UpdateWorkflowRequest = {
  name?: string;
  enabled?: boolean;
  draftGraph?: JsonObject;
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
  method?: "GET" | "POST" | "PUT" | "PATCH" | "DELETE";
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

  listContacts(workspaceId: string, page = 0, size = 50) {
    return this.request<PageResponse<Contact>>(
      `/api/contacts?workspaceId=${encodeURIComponent(workspaceId)}&page=${page}&size=${size}`
    );
  }

  createContact(request: CreateContactRequest) {
    return this.request<Contact>("/api/contacts", {
      method: "POST",
      body: request,
    });
  }

  getContact(id: string, workspaceId: string) {
    return this.request<ContactDetail>(
      `/api/contacts/${id}?workspaceId=${encodeURIComponent(workspaceId)}`
    );
  }

  mergeContact(
    targetId: string,
    workspaceId: string,
    request: MergeContactRequest
  ) {
    return this.request<Contact>(
      `/api/contacts/${targetId}/merge?workspaceId=${encodeURIComponent(workspaceId)}`,
      { method: "POST", body: request }
    );
  }

  deleteContact(id: string, workspaceId: string) {
    return this.request<void>(
      `/api/contacts/${id}?workspaceId=${encodeURIComponent(workspaceId)}`,
      { method: "DELETE" }
    );
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

  listConversations(
    workspaceId: string,
    page = 0,
    size = 30,
    contactId?: string
  ) {
    let url = `/api/conversations?workspaceId=${encodeURIComponent(workspaceId)}&page=${page}&size=${size}`;

    if (contactId) {
      url += `&contactId=${encodeURIComponent(contactId)}`;
    }

    return this.request<PageResponse<Conversation>>(url);
  }

  getConversation(workspaceId: string, conversationId: string) {
    return this.request<Conversation>(
      `/api/conversations/${conversationId}?workspaceId=${encodeURIComponent(workspaceId)}`
    );
  }

  updateConversation(
    workspaceId: string,
    conversationId: string,
    request: UpdateConversationRequest
  ) {
    return this.request<Conversation>(
      `/api/conversations/${conversationId}?workspaceId=${encodeURIComponent(workspaceId)}`,
      { method: "PATCH", body: request }
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

  listInvites(workspaceId: string) {
    return this.request<WorkspaceInvite[]>(
      `/api/workspaces/${encodeURIComponent(workspaceId)}/invites`
    );
  }

  createInvite(workspaceId: string, request: CreateInviteRequest) {
    return this.request<WorkspaceInvite>(
      `/api/workspaces/${encodeURIComponent(workspaceId)}/invites`,
      { method: "POST", body: request }
    );
  }

  revokeInvite(workspaceId: string, inviteId: string) {
    return this.request<void>(
      `/api/workspaces/${encodeURIComponent(workspaceId)}/invites/${encodeURIComponent(inviteId)}`,
      { method: "DELETE" }
    );
  }

  getInvitePreview(token: string) {
    return this.request<InvitePreview>(
      `/api/invites/${encodeURIComponent(token)}`
    );
  }

  acceptInvite(token: string) {
    return this.request<{ workspaceId: string }>(
      `/api/invites/${encodeURIComponent(token)}/accept`,
      {
        method: "POST",
      }
    );
  }

  listMembers(workspaceId: string) {
    return this.request<WorkspaceMember[]>(
      `/api/workspaces/${encodeURIComponent(workspaceId)}/members`
    );
  }

  inviteMember(workspaceId: string, request: InviteMemberRequest) {
    return this.request<WorkspaceMember>(
      `/api/workspaces/${encodeURIComponent(workspaceId)}/members`,
      { method: "POST", body: request }
    );
  }

  updateMember(
    workspaceId: string,
    memberId: string,
    request: UpdateMemberRequest
  ) {
    return this.request<WorkspaceMember>(
      `/api/workspaces/${encodeURIComponent(workspaceId)}/members/${encodeURIComponent(memberId)}`,
      { method: "PATCH", body: request }
    );
  }

  removeMember(workspaceId: string, memberId: string) {
    return this.request<void>(
      `/api/workspaces/${encodeURIComponent(workspaceId)}/members/${encodeURIComponent(memberId)}`,
      { method: "DELETE" }
    );
  }

  listWorkflows(workspaceId: string) {
    return this.request<WorkflowDefinition[]>(
      `/api/workflows?workspaceId=${encodeURIComponent(workspaceId)}`
    );
  }

  createWorkflow(request: CreateWorkflowRequest) {
    return this.request<WorkflowDefinition>("/api/workflows", {
      method: "POST",
      body: request,
    });
  }

  getWorkflow(id: string, workspaceId: string) {
    return this.request<WorkflowDefinition>(
      `/api/workflows/${id}?workspaceId=${encodeURIComponent(workspaceId)}`
    );
  }

  updateWorkflow(
    id: string,
    workspaceId: string,
    request: UpdateWorkflowRequest
  ) {
    return this.request<WorkflowDefinition>(
      `/api/workflows/${id}?workspaceId=${encodeURIComponent(workspaceId)}`,
      { method: "PATCH", body: request }
    );
  }

  deleteWorkflow(id: string, workspaceId: string) {
    return this.request<void>(
      `/api/workflows/${id}?workspaceId=${encodeURIComponent(workspaceId)}`,
      { method: "DELETE" }
    );
  }

  // ── API Keys ──────────────────────────────────────────────────────────────

  listApiKeys(workspaceId: string) {
    return this.request<ApiKey[]>(
      `/api/workspaces/${encodeURIComponent(workspaceId)}/api-keys`
    );
  }

  createApiKey(workspaceId: string, request: CreateApiKeyRequest) {
    return this.request<CreateApiKeyResponse>(
      `/api/workspaces/${encodeURIComponent(workspaceId)}/api-keys`,
      { method: "POST", body: request }
    );
  }

  revokeApiKey(workspaceId: string, keyId: string) {
    return this.request<void>(
      `/api/workspaces/${encodeURIComponent(workspaceId)}/api-keys/${encodeURIComponent(keyId)}`,
      { method: "DELETE" }
    );
  }

  // ── Webhooks ──────────────────────────────────────────────────────────────

  getWebhook(workspaceId: string) {
    return this.request<WebhookConfig>(
      `/api/workspaces/${encodeURIComponent(workspaceId)}/webhook`
    );
  }

  saveWebhook(workspaceId: string, request: SaveWebhookRequest) {
    return this.request<WebhookConfig>(
      `/api/workspaces/${encodeURIComponent(workspaceId)}/webhook`,
      { method: "PUT", body: request }
    );
  }

  deleteWebhook(workspaceId: string) {
    return this.request<void>(
      `/api/workspaces/${encodeURIComponent(workspaceId)}/webhook`,
      { method: "DELETE" }
    );
  }

  rotateWebhookSecret(workspaceId: string) {
    return this.request<RotateWebhookSecretResponse>(
      `/api/workspaces/${encodeURIComponent(workspaceId)}/webhook/rotate-secret`,
      { method: "POST" }
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
