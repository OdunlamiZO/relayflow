# RelayFlow Codebase Components

This document explains the main named parts in the codebase: classes, records, enums, React components, hooks, exported functions, important constants, and contract schemas. It is organized by responsibility rather than folder names.

It intentionally focuses on components that define behavior or shared contracts. Tiny local variables inside a function are not listed unless they carry architectural meaning.

## Table Of Contents

- [Backend Application And Configuration](#backend-application-and-configuration)
  - [`RelayFlowApiApplication`](#relayflowapiapplication)
  - [`SecurityConfiguration`](#securityconfiguration)
  - [`WebConfiguration`](#webconfiguration)
  - [`CredentialEncryptionService`](#credentialencryptionservice)
- [Authentication Backend](#authentication-backend)
  - [`AuthenticationController`](#authenticationcontroller)
  - [`AuthenticationService`](#authenticationservice)
  - [`EmailPasswordUserDetailsService`](#emailpassworduserdetailsservice)
  - [`OAuth2UserProvisioningService`](#oauth2userprovisioningservice)
  - [`PasswordResetService`](#passwordresetservice)
  - [`PasswordResetToken`](#passwordresettoken)
  - [`ProfileController`](#profilecontroller)
  - [`ProfileService`](#profileservice)
  - [`TwoFactorService`](#twofactorservice)
  - [`SecurityUtils`](#securityutils)
  - [`User`](#user)
  - [`UserIdentity`](#useridentity)
  - [`UserPreferences`](#userpreferences)
  - [`UserMfaMethod`](#usermfamethod)
  - [`TwoFactorChallenge`](#twofactorchallenge)
  - [`AuthenticationProvider`](#authenticationprovider)
  - [`MfaMethodType`](#mfamethodtype)
  - [Auth DTO Records](#auth-dto-records)
  - [Profile DTO Records](#profile-dto-records)
  - [`UserRepository`](#userrepository)
  - [Authentication Satellite Repositories](#authentication-satellite-repositories)
  - [`ApiKeyAuthentication`](#apikeyauthentication)
  - [`ApiKeyAuthenticationFilter`](#apikeyauthenticationfilter)
  - [`AsyncConfiguration`](#asyncconfiguration)
- [Common Backend](#common-backend)
  - [`ResourceNotFoundException`](#resourcenotfoundexception)
  - [`PageResponse`](#pageresponse)
  - [`ErrorResponse`](#errorresponse)
  - [`CommonExceptionHandler`](#commonexceptionhandler)
  - [`MapUtils`](#maputils)
- [Workspace Backend](#workspace-backend)
  - [`WorkspaceController`](#workspacecontroller)
  - [`WorkspaceService`](#workspaceservice)
  - [`WorkspaceMapper`](#workspacemapper)
  - [`WorkspaceAuthorizationService`](#workspaceauthorizationservice)
  - [`ApiKeyService`](#apikeyservice)
  - [`ApiKeyController`](#apikeycontroller)
  - [`WorkspaceInviteService`](#workspaceinviteservice)
  - [`WorkspaceInviteController`](#workspaceinvitecontroller)
- [Workspace Domain Entities And Enums](#workspace-domain-entities-and-enums)
  - [`Workspace`](#workspace)
  - [`ContactFieldDefinition`](#contactfielddefinition)
  - [`WorkspaceMember`](#workspacemember)
  - [`WorkspaceRole`](#workspacerole)
  - [`WorkspacePermission`](#workspacepermission)
  - [`WorkspaceInvite`](#workspaceinvite)
  - [`WorkspaceApiKey`](#workspaceapikey)
  - [`ReservedContactField`](#reservedcontactfield)
- [Workspace DTO Records](#workspace-dto-records)
- [Workspace Repositories](#workspace-repositories)
- [Channel Backend](#channel-backend)
  - [`ChannelAccountController`](#channelaccountcontroller)
  - [`ChannelAccountService`](#channelaccountservice)
  - [`ChannelAccountMapper`](#channelaccountmapper)
- [Channel Domain Entities And Enums](#channel-domain-entities-and-enums)
  - [`ChannelAccount`](#channelaccount)
  - [`ChannelProvider`](#channelprovider)
  - [`ChannelAccountStatus`](#channelaccountstatus)
- [Channel DTO Records](#channel-dto-records)
- [Channel Repositories](#channel-repositories)
- [Contact Backend](#contact-backend)
  - [`ContactController`](#contactcontroller)
  - [`ContactService`](#contactservice)
  - [`ContactMapper`](#contactmapper)
  - [`ReservedContactFieldResolver`](#reservedcontactfieldresolver)
- [Contact Domain Entities And Enums](#contact-domain-entities-and-enums)
  - [`Contact`](#contact)
  - [`ExternalIdentity`](#externalidentity)
- [Contact DTO Records](#contact-dto-records)
- [Contact Repositories](#contact-repositories)
- [Messaging Backend](#messaging-backend)
  - [`MessagingController`](#messagingcontroller)
  - [`MessagingService`](#messagingservice)
  - [`MessagingMapper`](#messagingmapper)
  - [`MessagingExceptionHandler`](#messagingexceptionhandler)
  - [`ConversationLockedException`](#conversationlockedexception)
  - [`OutboundMessageEvent`](#outboundmessageevent)
- [Messaging Domain Entities And Enums](#messaging-domain-entities-and-enums)
  - [`Conversation`](#conversation)
  - [`ConversationStatus`](#conversationstatus)
  - [`Message`](#message)
  - [`MessageDirection`](#messagedirection)
  - [`MessageSenderType`](#messagesendertype)
- [Messaging DTO Records](#messaging-dto-records)
- [Messaging Repositories](#messaging-repositories)
- [Public API Backend](#public-api-backend)
  - [`PublicApiController`](#publicapicontroller)
- [Webhook And Email Backend](#webhook-and-email-backend)
  - [`WebhookService`](#webhookservice)
  - [`WebhookDispatchService`](#webhookdispatchservice)
  - [`WebhookController`](#webhookcontroller)
  - [`EmailService` And `SmtpEmailService`](#emailservice-and-smtpemailservice)
  - [`WorkspaceWebhook`](#workspacewebhook)
  - [`WebhookEventType`](#webhookeventtype)
  - [`ContactSnapshotBuilder`](#contactsnapshotbuilder)
  - [`ContactSnapshot`](#contactsnapshot)
- [SSE Backend](#sse-backend)
  - [`SseController`](#ssecontroller)
  - [`WorkspaceSseService`](#workspacesseservice)
  - [`SseBroadcastEvent`](#ssebroadcastevent)
  - [`SseEventType`](#sseeventtype)
- [Telegram Backend](#telegram-backend)
  - [`TelegramController`](#telegramcontroller)
  - [`TelegramAdapter`](#telegramadapter)
  - [`TelegramWebhookRegistrar`](#telegramwebhookregistrar)
  - [`TelegramSendException`](#telegramsendexception)
  - [Telegram DTO Records](#telegram-dto-records)
- [WhatsApp Backend](#whatsapp-backend)
  - [`WhatsAppController`](#whatsappcontroller)
  - [`WhatsAppAdapter`](#whatsappadapter)
  - [`WhatsAppCredentials`](#whatsappcredentials)
  - [`WhatsAppSendException`](#whatsappsendexception)
  - [WhatsApp DTO Records](#whatsapp-dto-records)
- [Workflow Backend](#workflow-backend)
  - [`WorkflowController`](#workflowcontroller)
  - [`WorkflowService`](#workflowservice)
  - [`WorkflowGraphValidator`](#workflowgraphvalidator)
  - [`WorkflowValidationException`](#workflowvalidationexception)
- [Workflow Domain Entities And Enums](#workflow-domain-entities-and-enums)
  - [`NodeType`](#nodetype)
  - [`WorkflowDefinition`](#workflowdefinition)
  - [`WorkflowRun`](#workflowrun)
  - [`WorkflowRunStep`](#workflowrunstep)
  - [`WorkflowRunStatus`](#workflowrunstatus)
  - [`WorkflowRunStepStatus`](#workflowrunstepstatus)
- [Workflow DTO Records](#workflow-dto-records)
- [Workflow Engine Components](#workflow-engine-components)
  - [`WorkflowEngineService`](#workflowengineservice)
  - [`ExecutionContext`](#executioncontext)
  - [`VariableInterpolator`](#variableinterpolator)
  - [`GraphNode`](#graphnode)
  - [`GraphEdge`](#graphedge)
  - [`NodeExecutor`](#nodeexecutor)
  - [`NodeExecutionResult`](#nodeexecutionresult)
  - [`NodeExecutionException`](#nodeexecutionexception)
  - [Workflow Trigger Events](#workflow-trigger-events)
  - [`WorkflowTriggerListener`](#workflowtriggerlistener)
  - [`WorkflowResumeListener`](#workflowresumelistener)
- [Workflow Node Executors](#workflow-node-executors)
  - [`TriggerNodeExecutor`](#triggernodeexecutor)
  - [`SendMessageNodeExecutor`](#sendmessagenodeexecutor)
  - [`ConditionNodeExecutor`](#conditionnodeexecutor)
  - [`HttpRequestNodeExecutor`](#httprequestnodeexecutor)
  - [`SetVariableNodeExecutor`](#setvariablenodeexecutor)
  - [`SetContactFieldNodeExecutor`](#setcontactfieldnodeexecutor)
  - [`WaitForReplyNodeExecutor`](#waitforreplynodeexecutor)
  - [`JumpToNodeExecutor`](#jumptonodeexecutor)
  - [`EndConversationNodeExecutor`](#endconversationnodeexecutor)
- [Workflow Repositories](#workflow-repositories)
- [Frontend Route Components](#frontend-route-components)
  - [Root Layout Components](#root-layout-components)
  - [Root Route](#root-route)
  - [Auth Pages](#auth-pages)
  - [Setup Page](#setup-page)
  - [Inbox Pages](#inbox-pages)
  - [Workflow Pages](#workflow-pages)
  - [Contacts Pages](#contacts-pages)
  - [Invite Pages](#invite-pages)
  - [Reset Password Page](#reset-password-page)
  - [Profile Pages](#profile-pages)
  - [Settings Pages](#settings-pages)
- [Frontend Common Components](#frontend-common-components)
  - [`Spinner`](#spinner)
  - [`EmptyState`](#emptystate)
  - [`ConfirmModal`](#confirmmodal)
  - [`GoogleIcon`](#googleicon)
  - [`CopyButton`](#copybutton)
  - [`Select`](#select)
- [Frontend Providers](#frontend-providers)
  - [`QueryProvider`](#queryprovider)
  - [`ToastProvider`](#toastprovider)
- [Frontend Workspace Components](#frontend-workspace-components)
  - [`WorkspaceNav`](#workspacenav)
  - [`WorkspaceSwitcher`](#workspaceswitcher)
  - [`CreateWorkspaceForm`](#createworkspaceform)
- [Frontend Inbox Components](#frontend-inbox-components)
  - [`InboxShell`](#inboxshell)
  - [`ConversationList`](#conversationlist)
  - [`ConversationItem`](#conversationitem)
  - [`MessageThread`](#messagethread)
  - [`MessageBubble`](#messagebubble)
  - [`MessageComposer`](#messagecomposer)
- [Frontend Contacts Components](#frontend-contacts-components)
  - [`ContactsShell`](#contactsshell)
  - [`ContactDetailPanel`](#contactdetailpanel)
  - [`MergeContactModal`](#mergecontactmodal)
- [Frontend Profile Components](#frontend-profile-components)
  - [`ProfileShell`](#profileshell)
  - [`ConnectTelegramForm`](#connecttelegramform)
  - [`ConnectWhatsAppForm`](#connectwhatsappform)
- [Frontend Settings Components](#frontend-settings-components)
  - [`SettingsShell`](#settingsshell)
  - [`ChannelsList`](#channelslist)
  - [`MembersList`](#memberslist)
  - [`IntegrationsPanel`](#integrationspanel)
  - [`GeneralPanel`](#generalpanel)
  - [`CreateApiKeyModal`](#createapikeymodal)
  - [`WebhookConfigPanel`](#webhookconfigpanel)
- [Frontend Workflow Builder Components](#frontend-workflow-builder-components)
  - [`WorkflowEditor`](#workfloweditor)
  - [`EditorCanvas`](#editorcanvas)
  - [`NodeConfigPanel`](#nodeconfigpanel)
  - [`VariablePicker`](#variablepicker)
  - [`WorkflowsShell`](#workflowsshell)
  - [`WorkflowsList`](#workflowslist)
  - [`DeletableEdge`](#deletableedge)
- [Frontend Workflow Node Components](#frontend-workflow-node-components)
  - [`WorkflowNode`](#workflownode)
  - [`TriggerNode`](#triggernode)
  - [`SendMessageNode`](#sendmessagenode)
  - [`ConditionNode`](#conditionnode)
  - [`HttpRequestNode`](#httprequestnode)
  - [`SetVariableNode`](#setvariablenode)
  - [`SetContactFieldNode`](#setcontactfieldnode)
  - [`WaitForReplyNode`](#waitforreplynode)
  - [`JumpToNode`](#jumptonode)
  - [`EndConversationNode`](#endconversationnode)
  - [`nodes/index.ts`](#nodesindexts)
- [Frontend Hooks](#frontend-hooks)
  - [Auth Hooks](#auth-hooks)
  - [Profile Hooks](#profile-hooks)
  - [Workspace Hooks](#workspace-hooks)
  - [Channel Hooks](#channel-hooks)
  - [Contact Hooks](#contact-hooks)
  - [Inbox Hooks](#inbox-hooks)
  - [AI Agent Hooks](#ai-agent-hooks)
  - [Workflow Hooks](#workflow-hooks)
  - [Integration Hooks](#integration-hooks)
- [Frontend API Libraries](#frontend-api-libraries)
  - [`authentication-api.ts`](#authentication-apits)
  - [`messaging-api.ts`](#messaging-apits)
  - [`server-authentication.ts`](#server-authenticationts)
  - [`error-message.ts`](#error-messagets)
  - [`proxy.ts`](#proxyts)
- [Frontend Tests](#frontend-tests)
  - [`page.test.tsx`](#pagetesttsx)
  - [`ConversationList.test.tsx`](#conversationlisttesttsx)
  - [`WorkspaceSwitcher.test.tsx`](#workspaceswitchertesttsx)
  - [`messaging-api.test.ts`](#messaging-apitestts)
- [Contracts And Schemas](#contracts-and-schemas)
  - [`openapi.yaml`](#openapiyaml)
  - [`message-event.schema.json`](#message-eventschemajson)
  - [`webhook-event.schema.json`](#webhook-eventschemajson)
  - [`workflow-definition.schema.json`](#workflow-definitionschemajson)
- [Frontend Configuration Constants](#frontend-configuration-constants)
  - [`tailwind.config.ts`](#tailwindconfigts)
  - [`globals.css`](#globalscss)
  - [`next.config.mjs`, `postcss.config.mjs`, `eslint.config.mjs`, `vitest.config.ts`](#nextconfigmjs-postcssconfigmjs-eslintconfigmjs-vitestconfigts)
- [AI Agent Backend](#ai-agent-backend)
  - [`AiAgentConfiguration`](#aiagentconfiguration)
  - [`AiAgentInvocationLog`](#aiagentinvocationlog)
  - [`ConversationAiDraft`](#conversationaidraft)
  - [`AiAgentConfigurationService`](#aiagentconfigurationservice)
  - [`AiAgentConfigurationController`](#aiagentconfigurationcontroller)
  - [`ConversationAiDraftController`](#conversationaidraftcontroller)
  - [`AiAgentInvocationSlotClaimer`](#aiagentinvocationslotclaimer)
  - [`AiAgentInvocationService`](#aiagentinvocationservice)
  - [`AiAgentTriggerListener`](#aiagenttriggerlistener)
  - [`AiAgentContextAssembler`](#aiagentcontextassembler)
  - [`ContactCustomFieldWriter`](#contactcustomfieldwriter)
  - [`AgentWorkflowContext`](#agentworkflowcontext)
  - [`AiAgentInvocationCleanupScheduler`](#aiagentinvocationcleanupscheduler)
  - [LLM Abstraction](#llm-abstraction)
  - [AI Agent DTO Records](#ai-agent-dto-records)
  - [AI Agent Repositories](#ai-agent-repositories)
- [AI Agent Frontend Components](#ai-agent-frontend-components)
  - [`AiAgentPanel`](#aiagentpanel)
  - [`AiDraftBanner`](#aidraftbanner)
- [`apps/marketing`](#appsmarketing)
  - [`deployment-artifacts.ts` And The Self-Hosting Docs Page](#deployment-artifactsts-and-the-self-hosting-docs-page)

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
- `SecurityFilterChain`: permits public auth, health, and Telegram/WhatsApp webhook paths; protects the rest.
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
- `bootstrapStatus`: returns whether this self-hosted instance has completed first-run setup (`userRepository.count() == 0`). Polled by the frontend to decide between `/setup`, `/login`, and `/inbox`.
- `bootstrap`: one-time first-run endpoint that creates the initial admin account and workspace and establishes a session. Only succeeds while the instance has zero users; a second call returns `409`.
- `signup`: creates an email/password account from an accepted invite token (see [Invite-Gated Signup](#workspaceinviteservice)) and establishes a session. There is no open public signup — an account can only be created by bootstrapping the instance or accepting a workspace invite.
- `login`: authenticates credentials and either establishes a session or returns a 2FA challenge.
- `login2FA`: verifies a TOTP code for a pending login challenge and establishes a session.
- `resetPassword`: public — consumes a `PasswordResetToken` and sets a new password. See [`PasswordResetService`](#passwordresetservice).
- `logout`: invalidates the server session and expires the `JSESSIONID` browser cookie.

We need it as the public API boundary for session state.

### `AuthenticationService`

Business logic for authentication.

Important methods:

- `signup`: rejects if the email is already registered, creates the `User`/`UserPreferences`/`UserIdentity` rows, marks the email identity verified without a round-trip (the invite itself is the vouch — `WorkspaceInviteService.acceptInvite` already applies a case-insensitive email match and rejects revoked/expired invites), then calls `acceptInvite` to add the user to the invite's workspace. The whole method is `@Transactional`, so a bad/expired/mismatched invite token rolls back the user rows created above.
- `bootstrap`: guarded by `userRepository.count() == 0` (throws `409` otherwise). Creates the admin `User`/`UserPreferences`/`UserIdentity`, marks the identity verified for the same reason as signup (no mail server is guaranteed to be configured yet on a fresh instance), creates the first workspace via `WorkspaceService.createWorkspace`, and establishes a session.
- `getInstanceStatus`: read-only check of `userRepository.count() > 0`, backing `bootstrapStatus`.
- `login`: authenticates through Spring Security and returns user profile state or a short-lived 2FA challenge.
- `login2FA`: verifies the TOTP challenge and establishes a session.
- `getCurrentUser`: normalizes OAuth and email principals into `AuthenticatedUserResponse`.
- `establishSession`: writes an authenticated security context into the session.
- `establishSessionForUser`: creates a session without requiring plaintext password after signup, instance bootstrap, or 2FA completion.

We need it to keep controller code thin and centralize session/user lifecycle rules.

### `EmailPasswordUserDetailsService`

Loads RelayFlow users by email for Spring Security's DAO authentication provider.

We need it so the Spring authentication manager can validate email/password login against the database.

### `OAuth2UserProvisioningService`

Extends Spring's OAuth user service to provision or update users after Google login.

We need it so Google login creates a local `User` record and returns consistent profile data.

### `PasswordResetService`

`@Service` in `com.relayflow.api.authentication`. There is no self-service "change password" form — this is the only way a password gets set after signup.

Important methods:

- `issueForUser(userId)`: creates a `PasswordResetToken` and emails a `{webBaseUrl}/reset-password/{token}` link to the user's address. Throws `400` if the user has no `EMAIL` identity (e.g. a Google-only account — nothing to reset). Shared by both callers: `ProfileService.requestPasswordReset` (self-service) and `WorkspaceService.generatePasswordResetForMember` (owner-on-behalf-of-a-member), so the shape can't drift between them.
- `resetPassword(token, newPassword)`: validates the token (`PasswordResetToken.isValid()` — exists, unused, unexpired), sets the new BCrypt-encoded credential on the `EMAIL` identity, and marks the token used.

We need it as the single place password-setting logic lives, since it's reached from two different permission contexts (self, and workspace owner).

### `PasswordResetToken`

JPA entity mapped to `password_reset_tokens`, in `com.relayflow.api.authentication.domain`. Modeled on `WorkspaceInvite` — `token` is a random `UUID` (unique), `expires_at` is stamped 60 minutes past `created_at` in `@PrePersist`, and `used_at` (rather than a status enum or soft-delete column) tracks consumption. `isValid()` returns `usedAt == null && now < expiresAt`.

We need it so a reset link is single-use and time-boxed without a separate session-tracking mechanism.

### `ProfileController`

HTTP controller for current-user profile and security routes.

Endpoints:

- `getProfile`
- `updateProfile`
- `requestPasswordReset`: emails the current user a password reset link via `PasswordResetService.issueForUser`.
- `deleteAccount`
- `setup2FA`
- `enable2FA`
- `disable2FA`

We need it to keep account management separate from login/session endpoints.

### `ProfileService`

Business service for profile, account deletion, preferences, and MFA state.

Important methods:

- `getProfile`
- `updateProfile`
- `requestPasswordReset`: thin delegate to `PasswordResetService.issueForUser`.
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

Utility component for resolving the current RelayFlow user ID from Spring `Authentication`. Handles both email/password (`UserDetails`) and Google OAuth2 (`OAuth2User`) principals by resolving the principal's email and looking up the `User` row.

We need it because controllers should not duplicate principal parsing logic.

### `User`

JPA entity mapped to `users`.

Important fields:

- `id`: UUID primary key.
- `email`: login identifier.
- `displayName`, `avatarUrl`: profile fields.
- `createdAt`, `updatedAt`, `deletedAt`: lifecycle fields (soft delete via `deletedAt`).

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

JPA entity for per-user preferences — currently just `userId`/`createdAt`/`updatedAt` with no preference fields of its own (the one it had, `receiveEmailUpdates`, was removed along with the marketing-email opt-in checkbox now that RelayFlow is self-hosted-only). Kept as the landing spot for any future per-user setting rather than deleted outright.

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

### `AuthenticationProvider`

Enum of supported identity providers: email and Google.

We need it so user records can be interpreted correctly during login.

### `MfaMethodType`

Enum of MFA method types: TOTP and SMS.

We need it so MFA support can grow beyond TOTP without redesigning the table.

### Auth DTO Records

- `SignupRequest`: name, email, password, and the required `inviteToken` — signup cannot happen without an accepted invite.
- `SignupResponse`: authenticated state, user ID, email, display name, and the workspace ID the invite granted access to.
- `BootstrapRequest`: admin name, email, password, and initial workspace name for first-run instance setup.
- `BootstrapResponse`: same shape as `SignupResponse`, returned after the admin account and workspace are created.
- `InstanceStatusResponse`: `bootstrapped` flag — whether `POST /auth/bootstrap` has already been used on this instance.
- `LoginRequest`: email and password.
- `Login2FARequest`: 2FA challenge token and OTP.
- `AuthenticatedUserResponse`: normalized auth/session state for the frontend.
- `ResetPasswordRequest`: new password only — no current password, since possessing a valid token is the proof.

We need these records as stable API contracts between backend and frontend.

### Profile DTO Records

- `ProfileResponse`: user profile, providers, and 2FA state.
- `UpdateProfileRequest`: display name.
- `DeleteAccountRequest`: optional password for account deletion.
- `Setup2FAResponse`: `otpauth://` URI for QR display.
- `OtpRequest`: authenticator code.

We need these records for account-management UI/backend contracts.

### `UserRepository`

Spring Data repository for `User`. Also backs `AuthenticationService.getInstanceStatus`/`bootstrap` via `count()`.

Important queries include lookup by email.

We need it to keep persistence access declarative and testable.

### Authentication Satellite Repositories

- `UserIdentityRepository`: looks up identities by provider subject or user/provider.
- `UserPreferencesRepository`: stores per-user preferences.
- `UserMfaMethodRepository`: stores enabled/pending MFA methods.
- `TwoFactorChallengeRepository`: stores short-lived 2FA login challenges.
- `PasswordResetTokenRepository`: looks up a `PasswordResetToken` by its token UUID.

We need these repositories because login methods, profile preferences, and MFA are deliberately split out of the core `User` table.

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

## Common Backend

`com.relayflow.api.common` — small infrastructure shared across every other backend package, not owned by any single domain.

### `ResourceNotFoundException`

Runtime exception used when workspace-scoped data is missing or not accessible in that workspace.

We need it to avoid leaking whether records exist outside the requested workspace.

### `PageResponse`

`record PageResponse<T>(List<T> items, boolean hasMore, String nextCursor)` — generic paginated API response, used by every list endpoint across `workspace`, `channel`, `contact`, and `messaging`.

We need it so pagination shape doesn't drift between domains.

### `ErrorResponse`

`record ErrorResponse(String message, Instant timestamp)` — normalized API error body.

We need it so the frontend receives a consistent error shape regardless of which domain threw.

### `CommonExceptionHandler`

Global REST exception handler for exceptions that aren't specific to one domain — domain-specific exceptions (`ConversationLockedException`, `TelegramSendException`, `WorkflowValidationException`) each have their own small `@RestControllerAdvice` in their own package instead; Spring composes `@ExceptionHandler` methods across every advice bean regardless of which one declares them.

Handles:

- `ResourceNotFoundException` as `404`.
- `IllegalArgumentException` as `409`.
- validation errors as `400`.
- missing required request parameters as `400`.
- `AccessDeniedException` as `403`.
- `ResponseStatusException` — preserves its own status and reason.
- `AsyncRequestTimeoutException` (SSE emitter timeout) — bodiless `503`.
- unexpected exceptions as `500` with a generic message.

We need it so the frontend receives consistent `{ message, timestamp }` error responses.

### `MapUtils`

`copyMap(source)`: returns a mutable `LinkedHashMap` copy of `source`, or an empty one if `source` is null. Used by `ChannelAccountService`, `ContactService`, and `MessagingService` to defensively copy an incoming request's JSONB-bound map field (`metadata`, `rawProfile`, `rawPayload`) before persisting it.

We need it because that copy-or-empty logic was duplicated identically in all three services after the messaging domain split.

## Workspace Backend

`com.relayflow.api.workspace` — workspace CRUD, membership, invites, and API keys. Owns the tenant boundary the other domains (`channel`, `contact`, `messaging`) are scoped within.

### `WorkspaceController`

HTTP controller for workspace and workspace-member routes.

Important methods:

- `listWorkspaces`, `createWorkspace`
- `updateWorkspace`, `updateContactFieldDefinitions`
- `listMembers`, `inviteMember`, `updateMember`, `removeMember`, `generatePasswordResetForMember` — owner-only
- `transferOwnership`

We need it as the REST boundary for workspace settings and team management.

### `WorkspaceService`

Business service for workspace and membership persistence.

Important methods:

- `createWorkspace`: creates a workspace and owner membership. Used both by `AuthenticationService.bootstrap` (first-run instance setup) and by the authenticated "create another workspace" flow.
- `listWorkspaces`: lists workspaces by membership. Also used by `SseController` to check that a subscriber is a member of the workspace it's connecting to.
- `updateContactFieldDefinitions`: validates no key collides with a `ReservedContactField` key, then replaces `Workspace.contactFieldDefinitions`.
- `listWorkspaceMembers`, `inviteWorkspaceMember`, `updateWorkspaceMember`, `removeWorkspaceMember`, `transferOwnership`: owner/member management with single-owner safeguards.
- `generatePasswordResetForMember`: resolves `WorkspaceMember` → `userId`, then delegates to `PasswordResetService.issueForUser` — the same issuing path a member's own self-service request uses.
- `deleteWorkspace`: removes all workspace-owned data — reaches into `channel`/`contact`/`messaging`/`workflow`/`agent` repositories directly for the cascade, since those domains don't expose their own bulk-delete-by-workspace service method.
- `getWorkspace`: looks up a workspace by ID or throws `ResourceNotFoundException`. Called by `ChannelAccountService`, `ContactService`, and `MessagingService` to validate a workspace exists before scoping a query to it — the one piece of cross-domain API `workspace` exposes to the other three.

We need it because workspace/membership has cross-entity rules that should not live in controllers or repositories.

### `WorkspaceMapper`

Maps the `Workspace` JPA entity to `WorkspaceResponse`.

We need it to isolate the API response shape from the persistence entity shape.

### `WorkspaceAuthorizationService`

Service that resolves the authenticated user's workspace membership and checks owner or granular permissions. Used by every controller across `workspace`, `channel`, `contact`, and `messaging` that needs to authorize a workspace-scoped request.

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
- `acceptInvite(token, userId)`: idempotent on `ACCEPTED` (returns the workspace ID so the caller can redirect), throws `410 GONE` on `REVOKED`/`EXPIRED`, and requires a case-insensitive match between the accepting user's email and the invite's email (`403` otherwise).

There is no open public signup endpoint — `AuthenticationController.signup` calls `AuthenticationService.signup`, which creates the user rows and then calls `acceptInvite` directly to add the new user to the invite's workspace, reusing all of the above validation. An invite is the only way to create an account outside of first-run instance bootstrap (see [Instance Bootstrap](#authenticationcontroller)).

We need it for controlled onboarding of additional workspace users, and now for gating account creation entirely.

### `WorkspaceInviteController`

REST controller for workspace invites.

Endpoints:

- `listInvites`
- `createInvite`
- `revokeInvite`
- `previewInvite`
- `acceptInvite`

We need it for both owner invite management and the public invite acceptance flow.

## Workspace Domain Entities And Enums

### `Workspace`

JPA entity for a company/team workspace.

Important fields:

- `name`
- `contactFieldDefinitions`: `List<ContactFieldDefinition>` JSONB — the workspace's custom contact field schema. A key here can't collide with a `ReservedContactField` key (enforced in `WorkspaceService.updateContactFieldDefinitions`).

We need it as the tenant boundary for conversations, channels, workflows, and members.

### `ContactFieldDefinition`

`record ContactFieldDefinition(String key, String label, String description)` — one entry in `Workspace.contactFieldDefinitions`.

We need it so operators can define custom fields the AI agent extracts and/or a workflow writes onto a contact, beyond the four reserved fields.

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
- `CONTACT_FIELDS_WRITE`
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

### `ReservedContactField`

Enum: `DISPLAY_NAME` (`"displayName"`), `FIRST_NAME` (`"firstName"`), `LAST_NAME` (`"lastName"`), `PHONE` (`"phone"`), `EMAIL` (`"email"`), `COUNTRY` (`"country"`). Contact fields RelayFlow already derives automatically — a workspace can't redefine any of these as a custom field. Lives in `workspace.domain` (validated against `Workspace.contactFieldDefinitions`), even though the resolver that uses it (`ReservedContactFieldResolver`) lives in `contact`.

`isReserved(candidateKey)` does a case-insensitive comparison against each enum value's `key()` (`field.key.equalsIgnoreCase(trimmed)`) — both sides must be compared case-insensitively, not just the candidate, since a key like `displayName` isn't all-lowercase.

Carries no label or description — those are presentation copy that live in the frontend (`RESERVED_CONTACT_FIELDS` in `messaging-api.ts`), mirroring how `WorkspacePermission` carries no display copy either.

We need it as the single source of truth for which keys are reserved, used by `ReservedContactFieldResolver`, `ContactCustomFieldWriter`, `SetContactFieldNodeExecutor`, and `WorkspaceService.updateContactFieldDefinitions`'s validation.

## Workspace DTO Records

- `CreateWorkspaceRequest`, `UpdateWorkspaceRequest`, `WorkspaceResponse`
- `UpdateContactFieldDefinitionsRequest`
- `InviteMemberRequest`, `UpdateMemberRequest`, `WorkspaceMemberResponse`
- `CreateInviteRequest`, `WorkspaceInviteResponse`, `InvitePreviewResponse`
- `CreateApiKeyRequest`, `ApiKeyResponse`, `CreateApiKeyResponse`

We need these records to keep frontend/backend data exchange explicit and stable.

## Workspace Repositories

- `WorkspaceRepository`
- `WorkspaceMemberRepository`
- `WorkspaceInviteRepository`
- `WorkspaceApiKeyRepository`

These are Spring Data persistence interfaces. Their custom query methods express workspace lookup, member permission lookup, invite lookup, API key lookup by hash, and cleanup deletes.

We need them so services do not contain SQL or persistence boilerplate.

## Channel Backend

`com.relayflow.api.channel` — connected messaging channels (Telegram, WhatsApp, ...) that route inbound/outbound messages.

### `ChannelAccountController`

HTTP controller for channel account routes.

Important methods:

- `listChannelAccounts`, `createChannelAccount`, `disconnectChannelAccount`, `reconnectChannelAccount`

We need it as the REST boundary for channel connection management.

### `ChannelAccountService`

Business service for channel account persistence.

Important methods:

- `createChannelAccount`: persists encrypted channel credentials and registers Telegram webhook.
- `disconnectChannelAccount` / `reconnectChannelAccount`: toggles channel availability without deleting history.
- `getChannelAccount`: looks up a channel account within a workspace or throws `ResourceNotFoundException`. Called by `ContactService` (creating an external identity) and `MessagingService` (creating a conversation).

We need it because channel account provisioning has credential-encryption and webhook-registration rules that should not live in a controller.

### `ChannelAccountMapper`

Maps the `ChannelAccount` JPA entity to `ChannelAccountResponse`.

We need it to isolate the API response shape from the persistence entity shape.

## Channel Domain Entities And Enums

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

## Channel DTO Records

- `CreateChannelAccountRequest`, `ChannelAccountResponse`

We need these records to keep frontend/backend data exchange explicit and stable.

## Channel Repositories

- `ChannelAccountRepository`

Spring Data persistence interface. Its custom query methods express workspace scoping, active channel filtering, and cleanup deletes.

We need it so the service does not contain SQL or persistence boilerplate.

## Contact Backend

`com.relayflow.api.contact` — customer contacts and their per-channel external identities.

### `ContactController`

HTTP controller for contact and external identity routes.

Important methods:

- `listContacts`, `createContact`, `getContact`, `mergeContacts`, `deleteContact`
- `updateContactCustomFields` — requires `CONTACT_FIELDS_WRITE` permission or owner role
- `createExternalIdentity`

We need it as the REST boundary for the contacts UI and channel adapters creating identities.

### `ContactService`

Business service for contact and external identity persistence.

Important methods:

- `listContacts`, `createContact`, `getContactDetail`, `mergeContacts`, `deleteContact`
- `updateContactCustomFields`: replaces `Contact.customFields`, then returns `getContactDetail`.
- `getContactDetail`: merges `ReservedContactFieldResolver`'s auto-derived values with `Contact.customFields` — an explicitly-stored value always overrides a derived one.
- `createExternalIdentity`
- `getContact`: looks up a contact within a workspace or throws `ResourceNotFoundException`. Called by `MessagingService` when creating a conversation.

We need it because contacts have cross-entity rules (merge reassignment, derived-field precedence) that should not live in controllers or repositories.

### `ContactMapper`

Maps `Contact` and `ExternalIdentity` JPA entities to their response DTOs.

We need it to isolate the API response shape from the persistence entity shape.

### `ReservedContactFieldResolver`

`@Component` in the `contact` package. `resolve(contact, identities)` returns the reserved contact field values ({@link ReservedContactField}) that can already be derived from existing data, without asking the AI agent or an operator: `displayName` from `Contact.displayName`, `firstName`/`lastName` from a Telegram `ExternalIdentity`'s `rawProfile` (populated by `TelegramAdapter.buildRawProfile` from the inbound update's `from` user), `phone` (and, via `libphonenumber`, `country`) from a WhatsApp or SMS `ExternalIdentity`, `email` from an EMAIL identity. A key is omitted (not included with an empty value) when nothing can be derived.

Used by `ContactService.getContactDetail` to seed a contact's `customFields` response before the explicitly-stored values overwrite it (derived values only ever fill a gap, never override an explicit one), and by `ContactCustomFieldWriter` to check whether a reserved key is already effectively known before writing an AI extraction.

We need it so reserved fields are auto-filled wherever RelayFlow can already derive them, without hand-coding a Telegram-specific "share contact" flow or similar one-off hack for channels that don't expose the data.

## Contact Domain Entities And Enums

### `Contact`

JPA entity for a customer/contact inside a workspace.

Important fields:

- `displayName`
- `customFields`: `Map<String, String>` JSONB — explicitly-stored custom field values, keyed by a reserved key (see `ReservedContactField`) or a workspace-defined `ContactFieldDefinition` key. Written by manual edits, AI extraction (`ContactCustomFieldWriter`), or a workflow's Set Contact Field node. `ContactService.getContactDetail` merges this with `ReservedContactFieldResolver`'s auto-derived values for the API response — an explicitly-stored value always wins.

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

## Contact DTO Records

- `CreateContactRequest`, `ContactResponse`, `ContactDetailResponse`, `MergeContactRequest`
- `UpdateContactCustomFieldsRequest`
- `CreateExternalIdentityRequest`, `ExternalIdentityResponse`

We need these records to keep frontend/backend data exchange explicit and stable.

## Contact Repositories

- `ContactRepository`
- `ExternalIdentityRepository`

These are Spring Data persistence interfaces. Their custom query methods express workspace scoping, pagination, contact merge reassignment, and cleanup deletes.

We need them so services do not contain SQL or persistence boilerplate.

## Messaging Backend

`com.relayflow.api.messaging` — conversations and messages, the inbox core. Depends on `workspace`, `channel`, and `contact` for validating the parties a conversation references, but nothing in those three depends back on it.

### `MessagingController`

HTTP controller for conversation and message routes.

Important methods:

- `createConversation`, `listConversations`, `getConversation`, `updateConversation`
- `listMessages`, `createMessage`

We need it as the REST boundary for the inbox UI.

### `MessagingService`

Business service for conversation and message persistence.

Important methods:

- `createExternalIdentity`, `createConversation`
- `listConversations`, `getConversation`, `updateConversationStatus`, `listMessages`
- `createMessage`: stores messages, updates conversation timestamps, emits SSE events, and publishes outbound delivery events. Also auto-assigns an unassigned conversation to the first agent who replies, and clears `Conversation.escalatedAt`/`escalationReason` on that same first human (`MessageSenderType.AGENT`) outbound reply.

We need it because messaging has cross-entity rules that should not live in controllers or repositories.

### `MessagingMapper`

Maps `Conversation` and `Message` JPA entities to their response DTOs.

We need it to isolate the API response shape from the persistence entity shape.

### `MessagingExceptionHandler`

`@RestControllerAdvice` scoped to this package's one domain-specific exception. See [`CommonExceptionHandler`](#commonexceptionhandler) for the generic handlers shared across all domains.

Handles:

- `ConversationLockedException` as `409`.

We need it so a locked-conversation write attempt gets a clear, specific response instead of falling through to a generic `500`.

### `ConversationLockedException`

Runtime exception thrown when an agent or public API client tries to send a message while an active workflow owns the conversation.

We need it to enforce workflow-driven conversations without silently dropping agent replies.

### `OutboundMessageEvent`

Application event carrying a saved outbound message, its channel account, and optional button option labels.

We need it to decouple message persistence from provider delivery. The messaging service saves the message; adapters deliver it, optionally rendering Ask Question options as provider-native controls.

## Messaging Domain Entities And Enums

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
- `escalatedAt` / `escalationReason`: set by `AiAgentInvocationService.broadcastEscalation` when the AI agent escalates (keyword match or LLM-requested); cleared by `MessagingService.createMessage` on the next outbound message from a human agent (`MessageSenderType.AGENT`).
- `sessionStartedAt`: reset to `now()` whenever the conversation is reopened. Used by `AiAgentContextAssembler` to scope history to the current session only, preventing old closed-conversation messages from polluting the LLM context.

We need it as the inbox unit users read, select, and reply to. `lockedByWorkflow` and `lockedByAiAgent` prevent agents from interrupting active automation; `escalatedAt` surfaces conversations that need a human without gating who can reply.

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

- `CreateConversationRequest`, `UpdateConversationRequest`, `ConversationResponse`
- `CreateMessageRequest`, `MessageResponse`

We need these records to keep frontend/backend data exchange explicit and stable.

## Messaging Repositories

- `ConversationRepository`
- `MessageRepository`

These are Spring Data persistence interfaces. Their custom query methods express workspace scoping, conversation lookup, message cursors, and cleanup deletes.

We need them so services do not contain SQL or persistence boilerplate.

## Public API Backend

### `PublicApiController`

API-key-authenticated controller under `/public/v1`, in `com.relayflow.api.publicapi`.

Endpoints:

- `listConversations`
- `getConversation`
- `listMessages`
- `sendMessage`

We need it so third-party systems can inspect conversations and send outbound replies for a workspace.

## Webhook And Email Backend

### `WebhookService`

Creates, updates, deletes, retrieves, and rotates workspace webhook configuration, in `com.relayflow.api.webhook`.

Important behavior:

- the signing secret is never client-supplied. Creating a webhook auto-generates one, returned once as `generatedSecret` on that response only.
- `saveWebhook` never touches the secret, on create or update — changing it is only possible through `rotateSecret`.

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

### `EmailService` And `SmtpEmailService`

Email abstraction and its SMTP-backed implementation for transactional emails: `sendInvite`,
`sendEmailVerification`, `sendPasswordReset`, `sendDowngradeNotice`. SMTP works with any provider
(Resend, SES, Mailgun, Postmark, Gmail, a self-hosted mail server) rather than locking self-hosters
into one vendor's REST API. When `smtp.host` is blank, every method falls back to logging the link
at `INFO` instead of sending, so these flows are testable locally with no mail server.

We need them so invite/verification/reset delivery can be swapped or disabled without changing the domain logic that calls them.

### `WorkspaceWebhook`

JPA entity for a workspace's outbound webhook configuration, in `com.relayflow.api.webhook.domain`.

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

Enum of outbound webhook event types, in `com.relayflow.api.webhook.domain` alongside `WorkspaceWebhook`.

Values:

- `CONTACT_CREATED` maps to payload event name `contact.created`.
- `CONTACT_UPDATED` maps to payload event name `contact.updated` — dispatched from every contact-field write path: manual edit (`ContactService.updateContactCustomFields`), AI extraction (`ContactCustomFieldWriter`), and the workflow Set Contact Field node (`SetContactFieldNodeExecutor`).

We need it so persisted webhook subscriptions and dispatched payload names stay aligned.

### `ContactSnapshotBuilder`

Static builder in `com.relayflow.api.webhook` (not `.domain` — it's a builder/utility, not a domain entity or enum, so it stays alongside `WebhookDispatchService`/`WebhookService`). `build(contact)` returns a `ContactSnapshot`, shared by all three write paths above so the shape can't drift between them. Custom field values are flattened directly onto the contact map (`{id, displayName, orderNumber: "123", ...}`), not nested under a `customFields` key.

We need it because the same payload had to be built from three different packages (`contact`, `agent`, `workflow.engine.executor`), all of which already depend on `com.relayflow.api.webhook` for `WebhookDispatchService`.

### `ContactSnapshot`

`record ContactSnapshot(Map<String, Object> contact)` in `com.relayflow.api.webhook.dto` — the `contact.updated` webhook payload. Its one field is a dynamically-keyed map (arbitrary custom field keys) rather than a fixed set of record components, since a record can't declare fields unknown at compile time.

We need it so the outer payload shape (`{"contact": {...}}`) is a typed DTO like the rest of `webhook.dto`, even though the inner contact map stays dynamic.

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

Application event containing `workspaceId`, `eventType` (`SseEventType`), and `payload`.

We need it to decouple domain transactions from SSE delivery.

### `SseEventType`

Enum of SSE event names: `MESSAGE_CREATED`, `CONVERSATION_UPDATED`, `AI_DRAFT_CREATED`, `AI_ESCALATED`. `getEventName()` returns the dot-notation string (e.g. `"message.created"`) sent as the SSE event's `event:` field.

We need it so the ten-plus call sites that publish an `SseBroadcastEvent` share one source of truth for event names instead of each repeating the same string literal.

## Telegram Backend

### `TelegramController`

HTTP controller for Telegram webhooks.

Endpoints:

- `webhook`: dedicated bot webhook per channel account, at `POST /telegram/webhook/{channelAccountId}`.

We need it as Telegram's inbound HTTP entry point.

### `TelegramAdapter`

Telegram integration service.

Important methods:

- `handleWebhook`: accepts dedicated bot updates.
- `processInboundMessage`: normalizes a Telegram message into contact, external identity, conversation, and message records.
- `onOutboundMessage`: listens for outbound messages and sends them through Telegram.
- `sendTelegramMessage`: calls Telegram Bot API with retry and attaches a one-time reply keyboard when button options are present.
- `sendTelegramMessageQuietly`: best-effort bot replies for linking/error hints.
- `createIdentity`, `createConversation`, `buildDisplayName`: helper methods for inbound normalization.
- `buildRawProfile`: captures the inbound update's `firstName`/`lastName` (trimmed, omitted if blank) onto the `ExternalIdentity.rawProfile` map, so `ReservedContactFieldResolver` can derive them.

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
- Set Contact Field nodes have a field selected.
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
- `SET_CONTACT_FIELD`
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

### `SetContactFieldNodeExecutor`

Writes a value to a contact's custom field — a reserved key or a workspace-defined `ContactFieldDefinition` key.

Important config fields:

- `fieldKey`
- `value`

Important behavior:

- Loads the `Conversation` (and its `Contact`/`Workspace`) via `ConversationRepository`, checks `fieldKey` against `ReservedContactField.isReserved` and `Workspace.contactFieldDefinitions` — an unwritable key is a no-op, recorded in the step output as `skipped`.
- Unlike `ContactCustomFieldWriter`'s AI-extraction path, this always overwrites — it's a deliberate, operator-configured action (like a manual edit), not a speculative guess that needs a "don't clobber" guard.

We need it as the third way (alongside manual edit and AI extraction) to set a contact field value, for channels or data sources the AI agent can't reach and auto-derivation doesn't cover.

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
- `AuthenticationPanel` (`src/app/authentication-panel.tsx`): account-menu widget rendered in the header of every protected route layout (inbox, settings, contacts, workflows, profile). Shows a `Log in` / `Get started` link pair when signed out, or an avatar dropdown (profile link, sign out) when signed in. Guards against SSR/client hydration mismatch with a `useSyncExternalStore`-based `mounted` check rather than a `useState`+`useEffect` pattern.

We need these to make every page share providers, typography, and icon font setup, and to give every protected route a consistent account-menu entry point.

### Root Route

- `page.tsx` (`Home`): the root route no longer renders a marketing landing page — that content now lives in the separate `apps/marketing` site (see [`apps/marketing`](#appsmarketing)). `Home` is a server component that checks `getInstanceStatus()` and `getServerAuthenticationStatus()` and redirects to `/setup` (instance not yet bootstrapped), `/login` (unauthenticated), or `/inbox` (authenticated).

We need it as a pure traffic-router now that apps/web is a self-hosted-only app with no public marketing surface of its own.

### Auth Pages

- `(auth)/layout.tsx`: auth page shell.
- `login/page.tsx`: login form and Google login entry point.
- `signup/page.tsx`: signup form; requires a `?token=` invite token in the URL. Loads the invite preview via `useInvitePreview`, pre-fills the invite's email (read-only), and shows an "You need an invite" state with a link back to `/login` when no token is present. There is no self-serve signup — see [`WorkspaceInviteService`](#workspaceinviteservice).
- login/signup `metadata`: route-specific page metadata.
- login/signup `apiBaseUrl`: backend OAuth base URL used by Google buttons.

We need them for invite-gated account creation and login.

### Setup Page

- `setup/layout.tsx`: calls `redirectIfBootstrapped()` (redirects to `/login` if the instance already has an admin account), then renders the RelayFlow-branded card shell.
- `setup/page.tsx`: first-run instance bootstrap form — admin name, email, password, and initial workspace name — submitted via `useBootstrap`. On success, routes to `/inbox`. This screen can only ever run once per instance; see [Instance Bootstrap](#authenticationcontroller).

We need it so a freshly deployed self-hosted instance can create its first admin account without any pre-seeded credentials or an email round-trip.

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

- `invite/page.tsx`: reads `?token=` from the query string, loads the invite preview server-side, and renders `InviteAcceptCard`.

We need it so invited users can inspect and accept workspace invitations.

### Reset Password Page

- `reset-password/[token]/page.tsx`: server component; renders the RelayFlow-branded card shell (same layout as `/setup`/`/login`) around `ResetPasswordForm`. Unlike `/setup`/`/login`, this route has no auth-state redirect — it must stay reachable whether the visitor is logged out, or logged into a different account than the one being reset.
- `reset-password/[token]/ResetPasswordForm.tsx`: client component. New/confirm password fields with client-side match validation (mirrors the removed self-service change-password form), submits via `useResetPassword`. No preview/validation step before rendering the form — an invalid or expired token only surfaces as an inline error on submit, from the `400`/`404` `PasswordResetService.resetPassword` returns. On success, replaces the form with a confirmation and a link to `/login`.

We need it as the one place a password is ever actually set after signup — reached both from a self-requested link and one a workspace owner generates for a member.

### Profile Pages

- `profile/layout.tsx`: protected profile shell; redirects unauthenticated visitors away.
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

### `CopyButton`

Small button that copies a given `text` prop to the clipboard via `navigator.clipboard`, showing a checkmark for 2 seconds after copying.

We need it for webhook URLs shown in `ChannelsList`.

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

Displays the conversation list, paginated with infinite scroll. Two distinct empty states based on `useChannelAccounts`: if the workspace has no `ACTIVE` channel, the empty state links to `/settings` to connect one; if a channel is already connected but there are simply no conversations yet, it shows a plain "No conversations yet" message instead.

We need it as the inbox conversation selector.

### `ConversationItem`

Renders one conversation row.

Important values/functions:

- `STATUS_DOT`: maps status to color.
- `LOCALE`: timestamp locale.
- `formatTimestamp`: human-readable row timestamp.

Shows a red "error" icon next to the contact name when `conversation.escalatedAt` is set, titled with `escalationReason`.

We need it to make the conversation list scannable.

### `MessageThread`

Displays selected conversation messages and composer.

Important values:

- `STATUS_CHIP`: maps conversation state to chip styles.

Shows an "Escalated" pill in the header (same slot as the "Workflow" lock pill) when `conversation.escalatedAt` is set, titled with `escalationReason`.

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
- `ContactCustomFieldsSection`: lists reserved fields (`RESERVED_CONTACT_FIELD_KEYS`) and the workspace's defined `contactFieldDefinitions` together as one field list. Always viewable; editable only when the current member has `CONTACT_FIELDS_WRITE` (or is owner) — a member without it sees the same rows read-only, with "Not set" for an empty value, rather than the section being hidden. Keyed by `contact.id` on the parent so switching contacts remounts fresh local edit state instead of needing a sync effect.

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

- personal information (display name, read-only email).
- password, for email/password accounts — no in-place change form; `PasswordSection` sends the current user a reset link (`useRequestPasswordReset` → `POST /profile/request-password-reset`) and shows a toast, matching how the flow completes at `/reset-password/{token}` rather than on this page.
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

Settings page layout with `WorkspaceNav`, a responsive settings subnav (horizontal tab strip on mobile/tablet, vertical sidebar on desktop), and general, channel, member, AI agent, and integration sections, each rendered as an anchored section within a single scrollable pane. Active section highlighting is driven by an `IntersectionObserver` on the content pane.

Section visibility is permission-gated: `General` (workspace rename) and `Channels` require ownership or the relevant granular permission; `AI Agent` requires `AI_AGENT_WRITE`; `Integrations` requires `API_KEYS_WRITE` or `WEBHOOKS_WRITE`; `Members` is always shown.

We need it to create a stable place for general, channel, member, invite, AI agent, API key, and webhook settings.

### `ChannelsList`

Lists connected channel accounts and provides the connect/disconnect/reconnect UI.

Important constants:

- `PROVIDER_LABEL`: provider display names.
- `PROVIDER_ICON`: provider icon names.

Important helper:

- `ChannelItem`: renders one channel, its provider-specific webhook URL with a `CopyButton` when active, and a disconnect/reconnect confirmation.
- `ProviderButton`: selects Telegram or WhatsApp connection flow.

We need it because channel setup should live in workspace settings rather than the inbox conversation list.

### `MembersList`

Workspace member and invite management UI.

Important capabilities:

- list current members.
- invite a member with selected permissions.
- update member permissions.
- remove members.
- generate a password reset link for a member (owner-only, `password` icon button next to Edit permissions/Remove — gated the same way, `viewerIsOwner && !isOwner`) via `useGenerateMemberPasswordReset`.
- list and revoke pending invites.

`PERMISSION_GROUPS` is the actual grantable-permission checklist shown for both inviting and editing a member (`ALL_PERMISSION_LABELS` is a separate flat map used only for the collapsed pill display) — every `WorkspacePermission` value must have an entry here or an owner has no UI control to grant it, even if the backend already checks for it.

We need it so owners can control who can operate the workspace.

### `IntegrationsPanel`

Settings panel that groups API keys and webhook configuration.

We need it so external integration setup lives in one settings area.

### `GeneralPanel`

Settings home for basic workspace-level configuration.

- Workspace rename: owner-only. Tracks a local `edited` override over the fetched workspace name so the input stays controlled while typing, and disables the save button until the trimmed value differs from the persisted name. Uses `useWorkspace` and `useUpdateWorkspace`.
- Contact fields: defines the workspace's custom contact field schema (key/label/description). Visible to every member (read-only list, including the six reserved fields shown plainly alongside custom ones with no "Reserved" badge or callout — just what each field is, not how/whether it's auto-filled); the add/edit/remove form only renders for a member with `CONTACT_FIELDS_WRITE` or owner role. Blocks saving (and flags inline) if a key collides with a reserved key, via `isReservedContactFieldKey` — a case-insensitive check on both sides, since a reserved key like `displayName` isn't all-lowercase.

We need it as the settings home for basic workspace-level configuration that doesn't belong under channels, members, or integrations.

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
- `writableContactFieldOptions`: `SelectOption[]` combining `RESERVED_CONTACT_FIELD_KEYS` and the workspace's `contactFieldDefinitions` — the dropdown source for `SetContactFieldForm`'s field picker. Separate from `contactFieldVariables` (the `contact.data.<key>` entries offered by the `{{…}}` variable picker for *reading*), which only covers workspace-defined fields, not reserved ones — reading and writing use different field lists on purpose.

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
- `WHATSAPP_BUTTON_TITLE_LIMIT` (20) — `WaitForReplyForm` shows an inline warning under an option's text input once it exceeds this length, since WhatsApp truncates interactive button titles beyond it (only checked for three or fewer options, since that's the button-eligible range — see the `WhatsApp uses native buttons for up to three options` behavior in `TelegramAdapter`/`WhatsAppAdapter`).

Important form parts:

- `TriggerForm`
- `SendMessageForm`
- `ConditionForm`
- `KeyValueEditor`
- `ResponseMappingEditor`
- `HttpRequestForm`
- `SetVariableForm`
- `SetContactFieldForm`
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

### `SetContactFieldNode`

Visual contact-field-write node.

Important type:

- `SetContactFieldNodeData`

We need it to represent a workflow deliberately writing a contact field, distinct from Set Variable (run-scoped, not persisted).

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
- `useSignup`: invite-gated signup mutation with error toast support.
- `useBootstrap`: first-run instance bootstrap mutation with error toast support.
- `useResetPassword`: consumes a `/reset-password/{token}` link and sets a new password — no toast, since `ResetPasswordForm` renders its own inline success/error state instead.
- `useLogout`: logout mutation and cache cleanup.

We need them to keep auth forms/components declarative.

### Profile Hooks

- `useProfile`: fetches current profile state.
- `useUpdateProfile`: updates display name.
- `useRequestPasswordReset`: emails the current user a password reset link; success/error surfaced via toast.
- `useDeleteAccount`: deletes the current account and clears auth state.
- `useSetup2FA`: starts TOTP setup and returns an `otpauth://` URI.
- `useEnable2FA`: verifies OTP and enables TOTP.
- `useDisable2FA`: verifies OTP and disables TOTP.

We need them to keep profile/security mutations outside the profile component.

### Workspace Hooks

- `useWorkspaces`: fetches workspaces.
- `useWorkspace`: selects one workspace from the cached list.
- `useCreateWorkspace`: creates a workspace and invalidates workspace queries.
- `useUpdateWorkspace`: renames a workspace and invalidates workspace queries.
- `useCurrentMember`: fetches current workspace membership/permissions.
- `useWorkspaceMembers`: fetches workspace members.
- `useUpdateMember`: updates member role/permissions.
- `useRemoveMember`: removes a member.
- `useGenerateMemberPasswordReset`: owner action — emails a member a password reset link; success/error surfaced via toast.
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
- `useWorkspaceEvents`: opens SSE and invalidates React Query caches on `message.created`, `ai.draft.created`, and `ai.escalated` events (the last just invalidates the conversation list so `ConversationItem`/`MessageThread` pick up `escalatedAt`). The backend also emits `conversation.updated` over the same stream, but nothing here listens for it yet.

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

- `SignupPayload`: includes the required `inviteToken`.
- `SignupResponse`
- `BootstrapPayload`
- `BootstrapResponse`
- `InstanceStatusResponse`
- `LoginPayload`
- `Login2FAPayload`
- `AuthenticatedUserResponse`
- `ProfileResponse`
- `UpdateProfilePayload`
- `ResetPasswordPayload`
- `DeleteAccountPayload`
- `Setup2FAResponse`
- `OtpPayload`

Important functions:

- `signup`
- `bootstrap`
- `getInstanceStatus`
- `login`
- `login2FA`
- `resetPassword`
- `getProfile`
- `updateProfile`
- `requestPasswordReset`
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

Server-side auth and instance-status helpers used by Next routes/layouts.

Important pieces:

- `AuthenticationStatus`, `getServerAuthenticationStatus`: fetches `/auth/me` forwarding the request's session cookie so Server Components can check auth without a client round-trip.
- `requireAuthentication`: redirects to `/login` if unauthenticated.
- `redirectIfAuthenticated`: redirects to `/inbox` (or a given destination) if already authenticated.
- `InstanceStatus`, `getInstanceStatus`: fetches `/auth/bootstrap-status`; fails safe by treating the instance as already bootstrapped on any error, so a transient API outage never traps every visitor on `/setup`.
- `redirectIfNotBootstrapped`: redirects to `/setup` if the instance hasn't completed first-run setup.
- `redirectIfBootstrapped`: redirects to `/login` if the instance has already completed first-run setup — used by `setup/layout.tsx` so `/setup` can't be revisited after the fact.
- `apiBaseUrl`

We need it to protect server-rendered app routes, prevent logged-in users from staying on auth pages, and gate the one-time `/setup` route.

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

Tests the root route (`Home`)'s redirect logic: `/setup` when not bootstrapped, `/inbox` when authenticated, `/login` otherwise. Mocks `next/navigation`'s `redirect` (a throw-based API) as a plain spy, and mocks `getInstanceStatus`/`getServerAuthenticationStatus` from `server-authentication.ts`.

We need it to protect the instance-bootstrap/auth routing decision that gates every visitor before they reach a real page.

### `ConversationList.test.tsx`

Tests inbox conversation list states.

Important helpers:

- `mockConversationsHook`
- `baseHookReturn`
- `renderList`

We need it to protect the empty/loading/populated conversation list behavior.

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

It documents health, instance bootstrap, auth, workspaces, members, invites, API keys, webhooks, public API, channels, contacts, external identities, conversations, messages, workflows, SSE, Telegram webhooks, and WhatsApp webhooks.

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
- `extractionFields`: `List<ExtractionField>` JSONB (key + description) — fields the LLM is prompted to pull from the conversation; only a key on this list is ever exposed as an `agent.data.<key>` workflow variable or considered for contact persistence (see `AgentWorkflowContext` and `ContactCustomFieldWriter`)

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
   - `escalate` → stamp `Conversation.escalatedAt`/`escalationReason`, broadcast `ai.escalated` SSE + ESCALATED (see `broadcastEscalation`; cleared later by `MessagingService.createMessage` on the next human reply).
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

### `ContactCustomFieldWriter`

`@Service` in the `agent` package. `apply(conversation, extractedData, extractionFields)` persists LLM-extracted data onto the contact's `customFields`.

Important behavior:

- A key is only ever written if it's both a configured `extractionFields` key *and* a writable contact field key — one of the reserved keys (`ReservedContactField`) or a key the workspace has defined (`Workspace.contactFieldDefinitions`). A hallucinated or unconfigured key is dropped rather than persisted.
- Checks the *effective* value before writing, not just the raw `customFields` map — for a reserved key it also resolves what `ReservedContactFieldResolver` can already derive (e.g. `displayName` from the contact record, `phone` from a WhatsApp identity), fetching `ExternalIdentity` rows only when a candidate key is actually reserved (avoids an unconditional extra query on every call).
- Gap-filling only: never overwrites a value that's already effectively present, whether explicitly stored or auto-derived.

Called from `AiAgentInvocationService` (escalate / workflow-trigger / send paths — not the draft-creation path, which defers persistence until a human approves) and `AiAgentConfigurationService` (`sendDraft`, `triggerWorkflowFromDraft`).

We need it so AI-extracted data flows onto the contact record without ever clobbering a value someone already configured, and without polluting the contact with keys the workspace never asked the agent to extract.

### `AgentWorkflowContext`

Package-private static builder in the `agent` package. `build(reply, confidence, extractedData, extractionFields)` returns the workflow variable map seeded when the AI agent triggers a workflow run.

Always includes `agent.reply` (empty string if null) and `agent.confidence` (omitted if null). For `extractedData`, only a key matching a configured `extractionFields` key becomes an `agent.data.<key>` variable — same "known keys only" filter as `ContactCustomFieldWriter`, applied independently since a workflow variable and a persisted contact field are separate concerns.

We need it to stop the LLM's free-form `extractedData` output from leaking an unconfigured or hallucinated key into a workflow's variable namespace.

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
- Data extraction: list of `{key, description}` extraction fields. Only a key on this list is ever exposed as an `agent.data.<key>` workflow variable or considered for contact persistence — see `AgentWorkflowContext` and `ContactCustomFieldWriter`.

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


## `apps/marketing`

A static marketing site — no server, no database, no backend of any kind. Built with Next.js's
static export (`output: "export"` in `next.config.mjs`) and deployed to GitHub Pages by
`.github/workflows/deploy-pages.yml` on every push to `main` that touches this app.

Two pages: `page.tsx` (the landing page, linking to the GitHub repo and to the self-hosting
docs) and `app/docs/self-hosting/page.tsx` (the public setup guide).

We need it so RelayFlow has a public-facing site describing the product and pointing visitors at
the repo and the self-hosting docs, with zero infrastructure of its own to operate.

### `deployment-artifacts.ts` And The Self-Hosting Docs Page

`app/docs/self-hosting/page.tsx` embeds the real `docker-compose.yml`, `.env.example`, and
Caddyfile from the repo root, read by `deployment-artifacts.ts` **at build time** (`readFileSync`
against `../../` from the app's working directory) rather than copy-pasted — so the docs page
can never drift from the actual deployment files. `docker-compose.yml`'s content is additionally
filtered down to just the `prod`-profile services before embedding, since the full file also has
local-dev-only services that aren't relevant to a self-hosted operator.

We need this because a setup guide that quietly drifts from the files it tells you to copy is
worse than no guide at all.
