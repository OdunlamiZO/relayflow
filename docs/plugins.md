# Writing a RelayFlow Plugin

A plugin is a JAR that adds a provider to RelayFlow:

| Interface | What it does | Selected with |
|---|---|---|
| `ChatModelProvider` | Runs the AI agent's LLM calls | An agent's own LLM provider setting, then Redis `platform:llm:provider`, then the first installed one |
| `TranscriptionProvider` | Turns voice messages into text | Redis `platform:transcription:provider`, or the only one installed |
| `StorageProvider` | Saves and loads voice message recordings | Redis `platform:storage:provider`, or the only one installed |

The official plugins in [`plugins/`](../plugins) are complete examples: Anthropic and
OpenAI-compatible chat models, OpenAI-compatible transcription, and local disk storage.

## 1. Add the plugin API

Plugins compile against `relayflow-plugin-api`, published on [JitPack](https://jitpack.io). Use
the RelayFlow release tag you run as the version, with `provided` scope, since the API supplies
these classes at runtime:

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>com.github.OdunlamiZO.relayflow</groupId>
        <artifactId>relayflow-plugin-api</artifactId>
        <version>v1.4.0</version>
        <scope>provided</scope>
    </dependency>
</dependencies>
```

Target Java 21. The official plugins are on JitPack too, for example
`com.github.OdunlamiZO.relayflow:relayflow-plugin-openai-compatible`, so a plugin can depend on one
and extend it.

## 2. Implement a provider

```java
package com.example.relayflow;

import io.odunlamizo.relayflow.plugin.ChatModelProvider;
import io.odunlamizo.relayflow.plugin.ChatRequest;
import io.odunlamizo.relayflow.plugin.ProviderException;
import io.odunlamizo.relayflow.plugin.ProviderSettings;

public class ExampleChatModelProvider implements ChatModelProvider {

    private String apiKey = "";

    @Override
    public String id() {
        return "EXAMPLE";
    }

    @Override
    public String displayName() {
        return "Example";
    }

    @Override
    public void initialize(ProviderSettings settings) {
        apiKey = settings.get("EXAMPLE_API_KEY").orElse("");
    }

    @Override
    public String complete(ChatRequest request) {
        if (apiKey.isEmpty()) {
            throw new ProviderException("Set EXAMPLE_API_KEY to use the Example provider");
        }

        // Send request.systemPrompt(), request.messages() and request.model() to your API,
        // then return the model's reply text.
        return callYourApi(request);
    }
}
```

- `id()` is what goes in Redis and in an agent's settings. Use upper case with underscores, and
  keep it unique per provider type: two installed chat model plugins with the same ID stop the
  API from starting.
- `initialize` runs once at startup. `ProviderSettings.get(name)` reads environment variables, so
  document the ones your plugin needs.
- Throw `ProviderException`, or any runtime exception, when a call fails. RelayFlow logs it and
  carries on: a failed LLM call escalates the conversation to a human, and a failed transcription
  leaves the voice message for a human.
- Keep the class thread-safe. One instance serves every request.

### Chat models

`ChatRequest` holds the system prompt, the conversation as `ChatMessage`s (`user` or
`assistant`), and the model. RelayFlow has already added its JSON response instructions to the
system prompt, so return the model's reply text as is. Turn on your API's JSON mode if it has one.

The model is the agent's own when set, otherwise the Redis key
`platform:llm:<id>:model`, with the ID lowercased and `_` as `-` (`EXAMPLE` reads
`platform:llm:example:model`). It's null when neither is set, so fall back to a default model or
throw.

### Transcription

`TranscriptionRequest` holds the audio bytes, its MIME type (often `audio/ogg`), and the model from
`platform:transcription:<id>:model`, or null. Return the transcript; return an empty string when
there's no speech.

### Storage

`store(StorageRequest)` saves the bytes and returns a key; `load(key)` returns them. RelayFlow
keeps the key and your provider's ID on the message, and plays the recording through your
provider later, so keys must stay valid. Recordings are at most 25 MB.

## 3. Register it

List the class in a service file named after the interface, in
`src/main/resources/META-INF/services/`:

```text
# src/main/resources/META-INF/services/io.odunlamizo.relayflow.plugin.ChatModelProvider
com.example.relayflow.ExampleChatModelProvider
```

The class needs a public no-argument constructor.

## 4. Package it

Build one JAR that contains your plugin and its own libraries, but not `relayflow-plugin-api`. Use
the Maven Shade plugin, and relocate libraries the API also uses, such as Jackson, so the two
copies can't clash. The official plugins' `pom.xml` files and the root `pom.xml` show the setup.

## 5. Install it

Copy the JAR into the `plugins` folder next to `docker-compose.yml` and restart the API:

```bash
cp target/relayflow-plugin-example.jar plugins/
docker compose --profile prod restart api
```

The API logs `Plugin provider loaded` with your provider's ID. Then select it, for a chat model:

```bash
docker compose --profile prod exec redis redis-cli SET platform:llm:provider EXAMPLE
```

When running the API locally, put the JAR in `apps/api/plugins`, or set
`RELAYFLOW_PLUGINS_DIRECTORY`.

A plugin runs inside the API with access to its database, secrets, and memory. Only install
plugins you trust.
