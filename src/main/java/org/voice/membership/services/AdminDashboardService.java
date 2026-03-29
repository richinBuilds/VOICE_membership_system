package org.voice.membership.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.voice.membership.dtos.AdminDashboardFilters;
import org.voice.membership.dtos.AdminDashboardViewData;
import org.voice.membership.entities.User;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    private final UserFilterService userFilterService;
    private final AdminMemberService adminMemberService;

    public AdminDashboardViewData getDashboardData(AdminDashboardFilters filters) {
        List<User> allUsers = adminMemberService.getAllUsers();

        List<User> filteredUsers = userFilterService.filterUsers(
                allUsers,
                filters.address(),
                filters.city(),
                filters.province(),
                filters.chapter(),
                filters.minAge(),
                filters.maxAge(),
                filters.hearingLossType(),
                filters.equipmentType(),
                filters.startDate(),
                filters.endDate(),
                filters.paymentStatus(),
                filters.role());

        long adminCount = allUsers.stream().filter(u -> "ADMIN".equalsIgnoreCase(u.getRole())).count();
        long userCount = allUsers.stream().filter(u -> "USER".equalsIgnoreCase(u.getRole())).count();

        return AdminDashboardViewData.builder()
                .totalUsers(allUsers.size())
                .adminCount(adminCount)
                .userCount(userCount)
                .users(filteredUsers)
                .filters(filters)
                .build();
    }
}
