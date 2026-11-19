package io.odunlamizo.relayflow.audio;

// Docs: docs/codebase-components.md#messageaudiocontroller-and-messageaudioservice

import io.odunlamizo.relayflow.audio.MessageAudioService.AudioFile;
import io.odunlamizo.relayflow.workspace.WorkspaceAuthorizationService;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
public class MessageAudioController {

    private final MessageAudioService messageAudioService;

    private final WorkspaceAuthorizationService authorizationService;

    public MessageAudioController(
            MessageAudioService messageAudioService,
            WorkspaceAuthorizationService authorizationService) {
        this.messageAudioService = messageAudioService;
        this.authorizationService = authorizationService;
    }

    @GetMapping("/conversations/{conversationId}/messages/{messageId}/audio")
    ResponseEntity<byte[]> getAudio(
            @RequestParam @NotNull UUID workspaceId,
            @PathVariable UUID conversationId,
            @PathVariable UUID messageId,
            Authentication authentication) {
        authorizationService.assertMember(workspaceId, authentication);

        AudioFile audio = messageAudioService.getAudio(workspaceId, conversationId, messageId);

        return ResponseEntity.ok()
                .contentType(mediaType(audio.mimeType()))
                .cacheControl(CacheControl.noStore())
                .body(audio.content());
    }

    private static MediaType mediaType(String mimeType) {
        try {
            return MediaType.parseMediaType(mimeType);
        } catch (Exception exception) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
}
