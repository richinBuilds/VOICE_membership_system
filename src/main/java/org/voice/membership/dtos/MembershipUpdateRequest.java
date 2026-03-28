package org.voice.membership.dtos;

import jakarta.validation.constraints.NotBlank;

public record MembershipUpdateRequest(
        @NotBlank String name,
        @NotBlank String description,
        String price,
        @NotBlank String features) {
}
