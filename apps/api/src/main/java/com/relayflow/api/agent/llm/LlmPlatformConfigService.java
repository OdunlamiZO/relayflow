package com.relayflow.api.agent.llm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class LlmPlatformConfigService {

    private static final Logger log = LoggerFactory.getLogger(LlmPlatformConfigService.class);

    private static final String PROVIDER_KEY = "platform:llm:provider";

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
        } catch (Exception e) {
            log.warn("Failed to read Redis key={} — using default: {}", key, e.getMessage());
        }

        return defaultValue;
    }

    public LlmProvider getActiveProvider() {
        try {
            String value = redis.opsForValue().get(PROVIDER_KEY);

            if (value != null && !value.isBlank()) {
                return LlmProvider.valueOf(value.strip().toUpperCase());
            }
        } catch (IllegalArgumentException e) {
            log.warn(
                    "Unknown LLM provider value in Redis (key={}) — defaulting to ANTHROPIC: {}",
                    PROVIDER_KEY,
                    e.getMessage());
        } catch (Exception e) {
            log.warn(
                    "Failed to read LLM provider from Redis (key={}) — defaulting to ANTHROPIC: {}",
                    PROVIDER_KEY,
                    e.getMessage());
        }

        return LlmProvider.ANTHROPIC;
    }
}
