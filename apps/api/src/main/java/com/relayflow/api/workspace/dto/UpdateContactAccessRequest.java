package com.relayflow.api.workspace.dto;

import com.relayflow.api.workspace.domain.ContactAccess;
import jakarta.validation.constraints.NotNull;

public record UpdateContactAccessRequest(
        @NotNull ContactAccess contactAccess, String phoneRegion) {}
