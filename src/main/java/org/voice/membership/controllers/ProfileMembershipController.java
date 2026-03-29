package org.voice.membership.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import jakarta.validation.Valid;
import org.voice.membership.dtos.ProfileCancellationViewData;
import org.voice.membership.dtos.MembershipSelectionRequest;
import org.voice.membership.dtos.ProfileUpgradeCheckoutViewData;
import org.voice.membership.dtos.ProfileUpgradeViewData;
import org.voice.membership.services.MembershipCancellationService;
import org.voice.membership.services.ProfileMembershipService;

import java.security.Principal;

@Controller
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfileMembershipController {

    private final ProfileMembershipService profileMembershipService;

    @GetMapping("/upgrade-membership")
    public String upgradeMembershipPage(Model model, Principal principal) {
        ProfileUpgradeViewData viewData = profileMembershipService.buildUpgradePageViewData(principal.getName());
        if (viewData == null) {
            return "redirect:/profile?error=not_eligible_for_upgrade";
        }

        model.addAttribute("user", viewData.user());
        model.addAttribute("currentMembership", viewData.currentMembership());
        model.addAttribute("paidMemberships", viewData.paidMemberships());
        model.addAttribute("lineSeparator", System.lineSeparator());
        model.addAttribute("userName", viewData.userName());
        model.addAttribute("mode", "upgrade");
        return "upgrade-membership";
    }

    @PostMapping("/upgrade-membership/select")
    public String selectUpgradeMembership(@Valid @ModelAttribute MembershipSelectionRequest request,
            BindingResult bindingResult,
            Model model,
            Principal principal) {
        if (bindingResult.hasErrors()) {
            return "redirect:/profile/upgrade-membership?error=invalid_membership";
        }

        ProfileUpgradeCheckoutViewData viewData =
                profileMembershipService.buildUpgradeCheckoutViewData(principal.getName(), request.membershipId());
        if (viewData == null) {
            return "redirect:/profile?error=not_eligible_for_upgrade";
        }

        model.addAttribute("user", viewData.user());
        model.addAttribute("membership", viewData.currentMembership());
        model.addAttribute("upgradeMembership", viewData.upgradeMembership());
        model.addAttribute("membershipName", viewData.membershipName());
        model.addAttribute("membershipPrice", viewData.membershipPrice());
        model.addAttribute("userName", viewData.userName());
        model.addAttribute("paypalClientId", viewData.paypalClientId());
        model.addAttribute("paypalCurrency", viewData.paypalCurrency());
        model.addAttribute("mode", viewData.mode());
        return "checkout";
    }

    @GetMapping("/cancel-membership")
    public String cancelMembershipPage(Model model, Principal principal) {
        ProfileCancellationViewData viewData = profileMembershipService.buildCancellationViewData(principal.getName());
        if (viewData == null) {
            return "redirect:/profile?error=no_membership_to_cancel";
        }

        model.addAttribute("user", viewData.user());
        model.addAttribute("userName", viewData.userName());
        model.addAttribute("currentMembershipName", viewData.currentMembershipName());
        model.addAttribute("isFree", viewData.isFree());
        return "cancel-membership";
    }

    @PostMapping("/cancel-membership")
    public String processCancelMembership(Principal principal) {
        MembershipCancellationService.CancellationResult result =
                profileMembershipService.cancelMembership(principal.getName());

        if (result.isSuccess()) {
            return "redirect:/profile?cancelled=true";
        }

        if ("User not found".equals(result.getMessage())) {
            return "redirect:/login";
        }

        return "redirect:/profile?error=cancellation_failed&message=" + result.getMessage();
    }
}
