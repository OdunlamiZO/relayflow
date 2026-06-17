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
- `signup`: creates an unverified email/password account and sends verification email.
- `verifyEmail`: verifies the email token and establishes a session.
- `login`: authenticates credentials and either establishes a session or returns a 2FA challenge.
- `login2FA`: verifies a TOTP code for a pending login challenge and establishes a session.
- `guest`: creates an anonymous guest session and workspace.
- `logout`: invalidates the server session and expires the `JSESSIONID` browser cookie.

We need it as the public API boundary for session state.

### `AuthenticationService`

Business logic for authentication.

Important methods:

- `signup`: validates email uniqueness, hashes password, creates an unverified `UserIdentity`, stores preferences, and emails a verification token.
- `verifyEmail`: validates token state, marks the email identity verified, and establishes a session.
- `login`: authenticates through Spring Security and returns user profile state or a short-lived 2FA challenge.
- `login2FA`: verifies the TOTP challenge and establishes a session.
- `createGuestSession`: creates anonymous user, workspace, and shared Telegram channel when configured.
- `getCurrentUser`: normalizes OAuth, email, and anonymous principals into `AuthenticatedUserResponse`.
- `establishSession`: writes an authenticated security context into the session.
- `establishSessionForUser`: creates a session without requiring plaintext password after email verification, 2FA completion, or anonymous session creation.

We need it to keep controller code thin and centralize session/user lifecycle rules.

### `EmailPasswordUserDetailsService`

Loads RelayFlow users by email for Spring Security's DAO authentication provider.

We need it so the Spring authentication manager can validate email/password login against the database.

### `OAuth2UserProvisioningService`

Extends Spring's OAuth user service to provision or update users after Google login.

We need it so Google login creates a local `User` record and returns consistent profile data.

### `ProfileController`

HTTP controller for current-user profile and security routes.

Endpoints:

- `getProfile`
- `updateProfile`
- `changePassword`
- `deleteAccount`
- `setup2FA`
- `enable2FA`
- `disable2FA`

We need it to keep account management separate from login/session endpoints.

### `ProfileService`

Business service for profile, password, account deletion, preferences, and MFA state.

Important methods:

- `getProfile`
- `updateProfile`
- `changePassword`
- `deleteAccount`
- `setup2FA`
- `enable2FA`
- `disable2FA`

We need it to coordinate user, identity, preferences, and MFA tables in one account-management layer.

### `TwoFactorService`

TOTP helper for generating secrets, building `otpauth://` URIs, and verifying authenticator codes.

Important methods:

- `generateSecret`
- `buildOtpauthUri`
- `isInvalidCode`

We need it to keep MFA cryptographic details out of controller/service workflow code.

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
- `anonymous`, `lastActiveAt`, `createdAt`, `updatedAt`, `deletedAt`: guest/account lifecycle fields.

We need it as the durable person/account anchor for workspaces and workspace membership. Login methods, preferences, and MFA are now satellite tables.

### `UserIdentity`

JPA entity representing one authentication identity for a user.

Important fields:

- `user`
- `provider`
- `providerSubject`
- `credential`
- `verified`
- `createdAt`

We need it so one user can have multiple login methods, such as email/password and Google OAuth.

### `UserPreferences`

JPA entity for per-user preferences.

Important fields:

- `userId`
- `receiveEmailUpdates`
- `createdAt`
- `updatedAt`

We need it to keep preferences out of the core identity row.

### `UserMfaMethod`

JPA entity for second-factor methods.

Important fields:

- `user`
- `type`
- `credential`
- `enabled`
- `createdAt`

We need it to support TOTP today and future MFA methods without changing the `users` table.

### `TwoFactorChallenge`

JPA entity for short-lived login challenges issued after password verification when 2FA is enabled.

Important fields:

- `user`
- `token`
- `expiresAt`

We need it so password verification and OTP verification can happen as two separate HTTP requests.

### `EmailVerificationToken`

JPA entity for email verification links.

Important fields:

- `user`
- `token`
- `expiresAt`
- `usedAt`

We need it so email/password accounts cannot log in until email ownership is confirmed.

### `AuthenticationProvider`

Enum of supported identity providers: email, Google, and anonymous.

We need it so user records can be interpreted correctly during login and cleanup.

### `MfaMethodType`

Enum of MFA method types: TOTP and SMS.

We need it so MFA support can grow beyond TOTP without redesigning the table.

### Auth DTO Records

- `SignupRequest`: user-supplied name, email, and password.
- `SignupResponse`: whether verification email was sent.
- `VerifyEmailRequest`: email verification token.
- `LoginRequest`: email and password.
- `Login2FARequest`: 2FA challenge token and OTP.
- `AuthenticatedUserResponse`: normalized auth/session state for the frontend.
- `GuestSessionResponse`: workspace ID created for guest mode.

We need these records as stable API contracts between backend and frontend.

### Profile DTO Records

- `ProfileResponse`: user profile, preferences, providers, and 2FA state.
- `UpdateProfileRequest`: display name and email update preference.
- `ChangePasswordRequest`: current and new password.
- `DeleteAccountRequest`: optional password for account deletion.
- `Setup2FAResponse`: `otpauth://` URI for QR display.
- `OtpRequest`: authenticator code.

We need these records for account-management UI/backend contracts.

### `UserRepository`

Spring Data repository for `User`.

Important queries include lookup by email and expired anonymous users.

We need it to keep persistence access declarative and testable.

### Authentication Satellite Repositories

- `UserIdentityRepository`: looks up identities by provider subject or user/provider.
- `UserPreferencesRepository`: stores per-user preferences.
- `UserMfaMethodRepository`: stores enabled/pending MFA methods.
- `TwoFactorChallengeRepository`: stores short-lived 2FA login challenges.
- `EmailVerificationTokenRepository`: stores email verification tokens.

We need these repositories because login methods, profile preferences, MFA, and email verification are deliberately split out of the core `User` table.

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
- `listWorkspaceMembers`, `inviteWorkspaceMember`, `updateWorkspaceMember`, `removeWorkspaceMember`, `transferOwnership`: owner/member management with single-owner safeguards.
- `createChannelAccount`: persists encrypted channel credentials and registers Telegram webhook; rejects guest workspaces with `403` (guests are limited to the shared Telegram bot).
- `createSharedBotChannelAccount`: creates a guest/shared Telegram channel account.
- `dropSharedTelegramChannel`: called on guest-to-real account conversion — hard-deletes the workspace's shared Telegram channel account along with its conversations, messages, external identities, and workflow runs/steps, then drops any contacts left with no remaining conversations or identities. Workflow definitions are preserved.
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
- `IllegalArgumentException` as `409`.
- validation errors as `400`.
- missing required request parameters as `400`.
- unexpected exceptions as `500` with a generic message.

We need it so frontend receives consistent `{ message, timestamp }` error responses.

### `ConversationLockedException`

Runtime exception thrown when an agent or public API client tries to send a message while an active workflow owns the conversation.

We need it to enforce workflow-driven conversations without silently dropping agent replies.

### `WorkspaceAuthorizationService`

Service that resolves the authenticated user's workspace membership and checks owner or granular permissions.

Important methods:

- `assertMember`: any workspace member can proceed.
- `assertOwner`: only owners can proceed.
- `getUser`: resolves the authenticated user ID for ownership-transfer checks.
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

Application event carrying a saved outbound message, its channel account, and optional button option labels.

We need it to decouple message persistence from provider delivery. The messaging service saves the message; adapters deliver it, optionally rendering Ask Question options as provider-native controls.

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

We need it for ownership, ownership transfer, and granular workspace authorization.

### `WorkspaceRole`

Enum for workspace roles: owner and member.

There is exactly one owner per workspace. Direct owner promotion/demotion is blocked in member updates; ownership moves through the transfer-ownership endpoint.

We need it so membership can become permission-aware while preserving a single workspace owner.

### `WorkspacePermission`

Enum of granular permissions for non-owner workspace members.

Values:

- `INBOX`
- `CONTACTS_DELETE`
- `WORKFLOWS_WRITE`
- `WORKFLOWS_DELETE`
- `CHANNELS_WRITE`
- `CHANNELS_DELETE`
- `AI_AGENT_WRITE`
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
- `lockedByWorkflow`: true while a workflow run owns the conversation.
- `lockedByAiAgent`: true during an active AI agent invocation pipeline run.
- `assigneeId`
- `lastMessageAt`
- `sessionStartedAt`: reset to `now()` whenever the conversation is reopened. Used by `AiAgentContextAssembler` to scope history to the current session only, preventing old closed-conversation messages from polluting the LLM context.

We need it as the inbox unit users read, select, and reply to. `lockedByWorkflow` and `lockedByAiAgent` prevent agents from interrupting active automation.

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
They also support invite lookup, API key lookup by hash, member permission lookup, plan-limit counts, contact merge reassignment, and public API filters.

We need them so services do not contain SQL or persistence boilerplate.

## Subscription Backend

### `PlanController`

Public controller for `GET /plans`.

It returns every plan with live Redis-backed limits, NGN pricing, billing interval, and whether checkout is currently available.

We need it so the frontend can render plan and upgrade UI without hardcoding commercial configuration.

### `SubscriptionController`

Workspace billing controller.

Endpoints:

- `getSubscription`: returns the workspace's current plan, status, limits, pricing, and upgrade flags.
- `startCheckout`: owner-only endpoint that initializes checkout for a paid plan and returns an authorization URL.
- `cancel`: owner-only endpoint that schedules cancellation at the end of the current billing period.

We need it to keep billing operations workspace-scoped and owner-controlled.

### `PaystackWebhookController`

Public Paystack webhook receiver at `POST /paystack/webhook`.

Handled events:

- `charge.success`: activates or renews a paid subscription.
- `subscription.create`: updates the current period end and stores Paystack's cancellation email token.
- `subscription.not_renew`: marks cancellation scheduled while access continues until the period end.
- `subscription.disable`: cancels and downgrades to FREE.
- `invoice.update`: marks unpaid subscriptions past due.

We need it because recurring billing state changes arrive asynchronously from Paystack.

**Race condition between `charge.success` and `subscription.create`:** Paystack fires both events at the same millisecond as separate HTTP requests, which Tomcat processes on separate threads. `subscription.create` carries the `subscriptionCode` and `emailToken` (required for cancellation), but it looks up the workspace by customerCode before `charge.success` has committed the activation — so it finds nothing and the token is lost.

Fix: the controller holds a `ConcurrentHashMap<String, PaystackSubscriptionData> pendingSubscriptionCreate` keyed by customerCode. When `subscription.create` cannot find a workspace, it caches the full payload instead of discarding it. After `charge.success` commits the activation, it checks the cache by customerCode, drains the entry, and immediately applies the `subscriptionCode` and `emailToken` via `setSubscriptionCode` + `renew`.

If the API is ever scaled to multiple instances, migrate `pendingSubscriptionCreate` to Redis — the in-memory map is invisible across instances.

### `SubscriptionService`

Core service for workspace subscription state and limit enforcement.

Important methods:

- `createFreeSubscription`: creates a FREE subscription row when a workspace is created.
- `getResponse`: builds the API response with live plan configuration.
- `activate`, `renew`, `markPastDue`, `markCancellationScheduled`, `scheduleCancel`, `downgrade`: mutate billing lifecycle state from provider events, user cancellation, and downgrade processing.
- `enforceLimit`: throws `PlanLimitExceededException` when a workspace has reached a plan cap.
- `enforceLimitOnEnable`: checks limits before re-enabling disabled channels or workflows.
- `findBySubscriptionCode`: locates subscriptions from provider subscription identifiers.
- `findByCustomerCode`: fallback lookup when Paystack creates the subscription code after the initial charge.
- `setSubscriptionCode`: stores a provider subscription code that arrives after activation.
- `refreshPeriodEnd`: best-effort provider verification that replaces an approximated period end and stores the cancellation token.
- `findDueForDowngrade`: finds scheduled cancellations and past-due subscriptions ready for downgrade.

We need it so channel, member, and workflow limits are enforced consistently outside the individual feature services.

### `SubscriptionCheckoutService`

Selects the configured payment provider for a target plan and initializes checkout.

It rejects FREE checkout, verifies the requester is the workspace owner, resolves the owner email, and delegates to a provider-specific `CheckoutProvider`.

We need it so new payment providers can be added without rewriting workspace billing flow.

### `PaystackCheckoutProvider`

Paystack implementation of `CheckoutProvider`.

It reads the plan code from Redis, sends workspace/plan metadata to Paystack, and returns Paystack's authorization URL.

We need it to keep Paystack transaction construction isolated from subscription orchestration.

### `BillingProvider` And `PaystackBillingProvider`

Provider-agnostic interface and Paystack implementation for subscription management after checkout.

Important methods:

- `verify`: checks provider-side subscription activity and next payment date.
- `cancel`: disables future provider charges using the provider subscription code and token.

We need these so scheduled cancellations and past-due verification are not hardcoded directly into `SubscriptionService`.

### `SubscriptionDowngradeScheduler`

Hourly scheduled job that processes downgrades.

It handles:

- `CANCELLATION_SCHEDULED`: downgrades after `currentPeriodEnd`.
- `PAST_DUE`: waits through a 7-day grace period, verifies with the billing provider, and downgrades if the subscription is still inactive.

We need it so cancellation and payment-failure downgrades happen after the correct access period instead of immediately.

### `PlanConfigurationService`

Reads plan configuration from Redis.

Important Redis keys:

- `relayflow:plan:{PLAN}:config`
- `relayflow:plan:{PLAN}:provider`
- `relayflow:paystack:plan:{PLAN}:code`

Plan values are `FREE`, `PRO_MONTHLY`, and `PRO_ANNUAL`. Billing interval values are `monthly` and `annual`. Configuration must be seeded in Redis before the API handles requests; missing config now raises an error instead of silently using hardcoded defaults.

We need it so limits, pricing, provider selection, and Paystack plan codes are explicit runtime configuration.

### `V21__workspace_subscriptions.sql`

Creates the `workspace_subscriptions` table and backfills a FREE subscription row for existing workspaces.

Important columns:

- `payment_provider`, `payment_customer_code`, `payment_subscription_code`, and `payment_subscription_token`: provider-neutral billing identifiers.
- `current_period_end`: period boundary used for scheduled cancellation and past-due downgrade timing.
- `downgrade_locked_channels` and `downgrade_locked_workflows`: counts shown in the billing UI after downgrade locking.

We need it so every workspace has explicit billing state while keeping payment-provider details nullable for free workspaces.

### Subscription Domain And DTO Records

- `WorkspaceSubscription`: JPA entity for the workspace's plan, status, provider codes, cancellation token, current period end, and downgrade lock counts.
- `WorkspaceSubscriptionRepository`: Spring Data repository for lookup by workspace, provider subscription code, provider customer code, and downgrade-due subscription queries.
- `Plan`, `SubscriptionStatus`, `BillingInterval`, `PaymentProvider`, `LimitType`: enums for billing tier, lifecycle, cadence, provider, and capped resources. `Plan` distinguishes FREE, monthly PRO, and annual PRO billing cycles.
- `PlanConfiguration`, `PlanLimits`: value records for live plan config and resource caps.
- `PlanLimitExceededException`: maps plan-cap violations to HTTP `402 Payment Required`.
- `PlanInfo`, `SubscriptionResponse`, `StartCheckoutRequest`, `CheckoutResponse`: REST DTOs for plan catalog, subscription state, checkout, and downgrade notices.

We need these to keep billing state explicit and separate from messaging/workflow entities.

## SSE Backend

### `SseController`

Exposes `GET /sse/workspace/{workspaceId}` as a text/event-stream endpoint.

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
- `sendTelegramMessage`: calls Telegram Bot API with retry and attaches a one-time reply keyboard when button options are present.
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

## WhatsApp Backend

### `WhatsAppController`

HTTP controller for WhatsApp Business Cloud API webhooks.

Endpoints:

- `verifyWebhook`: handles Meta's `hub.challenge` verification request.
- `webhook`: accepts inbound WhatsApp webhook payloads.

We need it as WhatsApp's inbound HTTP entry point.

### `WhatsAppAdapter`

WhatsApp integration service.

Important methods:

- `verifyWebhook`: validates Meta verification mode/token and returns the challenge text.
- `handleWebhook`: accepts inbound WhatsApp payloads.
- `processInboundMessage`: normalizes supported text or interactive button-reply messages into contact, identity, conversation, and message records.
- `onOutboundMessage`: listens for outbound messages and sends them through WhatsApp.
- `sendWhatsAppMessage`: calls Meta Graph API with retry, sending 1-3 options as native interactive buttons and 4+ options as numbered plain text.
- `resolveMessageText`: extracts text from inbound plain text and interactive button replies.
- `createIdentity`, `createConversation`, `resolveDisplayName`: helper methods for inbound normalization.

We need it to keep WhatsApp-specific webhook, credential, and Graph API behavior out of the channel-agnostic messaging service.

### `WhatsAppCredentials`

Plaintext credential record that is JSON-serialized and encrypted in `channel_accounts.encrypted_credentials`.

Important fields:

- `accessToken`
- `phoneNumberId`
- `verifyToken`

We need it because WhatsApp requires both outbound Graph API credentials and a webhook verification secret.

### `WhatsAppSendException`

Exception thrown when outbound WhatsApp delivery fails after retry.

We need it so failed WhatsApp delivery can be surfaced to callers and avoid pretending a message was delivered.

### WhatsApp DTO Records

- `WhatsAppWebhookPayload`
- `WhatsAppEntry`
- `WhatsAppChange`
- `WhatsAppValue`
- `WhatsAppContactEntry`
- `WhatsAppContactProfile`
- `WhatsAppMessage`
- `WhatsAppTextBody`
- `WhatsAppInteractive`
- `WhatsAppButtonReply`

We need these records to deserialize Meta's webhook JSON into typed Java data.

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
- HTTP Request nodes have a non-blank URL.
- Set Variable nodes have a non-blank variable name.
- Condition nodes: every non-fallback branch has at least one condition row with both `variable` and `operator` set. The last branch is always the fallback and is exempt.

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
- defined: route based on exact option match or option number, optionally saving the selected value into `responseVariable`, with default "Other" branch.

It publishes option labels through `OutboundMessageEvent` so adapters can render Telegram reply keyboards or WhatsApp interactive buttons.

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

### Profile Pages

- `profile/layout.tsx`: protected profile shell; redirects anonymous guests away.
- `profile/page.tsx`: renders `ProfileShell` behind a suspense boundary.

We need them for current-user account and security settings.

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

Banner warning anonymous users that guest data is temporary and prompting account creation. Copy also nudges that signing up keeps workflows and unlocks connecting your own channels.

We need it to communicate guest-mode lifecycle.

### `CopyButton`

Small button that copies a given `text` prop to the clipboard via `navigator.clipboard`, showing a checkmark for 2 seconds after copying.

We need it for webhook URLs and the shared Telegram bot deep link in `ChannelsList`.

### `UpgradeBanner`

Workspace-level plan prompt shown on core workspace pages when the current subscription recommends an upgrade.

It links owners back to the billing section for the selected workspace.

We need it so users who hit free-plan limits have a visible path to upgrade without leaving their current workflow.

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

Important state:

- `composerPrefill`: lifted state that pre-fills the composer when the agent clicks Edit on a draft.

Renders `AiDraftBanner` above the composer when an AI draft exists for the conversation.

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
- accepts `prefillText` and `onPrefillConsumed` props; a `useEffect` syncs the text and focuses the textarea when `prefillText` changes. Used when an agent clicks Edit on an AI draft.

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

## Frontend Profile Components

### `ProfileShell`

Client-side profile management screen.

Important sections:

- personal information and email-update preference.
- password change for email/password accounts.
- TOTP two-factor setup, enable, and disable flow.
- account deletion danger zone.

Important dependency:

- `QRCodeSVG`: renders QR codes from the backend `otpauth://` URI.

We need it so users can manage account details and security without leaving RelayFlow.

### `ConnectTelegramForm`

Form for adding a Telegram bot token to a workspace.

We need it for channel setup in settings.

### `ConnectWhatsAppForm`

Form for adding a WhatsApp Business Cloud API channel to a workspace.

Important fields:

- display name.
- access token.
- phone number ID.
- verify token.

We need it for WhatsApp channel setup in settings.

## Frontend Settings Components

### `SettingsShell`

Settings page layout with `WorkspaceNav`, settings subnav, and channel, member, AI agent, integration, and billing sections.

It shows the AI Agent section to owners and members with `AI_AGENT_WRITE`. It shows billing only to workspace owners.

We need it to create a stable place for channel, member, invite, AI agent, API key, webhook, and subscription settings.

### `ChannelsList`

Lists connected channel accounts and provides the connect/disconnect/reconnect UI. The "Add channel" section is hidden entirely for guest workspaces (`isAnonymous`), since they're limited to the shared Telegram bot.

Important constants:

- `PROVIDER_LABEL`: provider display names.
- `PROVIDER_ICON`: provider icon names.
- `sharedBotUsername`: from `NEXT_PUBLIC_SHARED_BOT_USERNAME`, used to build the shared bot's deep link.

Important helper:

- `ChannelItem`: renders one channel, provider-specific webhook URL with a `CopyButton`, a "Guest mode only" badge plus a persistent deep link + `CopyButton` for shared channels, and disconnect confirmation.
- `ProviderButton`: selects Telegram or WhatsApp connection flow.

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

### `BillingPanel`

Owner-only billing settings panel.

Important helpers:

- `StatusBanner`: explains past-due and scheduled-cancellation states.
- `DowngradeBanner`: shows counts of channels/workflows disabled after downgrade.
- `CurrentPlanCard`: renders current plan, limits, renewal/access date, and cancellation action.
- `PlanCard`: lets owners choose monthly or annual PRO checkout.

It uses the subscription hooks to load plan state, start checkout, and schedule cancellation.

We need it so owners can inspect limits, upgrade, switch billing cycle, and cancel from inside workspace settings.

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
- `useLogin`: login mutation with error toast support and 2FA challenge handling.
- `useLogin2FA`: completes the OTP challenge after password login.
- `useSignup`: signup mutation with error toast support; clears a stale `guestRecoveryToken` from localStorage on success (a guest account that signs up is converted in place, so its recovery token is no longer valid).
- `useLogout`: logout mutation and cache cleanup.

We need them to keep auth forms/components declarative.

### Profile Hooks

- `useProfile`: fetches current profile state.
- `useUpdateProfile`: updates display name and email-update preference.
- `useChangePassword`: changes an email/password account password.
- `useDeleteAccount`: deletes the current account and clears auth state.
- `useSetup2FA`: starts TOTP setup and returns an `otpauth://` URI.
- `useEnable2FA`: verifies OTP and enables TOTP.
- `useDisable2FA`: verifies OTP and disables TOTP.

We need them to keep profile/security mutations outside the profile component.

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
- `useConnectWhatsApp`: connects WhatsApp Business Cloud API credentials.
- `useDeleteChannelAccount`: disconnects a channel.
- `useReconnectChannelAccount`: re-enables a disabled channel.

We need them for settings/channel management.

### Subscription Hooks

- `usePlans`: fetches the public plan catalogue.
- `useSubscription`: fetches the selected workspace's subscription state and limits.
- `useStartCheckout`: initializes checkout and redirects to the returned authorization URL.
- `useCancelSubscription`: schedules subscription cancellation and invalidates the subscription cache.

We need them so billing UI can stay declarative and reuse the central API client.

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
- `useWorkspaceEvents`: opens SSE and invalidates React Query caches on `message.created`, `workspace.updated`, `ai.draft.created`, and `ai.escalated` events.

Important constants:

- `PAGE_SIZE`: conversation page size.
- `LIMIT`: message page size.
- `API_BASE_URL`: SSE base URL.

We need them for real-time inbox state.

### AI Agent Hooks

- `useAiAgentConfig(workspaceId)`: fetches workspace AI agent config; query key `["ai-agent-config", workspaceId]`.
- `useUpdateAiAgentConfig(workspaceId)`: mutation that PUTs config and updates the cached config via `setQueryData` on success.
- `useConversationAiDraft(workspaceId, conversationId)`: fetches the active AI draft; `retry: false`; enabled only when both IDs are truthy.
- `useSendAiDraft(workspaceId, conversationId)`: POSTs to `/ai-draft/send`; invalidates draft, messages, and conversations on success.
- `useDiscardAiDraft(workspaceId, conversationId)`: DELETEs the draft; invalidates the draft query.
- `useTriggerWorkflowFromDraft(workspaceId, conversationId)`: mutation that POSTs to `/ai-draft/trigger-workflow/{workflowId}`; invalidates the draft and conversations queries on success. Used by `AiDraftBanner` in workflow suggestion mode.

We need them to keep AI agent API access outside UI components and consistent with the hook-as-service-layer pattern.

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
- `SignupResponse`
- `LoginPayload`
- `Login2FAPayload`
- `AuthenticatedUserResponse`
- `GuestSessionResponse`
- `ProfileResponse`
- `UpdateProfilePayload`
- `ChangePasswordPayload`
- `DeleteAccountPayload`
- `Setup2FAResponse`
- `OtpPayload`

Important functions:

- `signup`
- `verifyEmail`
- `login`
- `login2FA`
- `createGuestSession`
- `getProfile`
- `updateProfile`
- `changePassword`
- `deleteAccount`
- `setup2FA`
- `enable2FA`
- `disable2FA`

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
- `errorMessage`: normalizes a backend error message.

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

It documents health, auth, workspaces, members, invites, API keys, webhooks, public API, channels, contacts, external identities, conversations, messages, workflows, subscriptions, Paystack webhooks, SSE, Telegram webhooks, and WhatsApp webhooks.

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

## AI Agent Backend

### `AiAgentConfiguration`

JPA entity mapped to `ai_agent_configs`. One row per workspace (unique constraint enforced at DB level).

Important fields:

- `workspace`
- `name`: display name shown in the AI Agent settings panel (default `"AI Agent"`).
- `enabled`
- `autonomyCeiling`: `DRAFT_ONLY` or `AUTO_SEND`
- `instructions`: free-text system prompt for the LLM
- `knowledgeBase`: `List<KnowledgeEntry>` stored as JSONB — Q&A pairs injected into the system prompt
- `escalationKeywords`: `List<String>` JSONB — deterministic pre-LLM keyword check
- `workflowMappings`: `List<WorkflowMapping>` JSONB — maps workflow IDs to trigger descriptions shown to the LLM

We need it to give each workspace a customizable agent persona and routing configuration.

### `AiAgentInvocationLog`

JPA entity mapped to `ai_agent_invocation_log`. One row per agent pipeline run.

Important fields:

- `workspace`, `conversation`
- `status`: `RUNNING`, `CLARIFYING`, `ESCALATED`, `DRAFTED`, `SENT`, `FAILED`
- `inputSnapshot`, `outputSnapshot`: JSONB observability snapshots
- `escalationReason`
- `startedAt`, `finishedAt`

A unique partial index `uq_ai_invocation_conversation_active` on `(conversation_id) WHERE status IN ('RUNNING', 'CLARIFYING')` is the primary race-condition fence — only one active invocation per conversation is allowed.

We need it for observability and to prevent concurrent agent invocations on the same conversation.

### `ConversationAiDraft`

JPA entity mapped to `conversation_ai_drafts`. One row per conversation (unique constraint).

Important fields:

- `workspace`, `conversation`
- `invocationLogId`
- `proposedReply`
- `suggestedActions`: `List<String>` JSONB

We need it to hold the AI-proposed reply until an agent sends, edits, or discards it.

### `AiAgentConfigurationService`

Business service for AI agent configuration CRUD and draft actions.

Important methods:

- `getOrCreateConfig(workspaceId)`: fetches or creates a default disabled config.
- `updateConfig(workspaceId, request)`: applies partial updates.
- `getDraft(workspaceId, conversationId)`: returns the active draft for a conversation.
- `sendDraft(workspaceId, conversationId)`: sends the draft as an outbound `SYSTEM` message via `MessagingService` and deletes it.
- `discardDraft(workspaceId, conversationId)`: deletes the draft without sending.
- `triggerWorkflowFromDraft(workspaceId, conversationId, workflowId)`: validates that the draft's `suggestedActions` contains `trigger_workflow:<workflowId>`, deletes the draft, and calls `WorkflowEngineService.executeWorkflow()`.

We need it to keep controller code thin.

### `AiAgentConfigurationController`

REST controller at `/workspaces/{workspaceId}/ai-agent-config`.

- `GET`: any workspace member.
- `PUT`: requires `AI_AGENT_WRITE` permission or owner role.

### `ConversationAiDraftController`

REST controller at `/workspaces/{workspaceId}/conversations/{conversationId}/ai-draft`.

All endpoints require `INBOX` permission. Endpoints: `GET`, `POST /send`, `DELETE`, `POST /trigger-workflow/{workflowId}`.

`POST /trigger-workflow/{workflowId}` validates that `workflowId` is in the draft's `suggestedActions`, discards the draft, and triggers the workflow — used when `DRAFT_ONLY` mode holds a workflow suggestion for human approval.

### `AiAgentInvocationSlotClaimer`

Package-private `@Component` with a `@Transactional(REQUIRES_NEW)` method `tryClaim(conversation, triggeringMessage)`.

Inserts an `AiAgentInvocationLog(RUNNING)` via `saveAndFlush()`. Returns `Optional.empty()` on `DataIntegrityViolationException` (unique slot already held). The inner transaction is isolated so a constraint failure only rolls back the claim attempt, not the outer pipeline transaction.

We need it because catching a constraint violation inside the same `@Transactional` method marks the outer transaction rollback-only; the separate bean with `REQUIRES_NEW` cleanly isolates the failure.

### `AiAgentInvocationService`

Core agent pipeline service. Runs `@Async` after the inbound message transaction commits.

Important method: `invoke(conversation, triggeringMessage)`

Flow:

1. Load enabled `AiAgentConfiguration`; abort if absent.
2. Close any prior `CLARIFYING` log for this conversation (frees the unique slot).
3. Claim the slot via `AiAgentInvocationSlotClaimer.tryClaim()`. If it returns empty, another invocation is already active — return immediately.
4. Re-check for an active `WorkflowRun` committed in the race window since step 3.
5. Deterministic keyword escalation check.
6. Assemble context via `AiAgentContextAssembler` and call LLM.
7. Decision:
   - `escalate` → broadcast SSE + ESCALATED.
   - `trigger_workflow:` in `suggestedActions` and autonomy ceiling is `AUTO_SEND` → `WorkflowEngineService.executeWorkflow()` + SENT.
   - `trigger_workflow:` action and `DRAFT_ONLY` ceiling → save draft with the action in `suggestedActions` + broadcast + DRAFTED (human approves via `POST /trigger-workflow/{workflowId}`).
   - `draftOnly` OR `confidence == "low"` OR `needsClarification` → save draft + broadcast + DRAFTED.
   - else → send reply + SENT.

We need it to handle the full agent decision loop safely with an isolated slot-claim mechanism.

### `AiAgentTriggerListener`

Spring event listener with `@Async @TransactionalEventListener(phase = AFTER_COMMIT)`.

Listens for `ConversationOpenedEvent` and `ConversationMessageReceivedEvent`. On each event, checks that the workspace has an enabled config and no active `WorkflowRun` before calling `AiAgentInvocationService.invoke()`.

We need it to run the agent asynchronously after the inbound message transaction commits.

### `AiAgentContextAssembler`

Builds the `AgentLlmRequest` from config and conversation history.

Important behavior:

- Fetches the last 20 messages created at or after `conversation.sessionStartedAt` (newest-first), reverses to chronological order. This scopes history to the current session so prior closed-conversation messages never pollute the context.
- Constructs the system prompt from `instructions` + `# WORKFLOW ROUTING` block (from `workflowMappings`, with directive wording: "MUST trigger that workflow — set reply to '' — Never write a reply AND trigger a workflow at the same time") + `# KNOWLEDGE BASE` (from `knowledgeBase`).
- Appends a `[Contact: name | Channel: PROVIDER]` footer to the last inbound message only.

We need it to keep LLM prompt construction separate from the invocation pipeline.

### `AiAgentInvocationCleanupScheduler`

Hourly `@Scheduled` job that deletes invocation logs older than `relayflow.agent.invocation-log-retention-days` (default 90, configured via `AGENT_INVOCATION_LOG_RETENTION_DAYS`).

We need it to prevent the invocation log table from growing without bound.

### LLM Abstraction

#### `LlmClient`

Interface: `provider()` returns `LlmProvider`; `complete(AgentLlmRequest)` returns `AgentLlmResponse`.

#### `LlmProvider`

Enum: `ANTHROPIC`, `OPENAI`, `OLLAMA`, `GROQ`.

#### `AgentLlmRequest`

Record: `systemPrompt`, `messages: List<LlmMessage>`, `model` (nullable — overrides Redis default when set).

#### `LlmMessage`

Record: `role` (`"user"` / `"assistant"`), `content`. Factory methods: `LlmMessage.user(content)`, `LlmMessage.assistant(content)`.

#### `AgentLlmResponse`

Record: `reply`, `confidence` (`"high"`, `"low"`, or `null`), `suggestedActions`, `escalate`, `needsClarification`.

#### `LlmPrompts`

Package-private constants class holding the shared `JSON_FORMAT_INSTRUCTION` string appended to every LLM system prompt. Shared by all three client implementations.

#### `AnthropicLlmClient`

Implements `LlmClient` via the Anthropic Java SDK (`anthropic-java:0.8.2`). Built in `@PostConstruct` using `ANTHROPIC_API_KEY`. Model read from Redis `platform:llm:anthropic:model`, default `claude-haiku-4-5`.

#### `AbstractOpenAiCompatibleLlmClient`

Package-private abstract base for `OpenAiLlmClient` and `OllamaLlmClient`. Implements the OpenAI-compatible chat completions contract: builds the JSON payload, POSTs to `/v1/chat/completions` via Spring `RestClient`, extracts `choices[0].message.content`, and parses it into `AgentLlmResponse`.

#### `OpenAiLlmClient`

Extends `AbstractOpenAiCompatibleLlmClient`. `RestClient` built once in `@PostConstruct` with `Authorization: Bearer {OPENAI_API_KEY}`. Model from Redis `platform:llm:openai:model`, default `gpt-4o-mini`.

#### `OllamaLlmClient`

Extends `AbstractOpenAiCompatibleLlmClient`. `RestClient` built per call with URL from Redis `platform:llm:ollama:url` (default `http://localhost:11434`). Model from Redis `platform:llm:ollama:model`, default `llama3.2`.

#### `GroqLlmClient`

Extends `AbstractOpenAiCompatibleLlmClient`. `RestClient` built in `@PostConstruct` using `GROQ_API_KEY`, base URL `https://api.groq.com/openai`. Model from Redis `platform:llm:groq:model`.

#### `LlmPlatformConfigService`

Reads the active `LlmProvider` from Redis key `platform:llm:provider`. Falls back to `ANTHROPIC` on missing/invalid values. Also exposes `getConfigValue(key, default)` — a generic Redis read with a fallback — used by all three clients for per-provider model and URL config.

#### `LlmClientFactory`

Collects all `LlmClient` beans into a `Map<LlmProvider, LlmClient>`. `getActiveClient()` resolves the provider at call-time so a Redis update takes effect immediately without restart.

### AI Agent DTO Records

- `AiAgentConfigurationResponse`: full config including `name` and all JSONB fields.
- `UpdateAiAgentConfigurationRequest`: partial update record; compact constructor normalizes null lists to empty.
- `ConversationAiDraftResponse`: draft fields including `proposedReply` and `suggestedActions`.

### AI Agent Repositories

- `AiAgentConfigurationRepository`: `findByWorkspaceId`, `findByWorkspaceIdAndEnabledTrue`.
- `AiAgentInvocationLogRepository`: `findByConversationIdAndStatus`, `existsActiveForConversation`, `deleteByStartedAtBefore` (`@Modifying` cleanup query).
- `ConversationAiDraftRepository`: `findByConversationId`, `deleteByConversationId`.

## AI Agent Frontend Components

### `AiAgentPanel`

Settings panel under `#ai-agent` in `SettingsShell`.

Sections:

- Enable toggle.
- Autonomy radio: `DRAFT_ONLY` (agent drafts for human review) / `AUTO_SEND` (agent sends directly when confident).
- Instructions textarea pre-filled with a skeleton template when empty.
- Knowledge base: list of `{question, answer}` pairs.
- Escalation keywords: tag-style list.
- Workflow mappings: workflow dropdown + trigger description rows.

Uses `useAiAgentConfig` and `useUpdateAiAgentConfig`. Follows the `GeneralPanel` save-button pattern.

We need it so workspace owners can configure the agent persona, routing, and escalation behavior.

### `AiDraftBanner`

Banner rendered above the `MessageComposer` in `MessageThread` when `useConversationAiDraft` returns data.

It detects whether the draft carries a `trigger_workflow:<id>` entry in `suggestedActions` and renders one of two modes:

**Draft mode** (no workflow action):
- **Send** — calls `useSendAiDraft`.
- **Edit** — calls `onEdit(draft.proposedReply)` to pre-fill the composer, then `useDiscardAiDraft`.
- **Discard** — calls `useDiscardAiDraft`.

**Workflow suggestion mode** (`trigger_workflow:` present):
- Header reads "AI workflow suggestion".
- Body reads "The AI suggests running a workflow to handle this conversation."
- **Run Workflow** button — calls `useTriggerWorkflowFromDraft(workflowId)`.
- **Discard** — calls `useDiscardAiDraft`.

Button row uses `flex-wrap` for mobile responsiveness. All buttons disabled while any mutation is pending.

We need it so agents can send AI replies, trigger AI-suggested workflows, or discard — all from the inbox without leaving the conversation.
