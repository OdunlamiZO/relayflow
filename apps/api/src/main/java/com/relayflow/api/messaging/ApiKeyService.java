package com.relayflow.api.messaging;

import com.relayflow.api.messaging.domain.WorkspaceApiKey;
import com.relayflow.api.messaging.dto.ApiKeyResponse;
import com.relayflow.api.messaging.dto.CreateApiKeyRequest;
import com.relayflow.api.messaging.dto.CreateApiKeyResponse;
import com.relayflow.api.messaging.repository.WorkspaceApiKeyRepository;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ApiKeyService {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyService.class);
    private static final String KEY_PREFIX = "rfk_";
    private static final int KEY_RANDOM_BYTES = 32;

    private final WorkspaceApiKeyRepository apiKeyRepository;

    public ApiKeyService(WorkspaceApiKeyRepository apiKeyRepository) {
        this.apiKeyRepository = apiKeyRepository;
    }

    @Transactional
    public CreateApiKeyResponse createApiKey(
            UUID workspaceId, UUID createdBy, CreateApiKeyRequest request) {
        // Generate a cryptographically random key: rfk_<64 hex chars>
        byte[] randomBytes = new byte[KEY_RANDOM_BYTES];
        new SecureRandom().nextBytes(randomBytes);
        String rawKey = KEY_PREFIX + HexFormat.of().formatHex(randomBytes);
        String prefix = rawKey.substring(0, Math.min(rawKey.length(), 12));

        WorkspaceApiKey apiKey = new WorkspaceApiKey();
        apiKey.setWorkspaceId(workspaceId);
        apiKey.setName(request.name());
        apiKey.setKeyPrefix(prefix);
        apiKey.setKeyHash(sha256(rawKey));
        apiKey.setCreatedBy(createdBy);
        apiKey.setExpiresAt(request.expiresAt());
        apiKeyRepository.save(apiKey);

        log.info(
                "API key created: id={}, workspace={}, expiresAt={}",
                apiKey.getId(),
                workspaceId,
                apiKey.getExpiresAt());

        return new CreateApiKeyResponse(
                apiKey.getId(),
                apiKey.getName(),
                apiKey.getKeyPrefix(),
                apiKey.getCreatedAt(),
                apiKey.getExpiresAt(),
                rawKey);
    }

    @Transactional(readOnly = true)
    public List<ApiKeyResponse> listApiKeys(UUID workspaceId) {
        return apiKeyRepository.findActive(workspaceId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public void revokeApiKey(UUID workspaceId, UUID keyId) {
        WorkspaceApiKey apiKey =
                apiKeyRepository
                        .findInWorkspace(keyId, workspaceId)
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.NOT_FOUND, "API key not found"));

        if (apiKey.isRevoked()) {
            return;
        }

        apiKey.setRevokedAt(Instant.now());
        apiKeyRepository.save(apiKey);

        log.info("API key revoked: id={}, workspace={}", keyId, workspaceId);
    }

    private ApiKeyResponse toResponse(WorkspaceApiKey k) {
        return new ApiKeyResponse(
                k.getId(),
                k.getName(),
                k.getKeyPrefix(),
                k.getCreatedAt(),
                k.getLastUsedAt(),
                k.getRevokedAt(),
                k.getExpiresAt());
    }

    public static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
