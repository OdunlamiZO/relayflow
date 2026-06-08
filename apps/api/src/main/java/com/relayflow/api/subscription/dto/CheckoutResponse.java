package com.relayflow.api.subscription.dto;

/**
 * Returned by the checkout endpoint. The frontend redirects the user to {@code authorizationUrl}.
 */
public record CheckoutResponse(String authorizationUrl) {}
