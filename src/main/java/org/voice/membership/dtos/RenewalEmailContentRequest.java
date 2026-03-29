package org.voice.membership.dtos;

import jakarta.validation.constraints.NotBlank;

public record RenewalEmailContentRequest(
        @NotBlank String renewalSubject,
        @NotBlank String renewalBody) {
}
