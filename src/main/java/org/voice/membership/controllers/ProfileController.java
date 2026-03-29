package org.voice.membership.controllers;

import org.voice.membership.dtos.ProfilePageViewData;
import org.voice.membership.dtos.UpdateUserRequest;
import org.voice.membership.entities.User;
import org.voice.membership.services.ProfileEditService;
import org.voice.membership.services.ProfileViewService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import java.security.Principal;

/**
 * 
 * Handles user dashboard and profile management.
 * Manages user profile viewing, editing, and child account operations.
 * Displays user membership information, manages child information (add, edit,
 * delete).
 * Provides endpoints for profile updates and child management on the user
 * dashboard.
 */
@Controller
@AllArgsConstructor
@RequestMapping("/profile")
public class ProfileController {

    private final ProfileEditService profileEditService;
    private final ProfileViewService profileViewService;

    @GetMapping
    public String profile(Model model, Principal principal) {
        try {
            ProfilePageViewData viewData = profileViewService.buildProfilePageView(principal.getName());
            if (viewData == null) {
                return "redirect:/login";
            }

            model.addAttribute("user", viewData.user());
            model.addAttribute("userName", viewData.userName());
            model.addAttribute("userEmail", viewData.userEmail());
            model.addAttribute("userPhone", viewData.userPhone());
            model.addAttribute("userAddress", viewData.userAddress());
            model.addAttribute("userCity", viewData.userCity());
            model.addAttribute("userProvince", viewData.userProvince());
            model.addAttribute("userPostalCode", viewData.userPostalCode());
            model.addAttribute("memberSince", viewData.memberSince());
            model.addAttribute("children", viewData.children());
            model.addAttribute("membershipStatus", viewData.membershipStatus());
            model.addAttribute("membershipType", viewData.membershipType());
            model.addAttribute("hasPaidMembership", viewData.hasPaidMembership());
            model.addAttribute("membershipExpiryDate", viewData.membershipExpiryDate());
            model.addAttribute("showBenefits", viewData.showBenefits());
            model.addAttribute("isMembershipExpired", viewData.isMembershipExpired());
            model.addAttribute("membershipBenefit", viewData.membershipBenefit());

            return "profile";
        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/login";
        }
    }

    @GetMapping("/edit")
    public String editProfile(Model model, Principal principal) {
        UpdateUserRequest updateUserRequest = profileEditService.buildUpdateRequest(principal.getName());
        if (updateUserRequest == null) {
            return "redirect:/login";
        }
        model.addAttribute("updateUserRequest", updateUserRequest);
        return "editProfile";
    }

    @PostMapping("/edit")
    public String editProfile(Model model,
            @Valid @ModelAttribute("updateUserRequest") UpdateUserRequest updateUserRequest,
            BindingResult bindingResult,
            Principal principal) {
        try {
            if (profileEditService.buildUpdateRequest(principal.getName()) == null) {
                return "redirect:/login";
            }

            if (bindingResult.hasErrors()) {
                model.addAttribute("updateUserRequest", updateUserRequest);
                return "editProfile";
            }

            User updatedUser = profileEditService.updateProfile(principal.getName(), updateUserRequest);
            if (updatedUser == null) {
                bindingResult.addError(new FieldError(
                        "updateUserRequest", "email", "email already exist. choose different"));
                model.addAttribute("updateUserRequest", updateUserRequest);
                return "editProfile";
            }

            return "redirect:/profile";
        } catch (Exception e) {
            model.addAttribute("updateUserRequest", updateUserRequest);
            return "editProfile";
        }
    }
}
