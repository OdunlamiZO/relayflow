package com.relayflow.api.license;

/**
 * Thrown when a self-hosted license key is missing, malformed, or fails signature verification.
 * Expired keys within the grace period do not throw this — see {@link LicenseKeyValidator}.
 */
public class LicenseVerificationException extends RuntimeException {

    public LicenseVerificationException(String message) {
        super(message);
    }

    public LicenseVerificationException(String message, Throwable cause) {
        super(message, cause);
    }
}
