package org.voice.membership.dtos;

public record CapturePayPalOrderRequest(Integer membershipId, String orderId) {
}
