package com.relayflow.api.webhook.dto;

/**
 * Returned once after a secret rotation.
 *
 * <p>The {@code secret} field contains the new plaintext HMAC signing key. It is <em>never
 * stored</em> in plaintext and will not be retrievable again — the caller must persist it
 * immediately.
 */
public record RotateWebhookSecretResponse(String secret) {}
