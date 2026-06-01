package com.relayflow.api.whatsapp;

/**
 * Plaintext credential blob stored (encrypted) in {@code channel_accounts.encrypted_credentials}
 * for WhatsApp channel accounts.
 *
 * <p>All three fields are required:
 *
 * <ul>
 *   <li>{@code accessToken} — permanent system-user access token from Meta
 *   <li>{@code phoneNumberId} — the Meta phone-number ID ({@code WABA > Phone numbers > ID})
 *   <li>{@code verifyToken} — a secret string the workspace sets in the Meta Developer Console
 *       webhook configuration; Meta echoes it back on the GET verification challenge
 * </ul>
 */
public record WhatsAppCredentials(String accessToken, String phoneNumberId, String verifyToken) {}
