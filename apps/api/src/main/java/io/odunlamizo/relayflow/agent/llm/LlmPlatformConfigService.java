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

    private static final String DEFAULT_PROVIDER = "ANTHROPIC";

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

    public String getActiveProvider() {
        String provider =
                getConfigValue(PROVIDER_KEY, DEFAULT_PROVIDER).toUpperCase().replace('-', '_');

        return FORMER_OPENAI_COMPATIBLE_PROVIDERS.contains(provider)
                ? "OPENAI_COMPATIBLE"
                : provider;
    }
}
