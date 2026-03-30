package org.voice.membership.dtos;

import lombok.Builder;

@Builder
public record UnreadCountResponse(long count) {
}
