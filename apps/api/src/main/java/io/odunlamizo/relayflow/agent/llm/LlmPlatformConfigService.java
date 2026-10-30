package io.odunlamizo.relayflow.agent.llm;

// Docs: docs/codebase-components.md#llmplatformconfigservice

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class LlmPlatformConfigService {

    private static final Logger log = LoggerFactory.getLogger(LlmPlatformConfigService.class);

    private static final String PROVIDER_KEY = "platform:llm:provider";

    private static final Set<String> FORMER_OPENAI_COMPATIBLE_PROVIDERS =
            Set.of("OPENAI", "GROQ", "OLLAMA", "OPENROUTER");

    private final StringRedisTemplate redis;

    public LlmPlatformConfigService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public String getConfigValue(String key, String defaultValue) {
        try {
            String value = redis.opsForValue().get(key);

            if (value != null && !value.isBlank()) {
                return value.strip();
            }
        } catch (Exception exception) {
            log.warn(
                    "Redis read failed, using default {} {}",
                    kv("key", key),
                    kv("error", exception.getMessage()));
        }

        return defaultValue;
    }

    public LlmProvider getActiveProvider() {
        try {
            String value = redis.opsForValue().get(PROVIDER_KEY);

            if (value != null && !value.isBlank()) {
                String provider = value.strip().toUpperCase().replace('-', '_');

                if (FORMER_OPENAI_COMPATIBLE_PROVIDERS.contains(provider)) {
                    return LlmProvider.OPENAI_COMPATIBLE;
                }

                return LlmProvider.valueOf(provider);
            }
        } catch (IllegalArgumentException exception) {
            log.warn(
                    "Unknown LLM provider in Redis, defaulting to ANTHROPIC {} {}",
                    kv("key", PROVIDER_KEY),
                    kv("error", exception.getMessage()));
        } catch (Exception exception) {
            log.warn(
                    "Redis read failed, defaulting LLM provider to ANTHROPIC {} {}",
                    kv("key", PROVIDER_KEY),
                    kv("error", exception.getMessage()));
        }

        return LlmProvider.ANTHROPIC;
    }
}
