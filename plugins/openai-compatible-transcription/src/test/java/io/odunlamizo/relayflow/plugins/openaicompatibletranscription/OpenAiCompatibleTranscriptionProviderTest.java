package io.odunlamizo.relayflow.plugins.openaicompatibletranscription;

// Docs: docs/codebase-components.md#official-plugin-tests

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sun.net.httpserver.HttpServer;
import io.odunlamizo.relayflow.plugin.ProviderException;
import io.odunlamizo.relayflow.plugin.TranscriptionRequest;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OpenAiCompatibleTranscriptionProviderTest {

    private final AtomicReference<String> receivedBody = new AtomicReference<>();

    private final AtomicReference<String> receivedAuthorization = new AtomicReference<>();

    private HttpServer server;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext(
                "/v1/audio/transcriptions",
                exchange -> {
                    receivedAuthorization.set(
                            exchange.getRequestHeaders().getFirst("authorization"));
                    receivedBody.set(
                            new String(
                                    exchange.getRequestBody().readAllBytes(),
                                    StandardCharsets.UTF_8));
                    byte[] bytes =
                            "{\"text\":\" I want to borrow 50,000 \"}"
                                    .getBytes(StandardCharsets.UTF_8);
                    exchange.sendResponseHeaders(200, bytes.length);
                    exchange.getResponseBody().write(bytes);
                    exchange.close();
                });
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void uploadsTheAudioAndReturnsTheTranscript() {
        Map<String, String> settings =
                Map.of(
                        "OPENAI_COMPATIBLE_TRANSCRIPTION_BASE_URL",
                        serverUrl(),
                        "OPENAI_COMPATIBLE_TRANSCRIPTION_API_KEY",
                        "gsk-test");

        String transcript =
                provider(settings)
                        .transcribe(
                                new TranscriptionRequest(
                                        "voice-bytes".getBytes(StandardCharsets.UTF_8),
                                        "audio/ogg; codecs=opus",
                                        "whisper-large-v3-turbo"));

        assertThat(transcript).isEqualTo("I want to borrow 50,000");
        assertThat(receivedAuthorization.get()).isEqualTo("Bearer gsk-test");
        assertThat(receivedBody.get())
                .contains("name=\"model\"\r\n\r\nwhisper-large-v3-turbo")
                .contains("filename=\"audio.ogg\"")
                .contains("voice-bytes");
    }

    @Test
    void fallsBackToTheChatProviderSettingsAndDefaultModel() {
        Map<String, String> settings =
                Map.of(
                        "OPENAI_COMPATIBLE_BASE_URL",
                        serverUrl(),
                        "OPENAI_COMPATIBLE_API_KEY",
                        "shared-key");

        provider(settings).transcribe(new TranscriptionRequest(new byte[0], "audio/mpeg", null));

        assertThat(receivedAuthorization.get()).isEqualTo("Bearer shared-key");
        assertThat(receivedBody.get())
                .contains("name=\"model\"\r\n\r\nwhisper-1")
                .contains("filename=\"audio.mp3\"");
    }

    @Test
    void aMissingBaseUrlThrows() {
        assertThatThrownBy(
                        () ->
                                provider(Map.of())
                                        .transcribe(
                                                new TranscriptionRequest(
                                                        new byte[0], "audio/ogg", null)))
                .isInstanceOf(ProviderException.class)
                .hasMessageContaining("OPENAI_COMPATIBLE_TRANSCRIPTION_BASE_URL");
    }

    @Test
    void picksAFileExtensionFromTheMimeType() {
        assertThat(OpenAiCompatibleTranscriptionProvider.fileExtension("audio/mp4"))
                .isEqualTo("m4a");
        assertThat(OpenAiCompatibleTranscriptionProvider.fileExtension("AUDIO/WAV"))
                .isEqualTo("wav");
        assertThat(OpenAiCompatibleTranscriptionProvider.fileExtension(null)).isEqualTo("ogg");
    }

    private String serverUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/v1";
    }

    private OpenAiCompatibleTranscriptionProvider provider(Map<String, String> settings) {
        OpenAiCompatibleTranscriptionProvider provider =
                new OpenAiCompatibleTranscriptionProvider();
        provider.initialize(name -> Optional.ofNullable(settings.get(name)));

        return provider;
    }
}
