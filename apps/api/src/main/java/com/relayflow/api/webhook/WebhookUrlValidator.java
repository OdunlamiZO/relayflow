package com.relayflow.api.webhook;

import java.net.IDN;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * Guards against SSRF via workspace-configured webhook URLs by rejecting URLs that resolve to
 * loopback, link-local, site-local (private), multicast, or wildcard addresses.
 */
@Component
public class WebhookUrlValidator {

    /** Throws {@link ResponseStatusException} (400) if {@code url} is not a safe webhook target. */
    public void validate(String url) {
        URI uri;

        try {
            uri = URI.create(url);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid webhook URL");
        }

        String scheme = uri.getScheme();

        if (scheme == null
                || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Webhook URL must use http or https");
        }

        String host = uri.getHost();

        if (host == null || host.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Webhook URL has no host");
        }

        if (!isPublicHost(host)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Webhook URL must not point to a private, loopback, or local address");
        }
    }

    /** Returns {@code true} if {@code url} still resolves to a public address. */
    public boolean isSafe(String url) {
        try {
            URI uri = URI.create(url);
            String host = uri.getHost();

            return host != null && isPublicHost(host);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private boolean isPublicHost(String host) {
        try {
            for (InetAddress address : InetAddress.getAllByName(IDN.toASCII(host))) {
                if (address.isLoopbackAddress()
                        || address.isLinkLocalAddress()
                        || address.isSiteLocalAddress()
                        || address.isMulticastAddress()
                        || address.isAnyLocalAddress()) {
                    return false;
                }
            }

            return true;
        } catch (UnknownHostException e) {
            return false;
        }
    }
}
