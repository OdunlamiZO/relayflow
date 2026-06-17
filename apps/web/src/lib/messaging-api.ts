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
  telegramLinked: boolean;
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
  lockedByAiAgent: boolean;
  assigneeId: string | null;
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

export type UpdateWorkspaceRequest = {
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
  assigneeId?: string;
};

export type UpdateConversationRequest = {
  status: ConversationStatus;
};

export type UpdateAssigneeRequest = {
  assigneeId: string | null;
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
  | "AI_AGENT_WRITE"
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

export type WorkflowRunStatus = "RUNNING" | "WAITING" | "COMPLETED" | "FAILED";
export type WorkflowRunStepStatus = "COMPLETED" | "FAILED" | "SKIPPED";

export type WorkflowRun = {
  id: string;
  workflowDefinitionId: string;
  conversationId: string;
  status: WorkflowRunStatus;
  startedAt: string;
  finishedAt: string | null;
  errorMessage: string | null;
  waitingAtNodeId: string | null;
};

export type WorkflowRunStep = {
  id: string;
  nodeId: string;
  nodeType: string;
  status: WorkflowRunStepStatus;
  inputSnapshot: JsonObject;
  outputSnapshot: JsonObject;
  errorMessage: string | null;
  startedAt: string;
  finishedAt: string | null;
  durationMs: number | null;
};

export type WorkflowRunDetail = WorkflowRun & {
  steps: WorkflowRunStep[];
};

export type Plan = "FREE" | "PRO_MONTHLY" | "PRO_ANNUAL";

export type BillingInterval = "monthly" | "annual";

export type SubscriptionStatus =
  | "ACTIVE"
  | "CANCELLATION_SCHEDULED"
  | "PAST_DUE"
  | "CANCELLED";

export type Subscription = {
  plan: Plan;
  status: SubscriptionStatus;
  /** Maximum channel accounts allowed. null means unlimited. */
  maxChannelAccounts: number | null;
  /** Maximum workflow definitions allowed. null means unlimited. */
  maxWorkflows: number | null;
  /** Maximum workspace members allowed. null means unlimited. */
  maxMembersPerWorkspace: number | null;
  /** End of current billing period. null for the FREE plan. */
  currentPeriodEnd: string | null;
  /** Monthly price in Nigerian Naira. null for the FREE plan. */
  priceNgn: number | null;
  /** Billing interval. null for the FREE plan. */
  billingInterval: BillingInterval | null;
  /** True when there is a paid plan this workspace can upgrade to right now. Settings always shows an upgrade section when true. */
  upgradeAvailable: boolean;
  /** True only when on FREE and upgradeAvailable is true. Paid-plan workspaces with a higher tier show the upgrade prompt in settings only. */
  upgradeRecommended: boolean;
  /**
   * Number of channel accounts that were disabled when the workspace was downgraded to FREE.
   * null when there is no downgrade notice to show.
   */
  downgradeLockedChannels: number | null;
  /**
   * Number of workflow definitions that were disabled when the workspace was downgraded to FREE.
   * null when there is no downgrade notice to show.
   */
  downgradeLockedWorkflows: number | null;
};

/** A single entry from the public plan catalogue — GET /plans. */
export type PlanInfo = {
  plan: Plan;
  /** Maximum channel accounts allowed. null means unlimited. */
  maxChannelAccounts: number | null;
  /** Maximum workflow definitions allowed. null means unlimited. */
  maxWorkflows: number | null;
  /** Maximum workspace members allowed. null means unlimited. */
  maxMembersPerWorkspace: number | null;
  /** Monthly price in Nigerian Naira. null for the FREE plan. */
  priceNgn: number | null;
  /** Billing interval. null for the FREE plan. */
  billingInterval: BillingInterval | null;
  /** True when this plan is currently available for purchase. Always false for FREE. */
  upgradeAvailable: boolean;
};

export type AutonomyCeiling = "DRAFT_ONLY" | "AUTO_SEND";

export type KnowledgeEntry = {
  question: string;
  answer: string;
};

export type WorkflowMapping = {
  workflowId: string;
  name: string;
  triggerDescription: string;
};

export type AiAgentConfiguration = {
  id: string;
  workspaceId: string;
  name: string;
  enabled: boolean;
  autonomyCeiling: AutonomyCeiling;
  instructions: string | null;
  knowledgeBase: KnowledgeEntry[];
  escalationKeywords: string[];
  workflowMappings: WorkflowMapping[];
  createdAt: string;
  updatedAt: string;
};

export type UpdateAiAgentConfigurationRequest = {
  name?: string;
  enabled: boolean;
  autonomyCeiling: AutonomyCeiling;
  instructions?: string | null;
  knowledgeBase?: KnowledgeEntry[];
  escalationKeywords?: string[];
  workflowMappings?: WorkflowMapping[];
};

export type ConversationAiDraft = {
  id: string;
  workspaceId: string;
  conversationId: string;
  invocationLogId: string | null;
  proposedReply: string;
  suggestedActions: string[];
  createdAt: string;
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
    return this.request<Workspace[]>("/workspaces");
  }

  createWorkspace(request: CreateWorkspaceRequest) {
    return this.request<Workspace>("/workspaces", {
      method: "POST",
      body: request,
    });
  }

  updateWorkspace(workspaceId: string, request: UpdateWorkspaceRequest) {
    return this.request<Workspace>(
      `/workspaces/${encodeURIComponent(workspaceId)}`,
      {
        method: "PATCH",
        body: request,
      }
    );
  }

  listChannelAccounts(workspaceId: string) {
    return this.request<ChannelAccount[]>(
      `/channel-accounts?workspaceId=${encodeURIComponent(workspaceId)}`
    );
  }

  createChannelAccount(request: CreateChannelAccountRequest) {
    return this.request<ChannelAccount>("/channel-accounts", {
      method: "POST",
      body: request,
    });
  }

  deleteChannelAccount(id: string, workspaceId: string) {
    return this.request<void>(
      `/channel-accounts/${id}?workspaceId=${encodeURIComponent(workspaceId)}`,
      { method: "DELETE" }
    );
  }

  reconnectChannelAccount(id: string, workspaceId: string) {
    return this.request<ChannelAccount>(
      `/channel-accounts/${id}/reconnect?workspaceId=${encodeURIComponent(workspaceId)}`,
      { method: "POST" }
    );
  }

  listContacts(workspaceId: string, page = 0, size = 50) {
    return this.request<PageResponse<Contact>>(
      `/contacts?workspaceId=${encodeURIComponent(workspaceId)}&page=${page}&size=${size}`
    );
  }

  createContact(request: CreateContactRequest) {
    return this.request<Contact>("/contacts", {
      method: "POST",
      body: request,
    });
  }

  getContact(id: string, workspaceId: string) {
    return this.request<ContactDetail>(
      `/contacts/${id}?workspaceId=${encodeURIComponent(workspaceId)}`
    );
  }

  mergeContact(
    targetId: string,
    workspaceId: string,
    request: MergeContactRequest
  ) {
    return this.request<Contact>(
      `/contacts/${targetId}/merge?workspaceId=${encodeURIComponent(workspaceId)}`,
      { method: "POST", body: request }
    );
  }

  deleteContact(id: string, workspaceId: string) {
    return this.request<void>(
      `/contacts/${id}?workspaceId=${encodeURIComponent(workspaceId)}`,
      { method: "DELETE" }
    );
  }

  createExternalIdentity(request: CreateExternalIdentityRequest) {
    return this.request<ExternalIdentity>("/external-identities", {
      method: "POST",
      body: request,
    });
  }

  createConversation(request: CreateConversationRequest) {
    return this.request<Conversation>("/conversations", {
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
    let url = `/conversations?workspaceId=${encodeURIComponent(workspaceId)}&page=${page}&size=${size}`;

    if (contactId) {
      url += `&contactId=${encodeURIComponent(contactId)}`;
    }

    return this.request<PageResponse<Conversation>>(url);
  }

  getConversation(workspaceId: string, conversationId: string) {
    return this.request<Conversation>(
      `/conversations/${conversationId}?workspaceId=${encodeURIComponent(workspaceId)}`
    );
  }

  updateConversation(
    workspaceId: string,
    conversationId: string,
    request: UpdateConversationRequest
  ) {
    return this.request<Conversation>(
      `/conversations/${conversationId}?workspaceId=${encodeURIComponent(workspaceId)}`,
      { method: "PATCH", body: request }
    );
  }

  updateConversationAssignee(
    workspaceId: string,
    conversationId: string,
    request: UpdateAssigneeRequest
  ) {
    return this.request<Conversation>(
      `/conversations/${conversationId}/assignee?workspaceId=${encodeURIComponent(workspaceId)}`,
      { method: "PATCH", body: request }
    );
  }

  listMessages(
    workspaceId: string,
    conversationId: string,
    params?: { before?: string; limit?: number }
  ) {
    let url = `/conversations/${conversationId}/messages?workspaceId=${encodeURIComponent(workspaceId)}`;

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
      `/conversations/${conversationId}/messages?workspaceId=${encodeURIComponent(
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
      `/workspaces/${encodeURIComponent(workspaceId)}/invites`
    );
  }

  createInvite(workspaceId: string, request: CreateInviteRequest) {
    return this.request<WorkspaceInvite>(
      `/workspaces/${encodeURIComponent(workspaceId)}/invites`,
      { method: "POST", body: request }
    );
  }

  revokeInvite(workspaceId: string, inviteId: string) {
    return this.request<void>(
      `/workspaces/${encodeURIComponent(workspaceId)}/invites/${encodeURIComponent(inviteId)}`,
      { method: "DELETE" }
    );
  }

  getInvitePreview(token: string) {
    return this.request<InvitePreview>(`/invites/${encodeURIComponent(token)}`);
  }

  acceptInvite(token: string) {
    return this.request<{ workspaceId: string }>(
      `/invites/${encodeURIComponent(token)}/accept`,
      {
        method: "POST",
      }
    );
  }

  listMembers(workspaceId: string) {
    return this.request<WorkspaceMember[]>(
      `/workspaces/${encodeURIComponent(workspaceId)}/members`
    );
  }

  inviteMember(workspaceId: string, request: InviteMemberRequest) {
    return this.request<WorkspaceMember>(
      `/workspaces/${encodeURIComponent(workspaceId)}/members`,
      { method: "POST", body: request }
    );
  }

  updateMember(
    workspaceId: string,
    memberId: string,
    request: UpdateMemberRequest
  ) {
    return this.request<WorkspaceMember>(
      `/workspaces/${encodeURIComponent(workspaceId)}/members/${encodeURIComponent(memberId)}`,
      { method: "PATCH", body: request }
    );
  }

  removeMember(workspaceId: string, memberId: string) {
    return this.request<void>(
      `/workspaces/${encodeURIComponent(workspaceId)}/members/${encodeURIComponent(memberId)}`,
      { method: "DELETE" }
    );
  }

  listWorkflows(workspaceId: string) {
    return this.request<WorkflowDefinition[]>(
      `/workflows?workspaceId=${encodeURIComponent(workspaceId)}`
    );
  }

  createWorkflow(request: CreateWorkflowRequest) {
    return this.request<WorkflowDefinition>("/workflows", {
      method: "POST",
      body: request,
    });
  }

  getWorkflow(id: string, workspaceId: string) {
    return this.request<WorkflowDefinition>(
      `/workflows/${id}?workspaceId=${encodeURIComponent(workspaceId)}`
    );
  }

  updateWorkflow(
    id: string,
    workspaceId: string,
    request: UpdateWorkflowRequest
  ) {
    return this.request<WorkflowDefinition>(
      `/workflows/${id}?workspaceId=${encodeURIComponent(workspaceId)}`,
      { method: "PATCH", body: request }
    );
  }

  listWorkflowRuns(
    workflowId: string,
    workspaceId: string,
    page = 0,
    size = 30
  ) {
    return this.request<PageResponse<WorkflowRun>>(
      `/workflows/${workflowId}/runs?workspaceId=${encodeURIComponent(workspaceId)}&page=${page}&size=${size}`
    );
  }

  getWorkflowRun(workflowId: string, runId: string, workspaceId: string) {
    return this.request<WorkflowRunDetail>(
      `/workflows/${workflowId}/runs/${runId}?workspaceId=${encodeURIComponent(workspaceId)}`
    );
  }

  deleteWorkflow(id: string, workspaceId: string) {
    return this.request<void>(
      `/workflows/${id}?workspaceId=${encodeURIComponent(workspaceId)}`,
      { method: "DELETE" }
    );
  }

  // ── API Keys ──────────────────────────────────────────────────────────────

  listApiKeys(workspaceId: string) {
    return this.request<ApiKey[]>(
      `/workspaces/${encodeURIComponent(workspaceId)}/api-keys`
    );
  }

  createApiKey(workspaceId: string, request: CreateApiKeyRequest) {
    return this.request<CreateApiKeyResponse>(
      `/workspaces/${encodeURIComponent(workspaceId)}/api-keys`,
      { method: "POST", body: request }
    );
  }

  revokeApiKey(workspaceId: string, keyId: string) {
    return this.request<void>(
      `/workspaces/${encodeURIComponent(workspaceId)}/api-keys/${encodeURIComponent(keyId)}`,
      { method: "DELETE" }
    );
  }

  // ── Webhooks ──────────────────────────────────────────────────────────────

  getWebhook(workspaceId: string) {
    return this.request<WebhookConfig>(
      `/workspaces/${encodeURIComponent(workspaceId)}/webhook`
    );
  }

  saveWebhook(workspaceId: string, request: SaveWebhookRequest) {
    return this.request<WebhookConfig>(
      `/workspaces/${encodeURIComponent(workspaceId)}/webhook`,
      { method: "PUT", body: request }
    );
  }

  deleteWebhook(workspaceId: string) {
    return this.request<void>(
      `/workspaces/${encodeURIComponent(workspaceId)}/webhook`,
      { method: "DELETE" }
    );
  }

  rotateWebhookSecret(workspaceId: string) {
    return this.request<RotateWebhookSecretResponse>(
      `/workspaces/${encodeURIComponent(workspaceId)}/webhook/rotate-secret`,
      { method: "POST" }
    );
  }

  // ── Subscription ──────────────────────────────────────────────────────────

  /** Returns the public plan catalogue with live limits, pricing, and availability. */
  getPlans() {
    return this.request<PlanInfo[]>("/plans");
  }

  getSubscription(workspaceId: string) {
    return this.request<Subscription>(
      `/workspaces/${encodeURIComponent(workspaceId)}/subscription`
    );
  }

  /**
   * Initializes a checkout session for the given plan. Returns a Paystack authorization URL;
   * redirect the user there to complete payment.
   */
  startCheckout(workspaceId: string, plan: Plan) {
    return this.request<{ authorizationUrl: string }>(
      `/workspaces/${encodeURIComponent(workspaceId)}/subscription/checkout`,
      { method: "POST", body: { plan } }
    );
  }

  /**
   * Schedules cancellation of the workspace subscription at the end of the current billing period.
   * The workspace retains PRO access until currentPeriodEnd.
   */
  cancelSubscription(workspaceId: string) {
    return this.request<void>(
      `/workspaces/${encodeURIComponent(workspaceId)}/subscription`,
      { method: "DELETE" }
    );
  }

  getAiAgentConfiguration(workspaceId: string) {
    return this.request<AiAgentConfiguration>(
      `/workspaces/${encodeURIComponent(workspaceId)}/ai-agent-config`
    );
  }

  updateAiAgentConfiguration(
    workspaceId: string,
    request: UpdateAiAgentConfigurationRequest
  ) {
    return this.request<AiAgentConfiguration>(
      `/workspaces/${encodeURIComponent(workspaceId)}/ai-agent-config`,
      { method: "PUT", body: request }
    );
  }

  getConversationAiDraft(workspaceId: string, conversationId: string) {
    return this.request<ConversationAiDraft>(
      `/workspaces/${encodeURIComponent(workspaceId)}/conversations/${encodeURIComponent(conversationId)}/ai-draft`
    );
  }

  sendAiDraft(workspaceId: string, conversationId: string) {
    return this.request<Message>(
      `/workspaces/${encodeURIComponent(workspaceId)}/conversations/${encodeURIComponent(conversationId)}/ai-draft/send`,
      { method: "POST" }
    );
  }

  discardAiDraft(workspaceId: string, conversationId: string) {
    return this.request<void>(
      `/workspaces/${encodeURIComponent(workspaceId)}/conversations/${encodeURIComponent(conversationId)}/ai-draft`,
      { method: "DELETE" }
    );
  }

  triggerWorkflowFromDraft(
    workspaceId: string,
    conversationId: string,
    workflowId: string
  ) {
    return this.request<void>(
      `/workspaces/${encodeURIComponent(workspaceId)}/conversations/${encodeURIComponent(conversationId)}/ai-draft/trigger-workflow/${encodeURIComponent(workflowId)}`,
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
