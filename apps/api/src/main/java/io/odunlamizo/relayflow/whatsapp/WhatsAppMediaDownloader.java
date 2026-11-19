package io.odunlamizo.relayflow.whatsapp;

// Docs: docs/codebase-components.md#whatsappmediadownloader

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.odunlamizo.relayflow.audio.ChannelMediaDownloader;
import io.odunlamizo.relayflow.channel.domain.ChannelAccount;
import io.odunlamizo.relayflow.channel.domain.ChannelProvider;
import io.odunlamizo.relayflow.security.CredentialEncryptionService;
import java.net.URI;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class WhatsAppMediaDownloader implements ChannelMediaDownloader {

    private static final String MEDIA_URL = "https://graph.facebook.com/v21.0/{mediaId}";

    private final RestTemplate restTemplate;

    private final CredentialEncryptionService credentialEncryptionService;

    private final ObjectMapper objectMapper;

    public WhatsAppMediaDownloader(
            RestTemplate restTemplate,
            CredentialEncryptionService credentialEncryptionService,
            ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.credentialEncryptionService = credentialEncryptionService;
        this.objectMapper = objectMapper;
    }

    @Override
    public ChannelProvider provider() {
        return ChannelProvider.WHATSAPP;
    }

    @Override
    public byte[] download(ChannelAccount channelAccount, String mediaId) {
        HttpEntity<Void> request = new HttpEntity<>(authorizationHeaders(channelAccount));

        JsonNode media =
                restTemplate
                        .exchange(MEDIA_URL, HttpMethod.GET, request, JsonNode.class, mediaId)
                        .getBody();
        String url = media != null ? media.path("url").asText("") : "";

        if (url.isEmpty()) {
            throw new IllegalStateException("WhatsApp returned no media URL for " + mediaId);
        }

        byte[] audio =
                restTemplate
                        .exchange(URI.create(url), HttpMethod.GET, request, byte[].class)
                        .getBody();

        return audio != null ? audio : new byte[0];
    }

    private HttpHeaders authorizationHeaders(ChannelAccount channelAccount) {
        String json = credentialEncryptionService.decrypt(channelAccount.getEncryptedCredentials());
        WhatsAppCredentials credentials;

        try {
            credentials = objectMapper.readValue(json, WhatsAppCredentials.class);
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Failed to parse WhatsApp credentials for channel account "
                            + channelAccount.getId(),
                    exception);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(credentials.accessToken());

        return headers;
    }
}
