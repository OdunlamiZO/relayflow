package io.odunlamizo.relayflow.workspace;

import static net.logstash.logback.argument.StructuredArguments.kv;

import io.odunlamizo.relayflow.security.CredentialEncryptionService;
import io.odunlamizo.relayflow.workspace.domain.WorkspaceSecret;
import io.odunlamizo.relayflow.workspace.dto.SaveSecretRequest;
import io.odunlamizo.relayflow.workspace.dto.SecretResponse;
import io.odunlamizo.relayflow.workspace.repository.WorkspaceSecretRepository;
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
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A secret named \"" + request.name() + "\" already exists");
        }

        log.info(
                "Secret created {} {}",
                kv("workspaceId", workspaceId),
                kv("secretId", secret.getId()));

        return toResponse(secret);
    }

    @Transactional
    public SecretResponse updateSecret(UUID workspaceId, UUID secretId, SaveSecretRequest request) {
        WorkspaceSecret secret = getOrThrow(workspaceId, secretId);
        secret.setName(request.name());
        secret.setEncryptedValue(encryptionService.encrypt(request.value()));

        try {
            secretRepository.save(secret);
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A secret named \"" + request.name() + "\" already exists");
        }

        log.info("Secret updated {} {}", kv("workspaceId", workspaceId), kv("secretId", secretId));

        return toResponse(secret);
    }

    @Transactional
    public void deleteSecret(UUID workspaceId, UUID secretId) {
        WorkspaceSecret secret = getOrThrow(workspaceId, secretId);
        secretRepository.delete(secret);

        log.info("Secret deleted {} {}", kv("workspaceId", workspaceId), kv("secretId", secretId));
    }

    /** Never expose through a controller or write the result to a variable, snapshot, or log. */
    @Transactional(readOnly = true)
    public Optional<String> resolveDecrypted(UUID workspaceId, String name) {
        return secretRepository
                .findByWorkspaceAndName(workspaceId, name)
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

    private SecretResponse toResponse(WorkspaceSecret secret) {
        return new SecretResponse(
                secret.getId(), secret.getName(), secret.getCreatedAt(), secret.getUpdatedAt());
    }
}
