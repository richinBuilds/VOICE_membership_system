package org.voice.membership.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.voice.membership.dtos.AdminDashboardFilters;
import org.voice.membership.dtos.AdminDashboardViewData;
import org.voice.membership.services.AdminDashboardService;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
/**
 * Provides administrative dashboards, filtering, and export features.
 * Allows admins to view and export member data with various filters applied.
 */
public class AdminController {

    private final AdminDashboardService adminDashboardService;

    @GetMapping("/dashboard")
    public String adminDashboard(
            Model model,
            @RequestParam(required = false) String address,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String province,
            @RequestParam(required = false) String chapter,
            @RequestParam(required = false) Integer minAge,
            @RequestParam(required = false) Integer maxAge,
            @RequestParam(required = false) String hearingLossType,
            @RequestParam(required = false) String equipmentType,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String paymentStatus,
            @RequestParam(required = false) String role) {
            AdminDashboardFilters filters = AdminDashboardFilters.builder()
                .address(address)
                .city(city)
                .province(province)
                .chapter(chapter)
                .minAge(minAge)
                .maxAge(maxAge)
                .hearingLossType(hearingLossType)
                .equipmentType(equipmentType)
                .startDate(startDate)
                .endDate(endDate)
                .paymentStatus(paymentStatus)
                .role(role)
                .build();

            AdminDashboardViewData dashboardData = adminDashboardService.getDashboardData(filters);

            model.addAttribute("totalUsers", dashboardData.totalUsers());
            model.addAttribute("adminCount", dashboardData.adminCount());
            model.addAttribute("userCount", dashboardData.userCount());
            model.addAttribute("users", dashboardData.users());

            model.addAttribute("address", filters.address());
            model.addAttribute("city", filters.city());
            model.addAttribute("province", filters.province());
            model.addAttribute("chapter", filters.chapter());
            model.addAttribute("minAge", filters.minAge());
            model.addAttribute("maxAge", filters.maxAge());
            model.addAttribute("hearingLossType", filters.hearingLossType());
            model.addAttribute("equipmentType", filters.equipmentType());
            model.addAttribute("startDate", filters.startDate());
            model.addAttribute("endDate", filters.endDate());
            model.addAttribute("paymentStatus", filters.paymentStatus());
            model.addAttribute("role", filters.role());

        return "admin";
    }
}
