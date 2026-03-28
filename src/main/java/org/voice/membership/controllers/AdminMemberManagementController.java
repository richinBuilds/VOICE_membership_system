package org.voice.membership.controllers;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.voice.membership.dtos.*;
import org.voice.membership.entities.Role;
import org.voice.membership.entities.User;
import org.voice.membership.services.AdminExportService;
import org.voice.membership.services.AdminMemberService;
import org.voice.membership.services.AdminMemberViewService;
import org.voice.membership.services.MembershipService;

import java.io.IOException;
import java.util.List;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminMemberManagementController {

    private final AdminExportService adminExportService;
    private final AdminMemberService adminMemberService;
    private final AdminMemberViewService adminMemberViewService;
    private final MembershipService membershipService;

    @GetMapping("/user/{id}")
    @ResponseBody
    public ResponseEntity<ApiResponse<AdminUserDetailsResponse>> getUserDetails(@PathVariable Integer id) {
        User user = adminMemberService.getUserById(id);

        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(ApiResponse.success("User details fetched", adminMemberViewService.buildUserDetails(user)));
    }

    @GetMapping("/export-users")
    public void exportUsersToExcel(HttpServletResponse response) throws IOException {
        List<User> users = adminMemberService.getAllUsers();

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition",
                "attachment; filename=users_and_children_" + System.currentTimeMillis() + ".xlsx");

        adminExportService.exportUsersToExcel(users, response.getOutputStream());
    }

    @GetMapping("/edit-member/{id}")
    public String showEditMemberForm(@PathVariable Integer id, Model model, RedirectAttributes redirectAttributes) {
        try {
            User user = adminMemberService.getUserById(id);

            if (user == null) {
                redirectAttributes.addFlashAttribute("error", "Member not found");
                return "redirect:/admin/dashboard";
            }

            model.addAttribute("updateRequest", adminMemberViewService.buildUpdateRequest(user));
            model.addAttribute("user", user);
            model.addAttribute("memberships", membershipService.getActiveMemberships());

            return "admin-edit-member";

        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Error loading member profile");
            return "redirect:/admin/dashboard";
        }
    }

    @PostMapping("/edit-member/{id}")
    public String updateMemberProfile(
            @PathVariable Integer id,
            @Valid @ModelAttribute("updateRequest") AdminUpdateUserRequest updateRequest,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        try {
            User user = adminMemberService.getUserById(id);

            if (user == null) {
                redirectAttributes.addFlashAttribute("error", "Member not found or has been deleted");
                return "redirect:/admin/dashboard";
            }

            if (bindingResult.hasErrors()) {
                model.addAttribute("updateRequest", updateRequest);
                model.addAttribute("user", user);
                model.addAttribute("memberships", membershipService.getActiveMemberships());
                return "admin-edit-member";
            }

            User updatedUser = adminMemberService.updateMember(id, updateRequest);

            if (updatedUser == null) {
                model.addAttribute("emailError", "Email already exists. Please choose a different email.");
                model.addAttribute("updateRequest", updateRequest);
                model.addAttribute("user", user);
                model.addAttribute("memberships", membershipService.getActiveMemberships());
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

    @GetMapping("/add-member")
    public String showAddMemberForm(Model model) {
        model.addAttribute("memberRequest", new AdminAddMemberRequest());
        model.addAttribute("memberships", membershipService.getAllMemberships());
        return "admin-add-member";
    }

    @PostMapping("/add-member")
    public String addMember(
            @Valid @ModelAttribute("memberRequest") AdminAddMemberRequest memberRequest,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            Model model) {

        try {
            if (bindingResult.hasErrors()) {
                model.addAttribute("memberships", membershipService.getAllMemberships());
                return "admin-add-member";
            }

            User newUser = adminMemberService.createMember(memberRequest);

            if (newUser == null) {
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

    @PostMapping("/delete-member/{id}")
    public String deleteMember(
            @PathVariable Integer id,
            RedirectAttributes redirectAttributes) {

        try {
            User deletedUser = adminMemberService.deleteMember(id);

            if (deletedUser == null) {
                User existingUser = adminMemberService.getUserById(id);
                if (existingUser != null && existingUser.getRole() != null && existingUser.getRole().equals(Role.ADMIN.name())) {
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

            if (e.getCause() != null && e.getCause().getMessage() != null) {
                errorMessage += "Details: " + e.getCause().getMessage();
            } else if (e.getMessage() != null) {
                errorMessage += "Details: " + e.getMessage();
            }

            redirectAttributes.addFlashAttribute("error", errorMessage);
            return "redirect:/admin/dashboard";
        }
    }

    @GetMapping("/add-admin")
    public String showAddAdminForm(Model model) {
        model.addAttribute("adminRequest", new AdminAddAdminRequest());
        return "admin-add-admin";
    }

    @PostMapping("/add-admin")
    public String addAdmin(
            @Valid @ModelAttribute("adminRequest") AdminAddAdminRequest adminRequest,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            Model model) {

        try {
            if (bindingResult.hasErrors()) {
                return "admin-add-admin";
            }

            User newAdmin = adminMemberService.createAdmin(adminRequest);

            if (newAdmin == null) {
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
    public ResponseEntity<ApiResponse<BulkEmailResultResponse>> sendBulkEmail(
            @Valid @RequestBody BulkEmailRequest request,
            Authentication authentication) {
        String adminEmail = authentication.getName();
        String adminName = adminMemberService.getAdminNameByEmail(adminEmail);

        AdminMemberService.BulkEmailResult result = adminMemberService.sendBulkEmails(request, adminName);
        BulkEmailResultResponse data = BulkEmailResultResponse.builder()
                .successCount(result.successCount())
                .failureCount(result.failureCount())
                .build();

        if (result.successCount() == 0) {
            return ResponseEntity.badRequest().body(ApiResponse.error(result.message(), data));
        }

        return ResponseEntity.ok(ApiResponse.success(result.message(), data));
    }
}
