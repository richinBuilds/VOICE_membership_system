package org.voice.membership.dtos;

import lombok.Builder;

@Builder
public record BulkEmailResultResponse(int successCount, int failureCount) {
}
