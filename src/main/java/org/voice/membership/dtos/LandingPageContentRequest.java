package org.voice.membership.dtos;

import jakarta.validation.constraints.NotBlank;

public record LandingPageContentRequest(
        @NotBlank String heroTitle,
        @NotBlank String heroTagline,
        @NotBlank String benefitsTitle,
        @NotBlank String reasonsHeading,
        @NotBlank String reasonsContent) {
}
