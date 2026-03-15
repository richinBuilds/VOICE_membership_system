package org.voice.membership.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.voice.membership.dtos.AdminUpdateUserRequest;
import org.voice.membership.dtos.AdminAddMemberRequest;
import org.voice.membership.dtos.AdminAddAdminRequest;
import org.voice.membership.dtos.BulkEmailRequest;
import org.voice.membership.entities.Role;
import org.voice.membership.entities.User;
import org.voice.membership.repositories.UserRepository;
import org.voice.membership.repositories.MembershipRepository;
import org.voice.membership.services.EmailSenderService;
import org.voice.membership.services.UserFilterService;
import org.voice.membership.services.AdminExportService;
import org.voice.membership.services.AdminMemberService;
import org.voice.membership.services.AdminNotificationService;
import org.voice.membership.services.MembershipRenewalSchedulerService;
import org.voice.membership.entities.AdminNotification;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Controller
@RequestMapping("/admin")
/**
 * Provides administrative dashboards, filtering, and export features.
 * Allows admins to view and export member data with various filters applied.
 */
public class AdminController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MembershipRepository membershipRepository;

    @Autowired
    private EmailSenderService emailSenderService;

    @Autowired
    private UserFilterService userFilterService;

    @Autowired
    private AdminExportService adminExportService;

    @Autowired
    private AdminMemberService adminMemberService;

    @Autowired
    private AdminNotificationService adminNotificationService;

    @Autowired
    private MembershipRenewalSchedulerService membershipRenewalSchedulerService;

    @GetMapping("/dashboard")
    public String adminDashboard(
            Model model,
            Principal principal,
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

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String adminEmail = auth.getName();
        User admin = userRepository.findByEmail(adminEmail);

        String adminName = adminMemberService.formatAdminName(admin);
        model.addAttribute("adminName", adminName);
        model.addAttribute("adminEmail", adminEmail);

        List<User> allUsers = userRepository.findAll();

        List<User> filteredUsers = userFilterService.filterUsers(allUsers, address, city, province, chapter, minAge,
                maxAge,
                hearingLossType, equipmentType,
                startDate, endDate, paymentStatus, role);

        // Count users by role
        long adminCount = allUsers.stream().filter(u -> "ADMIN".equalsIgnoreCase(u.getRole())).count();
        long userCount = allUsers.stream().filter(u -> "USER".equalsIgnoreCase(u.getRole())).count();

        model.addAttribute("totalUsers", allUsers.size());
        model.addAttribute("adminCount", adminCount);
        model.addAttribute("userCount", userCount);
        model.addAttribute("users", filteredUsers);

        model.addAttribute("address", address);
        model.addAttribute("city", city);
        model.addAttribute("province", province);
        model.addAttribute("chapter", chapter);
        model.addAttribute("minAge", minAge);
        model.addAttribute("maxAge", maxAge);
        model.addAttribute("hearingLossType", hearingLossType);
        model.addAttribute("equipmentType", equipmentType);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("paymentStatus", paymentStatus);
        model.addAttribute("role", role);

        return "admin";
    }

    @GetMapping("/user/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getUserDetails(@PathVariable Integer id) {
        User user = userRepository.findById(id).orElse(null);

        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> userDetails = new HashMap<>();
        userDetails.put("id", user.getId());
        userDetails.put("firstName", user.getFirstName());
        userDetails.put("middleName", user.getMiddleName());
        userDetails.put("lastName", user.getLastName());
        userDetails.put("email", user.getEmail());
        userDetails.put("phone", user.getPhone());
        userDetails.put("address", user.getAddress());
        userDetails.put("postalCode", user.getPostalCode());
        userDetails.put("role", user.getRole() != null ? user.getRole() : "USER");
        userDetails.put("creation", user.getCreation());
        userDetails.put("children", user.getChildren());
        userDetails.put("membership", user.getMembership());

        return ResponseEntity.ok(userDetails);
    }

    @GetMapping("/export-users")
    public void exportUsersToExcel(HttpServletResponse response) throws IOException {
        List<User> users = userRepository.findAll();

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition",
                "attachment; filename=users_and_children_" + System.currentTimeMillis() + ".xlsx");

        adminExportService.exportUsersToExcel(users, response.getOutputStream());
    }

    /**
     * Display the edit member profile form
     */
    @GetMapping("/edit-member/{id}")
    public String showEditMemberForm(@PathVariable Integer id, Model model, RedirectAttributes redirectAttributes) {
        try {
            User user = userRepository.findById(id).orElse(null);

            if (user == null) {
                redirectAttributes.addFlashAttribute("error", "Member not found");
                return "redirect:/admin/dashboard";
            }

            // Create DTO from user
            AdminUpdateUserRequest updateRequest = AdminUpdateUserRequest.builder()
                    .userId(user.getId())
                    .firstName(user.getFirstName())
                    .middleName(user.getMiddleName())
                    .lastName(user.getLastName())
                    .email(user.getEmail())
                    .phone(user.getPhone())
                    .address(user.getAddress())
                    .city(user.getCity())
                    .province(user.getProvince())
                    .postalCode(user.getPostalCode())
                    .chapter(user.getChapter())
                    .membershipId(user.getMembership() != null ? user.getMembership().getId() : null)
                    .emailVerified(user.isEmailVerified())
                    .accountLocked(user.isAccountLocked())
                    .build();

            model.addAttribute("updateRequest", updateRequest);
            model.addAttribute("user", user);
            model.addAttribute("memberships", membershipRepository.findByActiveTrue());

            // Get admin info for header
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String adminEmail = auth.getName();
            User admin = userRepository.findByEmail(adminEmail);
            String adminName = adminMemberService.formatAdminName(admin);
            model.addAttribute("adminName", adminName);
            model.addAttribute("adminEmail", adminEmail);

            return "admin-edit-member";

        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Error loading member profile");
            return "redirect:/admin/dashboard";
        }
    }

    /**
     * Process the edit member profile form
     */
    @PostMapping("/edit-member/{id}")
    public String updateMemberProfile(
            @PathVariable Integer id,
            @Valid @ModelAttribute("updateRequest") AdminUpdateUserRequest updateRequest,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        try {
            // Load the user to be edited
            User user = userRepository.findById(id).orElse(null);

            if (user == null) {
                redirectAttributes.addFlashAttribute("error", "Member not found or has been deleted");
                return "redirect:/admin/dashboard";
            }

            // Check for validation errors
            if (bindingResult.hasErrors()) {
                model.addAttribute("updateRequest", updateRequest);
                model.addAttribute("user", user);
                model.addAttribute("memberships", membershipRepository.findByActiveTrue());

                // Get admin info for header
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                String adminEmail = auth.getName();
                User admin = userRepository.findByEmail(adminEmail);
                String adminName = adminMemberService.formatAdminName(admin);
                model.addAttribute("adminName", adminName);
                model.addAttribute("adminEmail", adminEmail);

                return "admin-edit-member";
            }

            // Attempt to update member using service
            User updatedUser = adminMemberService.updateMember(id, updateRequest);

            if (updatedUser == null) {
                // Email conflict
                bindingResult.addError(new FieldError("updateRequest", "email",
                        "Email already exists. Please choose a different email."));
                model.addAttribute("updateRequest", updateRequest);
                model.addAttribute("user", user);
                model.addAttribute("memberships", membershipRepository.findByActiveTrue());

                // Get admin info
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                String adminEmail = auth.getName();
                User admin = userRepository.findByEmail(adminEmail);
                String adminName = adminMemberService.formatAdminName(admin);
                model.addAttribute("adminName", adminName);
                model.addAttribute("adminEmail", adminEmail);

                return "admin-edit-member";
            }

            redirectAttributes.addFlashAttribute("success",
                    "Member profile for " + updatedUser.getFirstName() + " " + updatedUser.getLastName()
                            + " has been successfully updated");
            return "redirect:/admin/dashboard";

        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error",
                    "System error occurred while updating the profile. Please try again.");
            return "redirect:/admin/dashboard";
        }
    }

    /**
     * Display the add member form for admin to manually add a new member.
     * 
     * @param model the model to add attributes
     * @return the add member page template name
     */
    @GetMapping("/add-member")
    public String showAddMemberForm(Model model, Principal principal) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String adminEmail = auth.getName();
        User admin = userRepository.findByEmail(adminEmail);

        String adminName = adminMemberService.formatAdminName(admin);

        model.addAttribute("adminName", adminName);
        model.addAttribute("adminEmail", adminEmail);
        model.addAttribute("memberRequest", new AdminAddMemberRequest());
        model.addAttribute("memberships", membershipRepository.findAll());

        return "admin-add-member";
    }

    /**
     * Handle add member form submission.
     * Creates a new user account with the provided details.
     * 
     * @param memberRequest      the validated member data
     * @param bindingResult      validation results
     * @param redirectAttributes flash attributes for success/error messages
     * @return redirect to admin dashboard
     */
    @PostMapping("/add-member")
    public String addMember(
            @Valid @ModelAttribute("memberRequest") AdminAddMemberRequest memberRequest,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            Model model,
            Principal principal) {

        try {
            // Check for validation errors
            if (bindingResult.hasErrors()) {
                StringBuilder errorMessage = new StringBuilder("Validation errors: ");
                for (FieldError error : bindingResult.getFieldErrors()) {
                    errorMessage.append(error.getField()).append(" - ").append(error.getDefaultMessage()).append("; ");
                }
                redirectAttributes.addFlashAttribute("error", errorMessage.toString());
                return "redirect:/admin/add-member";
            }

            // Check if passwords match
            if (!memberRequest.getPassword().equals(memberRequest.getConfirmPassword())) {
                redirectAttributes.addFlashAttribute("error",
                        "Passwords do not match. Please ensure both password fields are identical.");
                return "redirect:/admin/add-member";
            }

            // Attempt to create member using service
            User newUser = adminMemberService.createMember(memberRequest);

            if (newUser == null) {
                // Email already exists
                redirectAttributes.addFlashAttribute("error",
                        "A member with email " + memberRequest.getEmail() + " already exists in the system.");
                return "redirect:/admin/add-member";
            }

            redirectAttributes.addFlashAttribute("success",
                    "Member " + newUser.getFirstName() + " " + newUser.getLastName()
                            + " has been successfully added to the system.");
            return "redirect:/admin/dashboard";

        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error",
                    "System error occurred while adding the member. Please try again.");
            return "redirect:/admin/add-member";
        }
    }

    /**
     * Handle member deletion.
     * Removes a member from the system after confirmation.
     * 
     * @param id                 the user ID to delete
     * @param redirectAttributes flash attributes for success/error messages
     * @return redirect to admin dashboard
     */
    @PostMapping("/delete-member/{id}")
    public String deleteMember(
            @PathVariable Integer id,
            RedirectAttributes redirectAttributes) {

        try {
            User deletedUser = adminMemberService.deleteMember(id);

            if (deletedUser == null) {
                User user = userRepository.findById(id).orElse(null);
                if (user != null && user.getRole() != null && user.getRole().equals(Role.ADMIN.name())) {
                    redirectAttributes.addFlashAttribute("error",
                            "Cannot delete admin accounts. Please contact system administrator.");
                } else {
                    redirectAttributes.addFlashAttribute("error",
                            "Member not found. The member may have already been deleted.");
                }
                return "redirect:/admin/dashboard";
            }

            String memberName = deletedUser.getFirstName() + " " + deletedUser.getLastName();
            redirectAttributes.addFlashAttribute("success",
                    "Member " + memberName + " has been successfully deleted from the system.");
            return "redirect:/admin/dashboard";

        } catch (Exception e) {
            e.printStackTrace();
            String errorMessage = "System error occurred while deleting the member. ";

            // Add specific error details
            if (e.getCause() != null && e.getCause().getMessage() != null) {
                errorMessage += "Details: " + e.getCause().getMessage();
            } else if (e.getMessage() != null) {
                errorMessage += "Details: " + e.getMessage();
            }

            redirectAttributes.addFlashAttribute("error", errorMessage);
            return "redirect:/admin/dashboard";
        }
    }

    /**
     * Display the add admin form for creating a new admin account.
     * 
     * @param model the model to add attributes
     * @return the add admin page template name
     */
    @GetMapping("/add-admin")
    public String showAddAdminForm(Model model, Principal principal) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String adminEmail = auth.getName();
        User admin = userRepository.findByEmail(adminEmail);

        String adminName = adminMemberService.formatAdminName(admin);

        model.addAttribute("adminName", adminName);
        model.addAttribute("adminEmail", adminEmail);
        model.addAttribute("adminRequest", new AdminAddAdminRequest());

        return "admin-add-admin";
    }

    /**
     * Handle add admin form submission.
     * Creates a new admin account with the provided details.
     * 
     * @param adminRequest       the validated admin data
     * @param bindingResult      validation results
     * @param redirectAttributes flash attributes for success/error messages
     * @return redirect to admin dashboard
     */
    @PostMapping("/add-admin")
    public String addAdmin(
            @Valid @ModelAttribute("adminRequest") AdminAddAdminRequest adminRequest,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            Model model,
            Principal principal) {

        try {
            // Check for validation errors
            if (bindingResult.hasErrors()) {
                StringBuilder errorMessage = new StringBuilder("Validation errors: ");
                for (FieldError error : bindingResult.getFieldErrors()) {
                    errorMessage.append(error.getField()).append(" - ").append(error.getDefaultMessage()).append("; ");
                }
                redirectAttributes.addFlashAttribute("error", errorMessage.toString());
                return "redirect:/admin/add-admin";
            }

            // Check if passwords match
            if (!adminRequest.getPassword().equals(adminRequest.getConfirmPassword())) {
                redirectAttributes.addFlashAttribute("error",
                        "Passwords do not match. Please ensure both password fields are identical.");
                return "redirect:/admin/add-admin";
            }

            // Attempt to create admin using service
            User newAdmin = adminMemberService.createAdmin(adminRequest);

            if (newAdmin == null) {
                // Email already exists
                redirectAttributes.addFlashAttribute("error",
                        "An admin account with email " + adminRequest.getEmail() + " already exists in the system.");
                return "redirect:/admin/add-admin";
            }

            redirectAttributes.addFlashAttribute("success",
                    "Admin account for " + newAdmin.getFirstName() + " " + newAdmin.getLastName()
                            + " has been successfully created.");
            return "redirect:/admin/dashboard?role=ADMIN";

        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error",
                    "System error occurred while creating the admin account. Please try again.");
            return "redirect:/admin/add-admin";
        }
    }

    @PostMapping("/send-bulk-email")
    @ResponseBody
    public ResponseEntity<Map<String, String>> sendBulkEmail(
            @RequestBody BulkEmailRequest request,
            Authentication authentication) {

        Map<String, String> response = new HashMap<>();

        try {
            // Get admin info
            String adminEmail = authentication.getName();
            User admin = userRepository.findByEmail(adminEmail);
            String adminName = adminMemberService.formatAdminName(admin);

            // Validate request
            if (request.getRecipientIds() == null || request.getRecipientIds().isEmpty()) {
                response.put("status", "error");
                response.put("message", "No recipients selected");
                return ResponseEntity.badRequest().body(response);
            }

            if (request.getSubject() == null || request.getSubject().trim().isEmpty()) {
                response.put("status", "error");
                response.put("message", "Subject cannot be empty");
                return ResponseEntity.badRequest().body(response);
            }

            if (request.getMessageBody() == null || request.getMessageBody().trim().isEmpty()) {
                response.put("status", "error");
                response.put("message", "Message body cannot be empty");
                return ResponseEntity.badRequest().body(response);
            }

            int successCount = 0;
            int failureCount = 0;
            StringBuilder failedEmails = new StringBuilder();

            // Send email to each recipient
            for (Integer userId : request.getRecipientIds()) {
                try {
                    User user = userRepository.findById(userId).orElse(null);
                    if (user != null && user.getEmail() != null) {
                        emailSenderService.sendCustomEmail(
                                user.getEmail(),
                                request.getSubject(),
                                request.getMessageBody(),
                                adminName);
                        successCount++;
                    } else {
                        failureCount++;
                        if (user != null) {
                            failedEmails.append(user.getFirstName()).append(" ")
                                    .append(user.getLastName()).append(", ");
                        }
                    }
                } catch (Exception e) {
                    failureCount++;
                    User user = userRepository.findById(userId).orElse(null);
                    if (user != null) {
                        failedEmails.append(user.getEmail()).append(", ");
                    }
                }
            }

            // Build response message
            String message = "Emails sent successfully to " + successCount + " recipient(s)";
            if (failureCount > 0) {
                message += ". Failed to send to " + failureCount + " recipient(s)";
                if (failedEmails.length() > 0) {
                    String failed = failedEmails.toString();
                    if (failed.endsWith(", ")) {
                        failed = failed.substring(0, failed.length() - 2);
                    }
                    message += ": " + failed;
                }
            }

            if (successCount == 0) {
                response.put("status", "error");
                response.put("message", "Failed to send emails to all recipients");
                return ResponseEntity.badRequest().body(response);
            }

            response.put("status", "success");
            response.put("message", message);
            response.put("successCount", String.valueOf(successCount));
            response.put("failureCount", String.valueOf(failureCount));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            response.put("status", "error");
            response.put("message", "Server error: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    // ==================== Notification API Endpoints ====================

    /**
     * Get all unread notifications for admin
     */
    @GetMapping("/api/admin/notifications/unread")
    @ResponseBody
    public ResponseEntity<List<AdminNotification>> getUnreadNotifications() {
        List<AdminNotification> notifications = adminNotificationService.getUnreadNotifications();
        return ResponseEntity.ok(notifications);
    }

    /**
     * Get notification count
     */
    @GetMapping("/api/admin/notifications/count")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getNotificationCount() {
        long count = adminNotificationService.getUnreadCount();
        Map<String, Object> response = new HashMap<>();
        response.put("count", count);
        return ResponseEntity.ok(response);
    }

    /**
     * Mark a notification as read
     */
    @PostMapping("/api/admin/notifications/{id}/read")
    @ResponseBody
    public ResponseEntity<Map<String, String>> markNotificationAsRead(@PathVariable Long id) {
        Map<String, String> response = new HashMap<>();
        try {
            adminNotificationService.markAsRead(id);
            response.put("status", "success");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Dismiss all notifications
     */
    @PostMapping("/api/admin/notifications/dismiss-all")
    @ResponseBody
    public ResponseEntity<Map<String, String>> dismissAllNotifications() {
        Map<String, String> response = new HashMap<>();
        try {
            log.info("Dismissing all notifications");
            adminNotificationService.dismissAllNotifications();
            response.put("status", "success");
            response.put("message", "All notifications dismissed");
            log.info("Successfully dismissed all notifications");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to dismiss notifications: {}", e.getMessage(), e);
            response.put("status", "error");
            response.put("message", "Failed to dismiss notifications: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Dismiss a single notification
     */
    @PostMapping("/api/admin/notifications/{id}/dismiss")
    @ResponseBody
    public ResponseEntity<Map<String, String>> dismissNotification(@PathVariable Long id) {
        Map<String, String> response = new HashMap<>();
        try {
            adminNotificationService.dismissNotification(id);
            response.put("status", "success");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Get notification details with associated users
     */
    @GetMapping("/api/admin/notifications/{id}/details")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getNotificationDetails(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            AdminNotification notification = adminNotificationService.getNotificationById(id);
            if (notification == null) {
                response.put("status", "error");
                response.put("message", "Notification not found");
                return ResponseEntity.notFound().build();
            }

            List<User> newMembers = adminNotificationService.getNewPaidMembersForNotification(id);

            response.put("notification", notification);
            response.put("users", newMembers);
            response.put("status", "success");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Admin Notifications Dashboard Page
     */
    @GetMapping("/notifications")
    public String notificationsDashboard(Model model, Principal principal) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String adminEmail = auth.getName();
        User admin = userRepository.findByEmail(adminEmail);

        String adminName = adminMemberService.formatAdminName(admin);
        model.addAttribute("adminName", adminName);
        model.addAttribute("adminEmail", adminEmail);

        // Get all new users from unread notifications
        List<User> newUsers = adminNotificationService.getAllNewUsersFromNotifications();
        model.addAttribute("newUsers", newUsers);

        return "admin-notifications";
    }

    /**
     * Manually trigger the membership renewal reminder job.
     * Returns a detailed breakdown per reminder window so you can see exactly
     * how many members were found and whether emails were sent successfully.
     */
    @PostMapping("/trigger-renewal-reminders")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> triggerRenewalReminders() {
        try {
            log.info("Admin manually triggered membership renewal reminder job");
            Map<String, Object> result = membershipRenewalSchedulerService.sendRenewalReminders();
            result.put("status", "success");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error triggering renewal reminders: {}", e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * Preview which paid members have memberships expiring within the next N days
     * WITHOUT sending any emails. Defaults to 30 days.
     * Use this to verify your test data before triggering the job.
     *
     * Example: GET /admin/renewal-reminders/preview?withinDays=10
     */
    @GetMapping("/renewal-reminders/preview")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> previewRenewalReminders(
            @RequestParam(defaultValue = "30") int withinDays) {
        try {
            var members = membershipRenewalSchedulerService.previewExpiringMembers(withinDays);
            Map<String, Object> result = new HashMap<>();
            result.put("status", "success");
            result.put("withinDays", withinDays);
            result.put("membersFound", members.size());
            result.put("members", members);
            result.put("note", "No emails were sent. Use POST /admin/trigger-renewal-reminders to send.");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error previewing renewal reminders: {}", e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
}
