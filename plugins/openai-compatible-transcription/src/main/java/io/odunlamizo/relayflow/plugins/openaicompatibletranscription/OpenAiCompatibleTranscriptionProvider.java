package io.odunlamizo.relayflow.plugins.openaicompatibletranscription;

// Docs: docs/codebase-components.md#openaicompatibletranscriptionprovider

import com.fasterxml.jackson.databind.ObjectMapper;
import io.odunlamizo.relayflow.plugin.ProviderException;
import io.odunlamizo.relayflow.plugin.ProviderSettings;
import io.odunlamizo.relayflow.plugin.TranscriptionProvider;
import io.odunlamizo.relayflow.plugin.TranscriptionRequest;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class OpenAiCompatibleTranscriptionProvider implements TranscriptionProvider {

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(120);

    private static final Map<String, String> FILE_EXTENSIONS =
            Map.of(
                    "audio/ogg", "ogg",
                    "audio/opus", "ogg",
                    "audio/mpeg", "mp3",
                    "audio/mp4", "m4a",
                    "audio/x-m4a", "m4a",
                    "audio/aac", "m4a",
                    "audio/wav", "wav",
                    "audio/x-wav", "wav",
                    "audio/webm", "webm",
                    "audio/amr", "amr");

    private final HttpClient httpClient =
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    private String baseUrl = "";

    private String apiKey = "";

    @Override
    public String id() {
        return "OPENAI_COMPATIBLE";
    }

    @Override
    public String displayName() {
        return "OpenAI-compatible";
    }

    @Override
    public void initialize(ProviderSettings settings) {
        baseUrl =
                settings.get("OPENAI_COMPATIBLE_TRANSCRIPTION_BASE_URL")
                        .filter(url -> !url.isBlank())
                        .or(() -> settings.get("OPENAI_COMPATIBLE_BASE_URL"))
                        .map(url -> url.strip().replaceAll("/+$", ""))
                        .orElse("");
        apiKey =
                settings.get("OPENAI_COMPATIBLE_TRANSCRIPTION_API_KEY")
                        .filter(key -> !key.isBlank())
                        .or(() -> settings.get("OPENAI_COMPATIBLE_API_KEY"))
                        .map(String::strip)
                        .orElse("");
    }

    @Override
    public Optional<String> configurationProblem() {
        return baseUrl.isEmpty()
                ? Optional.of(
                        "Set OPENAI_COMPATIBLE_TRANSCRIPTION_BASE_URL or OPENAI_COMPATIBLE_BASE_URL"
                                + " to use the OpenAI-compatible transcription provider")
                : Optional.empty();
    }

    @Override
    public String transcribe(TranscriptionRequest request) {
        Optional<String> problem = configurationProblem();

        if (problem.isPresent()) {
            throw new ProviderException(problem.get());
        }

        if (request.model() == null || request.model().isBlank()) {
            throw new ProviderException(
                    "Choose a transcription model in Settings → Providers, or set the"
                            + " platform:transcription:openai-compatible:model Redis key");
        }

        String model = request.model();
        String boundary = "relayflow-" + UUID.randomUUID();

        HttpRequest.Builder builder =
                HttpRequest.newBuilder(URI.create(baseUrl + "/audio/transcriptions"))
                        .timeout(REQUEST_TIMEOUT)
                        .header("content-type", "multipart/form-data; boundary=" + boundary)
                        .POST(
                                HttpRequest.BodyPublishers.ofByteArray(
                                        buildMultipartBody(boundary, model, request)));

        if (!apiKey.isEmpty()) {
            builder.header("authorization", "Bearer " + apiKey);
        }

        HttpResponse<String> response = send(builder.build());

        if (response.statusCode() >= 400) {
            throw new ProviderException(
                    "The transcription API returned "
                            + response.statusCode()
                            + ": "
                            + response.body());
        }

        try {
            return objectMapper.readTree(response.body()).path("text").asText("").strip();
        } catch (Exception exception) {
            throw new ProviderException(
                    "The transcription API returned an unreadable response", exception);
        }
    }

    static String fileExtension(String mimeType) {
        if (mimeType == null) {
            return "ogg";
        }

        String baseType = mimeType.split(";")[0].strip().toLowerCase();

        return FILE_EXTENSIONS.getOrDefault(baseType, "ogg");
    }

    private byte[] buildMultipartBody(String boundary, String model, TranscriptionRequest request) {
        ByteArrayOutputStream body = new ByteArrayOutputStream();

        writeText(body, boundary, "model", model);
        writeText(body, boundary, "response_format", "json");
        writeString(
                body,
                "--"
                        + boundary
                        + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\"audio."
                        + fileExtension(request.mimeType())
                        + "\"\r\nContent-Type: "
                        + (request.mimeType() != null
                                ? request.mimeType()
                                : "application/octet-stream")
                        + "\r\n\r\n");
        body.writeBytes(request.audio());
        writeString(body, "\r\n--" + boundary + "--\r\n");

        return body.toByteArray();
    }

    private void writeText(ByteArrayOutputStream body, String boundary, String name, String value) {
        writeString(
                body,
                "--"
                        + boundary
                        + "\r\nContent-Disposition: form-data; name=\""
                        + name
                        + "\"\r\n\r\n"
                        + value
                        + "\r\n");
    }

    private void writeString(ByteArrayOutputStream body, String text) {
        body.writeBytes(text.getBytes(StandardCharsets.UTF_8));
    }

    private HttpResponse<String> send(HttpRequest httpRequest) {
        try {
            return httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();

            throw new ProviderException("The transcription request was interrupted", exception);
        } catch (Exception exception) {
            throw new ProviderException("Could not reach the transcription API", exception);
        }
    }
}
