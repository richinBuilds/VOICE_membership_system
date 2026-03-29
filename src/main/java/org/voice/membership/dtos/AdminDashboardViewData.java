package org.voice.membership.dtos;

import org.voice.membership.entities.User;

import java.util.List;

import lombok.Builder;

@Builder
public record AdminDashboardViewData(
        int totalUsers,
        long adminCount,
        long userCount,
        List<User> users,
        AdminDashboardFilters filters) {
}
