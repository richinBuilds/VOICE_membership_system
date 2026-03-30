package org.voice.membership.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.voice.membership.dtos.LandingPageContentRequest;
import org.voice.membership.dtos.MembershipUpdateRequest;
import org.voice.membership.dtos.RenewalEmailContentRequest;
import org.voice.membership.entities.Membership;
import org.voice.membership.entities.User;
import org.voice.membership.services.AdminNotificationService;
import org.voice.membership.services.LandingPageService;
import org.voice.membership.services.MembershipService;

import java.util.List;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminContentController {

    private final AdminNotificationService adminNotificationService;
    private final LandingPageService landingPageService;
    private final MembershipService membershipService;

    @GetMapping("/notifications")
    public String notificationsDashboard(Model model) {
        List<User> newUsers = adminNotificationService.getAllNewUsersFromNotifications();
        model.addAttribute("newUsers", newUsers);
        return "admin-notifications";
    }

    @GetMapping("/landing-page")
    public String landingPageEditor(Model model) {
        model.addAttribute("heroTitle", landingPageService.getHeroTitle());
        model.addAttribute("heroTagline", landingPageService.getHeroTagline());
        model.addAttribute("benefitsTitle", landingPageService.getBenefitsTitle());
        model.addAttribute("reasonsHeading", landingPageService.getReasonsHeading());
        model.addAttribute("reasonsContent", landingPageService.getReasonsContent());
        return "admin-landing-page";
    }

    @PostMapping("/landing-page/save")
    public String saveLandingPage(
            @Valid @ModelAttribute LandingPageContentRequest request,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Please complete all landing page content fields.");
            return "redirect:/admin/landing-page";
        }

        landingPageService.updateContent("hero_title", request.heroTitle());
        landingPageService.updateContent("hero_tagline", request.heroTagline());
        landingPageService.updateContent("benefits_title", request.benefitsTitle());
        landingPageService.updateContent("reasons_heading", request.reasonsHeading());
        landingPageService.updateContent("reasons_content", request.reasonsContent());

        redirectAttributes.addFlashAttribute("success", "Landing page content updated successfully.");
        return "redirect:/admin/landing-page";
    }

    @GetMapping("/memberships")
    public String editMembershipsPage(Model model) {
        List<Membership> memberships = membershipService.getActiveMembershipsOrdered();
        model.addAttribute("memberships", memberships);
        model.addAttribute("lineSeparator", System.lineSeparator());
        return "admin-edit-memberships";
    }

    @PostMapping("/memberships/{id}/save")
    public String saveMembership(
            @PathVariable("id") int id,
            @Valid @ModelAttribute MembershipUpdateRequest request,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Please complete all required membership fields.");
            return "redirect:/admin/memberships";
        }

        try {
            Membership membership = membershipService.updateMembership(
                    id,
                    request.name(),
                    request.description(),
                    request.price(),
                    request.features());
            if (membership == null) {
                redirectAttributes.addFlashAttribute("error", "Membership plan not found.");
                return "redirect:/admin/memberships";
            }
            redirectAttributes.addFlashAttribute("success",
                    "'" + membership.getName() + "' plan updated successfully.");
            return "redirect:/admin/memberships";
        } catch (NumberFormatException e) {
            redirectAttributes.addFlashAttribute("error",
                    "Invalid price format. Please enter a valid number.");
            return "redirect:/admin/memberships";
        }
    }

    @GetMapping("/renewal-email")
    public String renewalEmailEditor(Model model) {
        model.addAttribute("renewalSubject", landingPageService.getRenewalEmailSubject());
        model.addAttribute("renewalBody", landingPageService.getRenewalEmailBody());
        return "admin-renewal-email";
    }

    @PostMapping("/renewal-email/save")
    public String saveRenewalEmail(
            @Valid @ModelAttribute RenewalEmailContentRequest request,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Renewal email subject and body are required.");
            return "redirect:/admin/renewal-email";
        }

        landingPageService.updateContent("renewal_email_subject", request.renewalSubject().trim());
        landingPageService.updateContent("renewal_email_body", request.renewalBody().trim());
        redirectAttributes.addFlashAttribute("success", "Renewal reminder email updated successfully.");
        return "redirect:/admin/renewal-email";
    }
}
