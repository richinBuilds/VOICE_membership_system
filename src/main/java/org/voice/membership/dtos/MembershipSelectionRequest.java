package org.voice.membership.dtos;

import jakarta.validation.constraints.NotNull;

public record MembershipSelectionRequest(
        @NotNull(message = "Membership selection is required") Integer membershipId) {
}
