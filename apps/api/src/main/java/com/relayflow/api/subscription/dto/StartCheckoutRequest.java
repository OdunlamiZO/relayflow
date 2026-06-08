package com.relayflow.api.subscription.dto;

import com.relayflow.api.subscription.domain.Plan;
import jakarta.validation.constraints.NotNull;

public record StartCheckoutRequest(@NotNull Plan plan) {}
