package org.voice.membership.dtos;

import jakarta.validation.constraints.NotNull;

public record CreatePayPalOrderRequest(
	@NotNull(message = "Membership is required")
	Integer membershipId) {
}
