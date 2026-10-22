package io.odunlamizo.relayflow.webhook;

import java.net.IDN;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/** Rejects URLs resolving to loopback, link-local, private, multicast, or wildcard addresses. */
@Component
public class WebhookUrlValidator {

    public void validate(String url) {
        URI uri;

        try {
            uri = URI.create(url);
        } catch (IllegalArgumentException exception) {
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

    public boolean isSafe(String url) {
        try {
            URI uri = URI.create(url);
            String host = uri.getHost();

            return host != null && isPublicHost(host);
        } catch (IllegalArgumentException exception) {
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
        } catch (UnknownHostException exception) {
            return false;
        }
    }
}
