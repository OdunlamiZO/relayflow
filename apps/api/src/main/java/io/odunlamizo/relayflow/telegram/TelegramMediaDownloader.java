package io.odunlamizo.relayflow.telegram;

// Docs: docs/codebase-components.md#telegrammediadownloader

import com.fasterxml.jackson.databind.JsonNode;
import io.odunlamizo.relayflow.audio.ChannelMediaDownloader;
import io.odunlamizo.relayflow.channel.domain.ChannelAccount;
import io.odunlamizo.relayflow.channel.domain.ChannelProvider;
import io.odunlamizo.relayflow.security.CredentialEncryptionService;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
public class TelegramMediaDownloader implements ChannelMediaDownloader {

    private static final String GET_FILE_URL =
            "https://api.telegram.org/bot{token}/getFile?file_id={fileId}";

    private static final String FILE_URL = "https://api.telegram.org/file/bot{token}/{filePath}";

    private final RestTemplate restTemplate;

    private final CredentialEncryptionService credentialEncryptionService;

    public TelegramMediaDownloader(
            RestTemplate restTemplate, CredentialEncryptionService credentialEncryptionService) {
        this.restTemplate = restTemplate;
        this.credentialEncryptionService = credentialEncryptionService;
    }

    @Override
    public ChannelProvider provider() {
        return ChannelProvider.TELEGRAM;
    }

    @Override
    public byte[] download(ChannelAccount channelAccount, String fileId) {
        String botToken =
                credentialEncryptionService.decrypt(channelAccount.getEncryptedCredentials());

        byte[] audio;

        try {
            JsonNode file =
                    restTemplate.getForObject(GET_FILE_URL, JsonNode.class, botToken, fileId);
            String filePath = file != null ? file.path("result").path("file_path").asText("") : "";

            if (filePath.isEmpty()) {
                throw new IllegalStateException("Telegram returned no file path for " + fileId);
            }

            audio = restTemplate.getForObject(FILE_URL, byte[].class, botToken, filePath);
        } catch (RestClientException exception) {
            throw new IllegalStateException(
                    "Telegram file download failed: " + TelegramFailures.describe(exception));
        }

        return audio != null ? audio : new byte[0];
    }
}
