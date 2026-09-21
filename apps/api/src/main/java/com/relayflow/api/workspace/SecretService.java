package com.relayflow.api.workspace;

import com.relayflow.api.security.CredentialEncryptionService;
import com.relayflow.api.workspace.domain.WorkspaceSecret;
import com.relayflow.api.workspace.dto.SaveSecretRequest;
import com.relayflow.api.workspace.dto.SecretResponse;
import com.relayflow.api.workspace.repository.WorkspaceSecretRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Named, encrypted secret values a workspace's workflows can reference (as {@code
 * {{secrets.NAME}}}, resolved only by {@code HttpRequestNodeExecutor}) without the plaintext value
 * ever appearing in the workflow definition, a run log, or an API response.
 */
@Service
public class SecretService {

    private static final Logger log = LoggerFactory.getLogger(SecretService.class);

    private final WorkspaceSecretRepository secretRepository;

    private final CredentialEncryptionService encryptionService;

    public SecretService(
            WorkspaceSecretRepository secretRepository,
            CredentialEncryptionService encryptionService) {
        this.secretRepository = secretRepository;
        this.encryptionService = encryptionService;
    }

    @Transactional(readOnly = true)
    public List<SecretResponse> listSecrets(UUID workspaceId) {
        return secretRepository.findByWorkspace(workspaceId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public SecretResponse createSecret(UUID workspaceId, SaveSecretRequest request) {
        WorkspaceSecret secret = new WorkspaceSecret();
        secret.setWorkspaceId(workspaceId);
        secret.setName(request.name());
        secret.setEncryptedValue(encryptionService.encrypt(request.value()));

        try {
            secretRepository.save(secret);
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A secret named \"" + request.name() + "\" already exists");
        }

        log.info(
                "Secret created: id={}, name={}, workspace={}",
                secret.getId(),
                secret.getName(),
                workspaceId);

        return toResponse(secret);
    }

    @Transactional
    public SecretResponse updateSecret(UUID workspaceId, UUID secretId, SaveSecretRequest request) {
        WorkspaceSecret secret = getOrThrow(workspaceId, secretId);
        secret.setName(request.name());
        secret.setEncryptedValue(encryptionService.encrypt(request.value()));

        try {
            secretRepository.save(secret);
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A secret named \"" + request.name() + "\" already exists");
        }

        log.info(
                "Secret updated: id={}, name={}, workspace={}",
                secretId,
                secret.getName(),
                workspaceId);

        return toResponse(secret);
    }

    @Transactional
    public void deleteSecret(UUID workspaceId, UUID secretId) {
        WorkspaceSecret secret = getOrThrow(workspaceId, secretId);
        secretRepository.delete(secret);

        log.info("Secret deleted: id={}, workspace={}", secretId, workspaceId);
    }

    /**
     * Decrypts one secret by name for the workflow engine. Public only because its sole caller,
     * {@code HttpRequestNodeExecutor}, lives in a different package — never expose this via a
     * controller, and never write its result to a workflow variable, a run snapshot, or a log line.
     */
    @Transactional(readOnly = true)
    public Optional<String> resolveDecrypted(UUID workspaceId, String name) {
        return secretRepository
                .findByWorkspaceIdAndName(workspaceId, name)
                .map(WorkspaceSecret::getEncryptedValue)
                .map(encryptionService::decrypt);
    }

    private WorkspaceSecret getOrThrow(UUID workspaceId, UUID secretId) {
        return secretRepository
                .findInWorkspace(secretId, workspaceId)
                .orElseThrow(
                        () ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND, "Secret not found"));
    }

    private SecretResponse toResponse(WorkspaceSecret s) {
        return new SecretResponse(s.getId(), s.getName(), s.getCreatedAt(), s.getUpdatedAt());
    }
}
