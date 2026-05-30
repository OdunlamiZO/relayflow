# RelayFlow Codebase Components

This document explains the main named parts in the codebase: classes, records, enums, React components, hooks, exported functions, important constants, and contract schemas. It is organized by responsibility rather than folder names.

It intentionally focuses on components that define behavior or shared contracts. Tiny local variables inside a function are not listed unless they carry architectural meaning.

## Backend Application And Configuration

### `RelayFlowApiApplication`

The Spring Boot entry point. It starts the API process and enables Spring Boot component scanning for controllers, services, repositories, schedulers, and configuration.

We need it because every backend capability hangs off the Spring application context.

### `SecurityConfiguration`

Defines authentication and request security behavior.

Important beans:

- `PasswordEncoder`: BCrypt password hashing for email/password users.
- `AuthenticationManager`: authenticates email/password credentials through `EmailPasswordUserDetailsService`.
- `HttpSessionSecurityContextRepository`: stores authenticated sessions in the servlet session.
- `SecurityFilterChain`: permits public auth, health, Telegram webhook, Swagger, and docs paths; protects the rest.
- `CorsConfigurationSource`: allows credentialed frontend requests from `relayflow.web.base-url`.

We need it to make browser sessions, Google OAuth, email login, and protected API routes work consistently.

### `WebConfiguration`

Defines web-level backend infrastructure, including the `RestTemplate` bean used by Telegram webhook registration and Telegram API calls.

We need it so integrations can share a configured HTTP client instead of each service constructing its own.

### `CredentialEncryptionService`

Encrypts and decrypts channel credentials such as Telegram bot tokens.

Important methods:

- `encrypt(String plaintext)`: stores sensitive channel credentials safely.
- `decrypt(String ciphertext)`: recovers credentials only when needed for outbound provider calls or webhook registration.

We need it because channel credentials are product-critical secrets and should not be persisted as plaintext.

## Authentication Backend

### `AuthenticationController`

HTTP controller for auth routes.

Endpoints:

- `me`: returns the current session user.
- `signup`: creates an email/password account and establishes a session.
- `login`: authenticates credentials and establishes a session.
- `guest`: creates an anonymous guest session and workspace.
- `logout`: invalidates the server session and expires the `JSESSIONID` browser cookie.

We need it as the public API boundary for session state.

### `AuthenticationService`

Business logic for authentication.

Important methods:

- `signup`: validates email uniqueness, hashes password, saves the user, and signs them in.
- `login`: authenticates through Spring Security and returns user profile state.
- `createGuestSession`: creates anonymous user, workspace, and shared Telegram channel when configured.
- `getCurrentUser`: normalizes OAuth, email, and anonymous principals into `AuthenticatedUserResponse`.
- `establishSession`: writes an authenticated security context into the session.
- `establishAnonSession`: creates a session for anonymous users.

We need it to keep controller code thin and centralize session/user lifecycle rules.

### `EmailPasswordUserDetailsService`

Loads RelayFlow users by email for Spring Security's DAO authentication provider.

We need it so the Spring authentication manager can validate email/password login against the database.

### `OAuth2UserProvisioningService`

Extends Spring's OAuth user service to provision or update users after Google login.

We need it so Google login creates a local `User` record and returns consistent profile data.

### `SecurityUtils`

Utility component for extracting the current RelayFlow user ID and anonymous state from Spring `Authentication`.

We need it because controllers should not duplicate principal parsing logic.

### `GuestCleanupScheduler`

Scheduled component that purges expired anonymous guest data.

We need it because guest mode is temporary and should not leave permanent workspaces/messages behind.

### `User`

JPA entity mapped to `users`.

Important fields:

- `id`: UUID primary key.
- `email`: login identifier for email users and generated identifier for guests.
- `displayName`, `avatarUrl`: profile fields.
- `provider`, `providerSubject`: tells whether the user came from email, Google, or anonymous guest mode.
- `passwordHash`: BCrypt hash for email users.
- `anonymous`, `lastActiveAt`, `createdAt`: guest/session lifecycle fields.

We need it as the identity anchor for workspaces and workspace membership.

### `AuthenticationProvider`

Enum of supported identity providers: email, Google, and anonymous.

We need it so user records can be interpreted correctly during login and cleanup.

### Auth DTO Records

- `SignupRequest`: user-supplied name, email, and password.
- `LoginRequest`: email and password.
- `AuthenticatedUserResponse`: normalized auth/session state for the frontend.
- `GuestSessionResponse`: workspace ID created for guest mode.

We need these records as stable API contracts between backend and frontend.

### `UserRepository`

Spring Data repository for `User`.

Important queries include lookup by email/provider subject and expired anonymous users.

We need it to keep persistence access declarative and testable.

### `ApiKeyAuthentication`

Spring Security `Authentication` implementation representing a validated workspace API key.

Important value:

- `workspaceId`: workspace authorized by the API key.

We need it so public API controllers can resolve workspace scope without a browser session.

### `ApiKeyAuthenticationFilter`

Security filter that reads `X-Api-Key`, hashes it, validates a non-revoked/non-expired workspace API key, and populates the security context.

Important behavior:

- updates `lastUsedAt` at most once per hour.
- ignores invalid keys so protected public endpoints return unauthorized.

We need it to authenticate external systems through API keys.

### `AsyncConfiguration`

Spring async executor configuration.

Important executors:

- shared async executor for workflow/background tasks.
- `webhookExecutor` for outbound webhook delivery.

We need it so workflow execution and webhook delivery do not block inbound HTTP/webhook request handling.

## Messaging Backend

### `MessagingController`

HTTP controller for workspace, member, channel account, contact, identity, conversation, and message routes.

Important methods:

- `listWorkspaces`, `createWorkspace`
- `listMembers`, `inviteMember`, `updateMember`, `removeMember`
- `listChannelAccounts`, `createChannelAccount`, `disconnectChannelAccount`, `reconnectChannelAccount`
- `listContacts`, `createContact`, `getContact`, `mergeContacts`, `deleteContact`
- `createExternalIdentity`
- `createConversation`, `listConversations`, `getConversation`, `updateConversation`
- `listMessages`, `createMessage`

We need it as the REST boundary for the inbox and channel management UI.

### `MessagingService`

Business service for messaging persistence and message creation.

Important methods:

- `createWorkspace`: creates a workspace and owner membership.
- `createGuestWorkspace`: guest-specific workspace creation behavior.
- `listWorkspaces`: lists workspaces by membership.
- `listWorkspaceMembers`, `inviteWorkspaceMember`, `updateWorkspaceMember`, `removeWorkspaceMember`: owner/member management.
- `createChannelAccount`: persists encrypted channel credentials and registers Telegram webhook.
- `createSharedBotChannelAccount`: creates a guest/shared Telegram channel account.
- `disconnectChannelAccount` / `reconnectChannelAccount`: toggles channel availability without deleting history.
- `listContacts`, `createContact`, `getContactDetail`, `mergeContacts`, `deleteContact`
- `createExternalIdentity`, `createConversation`
- `listConversations`, `getConversation`, `updateConversationStatus`, `listMessages`
- `createMessage`: stores messages, updates conversation timestamps, emits SSE events, and publishes outbound delivery events.
- `deleteWorkspace`: removes all workspace-owned data for guest cleanup.

We need it because messaging has cross-entity rules that should not live in controllers or repositories.

### `MessagingMapper`

Maps JPA entities to response DTOs.

We need it to isolate the API response shape from the persistence entity shape.

### `MessagingExceptionHandler`

Global REST exception handler.

Handles:

- `TelegramSendException` as `502`.
- `ConversationLockedException` as `409`.
- `ResourceNotFoundException` as `404`.
- `WorkflowValidationException` as `400`.
- validation errors as `400`.

We need it so frontend receives consistent `{ message, timestamp }` error responses.

### `ConversationLockedException`

Runtime exception thrown when an agent or public API client tries to send a message while an active workflow owns the conversation.

We need it to enforce workflow-driven conversations without silently dropping agent replies.

### `WorkspaceAuthorizationService`

Service that resolves the authenticated user's workspace membership and checks owner or granular permissions.

Important methods:

- `assertMember`: any workspace member can proceed.
- `assertOwner`: only owners can proceed.
- `assertPermission`: owners bypass checks; members need the requested `WorkspacePermission`.

We need it so mutating workspace-scoped endpoints can enforce ownership and delegated access.

### `ApiKeyService`

Creates, lists, revokes, hashes, and validates workspace API keys.

Important behaviors:

- returns the plaintext key only during creation.
- stores only a SHA-256 key hash and safe prefix.
- supports optional expiry and revocation.

We need it so external systems can call RelayFlow public endpoints without browser sessions.

### `ApiKeyController`

REST controller for workspace API key management.

Endpoints:

- `listApiKeys`
- `createApiKey`
- `revokeApiKey`

We need it to let authorized workspace users manage public API credentials.

### `WorkspaceInviteService`

Creates, lists, revokes, previews, and accepts workspace invites.

Important behaviors:

- stores expiring invite tokens.
- carries a permission set into accepted membership.
- requires the accepting user's email to match the invite.

We need it for controlled onboarding of additional workspace users.

### `WorkspaceInviteController`

REST controller for workspace invites.

Endpoints:

- `listInvites`
- `createInvite`
- `revokeInvite`
- `previewInvite`
- `acceptInvite`

We need it for both owner invite management and the public invite acceptance flow.

### `PublicApiController`

API-key-authenticated controller under `/public/v1`.

Endpoints:

- `listConversations`
- `getConversation`
- `listMessages`
- `sendMessage`

We need it so third-party systems can inspect conversations and send outbound replies for a workspace.

### `WebhookService`

Creates, updates, deletes, retrieves, and rotates workspace webhook configuration.

We need it to centralize webhook URL, encrypted secret, enabled state, and subscribed events.

### `WebhookDispatchService`

Asynchronous webhook delivery service.

Important behavior:

- signs payloads with `X-RelayFlow-Signature`.
- posts JSON payloads to the configured URL.
- retries failed deliveries with backoff.
- currently supports `contact.created`.

We need it to notify external systems when RelayFlow creates important records.

### `WebhookController`

REST controller for workspace webhook configuration.

Endpoints:

- `getWebhook`
- `saveWebhook`
- `deleteWebhook`
- `rotateSecret`

We need it so authorized users can manage outbound integration webhooks.

### `EmailService` And `ResendEmailService`

Email abstraction and Resend-backed implementation for transactional emails.

We need them so invite delivery can be swapped or disabled without changing invite domain logic.

### `OutboundMessageEvent`

Application event carrying a saved outbound message and its channel account.

We need it to decouple message persistence from provider delivery. The messaging service saves the message; adapters deliver it.

### `ResourceNotFoundException`

Runtime exception used when workspace-scoped data is missing or not accessible in that workspace.

We need it to avoid leaking whether records exist outside the requested workspace.

## Messaging Domain Entities And Enums

### `Workspace`

JPA entity for a company/team workspace.

We need it as the tenant boundary for conversations, channels, workflows, and members.

### `WorkspaceMember`

JPA entity linking users to workspaces with a role.

Important fields:

- `workspaceId`
- `userId`
- `role`
- `permissions`
- `joinedAt`

We need it for ownership and granular workspace authorization.

### `WorkspaceRole`

Enum for workspace roles: owner and member.

We need it so membership can become permission-aware.

### `WorkspacePermission`

Enum of granular permissions for non-owner workspace members.

Values:

- `INBOX`
- `CONTACTS_DELETE`
- `WORKFLOWS_WRITE`
- `WORKFLOWS_DELETE`
- `CHANNELS_WRITE`
- `CHANNELS_DELETE`
- `API_KEYS_WRITE`
- `WEBHOOKS_WRITE`

We need it to delegate specific capabilities without making every teammate an owner.

### `WorkspaceInvite`

JPA entity for an expiring workspace invitation.

Important fields:

- `workspaceId`
- `email`
- `invitedBy`
- `token`
- `permissions`
- `createdAt`
- `expiresAt`
- `acceptedAt`
- `revokedAt`

We need it to invite users with a defined permission set and track invite state.

### `WorkspaceApiKey`

JPA entity for workspace public API credentials.

Important fields:

- `workspaceId`
- `name`
- `keyPrefix`
- `keyHash`
- `createdBy`
- `lastUsedAt`
- `revokedAt`
- `expiresAt`

We need it to authenticate external systems while storing no plaintext API key.

### `WorkspaceWebhook`

JPA entity for a workspace's outbound webhook configuration.

Important fields:

- `workspaceId`
- `url`
- `secret`
- `enabled`
- `events`
- `createdAt`
- `updatedAt`

We need it to persist webhook delivery settings and encrypted signing secrets.

### `WebhookEventType`

Enum of outbound webhook event types.

Current value:

- `CONTACT_CREATED` maps to payload event name `contact.created`.

We need it so persisted webhook subscriptions and dispatched payload names stay aligned.

### `ChannelAccount`

JPA entity representing a connected channel, currently Telegram.

Important fields:

- `workspace`: owner workspace.
- `provider`: channel provider.
- `name`: display name.
- `status`: active or disabled.
- `encryptedCredentials`: encrypted bot token or provider credentials.
- `metadata`: provider-specific data.

We need it to route inbound/outbound messages through the correct adapter.

### `ChannelProvider`

Enum of supported/planned channels: Telegram, WhatsApp, Instagram, Messenger, web chat, email, SMS.

We need it to keep the data model channel-agnostic.

### `ChannelAccountStatus`

Enum for `ACTIVE` or `DISABLED`.

We need it to stop message flow without deleting account history.

### `Contact`

JPA entity for a customer/contact inside a workspace.

We need it to group identities and conversations around the human customer.

### `ExternalIdentity`

JPA entity linking a contact to an external provider identity, such as a Telegram user/chat.

Important fields:

- `channelAccount`
- `provider`
- `externalUserId`
- `externalConversationId`
- `username`
- `rawProfile`

We need it to map channel-specific sender IDs back to RelayFlow contacts.

### `Conversation`

JPA entity for a customer thread.

Important fields:

- `workspace`
- `contact`
- `channelAccount`
- `status`
- `lockedByWorkflow`
- `assignedUserId`
- `lastMessageAt`

We need it as the inbox unit users read, select, and reply to. `lockedByWorkflow` prevents agents from interrupting an active workflow-owned interaction.

### `ConversationStatus`

Enum for open, pending, and closed conversation states.

We need it for inbox filtering and workflow behavior such as reopening closed conversations.

### `Message`

JPA entity for inbound/outbound messages.

Important fields:

- `direction`
- `senderType`
- `text`
- `providerMessageId`
- `rawPayload`
- `createdAt`

We need it as a durable conversation timeline.

### `MessageDirection`

Enum for inbound versus outbound messages.

We need it to render messages correctly and decide whether delivery is required.

### `MessageSenderType`

Enum for contact, agent, system, and workflow messages.

We need it to distinguish human replies from workflow/system automation.

## Messaging DTO Records

- `CreateWorkspaceRequest`, `WorkspaceResponse`
- `InviteMemberRequest`, `UpdateMemberRequest`, `WorkspaceMemberResponse`
- `CreateInviteRequest`, `WorkspaceInviteResponse`, `InvitePreviewResponse`
- `CreateApiKeyRequest`, `ApiKeyResponse`, `CreateApiKeyResponse`
- `CreateChannelAccountRequest`, `ChannelAccountResponse`
- `CreateContactRequest`, `ContactResponse`, `ContactDetailResponse`, `MergeContactRequest`
- `CreateExternalIdentityRequest`, `ExternalIdentityResponse`
- `CreateConversationRequest`, `UpdateConversationRequest`, `ConversationResponse`
- `CreateMessageRequest`, `MessageResponse`
- `PageResponse<T>`: generic paginated API response.
- `ErrorResponse`: normalized API error body.

We need these records to keep frontend/backend data exchange explicit and stable.

## Messaging Repositories

- `WorkspaceRepository`
- `WorkspaceMemberRepository`
- `WorkspaceInviteRepository`
- `WorkspaceApiKeyRepository`
- `ChannelAccountRepository`
- `ContactRepository`
- `ExternalIdentityRepository`
- `ConversationRepository`
- `MessageRepository`

These are Spring Data persistence interfaces. Their custom query methods express workspace scoping, pagination, active channel filtering, conversation lookup, message cursors, and cleanup deletes.
They also support invite lookup, API key lookup by hash, member permission lookup, contact merge reassignment, and public API filters.

We need them so services do not contain SQL or persistence boilerplate.

## SSE Backend

### `SseController`

Exposes `GET /api/sse/workspace/{workspaceId}` as a text/event-stream endpoint.

We need it for real-time inbox updates without polling every second.

### `WorkspaceSseService`

Tracks workspace SSE emitters and broadcasts named events.

Important methods:

- `subscribe`: creates/registers an emitter for a workspace.
- `broadcast`: sends payloads to all active emitters in a workspace.

We need it so message and workspace changes can update open browser sessions immediately.

### `SseBroadcastEvent`

Application event containing `workspaceId`, `eventName`, and `payload`.

We need it to decouple domain transactions from SSE delivery.

## Telegram Backend

### `TelegramController`

HTTP controller for Telegram webhooks.

Endpoints:

- `telegramWebhook`: dedicated bot webhook per channel account.
- `telegramSharedWebhook`: shared guest bot webhook.

We need it as Telegram's inbound HTTP entry point.

### `TelegramAdapter`

Telegram integration service.

Important methods:

- `handleWebhook`: accepts dedicated bot updates.
- `handleSharedBotWebhook`: accepts shared bot updates.
- `processInboundMessage`: normalizes a Telegram message into contact, external identity, conversation, and message records.
- `handleSharedBotStart`: links a Telegram user to a guest workspace via `/start {workspaceId}`.
- `handleSharedBotMessage`: routes shared bot messages to the most recently linked guest workspace.
- `onOutboundMessage`: listens for outbound messages and sends them through Telegram.
- `sendTelegramMessage`: calls Telegram Bot API with retry.
- `sendTelegramMessageQuietly`: best-effort bot replies for linking/error hints.
- `createIdentity`, `createConversation`, `buildDisplayName`: helper methods for inbound normalization.

We need it to keep Telegram-specific behavior out of the channel-agnostic messaging service.

### `TelegramWebhookRegistrar`

Registers a Telegram webhook for a dedicated bot token.

We need it so workspace-owned Telegram bots can receive inbound updates automatically.

### `TelegramSendException`

Exception threw when Telegram delivery fails.

We need it so outbound message creation can roll back and surface a useful `502` to the frontend.

### Telegram DTO Records

- `TelegramWebhookPayload`
- `TelegramMessage`
- `TelegramUser`
- `TelegramChat`

We need these records to deserialize Telegram's webhook JSON into typed Java data.

## Workflow Backend

### `WorkflowController`

REST controller for workflow definition CRUD.

Important methods:

- `listWorkflows`
- `createWorkflow`
- `getWorkflow`
- `updateWorkflow`
- `deleteWorkflow`

We need it so the workflow builder UI can create, edit, publish, and delete workflow definitions.

### `WorkflowService`

Business service for workflow definitions.

Important methods:

- `listWorkflows`
- `createWorkflow`
- `getWorkflow`
- `updateWorkflow`
- `deleteWorkflow`
- `getDefinition`
- `getWorkspace`
- `toDto`

We need it to validate workspace ownership existence, apply partial updates, and validate graphs before enabling workflows.

### `WorkflowGraphValidator`

Validates workflow graph JSON before publish.

Important validation rules:

- exactly one trigger node.
- known trigger event.
- known node types.
- every node is reachable from the trigger.
- Jump To links count as reachability paths even though they are stored in node data rather than edges.
- every condition branch has an edge.
- the required node content exists.
- defined Ask Question options and "Other" branch are connected.
- Jump To nodes have a configured target, cannot target themselves, and reference an existing node.

We need it because draft graphs can be incomplete, but published workflows must be executable.

### `WorkflowValidationException`

Runtime exception for publish-time workflow validation failures.

We need it so frontend can display actionable validation errors.

## Workflow Domain Entities And Enums

### `NodeType`

Enum of workflow node type identifiers understood by the backend.

Important values:

- `TRIGGER`
- `SEND_MESSAGE`
- `CONDITION`
- `HTTP_REQUEST`
- `SET_VARIABLE`
- `END_CONVERSATION`
- `WAIT_FOR_REPLY`
- `JUMP_TO`

Important methods:

- `getValue`: returns the camelCase graph JSON type string.
- `fromValue`: resolves a graph JSON type string back to a known enum value.

We need it so the validator, engine, and executors share one source of truth for recognized workflow node types.

### `WorkflowDefinition`

JPA entity storing a workflow's name, enabled state, and `draftGraph` JSON.

We need it as the saved graph edited by the builder and executed by the engine.

### `WorkflowRun`

JPA entity for one workflow execution.

Important fields:

- `workflowDefinition`
- `workspace`
- `conversation`
- `status`
- `startedAt`, `finishedAt`
- `errorMessage`
- `waitingAtNodeId`
- `contextSnapshot`
- `steps`

We need it for observability, retries/resume, and future run log UI.

### `WorkflowRunStep`

JPA entity for one executed workflow node.

Important fields:

- `nodeId`
- `nodeType`
- `status`
- `inputSnapshot`
- `outputSnapshot`
- `errorMessage`
- `startedAt`, `finishedAt`, `durationMs`

We need it so every workflow run is inspectable step by step.

### `WorkflowRunStatus`

Enum for run lifecycle: running, completed, failed, waiting.

We need it to distinguish active, done, broken, and paused workflow runs.

### `WorkflowRunStepStatus`

Enum for step lifecycle.

We need it for per-node execution logs.

## Workflow DTO Records

- `CreateWorkflowDefinitionRequest`: workspace ID and name.
- `UpdateWorkflowDefinitionRequest`: optional name, enabled state, and draft graph.
- `WorkflowDefinitionResponse`: API shape of a workflow definition.

We need these as the builder API contract.

## Workflow Engine Components

### `WorkflowEngineService`

Core workflow runtime.

Important methods:

- `executeWorkflow`: starts a run from a trigger event.
- `resumeWorkflow`: resumes a run paused by Ask Question.
- `setConversationLock`: marks a conversation as workflow-owned or releases it.
- `walk`: traverses the graph.
- `executeStep`: executes one node and records its step log.
- `resolveNextNode`: chooses the next node based on source handles.
- direct Jump To handling: skips normal edge resolution when a node result includes `jumpToNodeId`.
- `applyReply`: maps a contact reply to a variable or branch.
- `buildContext`: seeds built-in variables from workspace, conversation, contact, and triggering message.
- `newStep`, `finishStep`: create durable step logs.
- `parseNodes`, `parseEdges`, `buildAdjacency`: convert graph JSON into runtime records.

We need it to turn saved workflow graph definitions into real automation execution and to keep agent replies from racing active workflow runs.

### `ExecutionContext`

Mutable variable store for one workflow run.

Important methods:

- `setVariable`: creates or overwrites variables.
- `interpolate`: resolves `{{variable}}` placeholders.
- `snapshot`: captures variables for run logs.

We need it because RelayFlow's core automation promise depends on explicit, overwritable variables.

### `VariableInterpolator`

Static utility for resolving placeholders in text.

Important methods:

- `interpolate`
- `resolve`

We need it so messages, URLs, headers, bodies, and condition values can use workflow variables.

### `GraphNode`

Runtime record for a parsed graph node: `id`, `type`, and `data`.

We need it to avoid passing raw map structures into executors.

### `GraphEdge`

Runtime record for a parsed graph edge: `id`, `source`, `target`, and `sourceHandle`.

We need it to choose the next node after conditions, HTTP branches, and Ask Question options.

### `NodeExecutor`

Interface implemented by each node type.

Important methods:

- `nodeType`: identifies which graph node type the executor handles.
- `execute`: performs node behavior and returns routing/output info.

We need it so node behavior is pluggable and the engine does not contain every node's business logic.

The backend now returns `NodeType` from `nodeType`, not a raw string, so node identity is centralized.

### `NodeExecutionResult`

Record returned from node execution.

Important factory methods:

- `next`: proceed through default edge.
- `handle`: proceed through a named branch/handle.
- `waiting`: pause the run.
- `jumpTo`: redirect execution directly to another node ID.

We need it to standardize how nodes communicate routing, output, wait state, and direct jumps to the engine.

### `NodeExecutionException`

Runtime exception for node-level failures.

We need it so failed steps can be logged and runs can be marked failed.

### Workflow Trigger Events

- `ConversationOpenedEvent`: published when an inbound message opens or reopens a conversation.
- `ConversationMessageReceivedEvent`: published when a message arrives in an already-open conversation.

We need these events to start workflows and resume waiting workflows after transactions commit.

### `WorkflowTriggerListener`

Listens for `ConversationOpenedEvent` and starts enabled workflows whose trigger is `conversation_opened`.

We need it to connect inbound conversations to workflow automation without blocking the Telegram webhook transaction.

### `WorkflowResumeListener`

Listens for `ConversationMessageReceivedEvent` and resumes `WAITING` workflow runs for the conversation.

We need it to implement Ask Question / wait-for-reply behavior.

## Workflow Node Executors

### `TriggerNodeExecutor`

No-op executor for the trigger node.

We need it because the trigger is part of the graph and must be recorded/traversed, even though it does not perform an action.

### `SendMessageNodeExecutor`

Creates a workflow-authored outbound message, emits SSE, and publishes `OutboundMessageEvent`.

We need it to let workflows respond to contacts through the active channel.

### `ConditionNodeExecutor`

Evaluates configured branches against variables.

Supported operators include equality, comparison, contains, starts/ends with, is set, and is not set.

We need it for branching automation logic.

### `HttpRequestNodeExecutor`

Executes configurable HTTP requests.

Important config fields:

- `method`
- `url`
- `headers`
- `body`
- `contentType`
- `timeoutSeconds`
- `responseStatusVariable`
- `responseMappings`

It routes to `success` on 2xx and `error` otherwise, storing `error.message` on failure.

We need it to integrate workflows with external APIs while giving users timeout and response mapping control.

### `SetVariableNodeExecutor`

Sets or overwrites a workflow variable.

Important config fields:

- `variableName`
- `value`

We need it to directly address the product goal that variables must be overridable.

### `WaitForReplyNodeExecutor`

Sends a question to the contact and returns a waiting result.

Modes:

- generic: save reply text into `responseVariable`.
- defined: route based on exact option match, with default "Other" branch.

We need it for interactive customer automations.

### `JumpToNodeExecutor`

Redirects execution directly to another node by ID, bypassing normal edge traversal.

Important config fields:

- `targetNodeId`
- `maxJumps`

Important behavior:

- stores a per-node jump counter in the execution context.
- stops the branch normally when the configured jump limit is reached.

We need it to support controlled workflow loops without allowing unbounded execution.

### `EndConversationNodeExecutor`

Optionally sends a closing message and marks the conversation closed.

We need it for workflows that complete support flows or hand off after resolution.

## Workflow Repositories

- `WorkflowDefinitionRepository`
- `WorkflowRunRepository`
- `WorkflowRunStepRepository`

These persist workflow definitions and execution logs.

We need them for builder state, runtime lookup, waiting-run resume, and future run log UI.

## Frontend Route Components

### Root Layout Components

- `RootLayout`: global HTML/body wrapper, fonts, Material Symbols stylesheet, `QueryProvider`, and `ToastProvider`.
- `metadata`: global title/description.
- `viewport`: responsive viewport settings.
- `inter`, `jetBrainsMono`: Next font variables.

We need these to make every page share providers, typography, and icon font setup.

### Landing Page Components And Constants

- `Home`: public landing page.
- `FEATURES`: feature-card content.
- `FLOW_NODES`: landing workflow chain content.
- `PREVIEW_CONVOS`: inbox preview conversation data.
- `PREVIEW_MESSAGES`: message preview data.
- `AuthenticationPanel`: nav auth/session panel.
- `TryItButton`: creates guest session and routes to inbox.

We need them to present the product and let users enter signup, login, inbox, or guest mode.

### Auth Pages

- `(auth)/layout.tsx`: auth page shell.
- `login/page.tsx`: login form and Google login entry point.
- `signup/page.tsx`: signup form and Google login entry point.
- login/signup `metadata`: route-specific page metadata.
- login/signup `apiBaseUrl`: backend OAuth base URL used by Google buttons.

We need them for account creation and login.

### Inbox Pages

- `inbox/layout.tsx`: inbox route shell.
- `inbox/page.tsx`: server component that resolves workspace ID and renders `InboxShell`.
- `WorkspaceItem`, `SearchParams`, `Props`: route helper types.
- `apiBaseUrl`: server-side API base URL.

We need these to protect and bootstrap the inbox view.

### Workflow Pages

- `workflows/(list)/page.tsx`: resolves workspace and renders `WorkflowsShell`.
- `workflows/[id]/page.tsx`: renders `WorkflowEditor`.
- workflow route `layout.tsx` files: page layout wrappers.
- workflow `metadata`: route title/description.

We need them for workflow list and builder routes.

### Contacts Pages

- `contacts/(list)/page.tsx`: resolves workspace and renders `ContactsShell`.
- `contacts/(list)/layout.tsx`: contacts route wrapper.

We need them for the workspace contact-management surface.

### Invite Pages

- `invite/[token]/page.tsx`: loads invite preview and renders the invite acceptance flow.

We need it so invited users can inspect and accept workspace invitations.

### Settings Pages

- `settings/page.tsx`: resolves workspace and renders `SettingsShell`.
- `settings/layout.tsx`: settings route wrapper.
- settings `metadata`: route metadata.

We need them to separate workspace configuration from inbox conversation work.

## Frontend Common Components

### `Spinner`

Reusable loading indicator.

Important values:

- `Size`: `sm`, `md`, `lg`.
- `sizeClasses`: maps size to Tailwind dimensions.

We need it for consistent loading states.

### `EmptyState`

Reusable empty-state component with icon, title, description, and optional action.

We need it for inbox/workflow/settings states where there is no data yet.

### `ConfirmModal`

Reusable confirmation dialog, including destructive action support and pending state.

We need it before operations like channel disconnect.

### `GoogleIcon`

Small SVG Google brand icon component.

We need it for Google login/signup buttons without importing a large icon library.

### `GuestBanner`

Banner warning anonymous users that guest data is temporary and prompting account creation.

We need it to communicate guest-mode lifecycle.

### `Select`

Reusable styled select control.

Important type:

- `SelectOption`

We need it because workflow configuration panels use many controlled select inputs.

## Frontend Providers

### `QueryProvider`

Wraps the app in a TanStack Query `QueryClientProvider`.

Important behavior:

- default query `staleTime`: 30 seconds.
- default retry count: 1.

We need it for API caching and mutation invalidation across the app.

### `ToastProvider`

Provides app-wide toast notifications.

Important components/functions/constants:

- `ToastKind`: `error` or `success`.
- `Toast`, `ToastInput`, `ToastContextValue`: toast state types.
- `ToastContext`: React context.
- `TOAST_STYLES`, `TOAST_ICONS`: visual maps.
- `TOAST_EVENT`: custom browser event name.
- `ToastProvider`: exposes `showToast` without forcing page rerenders.
- `ToastViewport`: owns toast state and rendering.
- `useToast`: hook for mutation error/success feedback.

We need it so API errors appear consistently without adding a third-party toast library.

## Frontend Workspace Components

### `WorkspaceNav`

Icon sidebar linking to inbox, contacts, workflows, and settings for a workspace.

Important helper:

- `NavItem`: renders a sidebar icon link with tooltip and active state.

We need it as the persistent app navigation.

### `WorkspaceSwitcher`

Dropdown for switching workspaces and creating a new workspace inline.

Important functions:

- `close`
- `selectWorkspace`
- `handleCreate`

Important refs/state:

- `containerRef`
- `inputRef`
- `isOpen`
- `showCreate`
- `newName`

We need it because users may belong to or create multiple workspaces.

### `CreateWorkspaceForm`

Standalone workspace creation form.

We need it for first-run states where authenticated users have no workspace yet.

## Frontend Inbox Components

### `InboxShell`

Top-level inbox layout.

Important functions:

- `selectConversation`
- `handleBack`

It combines `WorkspaceNav`, `WorkspaceSwitcher`, `ConversationList`, and `MessageThread`.

We need it to coordinate sidebar selection, mobile layout behavior, and real-time workspace events.

### `ConversationList`

Displays conversations and guest Telegram connection empty states.

Important value:

- `sharedBotUsername`: environment-provided shared bot username.

We need it as the inbox conversation selector.

### `ConversationItem`

Renders one conversation row.

Important values/functions:

- `STATUS_DOT`: maps status to color.
- `LOCALE`: timestamp locale.
- `formatTimestamp`: human-readable row timestamp.

We need it to make the conversation list scannable.

### `MessageThread`

Displays selected conversation messages and composer.

Important values:

- `STATUS_CHIP`: maps conversation state to chip styles.

We need it as the main reading/reply surface.

### `MessageBubble`

Renders one message bubble.

Important function:

- `formatMessageTime`

We need it for readable inbound/outbound timeline rendering.

### `MessageComposer`

Form for sending outbound agent messages.

Important behavior:

- disables itself and shows a workflow-ownership notice when `lockedByWorkflow` is true.

We need it to let agents reply from the inbox without interrupting active workflow runs.

## Frontend Contacts Components

### `ContactsShell`

Top-level contacts layout with table, selected contact state, detail panel, delete confirmation, and merge modal.

Important helpers:

- `formatDate`
- `contactInitial`
- `ActionMenu`
- `ContactRow`

We need it for browsing, selecting, deleting, and merging contacts.

### `ContactDetailPanel`

Side panel for one contact.

Important helpers/constants:

- `CHANNEL_META`
- `formatDateFull`
- `contactInitial`

We need it to show linked channel identities and jump to the contact's inbox conversations.

### `MergeContactModal`

Modal that lets a user choose a target contact and merge the selected source contact into it.

Important helpers:

- `contactInitial`
- local search/filter state.
- merge error display.

We need it to clean up duplicate contacts while preserving identities and conversations.

### `ConnectTelegramForm`

Form for adding a Telegram bot token to a workspace.

We need it for channel setup in settings.

## Frontend Settings Components

### `SettingsShell`

Settings page layout with `WorkspaceNav`, settings subnav, and channel content.

We need it to create a stable place for channel, member, invite, API key, and webhook settings.

### `ChannelsList`

Lists connected channel accounts and provides the connect/disconnect/reconnect UI.

Important constants:

- `PROVIDER_LABEL`: provider display names.
- `PROVIDER_ICON`: provider icon names.

Important helper:

- `ChannelItem`: renders one channel, webhook URL, and disconnect confirmation.

We need it because channel setup should live in workspace settings rather than the inbox conversation list.

### `MembersList`

Workspace member and invite management UI.

Important capabilities:

- list current members.
- invite a member with selected permissions.
- update member permissions.
- remove members.
- list and revoke pending invites.

We need it so owners can control who can operate the workspace.

### `IntegrationsPanel`

Settings panel that groups API keys and webhook configuration.

We need it so external integration setup lives in one settings area.

### `CreateApiKeyModal`

Modal for naming an API key, optionally setting expiry, and showing the plaintext key once after creation.

We need it to make one-time secret handling explicit in the UI.

### `WebhookConfigPanel`

Settings form for webhook URL, enabled state, subscribed events, secret rotation, and deletion.

We need it to configure outbound workspace webhooks safely.

## Frontend Workflow Builder Components

### `WorkflowEditor`

React Flow provider wrapper for the workflow editor.

We need it because React Flow hooks require `ReactFlowProvider`.

### `EditorCanvas`

Main workflow builder implementation.

Important state:

- `nodes`, `edges`: React Flow graph state.
- `name`: workflow name.
- `isDirty`: unsaved graph/name changes.
- `publishError`: backend validation error.
- `nodeIdRef`: local dropped-node ID counter.

Important functions:

- `onConnect`: adds graph edges.
- `onDragOver`: enables node drops.
- `onDrop`: creates a new node from palette item.
- `handleSave`: saves draft graph.
- `handlePublishToggle`: publishes/unpublishes and surfaces validation errors.
- `closeConfigPanel`: deselects selected node.

Important constants:

- `edgeTypes`: maps React Flow default edge to `DeletableEdge`.
- `nodeTypes`: maps workflow node type strings to React components.
- `palette`: draggable node palette.

We need it as the central no-code workflow editing surface.

### `NodeConfigPanel`

Side panel for editing selected node data.

Important helpers:

- `extractWorkflowVariables`: finds user-defined variables from Set Variable, HTTP response mappings, and Ask Question nodes.
- `insertAtCursor`: inserts `{{variable}}` into inputs/textareas.
- `Field`: reusable label/action wrapper.
- `ConditionValueField`, `HeaderRow`: smaller controlled field components with their own refs.

Important constants:

- `inputCls`
- `TRIGGER_EVENTS`
- `CONDITION_OPERATORS`
- `NO_VALUE_OPERATORS`
- `HTTP_METHODS`
- `BODY_METHODS`
- `CONTENT_TYPES`
- `BODY_PLACEHOLDERS`
- `RESPONSE_TYPE_OPTIONS`
- `TYPE_LABEL`

Important form parts:

- `TriggerForm`
- `SendMessageForm`
- `ConditionForm`
- `KeyValueEditor`
- `ResponseMappingEditor`
- `HttpRequestForm`
- `SetVariableForm`
- `EndConversationForm`
- `WaitForReplyForm`

We need it to turn graph nodes into user-editable automation configuration.

### `VariablePicker`

Dropdown for inserting variables into text fields.

Important values/components:

- `WorkflowVariable`
- `BUILT_IN_VARIABLES`
- `VariablePicker`
- `VariableGroup`

We need it so users can discover and insert valid `{{variable}}` placeholders without memorizing names.

### `WorkflowsShell`

Workflow list layout using `WorkspaceNav`, workspace switcher/create controls, and `WorkflowsList`.

We need it as the workflow home screen.

### `WorkflowsList`

Lists workflow definitions and links to the editor.

We need it so users can choose which workflow to edit.

### `DeletableEdge`

Custom React Flow edge component with a delete affordance.

We need it so users can remove graph connections without complex keyboard interactions.

## Frontend Workflow Node Components

### `WorkflowNode`

Base visual node wrapper shared by all workflow nodes.

Important props include icon, label, selected state, source/target handle flags, footer, and children.

We need it for consistent node appearance and connection handles.

### `TriggerNode`

Visual node for workflow trigger configuration.

Important type/constant:

- `TriggerNodeData`
- `EVENT_LABELS`

We need it as the graph start point.

### `SendMessageNode`

Visual node for outbound message actions.

Important type:

- `SendMessageNodeData`

We need it to represent automated replies in the graph.

### `ConditionNode`

Visual branching node.

Important types/constants:

- `ConditionOperator`
- `ConditionBranch`
- `ConditionNodeData`
- `DEFAULT_CONDITION_BRANCHES`

We need it to support multi-branch logic.

### `HttpRequestNode`

Visual HTTP integration node.

Important types:

- `HttpHeader`
- `ResponseMapping`
- `HttpRequestNodeData`

We need it to show external API calls and configured mappings on the canvas.

### `SetVariableNode`

Visual variable assignment node.

Important type:

- `SetVariableNodeData`

We need it to represent variable creation/override steps.

### `WaitForReplyNode`

Visual Ask Question node with dynamic source handles for defined options.

Important types:

- `WaitForReplyOption`
- `WaitForReplyNodeData`

We need it for workflows that pause and continue based on a customer's reply.

### `JumpToNode`

Visual node for redirecting execution to another node.

Important type:

- `JumpToNodeData`

Important data fields:

- `targetNodeId`
- `targetNodeLabel`
- `maxJumps`

We need it to let workflow builders create bounded loops or return to an earlier step deliberately.

### `EndConversationNode`

Visual conversation-close node.

Important type:

- `EndConversationNodeData`

We need it to represent automation termination that closes a conversation.

### `nodes/index.ts`

Barrel export for workflow node components and shared node types.

We need it to simplify imports in `WorkflowEditor`.

## Frontend Hooks

### Auth Hooks

- `useAuthentication`: fetches current session user.
- `useLogin`: login mutation with error toast support.
- `useSignup`: signup mutation with error toast support.
- `useLogout`: logout mutation and cache cleanup.

We need them to keep auth forms/components declarative.

### Workspace Hooks

- `useWorkspaces`: fetches workspaces.
- `useWorkspace`: selects one workspace from the cached list.
- `useCreateWorkspace`: creates a workspace and invalidates workspace queries.
- `useCurrentMember`: fetches current workspace membership/permissions.
- `useWorkspaceMembers`: fetches workspace members.
- `useUpdateMember`: updates member role/permissions.
- `useRemoveMember`: removes a member.
- `useWorkspaceInvites`: fetches pending invites.
- `useCreateInvite` / `useInviteMember`: creates invites.
- `useRevokeInvite`: revokes invites.
- `useInvitePreview`: loads public invite preview data.
- `useAcceptInvite`: accepts an invite.

We need them for workspace-first navigation.

### Channel Hooks

- `useChannelAccounts`: fetches connected channels.
- `useConnectTelegram`: connects a Telegram bot token.
- `useDeleteChannelAccount`: disconnects a channel.
- `useReconnectChannelAccount`: re-enables a disabled channel.

We need them for settings/channel management.

### Contact Hooks

- `useContacts`: infinite query for contact pages.
- `useContact`: fetches one contact detail.
- `useDeleteContact`: deletes a contact and invalidates contact caches.
- `useMergeContact`: merges contacts and invalidates affected caches.

We need them for contact-management screens.

### Inbox Hooks

- `useConversations`: infinite query for conversations.
- `useMessages`: infinite query for messages.
- `useSendMessage`: creates outbound messages and invalidates conversation/message caches.
- `useUpdateConversation`: changes conversation status.
- `useWorkspaceEvents`: opens SSE and invalidates React Query caches on events.

Important constants:

- `PAGE_SIZE`: conversation page size.
- `LIMIT`: message page size.
- `API_BASE_URL`: SSE base URL.

We need them for real-time inbox state.

### Workflow Hooks

- `useWorkflows`: fetches workflow definitions.
- `useWorkflow`: fetches one workflow.
- `useCreateWorkflow`: creates a workflow and updates workflow caches.
- `useSaveWorkflow`: patches workflow name, graph, and enabled state.

We need them to keep workflow builder API access outside UI components.

### Integration Hooks

- `useApiKeys`: fetches workspace API keys.
- `useCreateApiKey`: creates a key and exposes the one-time plaintext secret.
- `useRevokeApiKey`: revokes a key.
- `useWorkspaceWebhook`: fetches webhook configuration.
- `useSaveWebhook`: creates/updates webhook configuration.
- `useDeleteWebhook`: deletes webhook configuration.
- `useRotateWebhookSecret`: rotates the signing secret and exposes the one-time plaintext value.

We need them for API key and webhook settings without embedding fetch logic in components.

## Frontend API Libraries

### `authentication-api.ts`

Frontend auth API wrapper.

Important types:

- `SignupPayload`
- `LoginPayload`
- `AuthenticatedUserResponse`
- `GuestSessionResponse`

Important functions:

- `signup`
- `login`
- `createGuestSession`

Important constant:

- `apiBaseUrl`

We need it so auth-related network calls are centralized.

### `messaging-api.ts`

Main typed API client for messaging, channels, workspaces, contacts, members, invites, integrations, and workflows.

Important exported types:

- provider/status/message/workflow/permission/webhook union types.
- request/response types mirroring OpenAPI.
- `PageResponse<T>`.

Important classes/constants/functions:

- `ApiError`: typed fetch error with HTTP status and details.
- `MessagingApiClient`: wraps all API calls.
- `defaultBaseUrl`: API base URL.
- `messagingApi`: shared client instance.
- `readResponseBody`: parses error responses.
- `errorMessage`: normalizes backend error message.

We need it so hooks/components do not hand-roll fetch calls.

### `server-authentication.ts`

Server-side auth helpers used by Next routes/layouts.

Important pieces:

- `AuthenticationStatus`
- `requireAuthentication`
- `redirectIfAuthenticated`
- `apiBaseUrl`

We need it to protect server-rendered app routes and prevent logged-in users from staying on auth pages.

### `error-message.ts`

Normalizes unknown errors into user-displayable strings.

We need it so mutation hooks can show clean toast messages.

### `proxy.ts`

Next middleware for route-level cookie checks.

Important pieces:

- `SESSION_COOKIE`
- `proxy`
- `config`

We need it for fast redirect behavior before protected pages render, while still allowing server-side auth validation to handle stale cookies.

## Frontend Tests

### `page.test.tsx`

Landing page component test suite.

Important helper:

- `renderHome`

We need it to protect the public landing page content and links.

### `ConversationList.test.tsx`

Tests inbox conversation list states.

Important helpers:

- `mockConversationsHook`
- `baseHookReturn`
- `renderList`

We need it to protect the guest/empty/conversation list behavior.

### `WorkspaceSwitcher.test.tsx`

Tests workspace dropdown behavior.

Important values/helpers:

- `mockPush`
- `mockCreateWorkspace`
- `renderSwitcher`

We need it because workspace switching affects navigation across the app.

### `messaging-api.test.ts`

Tests API client behavior.

We need it to protect fetch URL construction and error handling.

## Contracts And Schemas

### `openapi.yaml`

OpenAPI contract for backend REST endpoints.

It documents health, auth, workspaces, members, invites, API keys, webhooks, public API, channels, contacts, external identities, conversations, messages, workflows, SSE, and Telegram webhooks.

We need it as the external API source of truth and future client-generation input.

### `message-event.schema.json`

JSON schema for normalized message events.

Important fields:

- `workspaceId`
- `channelAccountId`
- `provider`
- `direction`
- `externalConversationId`
- `externalSenderId`
- `providerMessageId`
- `text`
- `rawPayload`

We need it for channel-normalized message event contracts.

### `webhook-event.schema.json`

JSON schema for outbound workspace webhook payloads.

Important fields:

- `event`
- `timestamp`
- `workspaceId`
- `data`

Currently documented event data:

- `contact.created`: includes contact and channel details.

We need it so third-party webhook consumers have a stable payload contract.

### `workflow-definition.schema.json`

JSON schema for persisted workflow graph JSON.

It now matches the current React Flow-style graph: `nodes`, `edges`, node `type`, node `position`, and node `data`.

We need it to document and eventually validate/import/export workflow definitions outside the UI.

## Frontend Configuration Constants

### `tailwind.config.ts`

Defines RelayFlow design tokens and Tailwind extensions.

Important parts:

- CSS variable color palette.
- dark/light variable blocks.
- custom font families.
- extra semantic colors such as teal and orange.

We need it so components use consistent design tokens instead of arbitrary colors.

### `globals.css`

Global CSS reset and root styles.

We need it to bind Tailwind base styles, fonts, body background, text color, and Material Symbols behavior.

### `next.config.mjs`, `postcss.config.mjs`, `eslint.config.mjs`, `vitest.config.ts`

Tooling configuration for Next, Tailwind/PostCSS, ESLint, and Vitest.

We need them to keep local development, linting, tests, and builds predictable.
