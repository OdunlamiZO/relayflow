package com.relayflow.api.workspace;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.relayflow.api.security.CredentialEncryptionService;
import com.relayflow.api.workspace.domain.WorkspaceSecret;
import com.relayflow.api.workspace.dto.SaveSecretRequest;
import com.relayflow.api.workspace.dto.SecretResponse;
import com.relayflow.api.workspace.repository.WorkspaceSecretRepository;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class SecretServiceTest {

    @Mock private WorkspaceSecretRepository secretRepository;

    // A real instance, not a mock — encrypt/decrypt round-tripping is exactly what this test
    // verifies, so it must run for real rather than be stubbed.
    private final CredentialEncryptionService encryptionService =
            new CredentialEncryptionService(Base64.getEncoder().encodeToString(new byte[32]));

    private SecretService service() {
        return new SecretService(secretRepository, encryptionService);
    }

    @Test
    void createEncryptsTheValueRatherThanStoringItInPlaintext() {
        UUID workspaceId = UUID.randomUUID();
        SaveSecretRequest request = new SaveSecretRequest("API_KEY", "sk-super-secret");

        SecretResponse response = service().createSecret(workspaceId, request);

        assertThat(response.name()).isEqualTo("API_KEY");

        ArgumentCaptor<WorkspaceSecret> captor = ArgumentCaptor.forClass(WorkspaceSecret.class);
        verify(secretRepository).save(captor.capture());
        assertThat(captor.getValue().getEncryptedValue()).doesNotContain("sk-super-secret");
        assertThat(captor.getValue().getEncryptedValue()).startsWith("ENC:");
    }

    @Test
    void resolveDecryptedRoundTripsTheOriginalValue() {
        UUID workspaceId = UUID.randomUUID();
        WorkspaceSecret secret = new WorkspaceSecret();
        secret.setWorkspaceId(workspaceId);
        secret.setName("API_KEY");
        secret.setEncryptedValue(encryptionService.encrypt("sk-super-secret"));

        when(secretRepository.findByWorkspaceIdAndName(workspaceId, "API_KEY"))
                .thenReturn(Optional.of(secret));

        Optional<String> resolved = service().resolveDecrypted(workspaceId, "API_KEY");

        assertThat(resolved).contains("sk-super-secret");
    }

    @Test
    void resolveDecryptedIsEmptyForAnUnknownName() {
        UUID workspaceId = UUID.randomUUID();
        when(secretRepository.findByWorkspaceIdAndName(workspaceId, "MISSING"))
                .thenReturn(Optional.empty());

        assertThat(service().resolveDecrypted(workspaceId, "MISSING")).isEmpty();
    }

    @Test
    void createRejectsADuplicateNameWithConflict() {
        UUID workspaceId = UUID.randomUUID();
        when(secretRepository.save(any())).thenThrow(new DataIntegrityViolationException("dup"));

        assertThatThrownBy(
                        () ->
                                service()
                                        .createSecret(
                                                workspaceId,
                                                new SaveSecretRequest("API_KEY", "value")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("API_KEY");
    }

    @Test
    void deleteThrowsNotFoundForAnUnknownSecret() {
        UUID workspaceId = UUID.randomUUID();
        UUID secretId = UUID.randomUUID();
        when(secretRepository.findInWorkspace(secretId, workspaceId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().deleteSecret(workspaceId, secretId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("not found");
    }
}
