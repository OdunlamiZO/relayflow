package io.odunlamizo.relayflow.whatsapp;

// Docs: docs/codebase-components.md#whatsappcredentials

public record WhatsAppCredentials(String accessToken, String phoneNumberId, String verifyToken) {}
