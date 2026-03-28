package org.voice.membership.dtos;

import lombok.Builder;

@Builder
public record WebhookStatusResponse(String status) {
}
