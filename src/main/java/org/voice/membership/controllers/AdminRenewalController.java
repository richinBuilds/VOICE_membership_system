package org.voice.membership.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.voice.membership.dtos.ApiResponse;
import org.voice.membership.dtos.RenewalPreviewResponse;
import org.voice.membership.services.MembershipRenewalSchedulerService;

@Slf4j
@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminRenewalController {

    private final MembershipRenewalSchedulerService membershipRenewalSchedulerService;

    @PostMapping("/trigger-renewal-reminders")
    @ResponseBody
    public ResponseEntity<ApiResponse<Object>> triggerRenewalReminders() {
        log.info("Admin manually triggered membership renewal reminder job");
        Object result = membershipRenewalSchedulerService.sendRenewalReminders();
        return ResponseEntity.ok(ApiResponse.success("Renewal reminders triggered", result));
    }

    @GetMapping("/renewal-reminders/preview")
    @ResponseBody
    public ResponseEntity<ApiResponse<RenewalPreviewResponse>> previewRenewalReminders(
            @RequestParam(defaultValue = "30") int withinDays) {
        var members = membershipRenewalSchedulerService.previewExpiringMembers(withinDays);
        RenewalPreviewResponse data = RenewalPreviewResponse.builder()
                .withinDays(withinDays)
                .membersFound(members.size())
                .members(members)
                .note("No emails were sent. Use POST /admin/trigger-renewal-reminders to send.")
                .build();
        return ResponseEntity.ok(ApiResponse.success("Renewal reminder preview fetched", data));
    }
}
