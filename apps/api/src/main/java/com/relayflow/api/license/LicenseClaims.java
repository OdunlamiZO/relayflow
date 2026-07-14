package com.relayflow.api.license;

import java.time.Instant;

/** Claims carried by a self-hosted license key, issued by the hosted RelayFlow backend. */
public record LicenseClaims(String customerId, Instant issuedAt, Instant expiresAt) {}
