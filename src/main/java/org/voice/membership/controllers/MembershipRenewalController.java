package org.voice.membership.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.voice.membership.services.MembershipRenewalReminderService;

import java.util.HashMap;
import java.util.Map;

/**
 * REST Controller for membership renewal operations.
 * Provides endpoints for checking and triggering renewal reminders.
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/membership-renewal")
@RequiredArgsConstructor
public class MembershipRenewalController {

    private final MembershipRenewalReminderService renewalReminderService;

    /**
     * Manually trigger renewal reminder check (for testing and immediate execution)
     */
    @PostMapping("/trigger-reminders")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> triggerRenewalReminders() {
        try {
            log.info("Admin triggered manual renewal reminder check");
            renewalReminderService.triggerManualReminderCheck();
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Renewal reminder check completed successfully");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error triggering renewal reminders", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Failed to trigger renewal reminders: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Get count of members expiring in specific days
     */
    @GetMapping("/expiring-count/{days}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getExpiringCount(@PathVariable int days) {
        try {
            long count = renewalReminderService.countMembersExpiringInDays(days);
            
            Map<String, Object> response = new HashMap<>();
            response.put("days", days);
            response.put("count", count);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting expiring member count", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get counts for all reminder intervals (1, 7 days)
     */
    @GetMapping("/expiring-summary")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getExpiringSummary() {
        try {
            Map<String, Object> response = new HashMap<>();
            response.put("expiring1Day", renewalReminderService.countMembersExpiringInDays(1));
            response.put("expiring7Days", renewalReminderService.countMembersExpiringInDays(7));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting expiring summary", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
