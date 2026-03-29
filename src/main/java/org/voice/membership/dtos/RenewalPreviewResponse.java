package org.voice.membership.dtos;

import lombok.Builder;

@Builder
public record RenewalPreviewResponse(int withinDays, int membersFound, Object members, String note) {
}
