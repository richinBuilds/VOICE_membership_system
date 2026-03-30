package org.voice.membership.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CapturePayPalOrderRequest(
	@NotNull(message = "Membership is required")
	Integer membershipId,
	@NotBlank(message = "Order ID is required")
	String orderId) {
}
