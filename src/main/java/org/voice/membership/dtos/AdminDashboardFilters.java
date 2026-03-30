package org.voice.membership.dtos;

import lombok.Builder;

@Builder
public record AdminDashboardFilters(
        String address,
        String city,
        String province,
        String chapter,
        Integer minAge,
        Integer maxAge,
        String hearingLossType,
        String equipmentType,
        String startDate,
        String endDate,
        String paymentStatus,
        String role) {
}
